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
   * @param options - Authentication options
   * @returns Promise<BiometricAuthResult>
   */
  static async authenticateBiometric(
    options?: BiometricAuthOptions
  ): Promise<BiometricAuthResult> {
    try {
      return await NativeBiometrics.authenticateBiometric(options);
    } catch (error) {
      return {
        success: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Authenticate using device PIN/passcode only
   * @returns Promise<BiometricAuthResult>
   */
  static async authenticatePIN(): Promise<BiometricAuthResult> {
    try {
      return await NativeBiometrics.authenticatePIN();
    } catch (error) {
      return {
        success: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * @deprecated Use authenticateBiometric instead
   */
  static async authenticate(
    options?: BiometricAuthOptions
  ): Promise<BiometricAuthResult> {
    return this.authenticateBiometric(options);
  }
}

export default ReactNativeBiometrics;
