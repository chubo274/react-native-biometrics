#import <Foundation/Foundation.h>
#import <LocalAuthentication/LocalAuthentication.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Helper class containing utility methods for biometric operations
 */
@interface BiometricsHelper : NSObject

/**
 * Converts LocalAuthentication error codes to custom error codes
 * @param errorCode The LAError code
 * @return String representation of the error code
 */
+ (NSString *)convertLAErrorToErrorCode:(NSInteger)errorCode;

/**
 * Gets the available biometric type on the device
 * @return String representing biometric type: "faceId", "touchId", or "none"
 */
+ (NSString *)getBiometricType;

@end

NS_ASSUME_NONNULL_END
