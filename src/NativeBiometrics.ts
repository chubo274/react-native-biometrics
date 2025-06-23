import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export enum BiometricType {
  FINGERPRINT = 'fingerprint',
  FACE_ID = 'faceId',
  TOUCH_ID = 'touchId',
  IRIS = 'iris',
  NONE = 'none',
}

export enum BiometricErrorCode {
  BIOMETRIC_NOT_AVAILABLE = 'BIOMETRIC_NOT_AVAILABLE',
  BIOMETRIC_NOT_ENROLLED = 'BIOMETRIC_NOT_ENROLLED',
  BIOMETRIC_PERMISSION_DENIED = 'BIOMETRIC_PERMISSION_DENIED',
  BIOMETRIC_LOCKOUT = 'BIOMETRIC_LOCKOUT',
  BIOMETRIC_LOCKOUT_PERMANENT = 'BIOMETRIC_LOCKOUT_PERMANENT',
  BIOMETRIC_AUTH_FAILED = 'BIOMETRIC_AUTH_FAILED',
  BIOMETRIC_USER_CANCEL = 'BIOMETRIC_USER_CANCEL',
  BIOMETRIC_SYSTEM_CANCEL = 'BIOMETRIC_SYSTEM_CANCEL',
  BIOMETRIC_PRESSED_OTHER_WAY = 'BIOMETRIC_PRESSED_OTHER_WAY',
  BIOMETRIC_UNKNOWN_ERROR = 'BIOMETRIC_UNKNOWN_ERROR',
}

export interface BiometricAvailability {
  isAvailable: boolean;
  allowAccess: boolean;
  biometricType: BiometricType;
  isLockout: boolean;
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}

export interface BiometricAuthResult {
  success: boolean;
  pressedOtherway?: boolean;
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}

export interface BiometricPermissionResult {
  success: boolean;
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}

export interface BiometricAuthOptions {
  otherwayWithPIN?: boolean;
}

export interface Spec extends TurboModule {
  // Check if biometric authentication is available on the device
  checkBiometricAvailability(): Promise<BiometricAvailability>;

  // Request permission to use biometric authentication
  requestBiometricPermission(): Promise<BiometricPermissionResult>;

  // Authenticate using biometric
  authenticateBiometric(
    options?: BiometricAuthOptions
  ): Promise<BiometricAuthResult>;

  // Authenticate using device PIN/passcode only
  authenticatePIN(): Promise<BiometricAuthResult>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Biometrics');
