package com.biometrics

import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import android.util.Base64

@ReactModule(name = BiometricsModule.NAME)
class BiometricsModule(reactContext: ReactApplicationContext) :
  NativeBiometricsSpec(reactContext) {

  private var isAuthenticating = false

  override fun getName(): String {
    return NAME
  }

  override fun checkBiometricAvailability(promise: Promise) {
    try {
      val biometricManager = BiometricManager.from(reactApplicationContext)
      val result = Arguments.createMap()
      
      // Get biometric type and preferred authenticator
      val (biometricType, preferredAuthenticator) = BiometricsHelper.getBiometricTypeAndAuthenticator(reactApplicationContext)
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
      val (biometricType, preferredAuthenticator) = BiometricsHelper.getBiometricTypeAndAuthenticator(reactApplicationContext)
      
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
        val currentActivity = reactApplicationContext.currentActivity
        if (currentActivity == null || currentActivity !is FragmentActivity) {
          val result = Arguments.createMap()
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_UNKNOWN_ERROR")
          result.putString("errorMessage", "Current activity is not a FragmentActivity")
          promise.resolve(result)
          isAuthenticating = false // Reset flag on error
          return@runOnUiThread
        }

        // Parse options with new structure
        val titlePrompt = options?.getString("titlePrompt") ?: "Biometric Authentication"
        val otherwayWith = options?.getString("otherwayWith") ?: "PIN"  // Default to PIN mode on Android
        val otherwayText = options?.getString("otherwayText") ?: "Another way"  // Fallback text for Android
        
        // Determine behavior based on otherwayWith
        val showCallback = otherwayWith == "callback"
        val showPIN = otherwayWith == "PIN"

        // Get biometric type and preferred authenticator
        val (biometricType, preferredAuthenticator) = BiometricsHelper.getBiometricTypeAndAuthenticator(reactApplicationContext)

        val executor = ContextCompat.getMainExecutor(reactApplicationContext)
        val biometricPrompt = BiometricPrompt(currentActivity, executor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              super.onAuthenticationError(errorCode, errString)
              isAuthenticating = false // Reset flag on error
              
              val result = Arguments.createMap()
              result.putBoolean("success", false)
              
              var customErrorCode = BiometricsHelper.convertBiometricErrorToErrorCode(errorCode)
              var pressedOtherway = false
              
              // Handle "other way" button press
              if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                if (showCallback) {
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
          .setTitle(titlePrompt)
        
        if (showPIN) {
          // Allow device credential fallback with preferred biometric authenticator
          promptInfoBuilder.setAllowedAuthenticators(
            preferredAuthenticator or BiometricManager.Authenticators.DEVICE_CREDENTIAL
          )
        } else if (showCallback) {
          // Show negative button with custom text for callback
          promptInfoBuilder
            .setAllowedAuthenticators(preferredAuthenticator)
            .setNegativeButtonText(otherwayText)
        } else {
          // Default fallback for other modes (like HIDE which is iOS-only)
          promptInfoBuilder
            .setAllowedAuthenticators(preferredAuthenticator)
            .setNegativeButtonText(otherwayText)
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
        val currentActivity = reactApplicationContext.currentActivity
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
              
              val customErrorCode = BiometricsHelper.convertBiometricErrorToErrorCode(errorCode)
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

  // MARK: - Private Key

  private fun getBiometricKeyAlias(): String {
    // Generate unique key alias based on package name to ensure one key per app
    val packageName = reactApplicationContext.packageName
    return "${packageName}.biometric.privatekey"
  }

  override fun createBiometricKey(promise: Promise) {
    try {
      val keyAlias = getBiometricKeyAlias()
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)

      // Check if key already exists
      if (keyStore.containsAlias(keyAlias)) {
        val result = Arguments.createMap()
        result.putBoolean("success", false)
        result.putString("errorCode", "BIOMETRIC_KEY_EXISTS")
        result.putString("errorMessage", "Biometric key already exists. Delete existing key before creating a new one.")
        promise.resolve(result)
        return
      }

      // Create key generation parameters with biometric authentication requirement
      val keyGenParameterSpec = KeyGenParameterSpec.Builder(
        keyAlias,
        KeyProperties.PURPOSE_SIGN
      ).apply {
        setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
        setDigests(KeyProperties.DIGEST_SHA256)
        setUserAuthenticationRequired(true)
        
        // Require biometric authentication for key usage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          // Allow both biometric and device credential authentication
          setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
          @Suppress("DEPRECATION")
          setUserAuthenticationValidityDurationSeconds(-1)
        }
        
        setInvalidatedByBiometricEnrollment(true)
      }.build()

      // Generate the key pair
      val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
      keyPairGenerator.initialize(keyGenParameterSpec)
      keyPairGenerator.generateKeyPair()

      val result = Arguments.createMap()
      result.putBoolean("success", true)
      promise.resolve(result)

    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("errorCode", "BIOMETRIC_KEY_CREATION_FAILED")
      result.putString("errorMessage", e.message ?: "Failed to create biometric key")
      promise.resolve(result)
    }
  }

  override fun biometricKeyExists(promise: Promise) {
    try {
      val keyAlias = getBiometricKeyAlias()
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)

      val result = Arguments.createMap()
      result.putBoolean("exists", keyStore.containsAlias(keyAlias))
      promise.resolve(result)

    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putBoolean("exists", false)
      result.putString("errorCode", "BIOMETRIC_KEY_CHECK_FAILED")
      result.putString("errorMessage", e.message ?: "Failed to check if biometric key exists")
      promise.resolve(result)
    }
  }

  override fun deleteBiometricKey(promise: Promise) {
    try {
      val keyAlias = getBiometricKeyAlias()
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)

      if (keyStore.containsAlias(keyAlias)) {
        keyStore.deleteEntry(keyAlias)
      }

      val result = Arguments.createMap()
      result.putBoolean("success", true)
      promise.resolve(result)

    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("errorCode", "BIOMETRIC_KEY_DELETION_FAILED")
      result.putString("errorMessage", e.message ?: "Failed to delete biometric key")
      promise.resolve(result)
    }
  }

  override fun createSignature(payload: String, options: ReadableMap?, promise: Promise) {
    if (isAuthenticating) {
      val result = Arguments.createMap()
      result.putBoolean("success", false)
      result.putString("errorCode", "BIOMETRIC_AUTH_IN_PROGRESS")
      result.putString("errorMessage", "Authentication already in progress")
      promise.resolve(result)
      return
    }

    UiThreadUtil.runOnUiThread {
      try {
        val keyAlias = getBiometricKeyAlias()
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // Check if key not exists
        if (!keyStore.containsAlias(keyAlias)) {
          val result = Arguments.createMap()
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_KEY_NOT_FOUND")
          result.putString("errorMessage", "Biometric key not found. Create a key first.")
          promise.resolve(result)
          return@runOnUiThread
        }

        // Parse options for custom prompt
        val titlePrompt = options?.getString("titlePrompt") ?: "Biometric Authentication"
        val otherwayWith = options?.getString("otherwayWith") ?: "PIN"  // Default to PIN mode on Android
        val otherwayText = options?.getString("otherwayText") ?: "Another way"  // Fallback text for Android

        // Get the private key and create signature object
        val privateKey = keyStore.getKey(keyAlias, null) as java.security.PrivateKey
        val signature = Signature.getInstance("SHA256withECDSA")
        // Important: Do NOT call signature.initSign() here - let BiometricPrompt handle it

        // Create biometric prompt for signature
        val activity = reactApplicationContext.currentActivity as? FragmentActivity
        if (activity == null) {
          val result = Arguments.createMap()
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_NO_ACTIVITY")
          result.putString("errorMessage", "No current activity available")
          promise.resolve(result)
          return@runOnUiThread
        }

        isAuthenticating = true

        val executor = ContextCompat.getMainExecutor(reactApplicationContext)
        val biometricPrompt = BiometricPrompt(activity, executor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              super.onAuthenticationError(errorCode, errString)
              isAuthenticating = false
              
              val result = Arguments.createMap()
              result.putBoolean("success", false)
              result.putString("errorCode", BiometricsHelper.convertBiometricErrorToErrorCode(errorCode))
              result.putString("errorMessage", errString.toString())
              
              // Handle otherwayWith callback option
              if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && otherwayWith == "callback") {
                result.putBoolean("pressedOtherway", true)
              }
              
              promise.resolve(result)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
              super.onAuthenticationSucceeded(result)
              isAuthenticating = false
              
              try {
                // Get the authenticated signature from crypto object
                val cryptoObject = result.cryptoObject
                val authenticatedSignature = cryptoObject?.signature

                if (authenticatedSignature != null) {
                  // Now we can safely use the authenticated signature
                  authenticatedSignature.update(payload.toByteArray())
                  val signatureBytes = authenticatedSignature.sign()
                  val base64Signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

                  val resultMap = Arguments.createMap()
                  resultMap.putBoolean("success", true)
                  resultMap.putString("signature", base64Signature)
                  promise.resolve(resultMap)
                } else {
                  val resultMap = Arguments.createMap()
                  resultMap.putBoolean("success", false)
                  resultMap.putString("errorCode", "BIOMETRIC_SIGNATURE_FAILED")
                  resultMap.putString("errorMessage", "Failed to get authenticated signature - crypto object is null")
                  promise.resolve(resultMap)
                }
              } catch (e: Exception) {
                val resultMap = Arguments.createMap()
                resultMap.putBoolean("success", false)
                resultMap.putString("errorCode", "BIOMETRIC_SIGNATURE_FAILED")
                resultMap.putString("errorMessage", e.message ?: "Failed to create signature")
                promise.resolve(resultMap)
              }
            }

            override fun onAuthenticationFailed() {
              super.onAuthenticationFailed()
              // Don't resolve promise here, wait for onAuthenticationError
            }
          })

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
          .setTitle(titlePrompt)
          .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        when (otherwayWith) {
          "callback" -> {
            // Show negative button, when pressed will call onAuthenticationError with ERROR_NEGATIVE_BUTTON
            promptBuilder.setNegativeButtonText(otherwayText)
          }
          "PIN" -> {
            // Allow both biometric and device credential (PIN/password/pattern)
            // This works because we created the key to accept both auth methods
            promptBuilder.setAllowedAuthenticators(
              BiometricManager.Authenticators.BIOMETRIC_STRONG or 
              BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
          }
          else -> {
            // Default to PIN mode for other modes (like HIDE which is iOS-only)
            promptBuilder.setAllowedAuthenticators(
              BiometricManager.Authenticators.BIOMETRIC_STRONG or 
              BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
          }
        }

        val promptInfo = promptBuilder.build()
        
        // Create crypto object with the signature - this is crucial for biometric key authentication
        val cryptoObject = BiometricPrompt.CryptoObject(signature)
        
        // Initialize signature with private key through crypto object
        try {
          signature.initSign(privateKey)
        } catch (e: Exception) {
          isAuthenticating = false
          val result = Arguments.createMap()
          result.putBoolean("success", false)
          result.putString("errorCode", "BIOMETRIC_SIGNATURE_FAILED")
          result.putString("errorMessage", "Failed to initialize signature: ${e.message}")
          promise.resolve(result)
          return@runOnUiThread
        }
        
        biometricPrompt.authenticate(promptInfo, cryptoObject)

      } catch (e: Exception) {
        isAuthenticating = false
        val result = Arguments.createMap()
        result.putBoolean("success", false)
        result.putString("errorCode", "BIOMETRIC_SIGNATURE_FAILED")
        result.putString("errorMessage", e.message ?: "Failed to create signature")
        promise.resolve(result)
      }
    }
  }

  companion object {
    const val NAME = "Biometrics"
  }
}
