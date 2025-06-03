# dfiles - React Native File Manager

A modern, feature-rich file manager app for Android built with React Native and TypeScript.

## Features

- **File Browsing**: Navigate through your device storage with an intuitive interface
- **Recent Files**: Quick access to your 5 most recently modified files on the home screen
- **File Operations**: Copy, cut, paste, rename, delete, and share files and folders
- **Compression**: Create ZIP archives and extract compressed files
- **Trash System**: Safe file deletion with restore capability - files go to trash instead of permanent deletion
- **File Info**: View detailed metadata including size, modification date, and file type
- **Bulk Operations**: Multi-select files for batch operations
- **Create Folders**: Add new folders with the floating action button
- **Dark/Light Theme**: Automatic theme switching with custom alerts that respect your theme choice
- **Comprehensive Permissions**: Full storage access with Android 11+ MANAGE_EXTERNAL_STORAGE support

## Tech Stack

- React Native 0.79.2 with TypeScript
- Custom themed UI components
- Native Android modules for storage permissions
- RNFS for file system operations
- React Native Zip Archive for compression

<img src="https://github.com/user-attachments/assets/a2d357f2-5779-4f56-bae5-42990c058280" width="300" />

---

This is a [**React Native**](https://reactnative.dev) project, bootstrapped using [`@react-native-community/cli`](https://github.com/react-native-community/cli).

# Getting Started

> **Note**: Make sure you have completed the [Set Up Your Environment](https://reactnative.dev/docs/set-up-your-environment) guide before proceeding.

## Step 1: Start Metro

First, you will need to run **Metro**, the JavaScript build tool for React Native.

To start the Metro dev server, run the following command from the root of your React Native project:

```sh
# Using npm
npm start

# OR using Yarn
yarn start
```

## Step 2: Build and run your app

With Metro running, open a new terminal window/pane from the root of your React Native project, and use one of the following commands to build and run your Android or iOS app:

### Android

```sh
# Using npm
npm run android

# OR using Yarn
yarn android
```
