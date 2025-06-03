import RNFS from 'react-native-fs';
import {zip, unzip} from 'react-native-zip-archive';
import {FileItem, SortOptions} from '../types';

export const getFileType = (filename: string): FileItem['type'] => {
  const extension = filename.toLowerCase().split('.').pop();

  const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
  const videoExtensions = ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv', '3gp'];
  const audioExtensions = ['mp3', 'wav', 'flac', 'aac', 'ogg', 'm4a'];
  const documentExtensions = [
    'pdf',
    'doc',
    'docx',
    'txt',
    'rtf',
    'xls',
    'xlsx',
    'ppt',
    'pptx',
  ];

  if (extension && imageExtensions.includes(extension)) {
    return 'image';
  }
  if (extension && videoExtensions.includes(extension)) {
    return 'video';
  }
  if (extension && audioExtensions.includes(extension)) {
    return 'audio';
  }
  if (extension && documentExtensions.includes(extension)) {
    return 'document';
  }

  return 'other';
};

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) {
    return '0 B';
  }

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

export const formatDate = (date: Date): string => {
  return (
    date.toLocaleDateString() +
    ' ' +
    date.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    })
  );
};

export const readDirectory = async (path: string): Promise<FileItem[]> => {
  try {
    const items = await RNFS.readDir(path);

    if (!items) {
      return [];
    }

    if (items.length === 0) {
      return [];
    }

    const fileItems: FileItem[] = items.map(item => {
      const fileItem: FileItem = {
        name: item.name,
        path: item.path,
        size: item.size || 0,
        isDirectory: item.isDirectory(),
        mtime: item.mtime || new Date(),
        type: item.isDirectory() ? undefined : getFileType(item.name),
      };
      return fileItem;
    });

    return fileItems;
  } catch (error) {
    console.error('Error reading directory:', error);

    const errorMessage = error instanceof Error ? error.message : String(error);

    // Handle specific Android restrictions - just return empty array, no alerts
    if (path.includes('/Android/data') || path.includes('/Android/obb')) {
      return [];
    }

    // Handle permission errors - just return empty array, no alerts
    if (errorMessage.includes('permission')) {
      return [];
    }

    // Handle null array error - just return empty array, no alerts
    if (errorMessage.includes('null array')) {
      return [];
    }

    // For any other error, just return empty array
    return [];
  }
};

export const sortFiles = (
  files: FileItem[],
  options: SortOptions,
): FileItem[] => {
  const {sortBy, sortOrder} = options;

  return [...files].sort((a, b) => {
    // Always put directories first
    if (a.isDirectory && !b.isDirectory) {
      return -1;
    }
    if (!a.isDirectory && b.isDirectory) {
      return 1;
    }

    let comparison = 0;

    switch (sortBy) {
      case 'name':
        comparison = a.name.toLowerCase().localeCompare(b.name.toLowerCase());
        break;
      case 'size':
        comparison = a.size - b.size;
        break;
      case 'date':
        comparison = a.mtime.getTime() - b.mtime.getTime();
        break;
      case 'type':
        const aType = a.type || 'folder';
        const bType = b.type || 'folder';
        comparison = aType.localeCompare(bType);
        break;
    }

    return sortOrder === 'asc' ? comparison : -comparison;
  });
};

export const deleteFiles = async (filePaths: string[]): Promise<boolean> => {
  try {
    // Move files to trash instead of permanently deleting
    await Promise.all(filePaths.map(path => moveToTrash(path)));
    return true;
  } catch (error) {
    console.error('Error moving files to trash:', error);
    return false;
  }
};

const generateUniqueFileName = async (
  destinationDir: string,
  fileName: string,
): Promise<string> => {
  const originalPath = `${destinationDir}/${fileName}`;

  // Check if file already exists
  try {
    await RNFS.stat(originalPath);
  } catch {
    // File doesn't exist, use original name
    return fileName;
  }

  // File exists, generate unique name
  const lastDotIndex = fileName.lastIndexOf('.');
  const nameWithoutExt =
    lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
  const extension = lastDotIndex > 0 ? fileName.substring(lastDotIndex) : '';

  let counter = 1;
  let newFileName: string;

  do {
    newFileName = `${nameWithoutExt} (${counter})${extension}`;
    counter++;
    try {
      await RNFS.stat(`${destinationDir}/${newFileName}`);
    } catch {
      // File doesn't exist, use this name
      break;
    }
  } while (counter < 1000); // Safety limit

  return newFileName;
};

