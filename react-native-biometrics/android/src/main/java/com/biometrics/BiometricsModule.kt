package com.biometrics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = BiometricsModule.NAME)
class BiometricsModule(reactContext: ReactApplicationContext) :
  NativeBiometricsSpec(reactContext) {

  override fun getName(): String {
    return NAME
  }

  // Helper method to convert BiometricPrompt error codes to our custom error codes
  private fun convertBiometricErrorToErrorCode(errorCode: Int): String {
    return when (errorCode) {
      BiometricPrompt.ERROR_NO_BIOMETRICS -> "BIOMETRIC_NOT_ENROLLED"
      BiometricPrompt.ERROR_HW_NOT_PRESENT -> "BIOMETRIC_NOT_AVAILABLE"
      BiometricPrompt.ERROR_HW_UNAVAILABLE -> "BIOMETRIC_NOT_AVAILABLE"
      BiometricPrompt.ERROR_LOCKOUT -> "BIOMETRIC_LOCKOUT"
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "BIOMETRIC_LOCKOUT_PERMANENT"
      BiometricPrompt.ERROR_USER_CANCELED -> "BIOMETRIC_USER_CANCEL"
      BiometricPrompt.ERROR_CANCELED -> "BIOMETRIC_SYSTEM_CANCEL"
      BiometricPrompt.ERROR_NO_SPACE -> "BIOMETRIC_NOT_AVAILABLE"
      BiometricPrompt.ERROR_TIMEOUT -> "BIOMETRIC_AUTH_FAILED"
      BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> "BIOMETRIC_AUTH_FAILED"
      BiometricPrompt.ERROR_VENDOR -> "BIOMETRIC_AUTH_FAILED"
      BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "BIOMETRIC_PRESSED_OTHER_WAY"
      else -> "BIOMETRIC_UNKNOWN_ERROR"
    }
  }

  // Helper method to get primary biometric type
  private fun getPrimaryBiometricType(): String {
    val context = reactApplicationContext
    val packageManager = context.packageManager
    
    // Check for face recognition first (newer technology)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)) {
        return "faceId"
      }
    }
    
    // Check for fingerprint
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
        return "fingerprint"
      }
    }
    
    // Check for iris (very rare)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)) {
        return "iris"
      }
    }
    
    return "none"
  }

  private fun hasUseBiometricPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ContextCompat.checkSelfPermission(
        reactApplicationContext,
        Manifest.permission.USE_BIOMETRIC
      ) == PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(
        reactApplicationContext,
        Manifest.permission.USE_FINGERPRINT
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  override fun checkBiometricAvailability(promise: Promise) {
    try {
      val biometricManager = BiometricManager.from(reactApplicationContext)
      val result = Arguments.createMap()
      
      // Use BIOMETRIC_STRONG for higher security level as requested
      val authResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
      val biometricType = getPrimaryBiometricType()
      val allowAccess = hasUseBiometricPermission()
      
      var isAvailable = false
      var isLockout = false
      var errorCode: String? = null
      var errorMessage: String? = null
      
      when (authResult) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
          isAvailable = true
        }
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
          errorCode = "BIOMETRIC_NOT_AVAILABLE"
          errorMessage = "No biometric hardware available"
        }
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
          errorCode = "BIOMETRIC_NOT_AVAILABLE"
          errorMessage = "Biometric hardware unavailable"
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
          errorCode = "BIOMETRIC_NOT_ENROLLED"
          errorMessage = "No biometric credentials enrolled"
        }
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
          errorCode = "BIOMETRIC_NOT_AVAILABLE"
          errorMessage = "Security update required"
        }
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
          errorCode = "BIOMETRIC_NOT_AVAILABLE"
          errorMessage = "Biometric authentication not supported"
        }
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
          errorCode = "BIOMETRIC_UNKNOWN_ERROR"
          errorMessage = "Biometric status unknown"
        }
        else -> {
          errorCode = "BIOMETRIC_UNKNOWN_ERROR"
          errorMessage = "Unknown biometric error"
        }
      }
      
      result.putBoolean("isAvailable", isAvailable)
      result.putBoolean("allowAccess", allowAccess)
      result.putString("biometricType", biometricType)
      result.putBoolean("isLockout", isLockout)
      
      if (errorCode != null) {
        result.putString("errorCode", errorCode)
      }
      if (errorMessage != null) {
        result.putString("errorMessage", errorMessage)
      }
      
      promise.resolve(result)
    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putBoolean("isAvailable", false)
      result.putBoolean("allowAccess", false)
      result.putString("biometricType", "none")
      result.putBoolean("isLockout", false)
      result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
      result.putString("errorMessage", e.message ?: "Unknown error")
      promise.resolve(result)
    }
  }

  override fun requestBiometricPermission(promise: Promise) {
    try {
      val result = Arguments.createMap()
      
      // Check if permission is already granted
      if (hasUseBiometricPermission()) {
        result.putBoolean("success", true)
        promise.resolve(result)
        return
      }
      
      // Check if we can request permission
      val currentActivity = currentActivity
      if (currentActivity == null) {
        result.putBoolean("success", false)
        result.putString("errorCode", "BIOMETRIC_PERMISSION_DENIED")
        result.putString("errorMessage", "No activity available to request permission")
        promise.resolve(result)
        return
      }
      
      // Request permission
      UiThreadUtil.runOnUiThread {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          val permissions = arrayOf(
            Manifest.permission.USE_BIOMETRIC,
            Manifest.permission.USE_FINGERPRINT
          )
          
          ActivityCompat.requestPermissions(currentActivity, permissions, 1)
          
          // Since we can't get the result synchronously, we'll check again
          // In a real implementation, you'd need to handle onRequestPermissionsResult
          if (hasUseBiometricPermission()) {
            result.putBoolean("success", true)
          } else {
            result.putBoolean("success", false)
            result.putString("errorCode", "BIOMETRIC_PERMISSION_DENIED")
            result.putString("errorMessage", "Permission denied")
          }
        } else {
          result.putBoolean("success", true)
        }
        
        promise.resolve(result)
      }
    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
      result.putString("errorMessage", e.message ?: "Unknown error")
      promise.resolve(result)
    }
  }

  override fun authenticate(reason: String, options: ReadableMap?, promise: Promise) {
    UiThreadUtil.runOnUiThread {
      try {
        val currentActivity = currentActivity
        if (currentActivity == null || currentActivity !is FragmentActivity) {
          val result = Arguments.createMap()
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
          result.putString("errorMessage", "Current activity is not a FragmentActivity")
          promise.resolve(result)
          return@runOnUiThread
        }

        // Parse options
        var otherwayWithPIN = false
        var textOtherway: String? = null
        
        if (options != null) {
          otherwayWithPIN = options.getBoolean("otherwayWithPIN")
          if (options.hasKey("textOtherway") && !options.isNull("textOtherway")) {
            textOtherway = options.getString("textOtherway")
          }
        }

        val executor = ContextCompat.getMainExecutor(reactApplicationContext)
        val biometricPrompt = BiometricPrompt(currentActivity, executor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              super.onAuthenticationError(errorCode, errString)
              val result = Arguments.createMap()
              result.putBoolean("success", false)
              
              var customErrorCode = convertBiometricErrorToErrorCode(errorCode)
              var pressedOtherway = false
              
              // Handle "other way" button press
              if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                if (!otherwayWithPIN) {
                  pressedOtherway = true
                  customErrorCode = "BIOMETRIC_PRESSED_OTHER_WAY"
                }
              }
              
              result.putString("errorCode", customErrorCode)
              result.putString("errorMessage", errString.toString())
              
              if (pressedOtherway) {
                result.putBoolean("pressedOtherway", true)
              }
              
              promise.resolve(result)
            }

            override fun onAuthenticationSucceeded(authResult: BiometricPrompt.AuthenticationResult) {
              super.onAuthenticationSucceeded(authResult)
              val result = Arguments.createMap()
              result.putBoolean("success", true)
              promise.resolve(result)
            }

            override fun onAuthenticationFailed() {
              super.onAuthenticationFailed()
              val result = Arguments.createMap()
              result.putBoolean("success", false)
              result.putString("errorCode", "BIOMETRIC_AUTH_FAILED")
              result.putString("errorMessage", "Authentication failed")
              promise.resolve(result)
            }
          })

        // Build prompt info based on options
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
          .setTitle("Biometric Authentication")
          .setSubtitle(reason)
        
        if (otherwayWithPIN) {
          // Allow device credential fallback
          promptInfoBuilder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
          )
        } else {
          // Only biometric, with custom negative button text
          promptInfoBuilder
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(textOtherway ?: "Cancel")
        }

        val promptInfo = promptInfoBuilder.build()
        biometricPrompt.authenticate(promptInfo)
        
      } catch (e: Exception) {
        val result = Arguments.createMap()
        result.putBoolean("success", false)
        result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
        result.putString("errorMessage", e.message ?: "Unknown error")
        promise.resolve(result)
      }
    }
  }

  companion object {
    const val NAME = "Biometrics"
  }
}
