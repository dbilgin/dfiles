import React, {useEffect, useState, useCallback} from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  RefreshControl,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  Modal,
  ScrollView,
  TextInput,
  NativeModules,
  BackHandler,
} from 'react-native';
import Share from 'react-native-share';
import FileViewer from 'react-native-file-viewer';
import {SafeAreaView} from 'react-native-safe-area-context';
import {StackNavigationProp} from '@react-navigation/stack';
import {Header} from '../components/Header';
import {FileItem} from '../components/FileItem';
import {CustomAlert} from '../components/CustomAlert';
import {useAppContext} from '../context/AppContext';
import {useCustomAlert} from '../hooks/useCustomAlert';
import {
  FileItem as FileItemType,
  SortOptions,
  RootStackParamList,
} from '../types';
import {
  readDirectory,
  sortFiles,
  deleteFiles,
  copyFiles,
  moveFiles,
  formatFileSize,
  formatDate,
  getFileType,
  renameFile,
  compressFile,
  decompressFile,
  createFolder,
  getRecentFiles,
  permanentlyDeleteFiles,
  moveToTrash,
  getTrashPath,
} from '../utils/fileUtils';
import {lightTheme, darkTheme} from '../utils/theme';
import Icon from 'react-native-vector-icons/MaterialIcons';

const {ApkInstaller} = NativeModules;

type FileManagerScreenNavigationProp = StackNavigationProp<
  RootStackParamList,
  'FileManager'
>;

interface FileManagerScreenProps {
  navigation: FileManagerScreenNavigationProp;
}

// Move modal components outside to prevent re-creation on every render
const RenameModal: React.FC<{
  visible: boolean;
  fileName: string;
  onFileNameChange: (name: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
  theme: any;
}> = ({visible, fileName, onFileNameChange, onConfirm, onCancel, theme}) => {
  const styles = createModalStyles(theme);

  return (
    <Modal
      visible={visible}
      transparent={true}
      animationType="fade"
      onRequestClose={onCancel}>
      <TouchableOpacity
        style={styles.modalOverlay}
        activeOpacity={1}
        onPress={onCancel}>
        <TouchableOpacity
          style={styles.modalContent}
          activeOpacity={1}
          onPress={() => {}}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>Rename File</Text>
            <TouchableOpacity onPress={onCancel} style={styles.closeButton}>
              <Icon name="close" size={24} color={theme.colors.text} />
            </TouchableOpacity>
          </View>

          <View style={styles.modalBody}>
            <Text style={styles.inputLabel}>New name:</Text>
            <TextInput
              style={styles.textInput}
              value={fileName}
              onChangeText={onFileNameChange}
              placeholder="Enter new file name"
              placeholderTextColor={theme.colors.textSecondary}
              autoFocus={true}
              selectTextOnFocus={true}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={onCancel}>
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.modalButton, styles.confirmButton]}
                onPress={onConfirm}
                disabled={!fileName.trim()}>
                <Text
                  style={[
                    styles.confirmButtonText,
                    !fileName.trim() && styles.disabledButtonText,
                  ]}>
                  Rename
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
};

const CreateFolderModal: React.FC<{
  visible: boolean;
  folderName: string;
  onFolderNameChange: (name: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
  theme: any;
}> = ({
  visible,
  folderName,
  onFolderNameChange,
  onConfirm,
  onCancel,
  theme,
}) => {
  const styles = createModalStyles(theme);

  return (
    <Modal
      visible={visible}
      transparent={true}
      animationType="fade"
      onRequestClose={onCancel}>
      <TouchableOpacity
        style={styles.modalOverlay}
        activeOpacity={1}
        onPress={onCancel}>
        <TouchableOpacity
          style={styles.modalContent}
          activeOpacity={1}
          onPress={() => {}}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>Create Folder</Text>
            <TouchableOpacity onPress={onCancel} style={styles.closeButton}>
              <Icon name="close" size={24} color={theme.colors.text} />
            </TouchableOpacity>
          </View>

          <View style={styles.modalBody}>
            <Text style={styles.inputLabel}>Folder name:</Text>
            <TextInput
              style={styles.textInput}
              value={folderName}
              onChangeText={onFolderNameChange}
              placeholder="Enter folder name"
              placeholderTextColor={theme.colors.textSecondary}
              autoFocus={true}
              selectTextOnFocus={true}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={onCancel}>
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.modalButton, styles.confirmButton]}
                onPress={onConfirm}
                disabled={!folderName.trim()}>
                <Text
                  style={[
                    styles.confirmButtonText,
                    !folderName.trim() && styles.disabledButtonText,
                  ]}>
                  Create
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
};