export const copyFiles = async (
  sourcePaths: string[],
  destinationDir: string,
): Promise<boolean> => {
  try {
    await Promise.all(
      sourcePaths.map(async sourcePath => {
        const fileName = sourcePath.split('/').pop() || '';
        const uniqueFileName = await generateUniqueFileName(
          destinationDir,
          fileName,
        );
        const destinationPath = `${destinationDir}/${uniqueFileName}`;

        const stat = await RNFS.stat(sourcePath);
        if (stat.isDirectory()) {
          await RNFS.mkdir(destinationPath);
          // Recursively copy directory contents
          const items = await RNFS.readDir(sourcePath);
          const subPaths = items.map(item => item.path);
          await copyFiles(subPaths, destinationPath);
        } else {
          await RNFS.copyFile(sourcePath, destinationPath);
        }
      }),
    );
    return true;
  } catch (error) {
    console.error('Error copying files:', error);
    return false;
  }
};

export const moveFiles = async (
  sourcePaths: string[],
  destinationDir: string,
): Promise<boolean> => {
  try {
    await Promise.all(
      sourcePaths.map(async sourcePath => {
        const fileName = sourcePath.split('/').pop() || '';
        const uniqueFileName = await generateUniqueFileName(
          destinationDir,
          fileName,
        );
        const destinationPath = `${destinationDir}/${uniqueFileName}`;
        await RNFS.moveFile(sourcePath, destinationPath);
      }),
    );
    return true;
  } catch (error) {
    console.error('Error moving files:', error);
    return false;
  }
};

export const createFolder = async (
  parentPath: string,
  folderName: string,
): Promise<boolean> => {
  try {
    const folderPath = `${parentPath}/${folderName}`;
    await RNFS.mkdir(folderPath);
    return true;
  } catch (error) {
    console.error('Error creating folder:', error);
    return false;
  }
};

export const renameFile = async (
  oldPath: string,
  newName: string,
): Promise<boolean> => {
  try {
    const parentDir = oldPath.substring(0, oldPath.lastIndexOf('/'));
    const newPath = `${parentDir}/${newName}`;
    await RNFS.moveFile(oldPath, newPath);
    return true;
  } catch (error) {
    console.error('Error renaming file:', error);
    return false;
  }
};

export const getStorageInfo = async () => {
  try {
    const freeSpace = await RNFS.getFSInfo();
    return {
      totalSpace: freeSpace.totalSpace,
      freeSpace: freeSpace.freeSpace,
    };
  } catch (error) {
    console.error('Error getting storage info:', error);
    return null;
  }
};

export const getCommonDirectories = () => {
  return [
    {
      name: 'Internal Storage',
      path: RNFS.ExternalStorageDirectoryPath,
      icon: 'storage',
    },
    {
      name: 'Downloads',
      path: `${RNFS.ExternalStorageDirectoryPath}/Download`,
      icon: 'download',
    },
    {
      name: 'Documents',
      path: `${RNFS.ExternalStorageDirectoryPath}/Documents`,
      icon: 'description',
    },
    {
      name: 'Pictures',
      path: `${RNFS.ExternalStorageDirectoryPath}/Pictures`,
      icon: 'image',
    },
    {
      name: 'Music',
      path: `${RNFS.ExternalStorageDirectoryPath}/Music`,
      icon: 'music-note',
    },
    {
      name: 'Movies',
      path: `${RNFS.ExternalStorageDirectoryPath}/Movies`,
      icon: 'movie',
    },
    {
      name: 'DCIM',
      path: `${RNFS.ExternalStorageDirectoryPath}/DCIM`,
      icon: 'camera-alt',
    },
    {
      name: 'Trash',
      path: getTrashPath(),
      icon: 'delete',
    },
  ];
};

