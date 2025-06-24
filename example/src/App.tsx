/**
 * Sample React Native App
 */

import React from 'react';
import { View, Text, StyleSheet, Button, SafeAreaView } from 'react-native';
import ReactNativeBiometrics, { BiometricOtherwayMode } from '@boindahood/react-native-biometrics'

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
      const result = await ReactNativeBiometrics.authenticateBiometric();
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

  const createBiometricKey = async () => {
    try {
      console.log('Testing create biometric key...');
      const result = await ReactNativeBiometrics.createBiometricKey();
      console.log('Create biometric key result:', result);
    } catch (error) {
      console.error('Error creating biometric key:', error);
    }
  };

  const createSignature = async () => {
    try {
      console.log('Testing create signature...');
      const payload = 'Test payload for signature';
      const result = await ReactNativeBiometrics.createSignature(payload, {
        titlePrompt: 'Sign with your biometric',
        otherwayText: 'Use PIN',
        otherwayWith: BiometricOtherwayMode.HIDE
      });
      console.log('Create signature result:', result);
    } catch (error) {
      console.error('Error creating signature:', error);
    }
  };

  const deleteBiometricKey = async () => {
    try {
      console.log('Testing delete biometric key...');
      const result = await ReactNativeBiometrics.deleteBiometricKey();
      console.log('Delete biometric key result:', result);
    } catch (error) {
      console.error('Error deleting biometric key:', error);
    }
  };

  const biometricKeyExists = async () => {
    try {
      console.log('Testing check if biometric key exists...');
      const result = await ReactNativeBiometrics.biometricKeyExists();
      console.log('Biometric key exists result:', result);
    } catch (error) {
      console.error('Error checking biometric key exists:', error);
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

        <View style={styles.separator} />

        <View style={styles.buttonContainer}>
          <Button
            title="Check Key Exists"
            onPress={biometricKeyExists}
            color="#9C27B0"
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Create Biometric Key"
            onPress={createBiometricKey}
            color="#607D8B"
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Create Signature"
            onPress={createSignature}
            color="#795548"
          />
        </View>

        <View style={styles.buttonContainer}>
          <Button
            title="Delete Biometric Key"
            onPress={deleteBiometricKey}
            color="#F44336"
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
  separator: {
    height: 20,
    width: 1000,
    backgroundColor: '#666666',
    marginBottom: 15,
  },
});

export default App;
