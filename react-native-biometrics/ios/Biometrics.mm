#import "Biometrics.h"
#import <LocalAuthentication/LocalAuthentication.h>

@implementation Biometrics
RCT_EXPORT_MODULE()

// Helper method to convert LAError to our custom error codes
- (NSString *)convertLAErrorToErrorCode:(LAError)errorCode {
    switch (errorCode) {
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
        default:
            if (@available(iOS 11.0, *)) {
                if (errorCode == LAErrorBiometryLockout) {
                    return @"BIOMETRIC_LOCKOUT_PERMANENT";
                }
            }
            return @"BIOMETRIC_UNKNOWN_ERROR";
    }
}

// Helper method to get biometric type
- (NSString *)getBiometricType {
    LAContext *context = [[LAContext alloc] init];
    NSError *error = nil;
    
    if (![context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error]) {
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

- (void)checkBiometricAvailability:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    LAContext *context = [[LAContext alloc] init];
    NSError *error = nil;
    
    BOOL canEvaluate = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
    NSString *biometricType = [self getBiometricType];
    BOOL isLockout = NO;
    BOOL allowAccess = YES; // iOS doesn't require explicit permission for biometrics
    
    // Check for lockout
    if (error && (error.code == LAErrorBiometryLockout)) {
        isLockout = YES;
    }
    
    // Check if biometrics are enrolled
    BOOL isAvailable = canEvaluate && ![biometricType isEqualToString:@"none"];
    
    NSDictionary *result = @{
        @"isAvailable": @(isAvailable),
        @"allowAccess": @(allowAccess),
        @"biometricType": biometricType,
        @"isLockout": @(isLockout),
        @"errorCode": error ? [self convertLAErrorToErrorCode:(LAError)error.code] : [NSNull null],
        @"errorMessage": error ? error.localizedDescription : [NSNull null]
    };
    
    resolve(result);
}

- (void)requestBiometricPermission:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    // iOS doesn't require explicit permission for biometrics
    // Check availability instead
    LAContext *context = [[LAContext alloc] init];
    NSError *error = nil;
    
    BOOL canEvaluate = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
    
    if (canEvaluate) {
        resolve(@{
            @"success": @YES
        });
    } else {
        resolve(@{
            @"success": @NO,
            @"errorCode": [self convertLAErrorToErrorCode:(LAError)error.code],
            @"errorMessage": error.localizedDescription
        });
    }
}

- (void)authenticate:(NSString *)reason 
             options:(JS::NativeBiometrics::BiometricAuthOptions &)options 
             resolve:(RCTPromiseResolveBlock)resolve 
              reject:(RCTPromiseRejectBlock)reject {
    
    LAContext *context = [[LAContext alloc] init];
    
    // Parse options
    BOOL otherwayWithPIN = NO;
    NSString *textOtherway = nil;
    
    // Extract values from the options struct
    if (options.otherwayWithPIN().has_value()) {
        otherwayWithPIN = options.otherwayWithPIN().value();
    }
    textOtherway = options.textOtherway();
    
    // Configure context based on options
    if (otherwayWithPIN) {
        // Use device passcode as fallback
        context.localizedFallbackTitle = textOtherway ?: @"Use Passcode";
    } else {
        // Custom fallback or no fallback
        if (textOtherway) {
            context.localizedFallbackTitle = textOtherway;
        } else {
            context.localizedFallbackTitle = @""; // Hide fallback button
        }
    }
    
    LAPolicy policy = otherwayWithPIN ? 
        LAPolicyDeviceOwnerAuthentication : 
        LAPolicyDeviceOwnerAuthenticationWithBiometrics;
    
    [context evaluatePolicy:policy
            localizedReason:reason
                      reply:^(BOOL success, NSError * _Nullable error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (success) {
                resolve(@{
                    @"success": @YES
                });
            } else {
                BOOL pressedOtherway = NO;
                
                // Check if user pressed the fallback button
                if (error.code == LAErrorUserFallback) {
                    if (!otherwayWithPIN) {
                        pressedOtherway = YES;
                    }
                }
                
                NSMutableDictionary *result = [@{
                    @"success": @NO,
                    @"errorCode": [self convertLAErrorToErrorCode:(LAError)error.code],
                    @"errorMessage": error.localizedDescription
                } mutableCopy];
                
                if (pressedOtherway) {
                    result[@"pressedOtherway"] = @YES;
                    result[@"errorCode"] = @"BIOMETRIC_PRESSED_OTHER_WAY";
                }
                
                resolve(result);
            }
        });
    }];
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeBiometricsSpecJSI>(params);
}

@end
