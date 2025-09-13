import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Image,
  RefreshControl,
  Dimensions,
} from 'react-native';
import ImageView from 'react-native-image-viewing';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {useAppContext} from '../context/AppContext';
import {useCustomAlert} from '../hooks/useCustomAlert';
import {lightTheme, darkTheme} from '../utils/theme';
import {readDirectory, moveToTrash} from '../utils/fileUtils';
import RNFS from 'react-native-fs';
import {CustomAlert} from '../components/CustomAlert';
import Share from 'react-native-share';

interface ImageFolder {
  name: string;
  path: string;
  images: ImageFile[];
  thumbnail?: string;
}

interface ImageFile {
  name: string;
  path: string;
  size: number;
  mtime: number;
}

export const GalleryScreen = () => {
  const {state} = useAppContext();
  const {showAlert, alertState, hideAlert} = useCustomAlert();
  const [imageFolders, setImageFolders] = useState<ImageFolder[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [collapsedFolders, setCollapsedFolders] = useState<Set<string>>(
    new Set(),
  );
  const [selectedImage, setSelectedImage] = useState<ImageFile | null>(null);
  const [imageModalVisible, setImageModalVisible] = useState(false);

  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const styles = createStyles(theme);

  const scanSubdirectories = useCallback(
    async (basePath: string, folders: ImageFolder[]) => {
      try {
        // Check if base directory exists first
        const exists = await RNFS.exists(basePath);
        if (!exists) {
          return;
        }

        const files = await readDirectory(basePath);
        const subdirs = files.filter(file => file.isDirectory);

        for (const subdir of subdirs) {
          try {
            const subdirPath = subdir.path;

            // Check if subdirectory exists before reading
            const subdirExists = await RNFS.exists(subdirPath);
            if (!subdirExists) {
              continue;
            }

            const subdirFiles = await readDirectory(subdirPath);
            const imageFiles: ImageFile[] = subdirFiles
              .filter(file => !file.isDirectory && isImageFile(file.name))
              .map(file => ({
                name: file.name,
                path: file.path,
                size: file.size,
                mtime: file.mtime.getTime(),
              }));

            if (imageFiles.length > 0) {
              const folderName = getFolderDisplayName(subdirPath);
              folders.push({
                name: folderName,
                path: subdirPath,
                images: imageFiles.sort((a, b) => b.mtime - a.mtime),
                thumbnail: imageFiles[0]?.path,
              });
            }
          } catch (error) {
            // Silently skip inaccessible subdirectories
          }
        }
      } catch (error) {
        // Silently skip if base directory is inaccessible
      }
    },
    [],
  );

  const scanForImages = async () => {
    try {
      setLoading(true);
      const folders: ImageFolder[] = [];

      // Common image directories to scan
      const imageDirectories = [
        '/storage/emulated/0/DCIM',
        '/storage/emulated/0/Pictures',
        '/storage/emulated/0/Movies',
        '/storage/emulated/0/Download',
        '/storage/emulated/0/WhatsApp/Media/WhatsApp Images',
        '/storage/emulated/0/Android/data/com.whatsapp/files/WhatsApp/Media/WhatsApp Images',
        '/storage/emulated/0/Telegram/Telegram Images',
        '/storage/emulated/0/Android/data/org.telegram.messenger/files/Telegram/Telegram Images',
        '/storage/emulated/0/Instagram',
        '/storage/emulated/0/Android/data/com.instagram.android/files/Instagram',
        '/storage/emulated/0/Android/data/com.instagram.android/cache',
      ];

      for (const dirPath of imageDirectories) {
        try {
          // Check if directory exists before trying to read it
          const exists = await RNFS.exists(dirPath);
          if (!exists) {
            continue;
          }

          const files = await readDirectory(dirPath);
          const imageFiles: ImageFile[] = files
            .filter(file => !file.isDirectory && isImageFile(file.name))
            .map(file => ({
              name: file.name,
              path: file.path,
              size: file.size,
              mtime: file.mtime.getTime(),
            }));

          if (imageFiles.length > 0) {
            const folderName = getFolderDisplayName(dirPath);
            folders.push({
              name: folderName,
              path: dirPath,
              images: imageFiles.sort((a, b) => b.mtime - a.mtime), // Sort by newest first
              thumbnail: imageFiles[0]?.path, // Use first image as thumbnail
            });
          }
        } catch (error) {
          // Silently skip directories that can't be accessed
          console.log(`Cannot access directory: ${dirPath}`);
        }
      }

      // Also scan subdirectories of DCIM and Pictures
      await scanSubdirectories('/storage/emulated/0/DCIM', folders);
      await scanSubdirectories('/storage/emulated/0/Pictures', folders);
      await scanSubdirectories('/storage/emulated/0/Movies', folders);

      setImageFolders(
        folders.sort((a, b) => b.images.length - a.images.length),
      );
    } catch (error) {
      console.error('Error scanning for images:', error);
      showAlert('Error', 'Failed to load images');
    } finally {
      setLoading(false);
    }
  };


  const isVideoFile = (filename: string) => {
    const videoExtensions = ['.mp4', '.avi', '.mov', '.mkv', '.webm'];
    return videoExtensions.some(ext => filename.toLowerCase().endsWith(ext));
  };

  const isImageFile = (filename: string): boolean => {
    const imageExtensions = [
      'jpg',
      'jpeg',
      'png',
      'gif',
      'bmp',
      'webp',
      'heic',
      'heif',
      'mp4',
      'avi',
      'mov',
      'mkv',
      'webm',
    ];
    const extension = filename.toLowerCase().split('.').pop();
    return extension ? imageExtensions.includes(extension) : false;
  };

  const getFolderDisplayName = (path: string): string => {
    const pathParts = path.split('/');
    const folderName = pathParts[pathParts.length - 1];

    // Map common folder names to more user-friendly names
    const folderNameMap: {[key: string]: string} = {
      DCIM: 'Camera',
      Pictures: 'Pictures',
      Download: 'Downloads',
      'WhatsApp Images': 'WhatsApp',
      'Telegram Images': 'Telegram',
      Instagram: 'Instagram',
      cache: 'Instagram Cache',
    };

    return folderNameMap[folderName] || folderName;
  };

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await scanForImages();
    setRefreshing(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const toggleFolderCollapse = (folderPath: string) => {
    setCollapsedFolders(prev => {
      const newSet = new Set(prev);
      if (newSet.has(folderPath)) {
        newSet.delete(folderPath);
      } else {
        newSet.add(folderPath);
      }
      return newSet;
    });
  };

  const handleImagePress = (image: ImageFile) => {
    setSelectedImage(image);
    setImageModalVisible(true);
  };

  const handleShareImage = async () => {
    if (!selectedImage) {
      return;
    }

    await Share.open({
      url: `file://${selectedImage.path}`,
      title: selectedImage.name,
    }).catch(err => {
      console.error(err);
    });
  };

  const handleDeleteImage = async () => {
    if (!selectedImage) {
      return;
    }

    showAlert('Delete Image', `Move "${selectedImage.name}" to trash?`, [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            const success = await moveToTrash(selectedImage.path);
            if (success) {
              setImageModalVisible(false);
              setSelectedImage(null);
              // Refresh the gallery
              await scanForImages();
            } else {
              showAlert('Error', 'Failed to delete image');
            }
          } catch (error) {
            console.error('Delete error:', error);
            showAlert('Error', 'Failed to delete image');
          }
        },
      },
    ]);
  };

  useEffect(() => {
    scanForImages();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run once on mount

  const renderImageItem = ({item}: {item: ImageFile}) => (
    <TouchableOpacity
      style={styles.imageItem}
      onPress={() => handleImagePress(item)}>
      <Image
        source={{uri: `file://${item.path}`}}
        style={styles.imageThumbnail}
        resizeMode="cover"
      />
      {isVideoFile(item.name) && (
        <Icon
          style={styles.videoIcon}
          name={'play-circle'}
          size={24}
          color={theme.colors.background}
        />
      )}
    </TouchableOpacity>
  );

  const renderFolderSection = ({item}: {item: ImageFolder}) => {
    const folder = item;
    const isCollapsed = collapsedFolders.has(folder.path);

    return (
      <View key={folder.path} style={styles.folderSection}>
        <TouchableOpacity
          style={styles.folderHeader}
          onPress={() => toggleFolderCollapse(folder.path)}>
          <View style={styles.folderHeaderLeft}>
            <Icon
              name={
                isCollapsed ? 'keyboard-arrow-right' : 'keyboard-arrow-down'
              }
              size={24}
              color={theme.colors.text}
            />
            <Text style={styles.folderTitle}>{folder.name}</Text>
            <Text style={styles.folderCount}>({folder.images.length})</Text>
          </View>
          {folder.thumbnail && (
            <Image
              source={{uri: `file://${folder.thumbnail}`}}
              style={styles.folderThumbnail}
              resizeMode="cover"
            />
          )}
        </TouchableOpacity>

        {!isCollapsed && (
          <FlatList
            data={folder.images}
            renderItem={renderImageItem}
            keyExtractor={i => i.path}
            numColumns={3}
            contentContainerStyle={styles.imagesGrid}
            scrollEnabled={false}
            showsVerticalScrollIndicator={false}
          />
        )}
      </View>
    );
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <Icon name="photo-library" size={64} color={theme.colors.icon} />
        <Text style={styles.loadingText}>Loading images...</Text>
      </View>
    );
  }

  if (imageFolders.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Icon name="photo-library" size={64} color={theme.colors.icon} />
        <Text style={styles.emptyTitle}>No Images Found</Text>
        <Text style={styles.emptySubtitle}>
          Make sure you have images in your device storage
        </Text>
        <TouchableOpacity style={styles.refreshButton} onPress={onRefresh}>
          <Text style={styles.refreshButtonText}>Refresh</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Gallery</Text>
        <Text style={styles.headerSubtitle}>
          {imageFolders.reduce(
            (total, folder) => total + folder.images.length,
            0,
          )}{' '}
          images in {imageFolders.length} folders
        </Text>
      </View>

      <FlatList
        style={styles.scrollView}
        data={imageFolders}
        renderItem={renderFolderSection}
        keyExtractor={item => item.path}
        numColumns={1}
        scrollEnabled
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
            tintColor={theme.colors.primary}
          />
        }
      />

      <ImageView
        images={[
          {
            uri: `file://${selectedImage?.path}`,
          },
        ]}
        imageIndex={0}
        visible={imageModalVisible}
        onRequestClose={() => setImageModalVisible(false)}
        FooterComponent={() => (
          <View style={styles.imageModalActions}>
            <TouchableOpacity
              style={styles.imageActionButton}
              onPress={handleShareImage}>
              <Icon name="share" size={24} color={theme.colors.background} />
              <Text style={styles.imageActionText}>Share</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.imageActionButton, styles.deleteButton]}
              onPress={handleDeleteImage}>
              <Icon name="delete" size={24} color="#fff" />
              <Text style={[styles.imageActionText, styles.deleteButtonText]}>
                Delete
              </Text>
            </TouchableOpacity>
          </View>
        )}
      />

      <CustomAlert
        visible={alertState.visible}
        title={alertState.title}
        message={alertState.message}
        buttons={alertState.buttons}
        onDismiss={hideAlert}
      />
    </View>
  );
};

