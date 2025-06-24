import NativeBiometrics, {
  type BiometricAvailability,
  type BiometricAuthResult,
  type BiometricPermissionResult,
  type BiometricAuthOptions,
  type BiometricKeyResult,
  type BiometricKeyExistsResult,
  type BiometricSignatureResult,
  BiometricType,
  BiometricErrorCode,
  BiometricOtherwayMode,
} from './NativeBiometrics';

export { BiometricType, BiometricErrorCode, BiometricOtherwayMode };
export type {
  BiometricAvailability,
  BiometricAuthResult,
  BiometricPermissionResult,
  BiometricAuthOptions,
  BiometricKeyResult,
  BiometricKeyExistsResult,
  BiometricSignatureResult,
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
      return await NativeBiometrics.authenticateBiometric(options ?? {}); // fallback for C++ in turbo module
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

  // Private Key Management Methods

  /**
   * Create a new biometric private key
   * Only one key per app is allowed for security
   * @returns Promise<BiometricKeyResult>
   */
  static async createBiometricKey(): Promise<BiometricKeyResult> {
    try {
      return await NativeBiometrics.createBiometricKey();
    } catch (error) {
      return {
        success: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Check if biometric private key exists
   * @returns Promise<BiometricKeyExistsResult>
   */
  static async biometricKeyExists(): Promise<BiometricKeyExistsResult> {
    try {
      return await NativeBiometrics.biometricKeyExists();
    } catch (error) {
      return {
        exists: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Delete the biometric private key
   * @returns Promise<BiometricKeyResult>
   */
  static async deleteBiometricKey(): Promise<BiometricKeyResult> {
    try {
      return await NativeBiometrics.deleteBiometricKey();
    } catch (error) {
      return {
        success: false,
        errorCode: BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Create signature with biometric authentication
   * @param payload - The data to sign (will be hashed internally)  
   * @param options - Authentication options for prompt customization
   * @returns Promise<BiometricSignatureResult>
   */
  static async createSignature(
    payload: string,
    options?: BiometricAuthOptions
  ): Promise<BiometricSignatureResult> {
    try {
      return await NativeBiometrics.createSignature(payload, options);
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