export const FileManagerScreen: React.FC<FileManagerScreenProps> = ({
  navigation,
}) => {
  const {state, dispatch} = useAppContext();
  const {alertState, showAlert, hideAlert} = useCustomAlert();
  const [refreshing, setRefreshing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [navigationHistory, setNavigationHistory] = useState<string[]>([
    state.currentPath,
  ]);
  const [currentHistoryIndex, setCurrentHistoryIndex] = useState(0);
  const [sortOptions] = useState<SortOptions>({
    sortBy: 'name',
    sortOrder: 'asc',
  });
  const [selectedFileForMetadata, setSelectedFileForMetadata] =
    useState<FileItemType | null>(null);
  const [renameModalVisible, setRenameModalVisible] = useState(false);
  const [fileToRename, setFileToRename] = useState<FileItemType | null>(null);
  const [newFileName, setNewFileName] = useState('');
  const [createFolderModalVisible, setCreateFolderModalVisible] =
    useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [recentFiles, setRecentFiles] = useState<FileItemType[]>([]);

  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const styles = createStyles(theme);

  const loadFiles = useCallback(
    async (path: string) => {
      try {
        setLoading(true);
        console.log('Loading files from path:', path);

        console.log('Reading directory:', path);
        const files = await readDirectory(path);
        console.log('Files found:', files.length);

        const sortedFiles = sortFiles(files, sortOptions);
        dispatch({type: 'SET_FILES', payload: sortedFiles});
        dispatch({type: 'SET_CURRENT_PATH', payload: path});
      } catch (error) {
        console.error('Error loading files:', error);
        // Just set empty files, no alert popups
        dispatch({type: 'SET_FILES', payload: []});
      } finally {
        setLoading(false);
      }
    },
    [dispatch, sortOptions],
  );

  const loadRecentFiles = useCallback(async () => {
    try {
      const recent = await getRecentFiles(5);
      setRecentFiles(recent);
    } catch (error) {
      console.error('Error loading recent files:', error);
      setRecentFiles([]);
    }
  }, []);

  const isRootDirectory = useCallback(() => {
    return state.currentPath === '/storage/emulated/0';
  }, [state.currentPath]);

  const isInTrash = useCallback(() => {
    return state.currentPath === getTrashPath();
  }, [state.currentPath]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    Promise.all([
      loadFiles(state.currentPath),
      isRootDirectory() ? loadRecentFiles() : Promise.resolve(),
    ]).finally(() => setRefreshing(false));
  }, [loadFiles, loadRecentFiles, state.currentPath, isRootDirectory]);

  useEffect(() => {
    loadFiles(state.currentPath);
    if (isRootDirectory()) {
      loadRecentFiles();
    }
  }, [loadFiles, loadRecentFiles, state.currentPath, isRootDirectory]);

  const navigateToPath = useCallback(
    (path: string) => {
      const newHistory = navigationHistory.slice(0, currentHistoryIndex + 1);
      newHistory.push(path);
      setNavigationHistory(newHistory);
      setCurrentHistoryIndex(newHistory.length - 1);
      loadFiles(path);
    },
    [navigationHistory, currentHistoryIndex, loadFiles],
  );

  const getCurrentFolderName = () => {
    const pathParts = state.currentPath.split('/').filter(part => part !== '');
    if (pathParts.length === 0 || state.currentPath === '/storage/emulated/0') {
      return 'Internal Storage';
    }
    return pathParts[pathParts.length - 1] || 'Storage';
  };

  const canGoBack = useCallback(() => {
    return (
      state.currentPath !== '/storage/emulated/0' && currentHistoryIndex > 0
    );
  }, [state.currentPath, currentHistoryIndex]);

  const goBack = useCallback(() => {
    if (canGoBack()) {
      const newIndex = currentHistoryIndex - 1;
      setCurrentHistoryIndex(newIndex);
      loadFiles(navigationHistory[newIndex]);
    } else if (state.currentPath !== '/storage/emulated/0') {
      // Go to parent directory
      const parentPath = state.currentPath.substring(
        0,
        state.currentPath.lastIndexOf('/'),
      );
      const targetPath = parentPath || '/storage/emulated/0';
      navigateToPath(targetPath);
    }
  }, [
    canGoBack,
    currentHistoryIndex,
    loadFiles,
    navigationHistory,
    state.currentPath,
    navigateToPath,
  ]);

  useEffect(() => {
    const backAction = () => {
      if (canGoBack()) {
        goBack();
        return true;
      }
      return false;
    };

    const backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      backAction,
    );

    return () => backHandler.remove();
  }, [canGoBack, goBack]);

  const handleFilePress = (item: FileItemType) => {
    if (item.isDirectory) {
      navigateToPath(item.path);
    } else {
      // Check if it's an APK file
      const extension = item.name.toLowerCase().split('.').pop();
      const apkExtensions = ['apk', 'xapk', 'xapks'];

      if (extension && apkExtensions.includes(extension)) {
        // Install the APK
        ApkInstaller.installApk(item.path)
          .then(() => {
            // APK installed successfully
          })
          .catch((error: any) => {
            // Show metadata modal for APK files
            console.error('APK installation failed:', error);
            setSelectedFileForMetadata(item);
          });
      } else {
        // Check if it's an XAPK file or other potentially unsupported formats
        const unsupportedExtensions = ['xapk', 'xapks'];

        if (extension && unsupportedExtensions.includes(extension)) {
          setSelectedFileForMetadata(item);
          return;
        }

        // Try to open file with default app
        FileViewer.open(item.path)
          .then(() => {
            // File opened successfully
          })
          .catch((_error: any) => {
            // Show metadata modal for unsupported files
            setSelectedFileForMetadata(item);
          });
      }
    }
  };

  const handleFileLongPress = (item: FileItemType) => {
    dispatch({type: 'TOGGLE_FILE_SELECTION', payload: item.path});
  };

  const handleSelectAll = () => {
    dispatch({type: 'SELECT_ALL_FILES'});
  };

  const handleClearSelection = () => {
    dispatch({type: 'CLEAR_SELECTION'});
  };

  const handleDelete = () => {
    if (state.selectedFiles.length === 0) {
      return;
    }

    showAlert(
      'Delete Files',
      `Are you sure you want to delete ${state.selectedFiles.length} item(s)?`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            const success = await deleteFiles(state.selectedFiles);
            if (success) {
              dispatch({type: 'REMOVE_FILES', payload: state.selectedFiles});
              dispatch({type: 'CLEAR_SELECTION'});
              onRefresh();
            } else {
              showAlert('Error', 'Failed to delete some files');
            }
          },
        },
      ],
    );
  };

  const handleDeletePermanentlyMultiple = () => {
    if (state.selectedFiles.length === 0) {
      return;
    }

    showAlert(
      'Delete Permanently',
      `Permanently delete ${state.selectedFiles.length} item(s)? This action cannot be undone.`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Delete Forever',
          style: 'destructive',
          onPress: async () => {
            const success = await permanentlyDeleteFiles(state.selectedFiles);
            if (success) {
              dispatch({type: 'REMOVE_FILES', payload: state.selectedFiles});
              dispatch({type: 'CLEAR_SELECTION'});
              onRefresh();
            } else {
              showAlert('Error', 'Failed to permanently delete some files');
            }
          },
        },
      ],
    );
  };

  const handleCopy = () => {
    if (state.selectedFiles.length === 0) {
      return;
    }
    dispatch({
      type: 'SET_CLIPBOARD',
      payload: {files: state.selectedFiles, operation: 'copy'},
    });
    dispatch({type: 'CLEAR_SELECTION'});
    showAlert('Copied', `${state.selectedFiles.length} item(s) copied`);
  };

  const handleCut = () => {
    if (state.selectedFiles.length === 0) {
      return;
    }
    dispatch({
      type: 'SET_CLIPBOARD',
      payload: {files: state.selectedFiles, operation: 'cut'},
    });
    dispatch({type: 'CLEAR_SELECTION'});
    showAlert('Cut', `${state.selectedFiles.length} item(s) cut`);
  };

  const handlePaste = async () => {
    if (state.clipboard.files.length === 0) {
      return;
    }

    const {files, operation} = state.clipboard;
    let success = false;

    if (operation === 'copy') {
      success = await copyFiles(files, state.currentPath);
    } else if (operation === 'cut') {
      success = await moveFiles(files, state.currentPath);
    }

    if (success) {
      dispatch({type: 'CLEAR_CLIPBOARD'});
      onRefresh();
      showAlert(
        'Success',
        `${files.length} item(s) ${operation === 'copy' ? 'copied' : 'moved'}`,
      );
    } else {
      showAlert('Error', `Failed to ${operation} files`);
    }
  };

  const handleShare = async () => {
    if (state.selectedFiles.length === 0) {
      return;
    }

    try {
      const shareOptions = {
        title: 'Share Files',
        urls: state.selectedFiles.map(path => `file://${path}`),
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
          console.error('Share error:', error);
          showAlert('Error', 'Failed to share files');
        }
      }
    }
  };

  const handleSettings = () => {
    navigation.navigate('Settings');
  };

  const handleRename = useCallback((file: FileItemType) => {
    setFileToRename(file);
    setNewFileName(file.name);
    setSelectedFileForMetadata(null);
    setRenameModalVisible(true);
  }, []);

  const handleInfo = (file: FileItemType) => {
    setSelectedFileForMetadata(file);
  };

  const handleCompress = async (file: FileItemType) => {
    showAlert('Compress', `Compress "${file.name}" into a ZIP archive?`, [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Compress',
        onPress: async () => {
          try {
            const success = await compressFile(file.path, state.currentPath);
            if (success) {
              onRefresh(); // Refresh the file list
            } else {
              showAlert('Error', 'Failed to compress file');
            }
          } catch (error) {
            console.error('Compression error:', error);
            showAlert('Error', 'Failed to compress file');
          }
        },
      },
    ]);
  };

  const handleDecompress = async (file: FileItemType) => {
    showAlert('Extract', `Extract "${file.name}" to the current folder?`, [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Extract',
        onPress: async () => {
          try {
            const success = await decompressFile(file.path, state.currentPath);
            if (success) {
              onRefresh(); // Refresh the file list
            } else {
              showAlert('Error', 'Failed to extract archive');
            }
          } catch (error) {
            console.error('Decompression error:', error);
            showAlert('Error', 'Failed to extract archive');
          }
        },
      },
    ]);
  };

  const confirmRename = async () => {
    if (!fileToRename || !newFileName.trim()) {
      return;
    }

    try {
      const success = await renameFile(fileToRename.path, newFileName.trim());
      if (success) {
        setRenameModalVisible(false);
        setFileToRename(null);
        setNewFileName('');
        onRefresh(); // Refresh the file list
      } else {
        showAlert('Error', 'Failed to rename file');
      }
    } catch (error) {
      console.error('Rename error:', error);
      showAlert('Error', 'Failed to rename file');
    }
  };

  const handleCreateFolder = useCallback(() => {
    setNewFolderName('');
    setCreateFolderModalVisible(true);
  }, []);

  const confirmCreateFolder = async () => {
    if (!newFolderName.trim()) {
      return;
    }

    try {
      const success = await createFolder(
        state.currentPath,
        newFolderName.trim(),
      );
      if (success) {
        setCreateFolderModalVisible(false);
        setNewFolderName('');
        onRefresh(); // Refresh the file list
      } else {
        showAlert('Error', 'Failed to create folder');
      }
    } catch (error) {
      console.error('Create folder error:', error);
      showAlert('Error', 'Failed to create folder');
    }
  };

  const handleRenameCancel = useCallback(() => {
    setRenameModalVisible(false);
    setFileToRename(null);
    setNewFileName('');
  }, []);

  const handleCreateFolderCancel = useCallback(() => {
    setCreateFolderModalVisible(false);
    setNewFolderName('');
  }, []);

  const handleFileNameChange = useCallback((name: string) => {
    setNewFileName(name);
  }, []);

  const handleFolderNameChange = useCallback((name: string) => {
    setNewFolderName(name);
  }, []);

  const handleDeletePermanently = async (file: FileItemType) => {
    showAlert(
      'Delete Permanently',
      `Permanently delete "${file.name}"? This action cannot be undone.`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Delete Forever',
          style: 'destructive',
          onPress: async () => {
            try {
              const success = await permanentlyDeleteFiles([file.path]);
              if (success) {
                onRefresh(); // Refresh the file list
              } else {
                showAlert('Error', 'Failed to delete file permanently');
              }
            } catch (error) {
              console.error('Permanent delete error:', error);
              showAlert('Error', 'Failed to delete file permanently');
            }
          },
        },
      ],
    );
  };

  const handleDeleteSingle = async (file: FileItemType) => {
    showAlert('Delete File', `Move "${file.name}" to trash?`, [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            const success = await moveToTrash(file.path);
            if (success) {
              onRefresh(); // Refresh the file list
            } else {
              showAlert('Error', 'Failed to move file to trash');
            }
          } catch (error) {
            console.error('Delete error:', error);
            showAlert('Error', 'Failed to move file to trash');
          }
        },
      },
    ]);
  };

  const renderFileItem = ({item}: {item: FileItemType}) => (
    <FileItem
      item={item}
      onPress={handleFilePress}
      onLongPress={handleFileLongPress}
      onRename={handleRename}
      onInfo={handleInfo}
      onCompress={handleCompress}
      onDecompress={handleDecompress}
      onDelete={handleDeleteSingle}
      onDeletePermanently={handleDeletePermanently}
      showAlert={showAlert}
    />
  );

  const renderEmptyList = () => (
    <View style={styles.emptyContainer}>
      {loading ? (
        <>
          <ActivityIndicator size="large" color={theme.colors.primary} />
          <Text style={styles.emptyText}>Loading files...</Text>
        </>
      ) : (
        <>
          <Icon name="folder-open" size={64} color={theme.colors.icon} />
          <Text style={styles.emptyText}>This folder is empty</Text>
        </>
      )}
    </View>
  );

  const FileMetadataModal = () => {
    if (!selectedFileForMetadata) {
      return null;
    }

    const file = selectedFileForMetadata;
    const fileType = file.isDirectory ? 'folder' : getFileType(file.name);
    const extension = file.isDirectory
      ? 'FOLDER'
      : file.name.toLowerCase().split('.').pop()?.toUpperCase() || 'UNKNOWN';

    return (
      <Modal
        visible={!!selectedFileForMetadata}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setSelectedFileForMetadata(null)}>
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setSelectedFileForMetadata(null)}>
          <TouchableOpacity
            style={styles.modalContent}
            activeOpacity={1}
            onPress={() => {}} // Prevent modal from closing when tapping content
          >
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>
                {file.isDirectory ? 'Folder' : 'File'} Information
              </Text>
              <TouchableOpacity
                onPress={() => setSelectedFileForMetadata(null)}
                style={styles.closeButton}>
                <Icon name="close" size={24} color={theme.colors.text} />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.modalBody}>
              <View style={styles.fileIconContainer}>
                <Icon
                  name={
                    file.isDirectory
                      ? 'folder'
                      : fileType === 'image'
                      ? 'image'
                      : fileType === 'video'
                      ? 'movie'
                      : fileType === 'audio'
                      ? 'music-note'
                      : fileType === 'document'
                      ? 'description'
                      : 'insert-drive-file'
                  }
                  size={64}
                  color={file.isDirectory ? '#FFA726' : theme.colors.primary}
                />
                <Text style={styles.fileTypeLabel}>
                  {extension} {file.isDirectory ? '' : 'File'}
                </Text>
              </View>

              <View style={styles.metadataContainer}>
                <View style={styles.metadataRow}>
                  <Text style={styles.metadataLabel}>Name:</Text>
                  <Text style={styles.metadataValue}>{file.name}</Text>
                </View>

                {!file.isDirectory && (
                  <View style={styles.metadataRow}>
                    <Text style={styles.metadataLabel}>Size:</Text>
                    <Text style={styles.metadataValue}>
                      {formatFileSize(file.size)}
                    </Text>
                  </View>
                )}

                <View style={styles.metadataRow}>
                  <Text style={styles.metadataLabel}>Modified:</Text>
                  <Text style={styles.metadataValue}>
                    {formatDate(file.mtime)}
                  </Text>
                </View>

                <View style={styles.metadataRow}>
                  <Text style={styles.metadataLabel}>Path:</Text>
                  <Text style={[styles.metadataValue, styles.pathText]}>
                    {file.path}
                  </Text>
                </View>

                <View style={styles.metadataRow}>
                  <Text style={styles.metadataLabel}>Type:</Text>
                  <Text style={styles.metadataValue}>
                    {file.isDirectory ? 'Folder' : fileType}
                  </Text>
                </View>
              </View>

              {!file.isDirectory && (
                <Text style={styles.noAppText}>
                  {['xapk', 'xapks'].includes(
                    file.name.toLowerCase().split('.').pop() || '',
                  )
                    ? 'No app available to open this file type.'
                    : 'Tap to open with default app.'}
                </Text>
              )}
            </ScrollView>
          </TouchableOpacity>
        </TouchableOpacity>
      </Modal>
    );
  };

  const renderRecentFilesSection = () => {
    if (!isRootDirectory() || recentFiles.length === 0) {
      return null;
    }

    return (
      <View style={styles.recentSection}>
        <Text style={styles.recentTitle}>Recent Files</Text>
        <FlatList
          data={recentFiles}
          horizontal
          showsHorizontalScrollIndicator={false}
          keyExtractor={(item, index) => `${item.path}-${index}`}
          renderItem={({item}) => (
            <TouchableOpacity
              style={styles.recentFileItem}
              onPress={() => handleFilePress(item)}>
              <View style={styles.recentFileIconContainer}>
                <Icon
                  name={
                    item.type === 'image'
                      ? 'image'
                      : item.type === 'video'
                      ? 'movie'
                      : item.type === 'audio'
                      ? 'music-note'
                      : item.type === 'document'
                      ? 'description'
                      : 'insert-drive-file'
                  }
                  size={32}
                  color={
                    item.type === 'image'
                      ? '#4CAF50'
                      : item.type === 'video'
                      ? '#F44336'
                      : item.type === 'audio'
                      ? '#9C27B0'
                      : item.type === 'document'
                      ? '#2196F3'
                      : theme.colors.icon
                  }
                />
              </View>
              <Text style={styles.recentFileName} numberOfLines={1}>
                {item.name}
              </Text>
            </TouchableOpacity>
          )}
          contentContainerStyle={styles.recentFilesContainer}
        />
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <Header
        title={getCurrentFolderName()}
        onBack={goBack}
        onSelectAll={handleSelectAll}
        onClearSelection={handleClearSelection}
        onDelete={handleDelete}
        onDeletePermanently={handleDeletePermanentlyMultiple}
        onCopy={handleCopy}
        onCut={handleCut}
        onShare={handleShare}
        onSettings={handleSettings}
        canGoBack={canGoBack()}
        isInTrash={isInTrash()}
      />

      <View style={styles.content}>
        {state.clipboard.files.length > 0 && (
          <TouchableOpacity style={styles.pasteBar} onPress={handlePaste}>
            <Icon name="content-paste" size={20} color={theme.colors.primary} />
            <Text style={styles.pasteText}>
              Paste {state.clipboard.files.length} item(s)
            </Text>
          </TouchableOpacity>
        )}

        {renderRecentFilesSection()}

        <FlatList
          data={state.files}
          renderItem={renderFileItem}
          keyExtractor={item => item.path}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              colors={[theme.colors.primary]}
              tintColor={theme.colors.primary}
            />
          }
          ListEmptyComponent={renderEmptyList}
          style={styles.list}
        />
      </View>

      {/* Floating Action Button */}
      {!state.isSelectionMode && (
        <TouchableOpacity
          style={styles.fab}
          onPress={handleCreateFolder}
          activeOpacity={0.8}>
          <Icon name="add" size={28} color={theme.colors.background} />
        </TouchableOpacity>
      )}

      <FileMetadataModal />
      <RenameModal
        visible={renameModalVisible}
        fileName={newFileName}
        onFileNameChange={handleFileNameChange}
        onConfirm={confirmRename}
        onCancel={handleRenameCancel}
        theme={theme}
      />
      <CreateFolderModal
        visible={createFolderModalVisible}
        folderName={newFolderName}
        onFolderNameChange={handleFolderNameChange}
        onConfirm={confirmCreateFolder}
        onCancel={handleCreateFolderCancel}
        theme={theme}
      />
      <CustomAlert
        visible={alertState.visible}
        title={alertState.title}
        message={alertState.message}
        buttons={alertState.buttons}
        onDismiss={hideAlert}
      />
    </SafeAreaView>
  );
};

