import NativeBiometrics, {
  type BiometricAvailability,
  type BiometricAuthResult,
  type BiometricPermissionResult,
  type BiometricAuthOptions,
  BiometricType,
  BiometricErrorCode,
} from './NativeBiometrics';

export { BiometricType, BiometricErrorCode };
export type {
  BiometricAvailability,
  BiometricAuthResult,
  BiometricPermissionResult,
  BiometricAuthOptions,
};

/**
 * React Native Biometrics Library
 * Provides biometric authentication functionality for iOS and Android
 */
export class ReactNativeBiometrics {
  /**
   * Check if biometric authentication is available on the device
   * @returns Promise<BiometricAvailability>
   */
  static async checkBiometricAvailability(): Promise<BiometricAvailability> {
    try {
      return await NativeBiometrics.checkBiometricAvailability();
    } catch (error) {
      return {
        isAvailable: false,
        allowAccess: false,
        biometricType: BiometricType.NONE,
        isLockout: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Request permission to use biometric authentication
   * @returns Promise<BiometricPermissionResult>
   */
  static async requestBiometricPermission(): Promise<BiometricPermissionResult> {
    try {
      return await NativeBiometrics.requestBiometricPermission();
    } catch (error) {
      return {
        success: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Authenticate using biometric
   * @param reason - The reason for authentication to show to the user
   * @param options - Authentication options
   * @returns Promise<BiometricAuthResult>
   */
  static async authenticate(
    reason: string,
    options?: BiometricAuthOptions
  ): Promise<BiometricAuthResult> {
    try {
      return await NativeBiometrics.authenticate(reason, options);
    } catch (error) {
      return {
        success: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }
}

export default ReactNativeBiometrics;