export const compressFile = async (
  filePath: string,
  outputDir: string,
): Promise<boolean> => {
  try {
    const fileName = filePath.split('/').pop() || 'archive';
    const nameWithoutExt = fileName.includes('.')
      ? fileName.substring(0, fileName.lastIndexOf('.'))
      : fileName;
    const zipPath = `${outputDir}/${nameWithoutExt}.zip`;

    // Check if file already exists and generate unique name
    let finalZipPath = zipPath;
    let counter = 1;
    while (await RNFS.exists(finalZipPath)) {
      finalZipPath = `${outputDir}/${nameWithoutExt} (${counter}).zip`;
      counter++;
    }

    const stat = await RNFS.stat(filePath);
    if (stat.isDirectory()) {
      // Compress directory
      await zip(filePath, finalZipPath);
    } else {
      // For single files, create a temporary directory and move the file there
      const tempDir = `${outputDir}/temp_${Date.now()}`;
      await RNFS.mkdir(tempDir);
      const tempFilePath = `${tempDir}/${fileName}`;
      await RNFS.copyFile(filePath, tempFilePath);
      await zip(tempDir, finalZipPath);
      await RNFS.unlink(tempDir);
    }

    return true;
  } catch (error) {
    console.error('Error compressing file:', error);
    return false;
  }
};

export const decompressFile = async (
  filePath: string,
  outputDir: string,
): Promise<boolean> => {
  try {
    const fileName = filePath.split('/').pop() || 'archive';
    const nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
    let extractDir = `${outputDir}/${nameWithoutExt}`;

    // Check if directory already exists and generate unique name
    let counter = 1;
    while (await RNFS.exists(extractDir)) {
      extractDir = `${outputDir}/${nameWithoutExt} (${counter})`;
      counter++;
    }

    await RNFS.mkdir(extractDir);
    await unzip(filePath, extractDir);

    return true;
  } catch (error) {
    console.error('Error decompressing file:', error);
    return false;
  }
};

export const getRecentFiles = async (limit: number = 5): Promise<FileItem[]> => {
  try {
    const basePath = RNFS.ExternalStorageDirectoryPath;
    const allFiles: FileItem[] = [];
    
    // Define directories to scan for recent files
    const directoriesToScan = [
      `${basePath}/Download`,
      `${basePath}/Documents`, 
      `${basePath}/Pictures`,
      `${basePath}/DCIM`,
      `${basePath}/Music`,
      `${basePath}/Movies`,
      basePath, // Root directory
    ];
    
    // Scan each directory for files (not subdirectories to keep it fast)
    for (const dirPath of directoriesToScan) {
      try {
        const exists = await RNFS.exists(dirPath);
        if (!exists) continue;
        
        const items = await RNFS.readDir(dirPath);
        
        // Only include files, not directories
        const files = items
          .filter(item => !item.isDirectory())
          .map((item): FileItem => ({
            name: item.name,
            path: item.path,
            size: item.size || 0,
            isDirectory: false,
            mtime: item.mtime || new Date(),
            type: getFileType(item.name),
          }));
        
        allFiles.push(...files);
      } catch (error) {
        // Skip directories we can't access
        continue;
      }
    }
    
    // Sort by modification time (most recent first) and limit results
    const recentFiles = allFiles
      .sort((a, b) => b.mtime.getTime() - a.mtime.getTime())
      .slice(0, limit);
    
    return recentFiles;
  } catch (error) {
    console.error('Error getting recent files:', error);
    return [];
  }
};

export const getTrashPath = (): string => {
  return `${RNFS.ExternalStorageDirectoryPath}/.trash`;
};

export const moveToTrash = async (filePath: string): Promise<boolean> => {
  try {
    const trashPath = getTrashPath();
    
    // Create trash directory if it doesn't exist
    try {
      await RNFS.stat(trashPath);
    } catch {
      await RNFS.mkdir(trashPath);
    }
    
    const fileName = filePath.split('/').pop() || '';
    const timestamp = Date.now();
    const trashedFileName = `${timestamp}_${fileName}`;
    const destinationPath = `${trashPath}/${trashedFileName}`;
    
    // Move file to trash
    await RNFS.moveFile(filePath, destinationPath);
    
    // Create metadata file to track original location
    const metadataPath = `${destinationPath}.meta`;
    const metadata = {
      originalPath: filePath,
      deletedAt: new Date().toISOString(),
      originalName: fileName,
    };
    await RNFS.writeFile(metadataPath, JSON.stringify(metadata, null, 2));
    
    return true;
  } catch (error) {
    console.error('Error moving file to trash:', error);
    return false;
  }
};

