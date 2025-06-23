package com.biometrics

import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = BiometricsModule.NAME)
class BiometricsModule(reactContext: ReactApplicationContext) :
  NativeBiometricsSpec(reactContext) {

  private var isAuthenticating = false

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

  // Helper method to get primary biometric type and preferred authenticator
  private fun getBiometricTypeAndAuthenticator(): Pair<String, Int> {
    val context = reactApplicationContext
    val packageManager = context.packageManager
    
    // Check hardware capabilities
    val hasFingerprint = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
    } else {
        false
    }
    val hasIris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)
    } else {
        false
    }
    
    // Check if any biometric hardware is available
    val hasHardware = hasFingerprint || hasFace || hasIris
    if (!hasHardware) {
      return Pair("none", BiometricManager.Authenticators.BIOMETRIC_WEAK)
    }
    
    // Priority: Fingerprint > Face > Iris (using BIOMETRIC_WEAK to avoid lockout)
    return when {
        hasFingerprint -> Pair("fingerprint", BiometricManager.Authenticators.BIOMETRIC_WEAK)
        hasFace -> Pair("faceId", BiometricManager.Authenticators.BIOMETRIC_WEAK)
        hasIris -> Pair("iris", BiometricManager.Authenticators.BIOMETRIC_WEAK)
        else -> Pair("none", BiometricManager.Authenticators.BIOMETRIC_WEAK)
    }
  }

  // Helper method to get primary biometric type (prioritize WEAK biometrics to avoid lockout)
  private fun getPrimaryBiometricType(): String {
    return getBiometricTypeAndAuthenticator().first
  }

  override fun checkBiometricAvailability(promise: Promise) {
    try {
      val biometricManager = BiometricManager.from(reactApplicationContext)
      val result = Arguments.createMap()
      
      // Get biometric type and preferred authenticator
      val (biometricType, preferredAuthenticator) = getBiometricTypeAndAuthenticator()
      val authResult = biometricManager.canAuthenticate(preferredAuthenticator)
      
      var isAvailable = false
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
          errorMessage = "Security update required for biometrics"
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
      
      // Android doesn't need explicit permission for biometrics (only manifest)
      // allowAccess is true if biometrics are available
      val allowAccess = (authResult == BiometricManager.BIOMETRIC_SUCCESS)
      
      result.putBoolean("isAvailable", isAvailable)
      result.putBoolean("allowAccess", allowAccess)
      result.putString("biometricType", biometricType)
      
      // Check for lockout status
      val isLockout = (authResult == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) // This can indicate temporary lockout
      result.putBoolean("isLockout", isLockout)
      
      // Only add error fields if there's an error
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
      result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
      result.putString("errorMessage", e.message ?: "Unknown error")
      promise.resolve(result)
    }
  }

  override fun requestBiometricPermission(promise: Promise) {
    try {
      val result = Arguments.createMap()
      val biometricManager = BiometricManager.from(reactApplicationContext)
      
      // Get biometric type and preferred authenticator for accurate checking
      val (biometricType, preferredAuthenticator) = getBiometricTypeAndAuthenticator()
      
      // Check with the preferred authenticator for this biometric type
      when (biometricManager.canAuthenticate(preferredAuthenticator)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
          // Device has biometric and ready to use
          result.putBoolean("success", true)
        }
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_NOT_AVAILABLE")
          result.putString("errorMessage", "No biometric hardware available")
        }
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_NOT_AVAILABLE")
          result.putString("errorMessage", "Biometric hardware unavailable")
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_NOT_ENROLLED")
          result.putString("errorMessage", "No biometric credentials enrolled")
        }
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_NOT_AVAILABLE")
          result.putString("errorMessage", "Security update required for biometrics")
        }
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_NOT_AVAILABLE")
          result.putString("errorMessage", "Biometric authentication not supported")
        }
        else -> {
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_NOT_AVAILABLE")
          result.putString("errorMessage", "Biometric authentication not available")
        }
      }
      
      promise.resolve(result)
    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
      result.putString("errorMessage", e.message ?: "Unknown error")
      promise.resolve(result)
    }
  }

  override fun authenticateBiometric(options: ReadableMap?, promise: Promise) {
    if (isAuthenticating) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("errorCode", "BIOMETRIC_AUTHENTICATION_IN_PROGRESS")
      result.putString("errorMessage", "Biometric authentication is already in progress")
      promise.resolve(result)
      return
    }

    isAuthenticating = true

    UiThreadUtil.runOnUiThread {
      try {
        val currentActivity = currentActivity
        if (currentActivity == null || currentActivity !is FragmentActivity) {
          val result = Arguments.createMap()
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
          result.putString("errorMessage", "Current activity is not a FragmentActivity")
          promise.resolve(result)
          isAuthenticating = false // Reset flag on error
          return@runOnUiThread
        }

        // Parse options
        var otherwayWithPIN = false
        
        if (options != null) {
          if (options.hasKey("otherwayWithPIN") && !options.isNull("otherwayWithPIN")) {
            otherwayWithPIN = options.getBoolean("otherwayWithPIN")
          }
        }

        // Get biometric type and preferred authenticator
        val (biometricType, preferredAuthenticator) = getBiometricTypeAndAuthenticator()

        val executor = ContextCompat.getMainExecutor(reactApplicationContext)
        val biometricPrompt = BiometricPrompt(currentActivity, executor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              super.onAuthenticationError(errorCode, errString)
              isAuthenticating = false // Reset flag on error
              
              val result = Arguments.createMap()
              result.putBoolean("success", false)
              
              var customErrorCode = convertBiometricErrorToErrorCode(errorCode)
              var pressedOtherway = false
              
              // Handle "other way" button press (only when not using otherwayWithPIN)
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
              isAuthenticating = false // Reset flag on success
              
              val result = Arguments.createMap()
              result.putBoolean("success", true)
              promise.resolve(result)
            }

            override fun onAuthenticationFailed() {
              super.onAuthenticationFailed()
              // onAuthenticationFailed is called for each failed attempt (finger not recognized)
              // but user still has more chances to try - DO NOT resolve Promise here
              // Only onAuthenticationError will be called when max attempts exceeded
            }
          })

        // Build prompt info based on options
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
          .setTitle("Biometric Authentication")
          .setSubtitle("Please authenticate to continue")
        
        if (otherwayWithPIN) {
          // Allow device credential fallback with preferred biometric authenticator
          promptInfoBuilder.setAllowedAuthenticators(
            preferredAuthenticator or BiometricManager.Authenticators.DEVICE_CREDENTIAL
          )
        } else {
          // Only biometric, with custom negative button text
          promptInfoBuilder
            .setAllowedAuthenticators(preferredAuthenticator)
            .setNegativeButtonText("Another Way")
        }

        val promptInfo = promptInfoBuilder.build()
        biometricPrompt.authenticate(promptInfo)
        
      } catch (e: Exception) {
        val result = Arguments.createMap()
        result.putBoolean("success", false)
        result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
        result.putString("errorMessage", e.message ?: "Unknown error")
        promise.resolve(result)
        isAuthenticating = false // Reset flag on exception
      }
    }
  }

  override fun authenticatePIN(promise: Promise) {
    if (isAuthenticating) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("errorCode", "BIOMETRIC_AUTHENTICATION_IN_PROGRESS")
      result.putString("errorMessage", "PIN authentication is already in progress")
      promise.resolve(result)
      return
    }

    isAuthenticating = true

    UiThreadUtil.runOnUiThread {
      try {
        val currentActivity = currentActivity
        if (currentActivity == null || currentActivity !is FragmentActivity) {
          val result = Arguments.createMap()
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
          result.putString("errorMessage", "Current activity is not a FragmentActivity")
          promise.resolve(result)
          isAuthenticating = false // Reset flag on error
          return@runOnUiThread
        }

        val executor = ContextCompat.getMainExecutor(reactApplicationContext)
        val biometricPrompt = BiometricPrompt(currentActivity, executor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              super.onAuthenticationError(errorCode, errString)
              isAuthenticating = false // Reset flag on error
              
              val result = Arguments.createMap()
              result.putBoolean("success", false)
              
              val customErrorCode = convertBiometricErrorToErrorCode(errorCode)
              result.putString("errorCode", customErrorCode)
              result.putString("errorMessage", errString.toString())
              
              promise.resolve(result)
            }

            override fun onAuthenticationSucceeded(authResult: BiometricPrompt.AuthenticationResult) {
              super.onAuthenticationSucceeded(authResult)
              isAuthenticating = false // Reset flag on success
              
              val result = Arguments.createMap()
              result.putBoolean("success", true)
              promise.resolve(result)
            }

            override fun onAuthenticationFailed() {
              super.onAuthenticationFailed()
              // onAuthenticationFailed is called for each failed attempt
              // but user still has more chances to try - DO NOT resolve Promise here
              // Only onAuthenticationError will be called when max attempts exceeded
            }
          })

        // Build prompt info for PIN/passcode only
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
          .setTitle("Device Authentication")
          .setSubtitle("Please enter your device PIN, pattern, or password")
          .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        val promptInfo = promptInfoBuilder.build()
        biometricPrompt.authenticate(promptInfo)
        
      } catch (e: Exception) {
        val result = Arguments.createMap()
        result.putBoolean("success", false)
        result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
        result.putString("errorMessage", e.message ?: "Unknown error")
        promise.resolve(result)
        isAuthenticating = false // Reset flag on exception
      }
    }
  }

  companion object {
    const val NAME = "Biometrics"
  }
}
