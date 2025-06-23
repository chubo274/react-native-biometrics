#import "Biometrics.h"
#import <LocalAuthentication/LocalAuthentication.h>
#import <React/RCTBridgeModule.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <BiometricsSpec/BiometricsSpec.h>
#endif

@implementation Biometrics

RCT_EXPORT_MODULE()

// MARK: - Helper Methods

- (NSString *)convertLAErrorToErrorCode:(NSInteger)errorCode {
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

- (NSString *)getBiometricType {
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

- (void)performCheckBiometricAvailability:(RCTPromiseResolveBlock)resolve 
                                   reject:(RCTPromiseRejectBlock)reject {
    LAContext *context = [[LAContext alloc] init];
    NSError *error = nil;
    
    BOOL canEvaluate = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
    NSString *biometricType = [self getBiometricType];
    BOOL isLockout = NO;
    
    // Check for lockout
    if (error && error.code == LAErrorBiometryLockout) {
        isLockout = YES;
    }
    
    // Check if biometrics are enrolled and available
    BOOL isAvailable = canEvaluate && ![biometricType isEqualToString:@"none"];
    
    // iOS requires permission check through canEvaluatePolicy
    // allowAccess is true if we can evaluate policy (permission granted)
    BOOL allowAccess = canEvaluate;
    
    NSMutableDictionary *result = [@{
        @"isAvailable": @(isAvailable),
        @"allowAccess": @(allowAccess),
        @"biometricType": biometricType,
        @"isLockout": @(isLockout)
    } mutableCopy];
    
    // Only add errorCode and errorMessage if there's an error
    if (error) {
        result[@"errorCode"] = [self convertLAErrorToErrorCode:error.code];
        result[@"errorMessage"] = error.localizedDescription;
    }
    
    resolve(result);
}

- (void)performRequestBiometricPermission:(RCTPromiseResolveBlock)resolve 
                                   reject:(RCTPromiseRejectBlock)reject {
    LAContext *context = [[LAContext alloc] init];
    NSError *error = nil;
    
    // First check if biometrics are available
    BOOL canEvaluate = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
    
    if (!canEvaluate) {
        // Case 1: Device doesn't support biometrics or biometrics not enrolled
        NSMutableDictionary *result = [@{
            @"success": @NO
        } mutableCopy];
        
        if (error) {
            result[@"errorCode"] = [self convertLAErrorToErrorCode:error.code];
            result[@"errorMessage"] = error.localizedDescription;
        }
        
        resolve(result);
        return;
    }
    
    // Case 2: Device supports biometrics and has biometrics enrolled
    // Check if app has permission by attempting authentication
    [context evaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics
            localizedReason:@"Allow this app to use biometric authentication"
                      reply:^(BOOL success, NSError *authError) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (success) {
                // Case 4: App has permission and authentication succeeded
                resolve(@{
                    @"success": @YES
                });
            } else if (authError) {
                if (authError.code == LAErrorUserCancel || 
                    authError.code == LAErrorSystemCancel) {
                    // Case 3: User saw prompt but canceled
                    // Still consider permission as requested, user just declined
                    resolve(@{
                        @"success": @NO,
                        @"errorCode": [self convertLAErrorToErrorCode:authError.code],
                        @"errorMessage": authError.localizedDescription
                    });
                } else if (authError.code == LAErrorAuthenticationFailed) {
                    // Authentication failed but permission was granted
                    resolve(@{
                        @"success": @YES
                    });
                } else {
                    // Other errors (lockout, biometry not available, etc.)
                    resolve(@{
                        @"success": @NO,
                        @"errorCode": [self convertLAErrorToErrorCode:authError.code],
                        @"errorMessage": authError.localizedDescription
                    });
                }
            } else {
                // Unexpected case
                resolve(@{
                    @"success": @NO,
                    @"errorCode": @"BIOMETRIC_UNKNOWN_ERROR",
                    @"errorMessage": @"Unknown error occurred"
                });
            }
        });
    }];
}

