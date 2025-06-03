/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useEffect, useState, useCallback} from 'react';
import {View, Text, StyleSheet, ActivityIndicator, AppState} from 'react-native';
import {NavigationContainer} from '@react-navigation/native';
import {createStackNavigator} from '@react-navigation/stack';
import {GestureHandlerRootView} from 'react-native-gesture-handler';
import {AppProvider} from './src/context/AppContext';
import {FileManagerScreen} from './src/screens/FileManagerScreen';
import {SettingsScreen} from './src/screens/SettingsScreen';
import {RootStackParamList} from './src/types';
import {requestStoragePermission, checkStoragePermission} from './src/utils/permissions';

const Stack = createStackNavigator<RootStackParamList>();

function App(): React.JSX.Element {
  const [permissionStatus, setPermissionStatus] = useState<'checking' | 'granted' | 'denied'>('checking');

  const handlePermissionRequest = useCallback(async () => {
    try {
      // First check if we already have permissions
      const hasPermission = await checkStoragePermission();
      if (hasPermission) {
        setPermissionStatus('granted');
        return;
      }

      // Request permissions if we don't have them
      const result = await requestStoragePermission();
      if (result.granted) {
        setPermissionStatus('granted');
      } else {
        // Don't show the old alert - the new dialog handles this
        setPermissionStatus('denied');
      }
    } catch (error) {
      console.error('Error handling permissions:', error);
      setPermissionStatus('denied');
    }
  }, []);

  const handleAppStateChange = useCallback(async (nextAppState: string) => {
    // When app comes back to foreground, check permissions again
    if (nextAppState === 'active' && permissionStatus === 'denied') {
      const hasPermission = await checkStoragePermission();
      if (hasPermission) {
        setPermissionStatus('granted');
      }
    }
  }, [permissionStatus]);

  useEffect(() => {
    handlePermissionRequest();
  }, [handlePermissionRequest]);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription?.remove();
  }, [handleAppStateChange]);

  if (permissionStatus === 'checking') {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>Checking permissions...</Text>
      </View>
    );
  }

  if (permissionStatus === 'denied') {
    return (
      <View style={styles.permissionContainer}>
        <Text style={styles.permissionTitle}>Waiting for Permission</Text>
        <Text style={styles.permissionText}>
          Please enable "All files access" for dfiles in your device settings, then return to the app.
        </Text>
      </View>
    );
  }

  return (
    <GestureHandlerRootView style={{flex: 1}}>
      <AppProvider>
        <NavigationContainer>
          <Stack.Navigator
            screenOptions={{
              headerShown: false,
            }}>
            <Stack.Screen name="FileManager" component={FileManagerScreen} />
            <Stack.Screen name="Settings" component={SettingsScreen} />
          </Stack.Navigator>
        </NavigationContainer>
      </AppProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  permissionContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#fff',
  },
  permissionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
    color: '#333',
  },
  permissionText: {
    fontSize: 16,
    textAlign: 'center',
    color: '#666',
    lineHeight: 24,
  },
});

export default App;
