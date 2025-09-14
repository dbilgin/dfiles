import React, {createContext, useContext, useReducer, ReactNode, useEffect} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {FileItem, AppState} from '../types';
import RNFS from 'react-native-fs';

const SHOW_META_FILES_KEY = 'showMetaFiles';
const DARK_MODE_KEY = 'darkMode';

interface AppContextType {
  state: AppState;
  dispatch: React.Dispatch<AppAction>;
}

export type AppAction =
  | {type: 'SET_CURRENT_PATH'; payload: string}
  | {type: 'SET_FILES'; payload: FileItem[]}
  | {type: 'TOGGLE_FILE_SELECTION'; payload: string}
  | {type: 'SELECT_ALL_FILES'}
  | {type: 'CLEAR_SELECTION'}
  | {type: 'SET_SELECTION_MODE'; payload: boolean}
  | {type: 'TOGGLE_DARK_MODE'}
  | {type: 'TOGGLE_META_FILES'}
  | {type: 'SET_DARK_MODE'; payload: boolean}
  | {type: 'SET_SHOW_META_FILES'; payload: boolean}
  | {type: 'SET_CLIPBOARD'; payload: {files: string[]; operation: 'copy' | 'cut'}}
  | {type: 'CLEAR_CLIPBOARD'}
  | {type: 'UPDATE_FILE'; payload: FileItem}
  | {type: 'REMOVE_FILES'; payload: string[]};

const initialState: AppState = {
  currentPath: RNFS.ExternalStorageDirectoryPath,
  files: [],
  selectedFiles: [],
  isSelectionMode: false,
  isDarkMode: false,
  showMetaFiles: false,
  clipboard: {
    files: [],
    operation: 'copy',
  },
};

function appReducer(state: AppState, action: AppAction): AppState {
  switch (action.type) {
    case 'SET_CURRENT_PATH':
      return {
        ...state,
        currentPath: action.payload,
        selectedFiles: [],
        isSelectionMode: false,
      };
    case 'SET_FILES':
      return {
        ...state,
        files: action.payload,
        selectedFiles: [],
        isSelectionMode: false,
      };
    case 'TOGGLE_FILE_SELECTION':
      const isSelected = state.selectedFiles.includes(action.payload);
      const newSelectedFiles = isSelected
        ? state.selectedFiles.filter(path => path !== action.payload)
        : [...state.selectedFiles, action.payload];
      return {
        ...state,
        selectedFiles: newSelectedFiles,
        isSelectionMode: newSelectedFiles.length > 0,
      };
    case 'SELECT_ALL_FILES':
      return {
        ...state,
        selectedFiles: state.files.map(file => file.path),
        isSelectionMode: true,
      };
    case 'CLEAR_SELECTION':
      return {
        ...state,
        selectedFiles: [],
        isSelectionMode: false,
      };
    case 'SET_SELECTION_MODE':
      return {
        ...state,
        isSelectionMode: action.payload,
        selectedFiles: action.payload ? state.selectedFiles : [],
      };
    case 'TOGGLE_DARK_MODE':
      const newDarkMode = !state.isDarkMode;
      AsyncStorage.setItem(DARK_MODE_KEY, JSON.stringify(newDarkMode));
      return {
        ...state,
        isDarkMode: newDarkMode,
      };
    case 'TOGGLE_META_FILES':
      const newShowMetaFiles = !state.showMetaFiles;
      AsyncStorage.setItem(SHOW_META_FILES_KEY, JSON.stringify(newShowMetaFiles));
      return  {
        ...state,
        showMetaFiles: newShowMetaFiles,
       };
    case 'SET_DARK_MODE':
      return {
        ...state,
        isDarkMode: action.payload,
      };
    case 'SET_SHOW_META_FILES':
      return {
        ...state,
        showMetaFiles: action.payload,
      };
    case 'SET_CLIPBOARD':
      return {
        ...state,
        clipboard: action.payload,
      };
    case 'CLEAR_CLIPBOARD':
      return {
        ...state,
        clipboard: {files: [], operation: 'copy'},
      };
    case 'UPDATE_FILE':
      return {
        ...state,
        files: state.files.map(file =>
          file.path === action.payload.path ? action.payload : file,
        ),
      };
    case 'REMOVE_FILES':
      return {
        ...state,
        files: state.files.filter(file => !action.payload.includes(file.path)),
        selectedFiles: state.selectedFiles.filter(
          path => !action.payload.includes(path),
        ),
      };
    default:
      return state;
  }
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({children}: {children: ReactNode}) {
  const [state, dispatch] = useReducer(appReducer, initialState);

  useEffect(() => {
    // Load settings from AsyncStorage on app startup
    const loadDarkModeSetting = async () => {
      try {
        const savedMetaFiles = await AsyncStorage.getItem(SHOW_META_FILES_KEY);
        if (savedMetaFiles !== null) {
          const isShowMetaFiles = JSON.parse(savedMetaFiles);
          dispatch({type: 'SET_SHOW_META_FILES', payload: isShowMetaFiles});
        }

        const savedDarkMode = await AsyncStorage.getItem(DARK_MODE_KEY);
        if (savedDarkMode !== null) {
          const isDarkMode = JSON.parse(savedDarkMode);
          dispatch({type: 'SET_DARK_MODE', payload: isDarkMode});
        }
      } catch (error) {
        console.error('Error loading settings:', error);
      }
    };

    loadDarkModeSetting();
  }, []);

  return (
    <AppContext.Provider value={{state, dispatch}}>
      {children}
    </AppContext.Provider>
  );
}

export function useAppContext() {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useAppContext must be used within an AppProvider');
  }
  return context;
} 