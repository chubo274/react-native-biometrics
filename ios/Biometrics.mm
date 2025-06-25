#import "Biometrics.h"
#import "BiometricsHelper.h"
#import <LocalAuthentication/LocalAuthentication.h>
#import <React/RCTBridgeModule.h>
#import <Security/Security.h>
#import <CommonCrypto/CommonCrypto.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <BiometricsSpec/BiometricsSpec.h>
#endif


@implementation Biometrics

RCT_EXPORT_MODULE()

// MARK: - Core Authentication Methods

- (void)performCheckBiometricAvailability:(RCTPromiseResolveBlock)resolve
                                   reject:(RCTPromiseRejectBlock)reject {
    LAContext *context = [[LAContext alloc] init];
    NSError *error = nil;
    
    // Check if app can use biometric authentication (hardware + enrolled + permission)
    BOOL canEvaluate = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthenticationWithBiometrics error:&error];
    
    // Get biometric type from hardware (independent of permission/enrollment)
    NSString *biometricType = [BiometricsHelper getBiometricType];
    
    // isAvailable = device has biometric hardware AND biometrics are enrolled
    // We need to distinguish between "no hardware" vs "hardware but not enrolled"
    BOOL hasHardware = ![biometricType isEqualToString:@"none"];
    BOOL isEnrolled = NO;
    BOOL allowAccess = canEvaluate;
    
    if (hasHardware) {
        if (canEvaluate) {
            // Can evaluate = hardware + enrolled + permission
            isEnrolled = YES;
        } else if (error) {
            // Check specific error to determine if biometric is enrolled
            if (error.code == LAErrorBiometryNotEnrolled) {
                isEnrolled = NO;
            } else if (error.code == LAErrorPasscodeNotSet) {
                // Device passcode not set, biometric may be enrolled but can't be used
                isEnrolled = NO;
            } else {
                // Other errors might still indicate enrolled biometric (e.g., permission issues)
                // Try a secondary check or assume enrolled if hardware exists
                isEnrolled = YES;
            }
        }
    }
    
    BOOL isAvailable = hasHardware && isEnrolled;
    
    NSMutableDictionary *result = [@{
        @"isAvailable": @(isAvailable),
        @"allowAccess": @(allowAccess),
        @"biometricType": biometricType
    } mutableCopy];
    
    // Add error information for genuine issues (not permission/enrollment)
    if (error && !hasHardware) {
        // Only add error if device truly doesn't have biometric hardware
        result[@"errorCode"] = [BiometricsHelper convertLAErrorToErrorCode:error.code];
        result[@"errorMessage"] = error.localizedDescription;
    } else if (error && hasHardware && !isEnrolled) {
        // Device has hardware but biometric not enrolled
        result[@"errorCode"] = [BiometricsHelper convertLAErrorToErrorCode:error.code];
        result[@"errorMessage"] = error.localizedDescription;
    }
    // If hasHardware && isEnrolled && !allowAccess, it's just permission issue - no error needed
    
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
            result[@"errorCode"] = [BiometricsHelper convertLAErrorToErrorCode:error.code];
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
                        @"errorCode": [BiometricsHelper convertLAErrorToErrorCode:authError.code],
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
                        @"errorCode": [BiometricsHelper convertLAErrorToErrorCode:authError.code],
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
    
    // Guard against null/undefined options
    if (options == nil || options == (id)[NSNull null]) {
        options = @{};
    }
    
    // Parse options with new structure
    NSString *titlePrompt = options[@"titlePrompt"] ?: @"Biometric Authentication";
    NSString *otherwayWith = options[@"otherwayWith"] ?: @"PIN"; // Default to PIN mode
    NSString *otherwayText = options[@"otherwayText"];
    
    // Determine behavior based on otherwayWith
    BOOL hideOtherway = [otherwayWith isEqualToString:@"hide"];
    BOOL showCallback = [otherwayWith isEqualToString:@"callback"];
    BOOL isPINMode = [otherwayWith isEqualToString:@"PIN"];
    
    LAContext *context = [[LAContext alloc] init];
    
    // Configure context based on options
    if (isPINMode) {
        // Use device passcode as fallback
        context.localizedFallbackTitle = @"Use Passcode";
    } else if (hideOtherway) {
        // Hide fallback button - use biometric only (iOS only feature)
        context.localizedFallbackTitle = @"";
    } else if (showCallback) {
        // Custom fallback button text
        context.localizedFallbackTitle = otherwayText ?: @"Another Way";
    } else {
        // Default to PIN mode for unknown modes
        context.localizedFallbackTitle = @"Use Passcode";
    }
    
    LAPolicy policy = (isPINMode || (!hideOtherway && !showCallback)) ? LAPolicyDeviceOwnerAuthentication : LAPolicyDeviceOwnerAuthenticationWithBiometrics;
    
    [context evaluatePolicy:policy localizedReason:titlePrompt reply:^(BOOL success, NSError * _Nullable error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (success) {
                resolve(@{
                    @"success": @YES
                });
            } else {
                BOOL pressedOtherway = NO;
                
                // Check if user pressed the fallback button
                if (error && [error isKindOfClass:[NSError class]] && error.code == LAErrorUserFallback) {
                    if (showCallback) {
                        pressedOtherway = YES;
                    }
                }
                
                NSMutableDictionary *result = [NSMutableDictionary dictionaryWithDictionary:@{
                    @"success": @NO,
                    @"errorCode": [BiometricsHelper convertLAErrorToErrorCode:error ? error.code : LAErrorSystemCancel],
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
    // For iOS, authenticatePIN uses the same implementation as authenticate with PIN mode
    NSDictionary *options = @{
        @"titlePrompt": @"Device Authentication",
        @"otherwayWith": @"PIN"
    };
    
    [self performAuthenticate:options resolve:resolve reject:reject];
}

// MARK: - Private Key Management Implementation

- (NSString *)getBiometricKeyIdentifier {
    // Generate a unique key identifier based on bundle ID to ensure one key per app
    NSString *bundleId = [[NSBundle mainBundle] bundleIdentifier];
    return [NSString stringWithFormat:@"%@.biometric.privatekey", bundleId];
}

- (void)performCreateBiometricKey:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject {
    NSString *keyIdentifier = [self getBiometricKeyIdentifier];
    
    // Check if key already exists
    OSStatus checkStatus = SecItemCopyMatching((__bridge CFDictionaryRef)@{
        (__bridge NSString *)kSecClass: (__bridge NSString *)kSecClassKey,
        (__bridge NSString *)kSecAttrApplicationTag: [keyIdentifier dataUsingEncoding:NSUTF8StringEncoding],
        (__bridge NSString *)kSecReturnRef: @YES
    }, NULL);
    
    if (checkStatus == errSecSuccess) {
        resolve(@{
            @"success": @NO,
            @"errorCode": @"BIOMETRIC_KEY_EXISTS",
            @"errorMessage": @"Biometric key already exists. Delete existing key before creating a new one."
        });
        return;
    }
    
    // Create new private key with biometric protection
    CFErrorRef error = NULL;
    SecAccessControlRef accessControl = SecAccessControlCreateWithFlags(
        kCFAllocatorDefault,
        kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        kSecAccessControlBiometryCurrentSet | kSecAccessControlPrivateKeyUsage,
        &error
    );
    
    if (error != NULL) {
        resolve(@{
            @"success": @NO,
            @"errorCode": @"BIOMETRIC_KEY_CREATION_FAILED",
            @"errorMessage": @"Failed to create access control for biometric key"
        });
        CFRelease(error);
        return;
    }
    
    NSDictionary *keyAttributes = @{
        (__bridge NSString *)kSecAttrKeyType: (__bridge NSString *)kSecAttrKeyTypeECSECPrimeRandom,
        (__bridge NSString *)kSecAttrKeySizeInBits: @256,
        (__bridge NSString *)kSecAttrTokenID: (__bridge NSString *)kSecAttrTokenIDSecureEnclave,
        (__bridge NSString *)kSecPrivateKeyAttrs: @{
            (__bridge NSString *)kSecAttrIsPermanent: @YES,
            (__bridge NSString *)kSecAttrApplicationTag: [keyIdentifier dataUsingEncoding:NSUTF8StringEncoding],
            (__bridge NSString *)kSecAttrAccessControl: (__bridge id)accessControl
        }
    };
    
    SecKeyRef privateKey = SecKeyCreateRandomKey((__bridge CFDictionaryRef)keyAttributes, &error);
    CFRelease(accessControl);
    
    if (error != NULL) {
        NSString *errorMessage = @"Failed to create biometric private key";
        if (@available(iOS 11.3, *)) {
            NSError *nsError = (__bridge NSError *)error;
            errorMessage = nsError.localizedDescription;
        }
        
        resolve(@{
            @"success": @NO,
            @"errorCode": @"BIOMETRIC_KEY_CREATION_FAILED",
            @"errorMessage": errorMessage
        });
        CFRelease(error);
        return;
    }
    
    if (privateKey) {
        CFRelease(privateKey);
        resolve(@{
            @"success": @YES
        });
    } else {
        resolve(@{
            @"success": @NO,
            @"errorCode": @"BIOMETRIC_KEY_CREATION_FAILED",
            @"errorMessage": @"Failed to create biometric private key"
        });
    }
}

- (void)performBiometricKeyExists:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject {
    NSString *keyIdentifier = [self getBiometricKeyIdentifier];
    
    OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)@{
        (__bridge NSString *)kSecClass: (__bridge NSString *)kSecClassKey,
        (__bridge NSString *)kSecAttrApplicationTag: [keyIdentifier dataUsingEncoding:NSUTF8StringEncoding],
        (__bridge NSString *)kSecReturnRef: @YES
    }, NULL);
    
    if (status == errSecSuccess) {
        resolve(@{
            @"exists": @YES
        });
    } else if (status == errSecItemNotFound) {
        resolve(@{
            @"exists": @NO
        });
    } else {
        resolve(@{
            @"exists": @NO,
            @"errorCode": @"BIOMETRIC_KEY_CHECK_FAILED",
            @"errorMessage": @"Failed to check if biometric key exists"
        });
    }
}

- (void)performDeleteBiometricKey:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject {
    NSString *keyIdentifier = [self getBiometricKeyIdentifier];
    
    OSStatus status = SecItemDelete((__bridge CFDictionaryRef)@{
        (__bridge NSString *)kSecClass: (__bridge NSString *)kSecClassKey,
        (__bridge NSString *)kSecAttrApplicationTag: [keyIdentifier dataUsingEncoding:NSUTF8StringEncoding]
    });
    
    if (status == errSecSuccess || status == errSecItemNotFound) {
        resolve(@{
            @"success": @YES
        });
    } else {
        resolve(@{
            @"success": @NO,
            @"errorCode": @"BIOMETRIC_KEY_DELETION_FAILED",
            @"errorMessage": @"Failed to delete biometric key"
        });
    }
}

- (void)performCreateSignature:(NSString *)payload
                       options:(NSDictionary *)options
                       resolve:(RCTPromiseResolveBlock)resolve
                        reject:(RCTPromiseRejectBlock)reject {
    // Note: The private key was created with biometric protection.
    // SecKeyCreateSignature will automatically prompt for biometrics when needed.
    // Custom prompt options are not supported by SecKeyCreateSignature.
    NSString *keyIdentifier = [self getBiometricKeyIdentifier];
    
    // Get the private key
    SecKeyRef privateKeyRef = NULL;
    OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)@{
        (__bridge NSString *)kSecClass: (__bridge NSString *)kSecClassKey,
        (__bridge NSString *)kSecAttrApplicationTag: [keyIdentifier dataUsingEncoding:NSUTF8StringEncoding],
        (__bridge NSString *)kSecReturnRef: @YES
    }, (CFTypeRef *)&privateKeyRef);
    
    if (status != errSecSuccess || privateKeyRef == NULL) {
        resolve(@{
            @"success": @NO,
            @"errorCode": @"BIOMETRIC_KEY_NOT_FOUND",
            @"errorMessage": @"Biometric key not found. Create a key first."
        });
        return;
    }
    
    // Convert payload to data and hash it
    NSData *payloadData = [payload dataUsingEncoding:NSUTF8StringEncoding];
    NSMutableData *hashedData = [NSMutableData dataWithLength:CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(payloadData.bytes, (CC_LONG)payloadData.length, (unsigned char *)hashedData.mutableBytes);
    
    // Create signature
    CFErrorRef error = NULL;
    NSData *signature = (NSData *)CFBridgingRelease(SecKeyCreateSignature(
        privateKeyRef,
        kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
        (__bridge CFDataRef)hashedData,
        &error
    ));
    
    CFRelease(privateKeyRef);
    
    if (error != NULL) {
        NSString *errorMessage = @"Failed to create signature";
        NSString *errorCode = @"BIOMETRIC_SIGNATURE_FAILED";
        
        if (@available(iOS 11.3, *)) {
            NSError *nsError = (__bridge NSError *)error;
            errorMessage = nsError.localizedDescription;
            
            // Check if it's a biometric authentication error
            if (nsError.code == errSecAuthFailed) {
                errorCode = @"BIOMETRIC_AUTH_FAILED";
            } else if (nsError.code == errSecUserCanceled) {
                errorCode = @"BIOMETRIC_USER_CANCEL";
            }
        }
        
        resolve(@{
            @"success": @NO,
            @"errorCode": errorCode,
            @"errorMessage": errorMessage
        });
        CFRelease(error);
        return;
    }
    
    if (signature) {
        NSString *base64Signature = [signature base64EncodedStringWithOptions:0];
        resolve(@{
            @"success": @YES,
            @"signature": base64Signature
        });
    } else {
        resolve(@{
            @"success": @NO,
            @"errorCode": @"BIOMETRIC_SIGNATURE_FAILED",
            @"errorMessage": @"Failed to create signature"
        });
    }
}

#ifdef RCT_NEW_ARCH_ENABLED

// MARK: - TurboModule methods

// Helper method to convert TurboModule options to NSDictionary
- (NSDictionary *)convertTurboModuleOptions:(const JS::NativeBiometrics::BiometricAuthOptions &)options {
    NSMutableDictionary *optionsDict = [NSMutableDictionary dictionary];

    // TurboModule spec properties are accessed directly as NSString*
    NSString *titlePrompt = options.titlePrompt();
    if (titlePrompt) {
        optionsDict[@"titlePrompt"] = titlePrompt;
    }

    NSString *otherwayWith = options.otherwayWith();
    if (otherwayWith) {
        optionsDict[@"otherwayWith"] = otherwayWith;
    }

    NSString *otherwayText = options.otherwayText();
    if (otherwayText) {
        optionsDict[@"otherwayText"] = otherwayText;
    }

    return optionsDict;
}

- (void)checkBiometricAvailability:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    [self performCheckBiometricAvailability:resolve reject:reject];
}

