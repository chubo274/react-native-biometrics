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
    NSError *error = nil;
    
    BOOL canEvaluate = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
    
    if (!canEvaluate) {
        return @"none";
    }
    
    if (@available(iOS 11.0, *)) {
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
        // iOS < 11.0, assume Touch ID if available
        return @"touchId";
    }
}

@end