export const restoreFromTrash = async (trashedFilePath: string): Promise<boolean> => {
  try {
    const metadataPath = `${trashedFilePath}.meta`;
    
    // Read original location from metadata
    const metadataContent = await RNFS.readFile(metadataPath);
    const metadata = JSON.parse(metadataContent);
    
    // Check if original directory still exists
    const originalDir = metadata.originalPath.substring(0, metadata.originalPath.lastIndexOf('/'));
    try {
      await RNFS.stat(originalDir);
    } catch {
      // Original directory doesn't exist, restore to Downloads
      metadata.originalPath = `${RNFS.ExternalStorageDirectoryPath}/Download/${metadata.originalName}`;
    }
    
    // Generate unique name if file already exists at original location
    let restorePath = metadata.originalPath;
    let counter = 1;
    while (await RNFS.exists(restorePath)) {
      const nameWithoutExt = metadata.originalName.includes('.') 
        ? metadata.originalName.substring(0, metadata.originalName.lastIndexOf('.'))
        : metadata.originalName;
      const extension = metadata.originalName.includes('.')
        ? metadata.originalName.substring(metadata.originalName.lastIndexOf('.'))
        : '';
      const newName = `${nameWithoutExt} (${counter})${extension}`;
      restorePath = `${originalDir}/${newName}`;
      counter++;
    }
    
    // Move file back to original location
    await RNFS.moveFile(trashedFilePath, restorePath);
    
    // Remove metadata file
    await RNFS.unlink(metadataPath);
    
    return true;
  } catch (error) {
    console.error('Error restoring file from trash:', error);
    return false;
  }
};

export const permanentlyDeleteFiles = async (filePaths: string[]): Promise<boolean> => {
  try {
    await Promise.all(filePaths.map(async (path) => {
      await RNFS.unlink(path);
      // Also remove metadata file if it exists
      try {
        await RNFS.unlink(`${path}.meta`);
      } catch {
        // Metadata file might not exist, ignore error
      }
    }));
    return true;
  } catch (error) {
    console.error('Error permanently deleting files:', error);
    return false;
  }
};

export const emptyTrash = async (): Promise<boolean> => {
  try {
    const trashPath = getTrashPath();
    
    try {
      const items = await RNFS.readDir(trashPath);
      await Promise.all(items.map(item => RNFS.unlink(item.path)));
    } catch {
      // Trash folder might not exist or be empty
    }
    
    return true;
  } catch (error) {
    console.error('Error emptying trash:', error);
    return false;
  }
};

export const getTrashFiles = async (): Promise<FileItem[]> => {
  try {
    const trashPath = getTrashPath();
    
    try {
      await RNFS.stat(trashPath);
    } catch {
      // Trash folder doesn't exist
      return [];
    }
    
    const items = await RNFS.readDir(trashPath);
    
    // Filter out metadata files and create FileItems with original names
    const trashFiles = await Promise.all(
      items
        .filter(item => !item.name.endsWith('.meta'))
        .map(async (item) => {
          let originalName = item.name;
          let deletedAt = item.mtime;
          
          // Try to read metadata for original name and deletion date
          try {
            const metadataContent = await RNFS.readFile(`${item.path}.meta`);
            const metadata = JSON.parse(metadataContent);
            originalName = metadata.originalName;
            deletedAt = new Date(metadata.deletedAt);
          } catch {
            // No metadata file, extract original name from timestamp prefix
            const underscoreIndex = item.name.indexOf('_');
            if (underscoreIndex > 0) {
              originalName = item.name.substring(underscoreIndex + 1);
            }
          }
          
          const fileItem: FileItem = {
            name: originalName,
            path: item.path,
            size: item.size || 0,
            isDirectory: item.isDirectory(),
            mtime: deletedAt || item.mtime || new Date(),
            type: item.isDirectory() ? undefined : getFileType(originalName),
          };
          return fileItem;
        })
    );
    
    return trashFiles;
  } catch (error) {
    console.error('Error reading trash files:', error);
    return [];
  }
};
