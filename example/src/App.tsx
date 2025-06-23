/**
 * Sample React Native App
 */

import { View, Text, StyleSheet, Button, SafeAreaView } from 'react-native';
import ReactNativeBiometrics from '@boindahood/react-native-biometrics';

function App() {
  console.log('App is starting...');

  const checkBiometricAvailability = async () => {
    try {
      console.log('Testing check biometric availability...');
      const result = await ReactNativeBiometrics.checkBiometricAvailability();
      console.log('Biometric availability:', result);
    } catch (error) {
      console.error('Error checking biometric availability:', error);
    }
  };

  const requestPermission = async () => {
    try {
      console.log('Testing request biometric permission...');
      const result = await ReactNativeBiometrics.requestBiometricPermission();
      console.log('Request permission result:', result);
    } catch (error) {
      console.error('Error requesting biometric permission:', error);
    }
  };

  const authenticateBiometric = async () => {
    try {
      console.log('Testing authentication...');
      const result = await ReactNativeBiometrics.authenticateBiometric({
        otherwayWithPIN: false,
      });
      console.log('Authentication result:', result);
    } catch (error) {
      console.error('Error during authentication:', error);
    }
  };

  const authenticatePIN = async () => {
    try {
      console.log('Testing authenticate with PIN...');
      const result = await ReactNativeBiometrics.authenticatePIN();
      console.log('Authenticate with PIN result:', result);
    } catch (error) {
      console.error('Error authenticating with PIN:', error);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>React Native Biometrics</Text>
        <Text style={styles.subtitle}>TurboModule Test App</Text>

        <View style={styles.buttonContainer}>
          <Button
            title="Check Biometric"
            onPress={checkBiometricAvailability}
            color="#2196F3"
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Request Permission"
            onPress={requestPermission}
            color="#4CAF50"
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Authenticate Biometric"
            onPress={authenticateBiometric}
            color="#FF9800"
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Authenticate with PIN"
            onPress={authenticatePIN}
            color="#4CAF50"
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 10,
    textAlign: 'center',
    color: '#333',
  },
  subtitle: {
    fontSize: 16,
    marginBottom: 30,
    textAlign: 'center',
    color: '#666',
  },
  buttonContainer: {
    marginBottom: 15,
    width: '80%',
  },
});

export default App;
