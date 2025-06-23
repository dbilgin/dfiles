import React, {useState} from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  TouchableWithoutFeedback,
  Modal,
  NativeModules,
} from 'react-native';
import Share from 'react-native-share';
import FileViewer from 'react-native-file-viewer';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {FileItem as FileItemType} from '../types';
import {formatFileSize, formatDate} from '../utils/fileUtils';
import {useAppContext} from '../context/AppContext';
import {lightTheme, darkTheme} from '../utils/theme';

const {ApkInstaller} = NativeModules;

interface FileItemProps {
  item: FileItemType;
  onPress: (item: FileItemType) => void;
  onLongPress: (item: FileItemType) => void;
  onRename?: (item: FileItemType) => void;
  onInfo?: (item: FileItemType) => void;
  onCompress?: (item: FileItemType) => void;
  onDecompress?: (item: FileItemType) => void;
  onDelete?: (item: FileItemType) => void;
  onDeletePermanently?: (item: FileItemType) => void;
  showAlert?: (
    title: string,
    message?: string,
    buttons?: Array<{
      text: string;
      onPress?: () => void;
      style?: 'default' | 'cancel' | 'destructive';
    }>,
  ) => void;
}

const getFileIcon = (item: FileItemType): string => {
  if (item.isDirectory) {
    return 'folder';
  }

  switch (item.type) {
    case 'image':
      return 'image';
    case 'video':
      return 'movie';
    case 'audio':
      return 'music-note';
    case 'document':
      return 'description';
    case 'apk':
      return 'android';
    default:
      return 'insert-drive-file';
  }
};

const getFileIconColor = (item: FileItemType, theme: any): string => {
  if (item.isDirectory) {
    return '#FFA726';
  }

  switch (item.type) {
    case 'image':
      return '#4CAF50';
    case 'video':
      return '#F44336';
    case 'audio':
      return '#9C27B0';
    case 'document':
      return '#2196F3';
    case 'apk':
      return '#3DDC84';
    default:
      return theme.colors.icon;
  }
};

