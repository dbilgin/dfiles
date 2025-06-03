import {
  Platform,
  PermissionsAndroid,
  Alert,
  Linking,
  NativeModules,
} from 'react-native';
import {PermissionStatus} from '../types';

const {ManageStorageModule} = NativeModules;

export const requestStoragePermission = async (): Promise<PermissionStatus> => {
  if (Platform.OS !== 'android') {
    return {granted: true, canRequestAgain: false};
  }

  try {
    const androidVersion = Platform.Version;

    if (androidVersion >= 30) {
      // Android 11+ - Check if we already have the permission
      const hasPermission = await ManageStorageModule.hasPermission();

      if (hasPermission) {
        return {granted: true, canRequestAgain: false};
      }

      // Show dialog explaining the permission and how to grant it
      Alert.alert(
        'All Files Access Permission Required',
        'This file manager needs access to all files to display documents, APK files, and other content.\n\nYou will be taken to the settings page where you can enable "All files access" for this app.',
        [
          {
            text: 'Cancel',
            style: 'cancel',
            onPress: () => {
              // User cancelled - set to denied
            },
          },
          {
            text: 'Open Settings',
            onPress: async () => {
              try {
                await ManageStorageModule.requestPermission();
              } catch (error) {
                console.error('Error requesting permission:', error);
              }
            },
          },
        ],
      );

      // Return pending status - user needs to manually enable in settings
      return {granted: false, canRequestAgain: true};
    } else {
      // Android 10 and below - Use regular storage permissions
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
        {
          title: 'Storage Permission',
          message: 'This app needs access to your storage to view files.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        },
      );

      return {
        granted: granted === PermissionsAndroid.RESULTS.GRANTED,
        canRequestAgain: granted !== PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN,
      };
    }
  } catch (error) {
    console.error('Permission request error:', error);
    return {granted: false, canRequestAgain: true};
  }
};

export const checkStoragePermission = async (): Promise<boolean> => {
  if (Platform.OS !== 'android') {
    return true;
  }

  try {
    const androidVersion = Platform.Version;

    if (androidVersion >= 30) {
      // Android 11+ - Use native module to check permission
      return await ManageStorageModule.hasPermission();
    } else {
      // Android 10 and below - Check regular storage permission
      return await PermissionsAndroid.check(
        PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
      );
    }
  } catch (error) {
    console.error('Permission check error:', error);
    return false;
  }
};

export const openAppSettings = async () => {
  try {
    if (Platform.OS === 'android') {
      await Linking.openSettings();
    }
  } catch (error) {
    console.error('Error opening app settings:', error);
    Alert.alert(
      'Settings Error',
      'Could not open app settings. Please manually go to Settings > Apps > dfiles > Permissions and enable storage permissions.',
    );
  }
};

export const showPermissionDeniedAlert = () => {
  Alert.alert(
    'Storage Permission Required',
    'This app needs storage permission to access and manage files. Please grant the permission to continue using the app.',
    [
      {
        text: 'Cancel',
        style: 'cancel',
      },
      {
        text: 'Open Settings',
        onPress: openAppSettings,
      },
    ],
  );
};
