export interface FileItem {
  name: string;
  path: string;
  size: number;
  isDirectory: boolean;
  mtime: Date;
  isSelected?: boolean;
  type?: 'image' | 'video' | 'audio' | 'document' | 'other';
}

export interface FolderInfo {
  name: string;
  path: string;
  itemCount: number;
}

export interface AppState {
  currentPath: string;
  files: FileItem[];
  selectedFiles: string[];
  isSelectionMode: boolean;
  isDarkMode: boolean;
  clipboard: {
    files: string[];
    operation: 'copy' | 'cut' | null;
  };
}

export interface NavigationState {
  history: string[];
  currentIndex: number;
}

export type SortBy = 'name' | 'size' | 'date' | 'type';
export type SortOrder = 'asc' | 'desc';

export interface SortOptions {
  sortBy: SortBy;
  sortOrder: SortOrder;
}

export interface PermissionStatus {
  granted: boolean;
  canRequestAgain: boolean;
}

export type RootStackParamList = {
  FileManager: undefined;
  Settings: undefined;
}; 