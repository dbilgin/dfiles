# dfiles - Android File Manager & Gallery

A simple, offline file manager and gallery app for Android built with Kotlin and Jetpack Compose.

## Features

- **File Management**: Browse, copy, cut, paste, rename, delete, and share files and folders
- **Batch Operations**: Multi-select files for bulk operations (delete, share, move)
- **Gallery**: Categorized view of images and videos grouped by folder with expandable sections
- **Media Viewing**:
  - Full-screen image viewer with swipe navigation and zoom
  - In-app video player with swipe between videos
  - Video thumbnail support
- **APK Installation**: Direct APK installation support
- **Deep Links**: Open files and folders from other apps
- **Offline Only**: No internet permissions - completely offline functionality

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern UI framework
- **Material 3** - Material Design components
- **MVVM Architecture** - ViewModel pattern with Repository
- **Coil** - Image loading and video thumbnails
- **Navigation Component** - Compose Navigation for routing

## Requirements

- Android 8.0 (API 26) or higher
- Storage permissions (MANAGE_EXTERNAL_STORAGE for Android 11+)

## Building

1. Clone the repository:

```bash
git clone https://github.com/dbilgin/dfiles.git
cd dfiles
```

2. Open in Android Studio or build from command line:

```bash
./gradlew assembleDebug
```

## Permissions

The app requires storage access to function as a file manager:

- **MANAGE_EXTERNAL_STORAGE** (Android 11+) - Full file system access
- **READ_EXTERNAL_STORAGE** (Android 10 and below)
- **WRITE_EXTERNAL_STORAGE** (Android 9 and below)
- **REQUEST_INSTALL_PACKAGES** - For APK installation
- **QUERY_ALL_PACKAGES** - To find apps for opening files
