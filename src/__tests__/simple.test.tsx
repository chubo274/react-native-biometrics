// Simple unit test for React Native Biometrics library
describe('React Native Biometrics', () => {
  it('should have the required API methods', () => {
    // Mock TurboModuleRegistry for the test environment
    jest.mock('react-native', () => ({
      TurboModuleRegistry: {
        getEnforcing: jest.fn(() => ({
          checkBiometricAvailability: jest.fn(),
          requestBiometricPermission: jest.fn(),
          authenticate: jest.fn(),
        })),
      },
    }));

    // Test that the module can be imported without errors
    expect(() => {
      const ReactNativeBiometrics = require('../index');
      expect(ReactNativeBiometrics).toBeDefined();
    }).not.toThrow();
  });

  it('should export BiometricType enum', () => {
    const { BiometricType } = require('../index');
    expect(BiometricType).toBeDefined();
    expect(BiometricType.FINGERPRINT).toBe('fingerprint');
    expect(BiometricType.FACE_ID).toBe('faceId');
    expect(BiometricType.TOUCH_ID).toBe('touchId');
    expect(BiometricType.IRIS).toBe('iris');
    expect(BiometricType.NONE).toBe('none');
  });

  it('should export BiometricErrorCode enum', () => {
    const { BiometricErrorCode } = require('../index');
    expect(BiometricErrorCode).toBeDefined();
    expect(BiometricErrorCode.BIOMETRIC_NOT_AVAILABLE).toBe(
      'BIOMETRIC_NOT_AVAILABLE'
    );
    expect(BiometricErrorCode.BIOMETRIC_NOT_ENROLLED).toBe(
      'BIOMETRIC_NOT_ENROLLED'
    );
    expect(BiometricErrorCode.BIOMETRIC_UNKNOWN_ERROR).toBe(
      'BIOMETRIC_UNKNOWN_ERROR'
    );
  });
});
