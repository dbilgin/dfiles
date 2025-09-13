import React from 'react';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {useAppContext} from '../context/AppContext';
import {lightTheme, darkTheme} from '../utils/theme';
import {FileManagerScreen} from '../screens/FileManagerScreen';
import {GalleryScreen} from '../screens/GalleryScreen';
import {SettingsScreen} from '../screens/SettingsScreen';
import {SafeAreaView} from 'react-native-safe-area-context';
import { StyleSheet } from 'react-native';

const Tab = createBottomTabNavigator();

export const TabNavigator = () => {
  const {state} = useAppContext();
  const theme = state.isDarkMode ? darkTheme : lightTheme;

  return (
    <SafeAreaView style={createStyles(theme).container}>
      <Tab.Navigator
        screenOptions={{
          headerShown: false,
          tabBarStyle: {
            backgroundColor: theme.colors.surface,
            borderTopColor: theme.colors.border,
            borderTopWidth: 1,
            height: 60,
            paddingBottom: 8,
            paddingTop: 8,
          },
          tabBarActiveTintColor: theme.colors.primary,
          tabBarInactiveTintColor: theme.colors.textSecondary,
          tabBarLabelStyle: {
            fontSize: 12,
            fontWeight: '500',
          },
        }}>
        <Tab.Screen
          name="FileManager"
          component={FileManagerScreen}
          options={{
            title: 'Files',
            tabBarIcon: ({color, size}) => (
              <Icon name="folder" size={size} color={color} />
            ),
          }}
        />
        <Tab.Screen
          name="Gallery"
          component={GalleryScreen}
          options={{
            title: 'Gallery',
            tabBarIcon: ({color, size}) => (
              <Icon name="photo-library" size={size} color={color} />
            ),
          }}
        />
        <Tab.Screen
          name="Settings"
          component={SettingsScreen}
          options={{
            title: 'Settings',
            tabBarIcon: ({color, size}) => (
              <Icon name="settings" size={size} color={color} />
            ),
          }}
        />
      </Tab.Navigator>
    </SafeAreaView>
  );
};

const createStyles = (theme: any) =>
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: theme.colors.surface,
    },
  });