export const FileItem: React.FC<FileItemProps> = ({
  item,
  onPress,
  onLongPress,
  onRename,
  onInfo,
  onCompress,
  onDecompress,
  onDelete,
  onDeletePermanently,
  showAlert,
}) => {
  const {state, dispatch} = useAppContext();
  const [showMenu, setShowMenu] = useState(false);
  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const isSelected = state.selectedFiles.includes(item.path);

  const styles = createStyles(theme);

  const handlePress = () => {
    if (state.isSelectionMode) {
      onLongPress(item);
    } else {
      onPress(item);
    }
  };

  const handleMenuPress = () => {
    setShowMenu(true);
  };

  const handleShare = async () => {
    setShowMenu(false);
    try {
      const shareOptions = {
        title: 'Share File',
        urls: [`file://${item.path}`],
      };
      await Share.open(shareOptions);
    } catch (error) {
      // Don't show alert if user cancels sharing
      if (error && typeof error === 'object' && 'message' in error) {
        const errorMessage = (error as any).message;
        if (
          !errorMessage.includes('User did not share') &&
          !errorMessage.includes('cancelled')
        ) {
          if (showAlert) {
            showAlert('Error', 'Failed to share file');
          }
        }
      }
    }
  };

  const handleCopy = () => {
    setShowMenu(false);
    dispatch({
      type: 'SET_CLIPBOARD',
      payload: {files: [item.path], operation: 'copy'},
    });
    if (showAlert) {
      showAlert('Copied', `${item.name} copied`);
    }
  };

  const handleCut = () => {
    setShowMenu(false);
    dispatch({
      type: 'SET_CLIPBOARD',
      payload: {files: [item.path], operation: 'cut'},
    });
    if (showAlert) {
      showAlert('Cut', `${item.name} cut`);
    }
  };

  const handleOpen = () => {
    setShowMenu(false);
    if (!item.isDirectory) {
      FileViewer.open(item.path).catch(() => {
        // Silently fail if no app can open the file
      });
    }
  };

  const handleRename = () => {
    setShowMenu(false);
    if (onRename) {
      onRename(item);
    }
  };

  const handleInfo = () => {
    setShowMenu(false);
    if (onInfo) {
      onInfo(item);
    }
  };

  const handleCompress = () => {
    setShowMenu(false);
    if (onCompress) {
      onCompress(item);
    }
  };

  const handleDecompress = () => {
    setShowMenu(false);
    if (onDecompress) {
      onDecompress(item);
    }
  };

  const handleDelete = () => {
    setShowMenu(false);
    if (onDelete) {
      onDelete(item);
    }
  };

  const handleDeletePermanently = () => {
    setShowMenu(false);
    if (onDeletePermanently) {
      onDeletePermanently(item);
    }
  };

  const handleInstall = () => {
    setShowMenu(false);
    if (item.type === 'apk') {
      ApkInstaller.installApk(item.path)
        .then(() => {
          // APK installed successfully
        })
        .catch((error: any) => {
          console.error('APK installation failed:', error);
          if (showAlert) {
            showAlert('Error', 'Failed to install APK');
          }
        });
    }
  };

  // Check if file is a supported archive format
  const isArchiveFile = () => {
    const extension = item.name.toLowerCase().split('.').pop();
    return ['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(extension || '');
  };

  // Check if file is an APK
  const isApkFile = () => {
    return item.type === 'apk';
  };

  return (
    <>
      <TouchableWithoutFeedback
        onPress={handlePress}
        onLongPress={() => onLongPress(item)}>
        <View
          style={[styles.container, isSelected && styles.selectedContainer]}>
          <View style={styles.leftSection}>
            {state.isSelectionMode && (
              <View style={styles.checkboxContainer}>
                <Icon
                  name={isSelected ? 'check-circle' : 'radio-button-unchecked'}
                  size={24}
                  color={isSelected ? theme.colors.primary : theme.colors.icon}
                />
              </View>
            )}
            <View style={styles.iconContainer}>
              <Icon
                name={getFileIcon(item)}
                size={32}
                color={getFileIconColor(item, theme)}
              />
            </View>
            <View style={styles.textContainer}>
              <Text
                style={[styles.fileName, isSelected && styles.selectedText]}
                numberOfLines={1}
                ellipsizeMode="middle">
                {item.name}
              </Text>
              <View style={styles.detailsContainer}>
                <Text style={styles.fileDetails}>
                  {item.isDirectory
                    ? 'Folder'
                    : `${formatFileSize(item.size)} • ${formatDate(
                        item.mtime,
                      )}`}
                </Text>
              </View>
            </View>
          </View>
          {!state.isSelectionMode && (
            <TouchableOpacity style={styles.moreButton} onPress={handleMenuPress}>
              <Icon name="more-vert" size={20} color={theme.colors.icon} />
            </TouchableOpacity>
          )}
        </View>
      </TouchableWithoutFeedback>

      <Modal
        visible={showMenu}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowMenu(false)}>
        <TouchableWithoutFeedback onPress={() => setShowMenu(false)}>
          <View style={styles.modalOverlay}>
            <View style={styles.menuContainer}>
              {!item.isDirectory && (
                <TouchableOpacity style={styles.menuItem} onPress={handleOpen}>
                  <Icon
                    name="open-in-new"
                    size={20}
                    color={theme.colors.text}
                  />
                  <Text style={styles.menuText}>Open</Text>
                </TouchableOpacity>
              )}
              <TouchableOpacity style={styles.menuItem} onPress={handleShare}>
                <Icon name="share" size={20} color={theme.colors.text} />
                <Text style={styles.menuText}>Share</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.menuItem} onPress={handleCopy}>
                <Icon name="content-copy" size={20} color={theme.colors.text} />
                <Text style={styles.menuText}>Copy</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.menuItem} onPress={handleCut}>
                <Icon name="content-cut" size={20} color={theme.colors.text} />
                <Text style={styles.menuText}>Cut</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.menuItem} onPress={handleRename}>
                <Icon name="edit" size={20} color={theme.colors.text} />
                <Text style={styles.menuText}>Rename</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.menuItem}
                onPress={handleCompress}>
                <Icon name="archive" size={20} color={theme.colors.text} />
                <Text style={styles.menuText}>Compress</Text>
              </TouchableOpacity>
              {!item.isDirectory && isArchiveFile() && (
                <TouchableOpacity
                  style={styles.menuItem}
                  onPress={handleDecompress}>
                  <Icon name="unarchive" size={20} color={theme.colors.text} />
                  <Text style={styles.menuText}>Extract</Text>
                </TouchableOpacity>
              )}
              <TouchableOpacity style={styles.menuItem} onPress={handleInfo}>
                <Icon name="info" size={20} color={theme.colors.text} />
                <Text style={styles.menuText}>Info</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.menuItem} onPress={handleDelete}>
                <Icon name="delete" size={20} color={theme.colors.text} />
                <Text style={styles.menuText}>Delete</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.menuItem} onPress={handleDeletePermanently}>
                <Icon name="delete-forever" size={20} color="#F44336" />
                <Text style={[styles.menuText, {color: '#F44336'}]}>Delete Permanently</Text>
              </TouchableOpacity>
              {isApkFile() && (
                <TouchableOpacity style={styles.menuItem} onPress={handleInstall}>
                  <Icon name="install" size={20} color={theme.colors.text} />
                  <Text style={styles.menuText}>Install</Text>
                </TouchableOpacity>
              )}
            </View>
          </View>
        </TouchableWithoutFeedback>
      </Modal>
    </>
  );
};

const createStyles = (theme: any) =>
  StyleSheet.create({
    container: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: theme.spacing.md,
      paddingVertical: theme.spacing.sm,
      backgroundColor: theme.colors.background,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    selectedContainer: {
      backgroundColor: theme.colors.selected,
    },
    leftSection: {
      flex: 1,
      flexDirection: 'row',
      alignItems: 'center',
    },
    checkboxContainer: {
      marginRight: theme.spacing.sm,
    },
    iconContainer: {
      marginRight: theme.spacing.md,
    },
    textContainer: {
      flex: 1,
    },
    fileName: {
      fontSize: theme.fontSize.md,
      fontWeight: '500',
      color: theme.colors.text,
      marginBottom: 2,
    },
    selectedText: {
      color: theme.colors.selectedText,
    },
    detailsContainer: {
      flexDirection: 'row',
      alignItems: 'center',
    },
    fileDetails: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.textSecondary,
    },
    moreButton: {
      padding: theme.spacing.xs,
    },
    modalOverlay: {
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      justifyContent: 'center',
      alignItems: 'center',
    },
    menuContainer: {
      backgroundColor: theme.colors.background,
      padding: theme.spacing.md,
      borderRadius: theme.spacing.md,
      width: '80%',
      maxWidth: 400,
    },
    menuItem: {
      flexDirection: 'row',
      alignItems: 'center',
      padding: theme.spacing.sm,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    menuText: {
      fontSize: theme.fontSize.md,
      fontWeight: '500',
      color: theme.colors.text,
      marginLeft: theme.spacing.sm,
    },
  });