const createStyles = (theme: any) =>
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: theme.colors.background,
    },
    content: {
      flex: 1,
    },
    list: {
      flex: 1,
    },
    emptyContainer: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
      paddingVertical: theme.spacing.xl,
    },
    emptyText: {
      fontSize: theme.fontSize.md,
      color: theme.colors.textSecondary,
      marginTop: theme.spacing.md,
    },
    pasteBar: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: theme.colors.surface,
      paddingHorizontal: theme.spacing.md,
      paddingVertical: theme.spacing.sm,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    pasteText: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.primary,
      marginLeft: theme.spacing.sm,
      fontWeight: '500',
    },
    modalOverlay: {
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      justifyContent: 'center',
      alignItems: 'center',
      padding: theme.spacing.md,
    },
    modalContent: {
      backgroundColor: theme.colors.surface,
      borderRadius: theme.spacing.sm,
      maxHeight: '80%',
      width: '100%',
      maxWidth: 400,
    },
    modalHeader: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: theme.spacing.md,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    modalTitle: {
      fontSize: theme.fontSize.lg,
      fontWeight: 'bold',
      color: theme.colors.text,
    },
    closeButton: {
      padding: theme.spacing.xs,
    },
    modalBody: {
      padding: theme.spacing.md,
    },
    fileIconContainer: {
      alignItems: 'center',
      marginBottom: theme.spacing.lg,
    },
    fileTypeLabel: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.textSecondary,
      marginTop: theme.spacing.xs,
      fontWeight: '500',
    },
    metadataContainer: {
      marginBottom: theme.spacing.md,
    },
    metadataRow: {
      marginBottom: theme.spacing.sm,
    },
    metadataLabel: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.textSecondary,
      fontWeight: '500',
      marginBottom: theme.spacing.xs,
    },
    metadataValue: {
      fontSize: theme.fontSize.md,
      color: theme.colors.text,
    },
    pathText: {
      fontSize: theme.fontSize.sm,
      fontFamily: 'monospace',
    },
    noAppText: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.textSecondary,
      textAlign: 'center',
      fontStyle: 'italic',
      marginTop: theme.spacing.md,
    },
    inputLabel: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.text,
      marginBottom: theme.spacing.xs,
      fontWeight: '500',
    },
    textInput: {
      borderWidth: 1,
      borderColor: theme.colors.border,
      borderRadius: theme.spacing.xs,
      paddingHorizontal: theme.spacing.sm,
      paddingVertical: theme.spacing.sm,
      fontSize: theme.fontSize.md,
      color: theme.colors.text,
      backgroundColor: theme.colors.background,
      marginBottom: theme.spacing.md,
    },
    modalButtons: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      gap: theme.spacing.sm,
    },
    modalButton: {
      flex: 1,
      paddingVertical: theme.spacing.sm,
      paddingHorizontal: theme.spacing.md,
      borderRadius: theme.spacing.xs,
      alignItems: 'center',
    },
    cancelButton: {
      backgroundColor: theme.colors.background,
      borderWidth: 1,
      borderColor: theme.colors.border,
    },
    confirmButton: {
      backgroundColor: theme.colors.primary,
    },
    cancelButtonText: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.text,
      fontWeight: '500',
    },
    confirmButtonText: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.background,
      fontWeight: '500',
    },
    disabledButtonText: {
      opacity: 0.5,
    },
    fab: {
      position: 'absolute',
      bottom: theme.spacing.xl,
      right: theme.spacing.xl,
      width: 56,
      height: 56,
      borderRadius: 28,
      backgroundColor: theme.colors.primary,
      justifyContent: 'center',
      alignItems: 'center',
      elevation: 8,
      shadowColor: '#000',
      shadowOffset: {
        width: 0,
        height: 4,
      },
      shadowOpacity: 0.3,
      shadowRadius: 4.65,
    },
    recentSection: {
      backgroundColor: theme.colors.surface,
      margin: theme.spacing.md,
      marginBottom: 0,
      borderRadius: theme.spacing.sm,
      padding: theme.spacing.md,
    },
    recentTitle: {
      fontSize: theme.fontSize.md,
      fontWeight: 'bold',
      color: theme.colors.text,
      marginBottom: theme.spacing.sm,
    },
    recentFilesContainer: {
      paddingHorizontal: theme.spacing.xs,
    },
    recentFileItem: {
      alignItems: 'center',
      paddingHorizontal: theme.spacing.sm,
      paddingVertical: theme.spacing.xs,
      marginHorizontal: theme.spacing.xs,
      borderRadius: theme.spacing.xs,
      backgroundColor: theme.colors.background,
      width: 80,
    },
    recentFileIconContainer: {
      marginBottom: theme.spacing.xs,
    },
    recentFileName: {
      fontSize: theme.fontSize.xs,
      fontWeight: '500',
      color: theme.colors.text,
      textAlign: 'center',
    },
  });