- (void)performAuthenticate:(NSDictionary *)options
                    resolve:(RCTPromiseResolveBlock)resolve 
                     reject:(RCTPromiseRejectBlock)reject {
    
    // Parse options
    BOOL otherwayWithPIN = NO;
    
    if (options[@"otherwayWithPIN"]) {
        otherwayWithPIN = [options[@"otherwayWithPIN"] boolValue];
    }
    
    LAContext *context = [[LAContext alloc] init];
    
    // Configure context based on options
    if (otherwayWithPIN) {
        // Use device passcode as fallback
        context.localizedFallbackTitle = @"Use Passcode";
    } else {
        // fallback Another button
        context.localizedFallbackTitle = @"Another way";
    }
    
    LAPolicy policy = otherwayWithPIN ? LAPolicyDeviceOwnerAuthentication : LAPolicyDeviceOwnerAuthenticationWithBiometrics;
    
    // Use a default reason for biometric authentication
    NSString *reason = otherwayWithPIN ? @"Please authenticate to continue" : @"Not recognized. Want to try another way?";
    
    [context evaluatePolicy:policy localizedReason:reason reply:^(BOOL success, NSError * _Nullable error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (success) {
                resolve(@{
                    @"success": @YES
                });
            } else {
                BOOL pressedOtherway = NO;
                
                // Check if user pressed the fallback button
                if (error && [error isKindOfClass:[NSError class]] && error.code == LAErrorUserFallback) {
                    if (!otherwayWithPIN) {
                        pressedOtherway = YES;
                    }
                }
                
                NSMutableDictionary *result = [NSMutableDictionary dictionaryWithDictionary:@{
                    @"success": @NO,
                    @"errorCode": [self convertLAErrorToErrorCode:error ? error.code : LAErrorSystemCancel],
                    @"errorMessage": error ? error.localizedDescription : @""
                }];
                
                if (pressedOtherway) {
                    result[@"pressedOtherway"] = @YES;
                    result[@"errorCode"] = @"BIOMETRIC_PRESSED_OTHER_WAY";
                }
                
                resolve(result);
            }
        });
    }];
}

- (void)performAuthenticatePIN:(RCTPromiseResolveBlock)resolve 
                        reject:(RCTPromiseRejectBlock)reject {
    // For iOS, authenticatePIN uses the same implementation as authenticate with otherwayWithPIN=true
    // This allows both biometric and PIN/passcode authentication with PIN as fallback
    NSDictionary *options = @{
        @"otherwayWithPIN": @YES
    };
    
    [self performAuthenticate:options resolve:resolve reject:reject];
}

#ifdef RCT_NEW_ARCH_ENABLED

// MARK: - TurboModule methods

- (void)checkBiometricAvailability:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    [self performCheckBiometricAvailability:resolve reject:reject];
}

- (void)requestBiometricPermission:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    [self performRequestBiometricPermission:resolve reject:reject];
}

- (void)authenticateBiometric:(JS::NativeBiometrics::BiometricAuthOptions &)options 
             resolve:(RCTPromiseResolveBlock)resolve 
              reject:(RCTPromiseRejectBlock)reject {
    
    // Convert TurboModule options to NSDictionary
    NSMutableDictionary *optionsDict = [NSMutableDictionary dictionary];
    
    if (options.otherwayWithPIN().has_value()) {
        optionsDict[@"otherwayWithPIN"] = @(options.otherwayWithPIN().value());
    }
    
    [self performAuthenticate:optionsDict resolve:resolve reject:reject];
}

- (void)authenticatePIN:(RCTPromiseResolveBlock)resolve 
              reject:(RCTPromiseRejectBlock)reject {
    [self performAuthenticatePIN:resolve reject:reject];
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeBiometricsSpecJSI>(params);
}

#else

// MARK: - Legacy Bridge Methods

RCT_EXPORT_METHOD(checkBiometricAvailability:(RCTPromiseResolveBlock)resolve 
                                      reject:(RCTPromiseRejectBlock)reject) {
    [self performCheckBiometricAvailability:resolve reject:reject];
}

RCT_EXPORT_METHOD(requestBiometricPermission:(RCTPromiseResolveBlock)resolve 
                                      reject:(RCTPromiseRejectBlock)reject) {
    [self performRequestBiometricPermission:resolve reject:reject];
}

RCT_EXPORT_METHOD(authenticateBiometric:(NSDictionary *)options
                      resolve:(RCTPromiseResolveBlock)resolve 
                       reject:(RCTPromiseRejectBlock)reject) {
    [self performAuthenticate:options resolve:resolve reject:reject];
}

RCT_EXPORT_METHOD(authenticatePIN:(RCTPromiseResolveBlock)resolve 
                       reject:(RCTPromiseRejectBlock)reject) {
    [self performAuthenticatePIN:resolve reject:reject];
}

// Keep backward compatibility
RCT_EXPORT_METHOD(authenticate:(NSDictionary *)options
                      resolve:(RCTPromiseResolveBlock)resolve 
                       reject:(RCTPromiseRejectBlock)reject) {
    [self performAuthenticate:options resolve:resolve reject:reject];
}

#endif

@end
