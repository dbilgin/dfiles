import {useState} from 'react';
import {Alert} from 'react-native';
import Share from 'react-native-share';
import {
  deleteFiles,
  copyFiles,
  moveFiles,
  createFolder,
  renameFile,
} from '../utils/fileUtils';

export const useFileOperations = () => {
  const [loading, setLoading] = useState(false);

  const handleDelete = async (filePaths: string[]): Promise<boolean> => {
    if (filePaths.length === 0) return false;

    return new Promise((resolve) => {
      Alert.alert(
        'Delete Files',
        `Are you sure you want to delete ${filePaths.length} item(s)?`,
        [
          {text: 'Cancel', style: 'cancel', onPress: () => resolve(false)},
          {
            text: 'Delete',
            style: 'destructive',
            onPress: async () => {
              setLoading(true);
              const success = await deleteFiles(filePaths);
              setLoading(false);
              if (!success) {
                Alert.alert('Error', 'Failed to delete some files');
              }
              resolve(success);
            },
          },
        ]
      );
    });
  };

  const handleCopy = async (
    sourcePaths: string[],
    destinationDir: string
  ): Promise<boolean> => {
    if (sourcePaths.length === 0) return false;

    setLoading(true);
    const success = await copyFiles(sourcePaths, destinationDir);
    setLoading(false);

    if (success) {
      Alert.alert(
        'Success',
        `${sourcePaths.length} item(s) copied successfully`
      );
    } else {
      Alert.alert('Error', 'Failed to copy files');
    }

    return success;
  };

  const handleMove = async (
    sourcePaths: string[],
    destinationDir: string
  ): Promise<boolean> => {
    if (sourcePaths.length === 0) return false;

    setLoading(true);
    const success = await moveFiles(sourcePaths, destinationDir);
    setLoading(false);

    if (success) {
      Alert.alert(
        'Success',
        `${sourcePaths.length} item(s) moved successfully`
      );
    } else {
      Alert.alert('Error', 'Failed to move files');
    }

    return success;
  };

  const handleShare = async (filePaths: string[]): Promise<boolean> => {
    if (filePaths.length === 0) return false;

    try {
      const shareOptions = {
        title: 'Share Files',
        urls: filePaths.map(path => `file://${path}`),
      };
      await Share.open(shareOptions);
      return true;
    } catch (error) {
      console.error('Share error:', error);
      Alert.alert('Error', 'Failed to share files');
      return false;
    }
  };

  const handleCreateFolder = async (
    parentPath: string,
    folderName?: string
  ): Promise<boolean> => {
    return new Promise((resolve) => {
      Alert.prompt(
        'Create Folder',
        'Enter folder name:',
        [
          {text: 'Cancel', style: 'cancel', onPress: () => resolve(false)},
          {
            text: 'Create',
            onPress: async (name) => {
              if (!name || name.trim() === '') {
                Alert.alert('Error', 'Please enter a valid folder name');
                resolve(false);
                return;
              }

              setLoading(true);
              const success = await createFolder(parentPath, name.trim());
              setLoading(false);

              if (success) {
                Alert.alert('Success', 'Folder created successfully');
              } else {
                Alert.alert('Error', 'Failed to create folder');
              }

              resolve(success);
            },
          },
        ],
        'plain-text',
        folderName || ''
      );
    });
  };

  const handleRename = async (
    filePath: string,
    currentName?: string
  ): Promise<boolean> => {
    return new Promise((resolve) => {
      Alert.prompt(
        'Rename',
        'Enter new name:',
        [
          {text: 'Cancel', style: 'cancel', onPress: () => resolve(false)},
          {
            text: 'Rename',
            onPress: async (newName) => {
              if (!newName || newName.trim() === '') {
                Alert.alert('Error', 'Please enter a valid name');
                resolve(false);
                return;
              }

              setLoading(true);
              const success = await renameFile(filePath, newName.trim());
              setLoading(false);

              if (success) {
                Alert.alert('Success', 'Item renamed successfully');
              } else {
                Alert.alert('Error', 'Failed to rename item');
              }

              resolve(success);
            },
          },
        ],
        'plain-text',
        currentName || ''
      );
    });
  };

  return {
    loading,
    handleDelete,
    handleCopy,
    handleMove,
    handleShare,
    handleCreateFolder,
    handleRename,
  };
}; 