- (void)requestBiometricPermission:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
    [self performRequestBiometricPermission:resolve reject:reject];
}

- (void)authenticateBiometric:(const JS::NativeBiometrics::BiometricAuthOptions &)options
                      resolve:(RCTPromiseResolveBlock)resolve
                       reject:(RCTPromiseRejectBlock)reject {
    [self performAuthenticate:[self convertTurboModuleOptions:options] resolve:resolve reject:reject];
}

- (void)authenticatePIN:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject {
    [self performAuthenticatePIN:resolve reject:reject];
}

- (void)createBiometricKey:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject {
    [self performCreateBiometricKey:resolve reject:reject];
}

- (void)biometricKeyExists:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject {
    [self performBiometricKeyExists:resolve reject:reject];
}

- (void)deleteBiometricKey:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject {
    [self performDeleteBiometricKey:resolve reject:reject];
}

- (void)createSignature:(NSString *)payload
                options:(const JS::NativeBiometrics::BiometricAuthOptions &)options
                resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject {
    [self performCreateSignature:payload options:[self convertTurboModuleOptions:options] resolve:resolve reject:reject];
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


RCT_EXPORT_METHOD(createBiometricKey:(RCTPromiseResolveBlock)resolve
                                   reject:(RCTPromiseRejectBlock)reject) {
    [self performCreateBiometricKey:resolve reject:reject];
}

RCT_EXPORT_METHOD(biometricKeyExists:(RCTPromiseResolveBlock)resolve
                                   reject:(RCTPromiseRejectBlock)reject) {
    [self performBiometricKeyExists:resolve reject:reject];
}

RCT_EXPORT_METHOD(deleteBiometricKey:(RCTPromiseResolveBlock)resolve
                                   reject:(RCTPromiseRejectBlock)reject) {
    [self performDeleteBiometricKey:resolve reject:reject];
}

RCT_EXPORT_METHOD(createSignature:(NSString *)payload
                            options:(NSDictionary *)options
                            resolve:(RCTPromiseResolveBlock)resolve
                             reject:(RCTPromiseRejectBlock)reject) {
    // iOS automatically prompts for biometric authentication when accessing the private key
    [self performCreateSignature:payload options:options resolve:resolve reject:reject];
}

// Keep backward compatibility
RCT_EXPORT_METHOD(authenticate:(NSDictionary *)options
                        resolve:(RCTPromiseResolveBlock)resolve
                         reject:(RCTPromiseRejectBlock)reject) {
    [self performAuthenticate:options resolve:resolve reject:reject];
}

#endif

@end