// Create separate styles function for modals
const createModalStyles = (theme: any) =>
  StyleSheet.create({
    modalOverlay: {
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      justifyContent: 'center',
      alignItems: 'center',
      padding: theme.spacing.md,
    },
    modalContent: {
      backgroundColor: theme.colors.surface,
      borderRadius: theme.spacing.sm,
      maxHeight: '80%',
      width: '100%',
      maxWidth: 400,
    },
    modalHeader: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: theme.spacing.md,
      borderBottomWidth: 1,
      borderBottomColor: theme.colors.border,
    },
    modalTitle: {
      fontSize: theme.fontSize.lg,
      fontWeight: 'bold',
      color: theme.colors.text,
    },
    closeButton: {
      padding: theme.spacing.xs,
    },
    modalBody: {
      padding: theme.spacing.md,
    },
    inputLabel: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.text,
      marginBottom: theme.spacing.xs,
      fontWeight: '500',
    },
    textInput: {
      borderWidth: 1,
      borderColor: theme.colors.border,
      borderRadius: theme.spacing.xs,
      paddingHorizontal: theme.spacing.sm,
      paddingVertical: theme.spacing.sm,
      fontSize: theme.fontSize.md,
      color: theme.colors.text,
      backgroundColor: theme.colors.background,
      marginBottom: theme.spacing.md,
    },
    modalButtons: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      gap: theme.spacing.sm,
    },
    modalButton: {
      flex: 1,
      paddingVertical: theme.spacing.sm,
      paddingHorizontal: theme.spacing.md,
      borderRadius: theme.spacing.xs,
      alignItems: 'center',
    },
    cancelButton: {
      backgroundColor: theme.colors.background,
      borderWidth: 1,
      borderColor: theme.colors.border,
    },
    confirmButton: {
      backgroundColor: theme.colors.primary,
    },
    cancelButtonText: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.text,
      fontWeight: '500',
    },
    confirmButtonText: {
      fontSize: theme.fontSize.sm,
      color: theme.colors.background,
      fontWeight: '500',
    },
    disabledButtonText: {
      opacity: 0.5,
    },
  });
