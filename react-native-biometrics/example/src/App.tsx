import { useState, useEffect } from 'react';
import {
  Text,
  View,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ScrollView,
  SafeAreaView,
} from 'react-native';
import ReactNativeBiometrics, {
  BiometricType,
  BiometricErrorCode,
  type BiometricAvailability,
  type BiometricAuthResult,
  type BiometricPermissionResult,
} from 'react-native-biometrics';

export default function App() {
  const [availability, setAvailability] =
    useState<BiometricAvailability | null>(null);
  const [permissionResult, setPermissionResult] =
    useState<BiometricPermissionResult | null>(null);
  const [lastAuthResult, setLastAuthResult] =
    useState<BiometricAuthResult | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkBiometricStatus();
  }, []);

  const checkBiometricStatus = async () => {
    try {
      setLoading(true);
      const availabilityResult =
        await ReactNativeBiometrics.checkBiometricAvailability();
      setAvailability(availabilityResult);
    } catch (error) {
      console.error('Error checking biometric status:', error);
      Alert.alert('Error', 'Failed to check biometric status');
    } finally {
      setLoading(false);
    }
  };

  const handleRequestPermission = async () => {
    try {
      setLoading(true);
      const result = await ReactNativeBiometrics.requestBiometricPermission();
      setPermissionResult(result);

      if (result.success) {
        Alert.alert('Success', 'Biometric permission granted');
        // Refresh status after permission granted
        await checkBiometricStatus();
      } else {
        Alert.alert(
          'Permission Denied',
          result.errorMessage || 'Failed to get permission'
        );
      }
    } catch (error) {
      console.error('Error requesting permission:', error);
      Alert.alert('Error', 'Failed to request permission');
    } finally {
      setLoading(false);
    }
  };

  const handleAuthenticate = async (options?: {
    otherwayWithPIN?: boolean;
    textOtherway?: string;
  }) => {
    if (!availability?.isAvailable) {
      Alert.alert('Error', 'Biometric authentication is not available');
      return;
    }

    try {
      setLoading(true);
      const result = await ReactNativeBiometrics.authenticate(
        'Please authenticate to continue',
        options
      );

      setLastAuthResult(result);

      if (result.success) {
        Alert.alert('Success', 'Authentication successful!');
      } else if (result.pressedOtherway) {
        Alert.alert('Other Way', 'User pressed the other way button');
      } else {
        Alert.alert(
          'Authentication Failed',
          result.errorMessage || 'Authentication failed'
        );
      }
    } catch (error) {
      console.error('Error during authentication:', error);
      Alert.alert('Error', 'Authentication error occurred');
    } finally {
      setLoading(false);
    }
  };

  const renderBiometricType = (type: BiometricType) => {
    const typeLabels = {
      [BiometricType.FINGERPRINT]: 'Fingerprint',
      [BiometricType.FACE_ID]: 'Face ID',
      [BiometricType.TOUCH_ID]: 'Touch ID',
      [BiometricType.IRIS]: 'Iris',
      [BiometricType.NONE]: 'None',
    };
    return typeLabels[type] || type;
  };

  const renderErrorCode = (code?: BiometricErrorCode) => {
    if (!code) return 'None';
    return code.replace('BIOMETRIC_', '').replace(/_/g, ' ').toLowerCase();
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <Text style={styles.title}>React Native Biometrics Demo</Text>

        {/* Biometric Availability Status */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Biometric Availability</Text>
          {availability ? (
            <View style={styles.statusContainer}>
              <Text
                style={[
                  styles.statusText,
                  availability.isAvailable ? styles.success : styles.error,
                ]}
              >
                Available: {availability.isAvailable ? 'Yes' : 'No'}
              </Text>
              <Text
                style={[
                  styles.statusText,
                  availability.allowAccess ? styles.success : styles.error,
                ]}
              >
                Allow Access: {availability.allowAccess ? 'Yes' : 'No'}
              </Text>
              <Text style={styles.statusText}>
                Type: {renderBiometricType(availability.biometricType)}
              </Text>
              <Text
                style={[
                  styles.statusText,
                  availability.isLockout ? styles.error : styles.success,
                ]}
              >
                Lockout: {availability.isLockout ? 'Yes' : 'No'}
              </Text>
              {availability.errorCode && (
                <Text style={styles.errorText}>
                  Error: {renderErrorCode(availability.errorCode)}
                </Text>
              )}
              {availability.errorMessage && (
                <Text style={styles.errorText}>
                  Message: {availability.errorMessage}
                </Text>
              )}
            </View>
          ) : (
            <Text style={styles.loadingText}>Loading...</Text>
          )}

          <TouchableOpacity
            style={styles.button}
            onPress={checkBiometricStatus}
            disabled={loading}
          >
            <Text style={styles.buttonText}>Refresh Status</Text>
          </TouchableOpacity>
        </View>

        {/* Permission Status */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Permission</Text>
          {permissionResult ? (
            <View style={styles.statusContainer}>
              <Text
                style={[
                  styles.statusText,
                  permissionResult.success ? styles.success : styles.error,
                ]}
              >
                Status: {permissionResult.success ? 'Granted' : 'Denied'}
              </Text>
              {permissionResult.errorCode && (
                <Text style={styles.errorText}>
                  Error: {renderErrorCode(permissionResult.errorCode)}
                </Text>
              )}
              {permissionResult.errorMessage && (
                <Text style={styles.errorText}>
                  Message: {permissionResult.errorMessage}
                </Text>
              )}
            </View>
          ) : (
            <Text style={styles.statusText}>Not requested yet</Text>
          )}

          <TouchableOpacity
            style={styles.button}
            onPress={handleRequestPermission}
            disabled={loading}
          >
            <Text style={styles.buttonText}>Request Permission</Text>
          </TouchableOpacity>
        </View>

        {/* Authentication */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Authentication</Text>

          {lastAuthResult && (
            <View style={styles.statusContainer}>
              <Text
                style={[
                  styles.statusText,
                  lastAuthResult.success ? styles.success : styles.error,
                ]}
              >
                Result: {lastAuthResult.success ? 'Success' : 'Failed'}
              </Text>
              {lastAuthResult.pressedOtherway && (
                <Text style={styles.statusText}>Other Way Pressed: Yes</Text>
              )}
              {lastAuthResult.errorCode && (
                <Text style={styles.errorText}>
                  Error: {renderErrorCode(lastAuthResult.errorCode)}
                </Text>
              )}
              {lastAuthResult.errorMessage && (
                <Text style={styles.errorText}>
                  Message: {lastAuthResult.errorMessage}
                </Text>
              )}
            </View>
          )}

          <TouchableOpacity
            style={styles.button}
            onPress={() => handleAuthenticate()}
            disabled={loading || !availability?.isAvailable}
          >
            <Text style={styles.buttonText}>Authenticate (Biometric Only)</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.button}
            onPress={() => handleAuthenticate({ otherwayWithPIN: true })}
            disabled={loading || !availability?.isAvailable}
          >
            <Text style={styles.buttonText}>
              Authenticate (With PIN Fallback)
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.button}
            onPress={() =>
              handleAuthenticate({
                otherwayWithPIN: false,
                textOtherway: 'Use Password',
              })
            }
            disabled={loading || !availability?.isAvailable}
          >
            <Text style={styles.buttonText}>
              Authenticate (Custom Fallback)
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollContent: {
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 20,
    color: '#333',
  },
  section: {
    backgroundColor: 'white',
    padding: 15,
    marginBottom: 15,
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  statusContainer: {
    marginBottom: 15,
  },
  statusText: {
    fontSize: 14,
    marginBottom: 5,
    color: '#333',
  },
  success: {
    color: '#4CAF50',
  },
  error: {
    color: '#F44336',
  },
  errorText: {
    fontSize: 12,
    color: '#F44336',
    fontStyle: 'italic',
  },
  loadingText: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
  },
  button: {
    backgroundColor: '#2196F3',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 5,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '500',
  },
});
