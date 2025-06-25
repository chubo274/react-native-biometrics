#import "BiometricsHelper.h"

@implementation BiometricsHelper

+ (NSString *)convertLAErrorToErrorCode:(NSInteger)errorCode {
    LAError laErrorCode = (LAError)errorCode;
    
    switch (laErrorCode) {
        case LAErrorBiometryNotAvailable:
            return @"BIOMETRIC_NOT_AVAILABLE";
        case LAErrorBiometryNotEnrolled:
            return @"BIOMETRIC_NOT_ENROLLED";
        case LAErrorBiometryLockout:
            return @"BIOMETRIC_LOCKOUT";
        case LAErrorUserCancel:
            return @"BIOMETRIC_USER_CANCEL";
        case LAErrorSystemCancel:
            return @"BIOMETRIC_SYSTEM_CANCEL";
        case LAErrorAuthenticationFailed:
            return @"BIOMETRIC_AUTH_FAILED";
        case LAErrorUserFallback:
            return @"BIOMETRIC_USER_FALLBACK";
        default:
            if (@available(iOS 11.0, *)) {
                if (laErrorCode == LAErrorBiometryLockout) {
                    return @"BIOMETRIC_LOCKOUT_PERMANENT";
                }
            }
            return @"BIOMETRIC_UNKNOWN_ERROR";
    }
}

+ (NSString *)getBiometricType {
    LAContext *context = [[LAContext alloc] init];
    
    if (@available(iOS 11.0, *)) {
        // On iOS 11+, we can check biometryType directly without evaluating policy
        // This tells us what hardware is available regardless of permission
        switch (context.biometryType) {
            case LABiometryTypeFaceID:
                return @"faceId";
            case LABiometryTypeTouchID:
                return @"touchId";
            case LABiometryTypeNone:
                return @"none";
            default:
                return @"none";
        }
    } else {
        // iOS < 11.0, need to check if device supports biometric authentication
        NSError *error = nil;
        BOOL canEvaluate = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
        
        if (canEvaluate || (error && error.code != LAErrorBiometryNotAvailable)) {
            // Device has Touch ID hardware (available or enrolled but permission denied)
            return @"touchId";
        }
        return @"none";
    }
}

@end