const {width: screenWidth, height: screenHeight} = Dimensions.get('window');
const imageSize = (screenWidth - 60) / 3; // 3 columns with margins

const createStyles = (theme: any) =>
  StyleSheet.create({
    videoIcon: {position: 'absolute', top: 0, left: 0},
    container: {
      flex: 1,
      backgroundColor: theme.colors.background,
    },
    loadingContainer: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
      backgroundColor: theme.colors.background,
    },
    loadingText: {
      fontSize: 16,
      color: theme.colors.text,
      marginTop: 16,
    },
    emptyContainer: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
      backgroundColor: theme.colors.background,
      paddingHorizontal: 32,
    },
    emptyTitle: {
      fontSize: 24,
      fontWeight: 'bold',
      color: theme.colors.text,
      marginTop: 16,
    },
    emptySubtitle: {
      fontSize: 16,
      color: theme.colors.textSecondary,
      textAlign: 'center',
      marginTop: 8,
      lineHeight: 22,
    },
    refreshButton: {
      backgroundColor: theme.colors.primary,
      paddingHorizontal: 24,
      paddingVertical: 12,
      borderRadius: 8,
      marginTop: 24,
    },
    refreshButtonText: {
      color: theme.colors.background,
      fontSize: 16,
      fontWeight: '600',
    },
    header: {
      paddingHorizontal: 16,
      paddingVertical: 16,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    headerTitle: {
      fontSize: 28,
      fontWeight: 'bold',
      color: theme.colors.text,
    },
    headerSubtitle: {
      fontSize: 14,
      color: theme.colors.textSecondary,
      marginTop: 4,
    },
    scrollView: {
      flex: 1,
    },
    folderSection: {
      marginBottom: 16,
      flex: 1,
    },
    folderHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: 16,
      paddingVertical: 12,
      backgroundColor: theme.colors.surface,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    folderHeaderLeft: {
      flexDirection: 'row',
      alignItems: 'center',
      flex: 1,
    },
    folderTitle: {
      fontSize: 18,
      fontWeight: '600',
      color: theme.colors.text,
      marginLeft: 8,
    },
    folderCount: {
      fontSize: 14,
      color: theme.colors.textSecondary,
      marginLeft: 4,
    },
    folderThumbnail: {
      width: 40,
      height: 40,
      borderRadius: 8,
      marginLeft: 12,
    },
    imagesGrid: {
      padding: 8,
    },
    imageItem: {
      margin: 2,
    },
    imageThumbnail: {
      width: imageSize,
      height: imageSize,
      borderRadius: 8,
    },
    // Full-screen image modal styles
    imageModalOverlay: {
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.9)',
      justifyContent: 'center',
      alignItems: 'center',
    },
    imageModalCloseArea: {
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
    },
    imageModalContent: {
      flex: 1,
      width: '100%',
      justifyContent: 'center',
      alignItems: 'center',
    },
    fullScreenImage: {
      width: screenWidth,
      height: screenHeight * 0.8,
    },
    imageModalActions: {
      position: 'absolute',
      bottom: 50,
      left: 0,
      right: 0,
      flexDirection: 'row',
      justifyContent: 'center',
      paddingHorizontal: 32,
    },
    imageActionButton: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: theme.colors.primary,
      paddingHorizontal: 24,
      paddingVertical: 12,
      borderRadius: 25,
      marginHorizontal: 8,
    },
    deleteButton: {
      backgroundColor: '#F44336',
    },
    imageActionText: {
      color: theme.colors.background,
      fontSize: 16,
      fontWeight: '600',
      marginLeft: 8,
    },
    deleteButtonText: {
      color: '#fff',
    },
  });
