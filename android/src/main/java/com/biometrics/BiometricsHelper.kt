package com.biometrics

import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.facebook.react.bridge.ReactApplicationContext

object BiometricsHelper {

  // Helper method to convert BiometricPrompt error codes to our custom error codes
  fun convertBiometricErrorToErrorCode(errorCode: Int): String {
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
  fun getBiometricTypeAndAuthenticator(context: ReactApplicationContext): Pair<String, Int> {
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
}
