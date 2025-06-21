# React Native Biometrics

A comprehensive React Native library for biometric authentication on iOS and Android platforms.

[![npm version](https://badge.fury.io/js/react-native-biometrics.svg)](https://badge.fury.io/js/react-native-biometrics)
[![npm downloads](https://img.shields.io/npm/dt/react-native-biometrics.svg)](https://www.npmjs.com/package/react-native-biometrics)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Features

- 🔐 **Biometric Authentication**: Support for fingerprint, Face ID, Touch ID, and iris recognition
- 🛡️ **Strong Security**: Uses BIOMETRIC_STRONG authenticators on Android for enhanced security
- 📱 **Cross-Platform**: Works seamlessly on both iOS and Android
- 🎯 **TypeScript Support**: Full TypeScript definitions included
- 🔄 **Flexible Fallback**: Support for device PIN/passcode fallback
- 📊 **Comprehensive Status**: Detailed availability and permission status
- 🚫 **Lockout Detection**: Detects and reports biometric lockout status
- 🎨 **Customizable UI**: Custom text for fallback buttons

## Installation

```bash
npm install react-native-biometrics
# or
yarn add react-native-biometrics
```

### iOS Setup

1. Add the following to your `Info.plist`:

```xml
<key>NSFaceIDUsageDescription</key>
<string>This app uses Face ID for secure authentication</string>
```

2. The library automatically links with LocalAuthentication framework.

### Android Setup

1. Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

2. Ensure your `minSdkVersion` is at least 23 in `android/app/build.gradle`:

```gradle
android {
    defaultConfig {
        minSdkVersion 23
        // ...
    }
}
```

## Usage

### Basic Usage

```typescript
import ReactNativeBiometrics, { 
  BiometricType, 
  BiometricErrorCode 
} from 'react-native-biometrics';

// Check if biometric authentication is available
const checkAvailability = async () => {
  const result = await ReactNativeBiometrics.checkBiometricAvailability();
  
  if (result.isAvailable) {
    console.log(`Biometric type: ${result.biometricType}`);
    console.log(`Allow access: ${result.allowAccess}`);
    console.log(`Is locked out: ${result.isLockout}`);
  } else {
    console.log(`Error: ${result.errorMessage}`);
  }
};

// Request biometric permission (Android)
const requestPermission = async () => {
  const result = await ReactNativeBiometrics.requestBiometricPermission();
  
  if (result.success) {
    console.log('Permission granted');
  } else {
    console.log(`Permission denied: ${result.errorMessage}`);
  }
};

// Authenticate with biometrics
const authenticate = async () => {
  const result = await ReactNativeBiometrics.authenticate(
    'Please authenticate to access your account'
  );
  
  if (result.success) {
    console.log('Authentication successful');
  } else if (result.pressedOtherway) {
    console.log('User pressed other way button');
  } else {
    console.log(`Authentication failed: ${result.errorMessage}`);
  }
};
```

### Advanced Usage

#### Authentication with PIN Fallback

```typescript
const authenticateWithPIN = async () => {
  const result = await ReactNativeBiometrics.authenticate(
    'Authenticate to continue',
    {
      otherwayWithPIN: true, // Allow device PIN as fallback
    }
  );
  
  if (result.success) {
    console.log('Authenticated successfully');
  }
};
```

#### Custom Fallback Button

```typescript
const authenticateWithCustomFallback = async () => {
  const result = await ReactNativeBiometrics.authenticate(
    'Authenticate to continue',
    {
      otherwayWithPIN: false, // Don't use PIN fallback
      textOtherway: 'Use Password', // Custom button text
    }
  );
  
  if (result.pressedOtherway) {
    // Handle custom fallback action
    console.log('User wants to use password');
  }
};
```

## API Reference

### Methods

#### `checkBiometricAvailability()`

Checks if biometric authentication is available on the device.

**Returns**: `Promise<BiometricAvailability>`

```typescript
interface BiometricAvailability {
  isAvailable: boolean;        // Device supports and has biometrics enrolled
  allowAccess: boolean;        // App has permission to use biometrics
  biometricType: BiometricType; // Primary biometric type available
  isLockout: boolean;          // Device is locked out due to too many attempts
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}
```

#### `requestBiometricPermission()`

Requests permission to use biometric authentication (Android only).

**Returns**: `Promise<BiometricPermissionResult>`

```typescript
interface BiometricPermissionResult {
  success: boolean;
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}
```

#### `authenticate(reason, options?)`

Performs biometric authentication.

**Parameters**:
- `reason`: string - Message shown to user during authentication
- `options?`: BiometricAuthOptions - Authentication options

**Returns**: `Promise<BiometricAuthResult>`

```typescript
interface BiometricAuthOptions {
  otherwayWithPIN?: boolean;  // Allow device PIN/passcode fallback
  textOtherway?: string;      // Custom text for fallback button
}

interface BiometricAuthResult {
  success: boolean;
  pressedOtherway?: boolean;  // User pressed fallback button
  errorCode?: BiometricErrorCode;
  errorMessage?: string;
}
```

### Types

#### `BiometricType`

```typescript
enum BiometricType {
  FINGERPRINT = 'fingerprint',
  FACE_ID = 'faceId',
  TOUCH_ID = 'touchId',
  IRIS = 'iris',
  NONE = 'none',
}
```

#### `BiometricErrorCode`

```typescript
enum BiometricErrorCode {
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
```

## Error Handling

The library provides comprehensive error handling with standardized error codes across platforms:

```typescript
const result = await ReactNativeBiometrics.authenticate('Authenticate');

if (!result.success) {
  switch (result.errorCode) {
    case BiometricErrorCode.BIOMETRIC_NOT_AVAILABLE:
      console.log('Biometric hardware not available');
      break;
    case BiometricErrorCode.BIOMETRIC_NOT_ENROLLED:
      console.log('No biometric credentials enrolled');
      break;
    case BiometricErrorCode.BIOMETRIC_LOCKOUT:
      console.log('Too many failed attempts, try again later');
      break;
    case BiometricErrorCode.BIOMETRIC_USER_CANCEL:
      console.log('User canceled authentication');
      break;
    case BiometricErrorCode.BIOMETRIC_PRESSED_OTHER_WAY:
      console.log('User chose alternative authentication');
      break;
    // Handle other error codes...
  }
}
```

## Platform Differences

### iOS
- Uses Local Authentication framework
- Face ID and Touch ID support
- No explicit permission required
- Supports device passcode fallback

### Android
- Uses AndroidX Biometric library
- BIOMETRIC_STRONG authenticators only (enhanced security)
- Requires USE_BIOMETRIC permission
- Supports fingerprint, face, and iris recognition
- Supports device PIN/pattern/password fallback

## Example App

The library includes a comprehensive example app demonstrating all features. To run the example:

```bash
cd example
yarn install
yarn ios    # or yarn android
```

## Requirements

- React Native >= 0.70
- iOS >= 12.0
- Android API >= 23

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

## Support

- 📖 [Documentation](https://github.com/chubo274/react-native-biometrics#readme)
- 🐛 [Bug Reports](https://github.com/chubo274/react-native-biometrics/issues)
- 💡 [Feature Requests](https://github.com/chubo274/react-native-biometrics/issues)

---

Made with ❤️ by [boindahood](https://github.com/chubo274)
