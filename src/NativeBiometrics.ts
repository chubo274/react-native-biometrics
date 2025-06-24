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

export enum BiometricOtherwayMode {
  HIDE = 'hide', // hide the other way option only on IOS
  CALLBACK = 'callback', // callback return pressedOtherway = true to easy handle the case when user pressed other way
  PIN = 'PIN', // use device PIN/passcode to authenticate
}

export interface BiometricAvailability {
  isAvailable: boolean;
  allowAccess: boolean;
  biometricType: BiometricType;
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
  titlePrompt?: string;
  otherwayWith?: BiometricOtherwayMode;
  otherwayText?: string; // Only effective when otherwayWith = 'callback'
}

export interface BiometricKeyResult {
  success: boolean;
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}

export interface BiometricKeyExistsResult {
  exists: boolean;
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}

export interface BiometricSignatureResult {
  success: boolean;
  signature?: string; // Base64 encoded signature
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
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

  // Private Key Management Methods
  // Create a new biometric private key (only one per app)
  createBiometricKey(): Promise<BiometricKeyResult>;

  // Check if biometric private key exists
  biometricKeyExists(): Promise<BiometricKeyExistsResult>;

  // Delete the biometric private key
  deleteBiometricKey(): Promise<BiometricKeyResult>;

  // Create signature with biometric authentication
  createSignature(
    payload: string,
    options?: BiometricAuthOptions
  ): Promise<BiometricSignatureResult>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Biometrics');
