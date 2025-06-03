export interface Theme {
  colors: {
    primary: string;
    background: string;
    surface: string;
    text: string;
    textSecondary: string;
    border: string;
    accent: string;
    error: string;
    success: string;
    warning: string;
    selected: string;
    selectedText: string;
    icon: string;
    headerBackground: string;
    headerText: string;
  };
  spacing: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
  };
  borderRadius: {
    sm: number;
    md: number;
    lg: number;
  };
  fontSize: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
  };
}

export const lightTheme: Theme = {
  colors: {
    primary: '#2196F3',
    background: '#FFFFFF',
    surface: '#F5F5F5',
    text: '#212121',
    textSecondary: '#757575',
    border: '#E0E0E0',
    accent: '#FF4081',
    error: '#F44336',
    success: '#4CAF50',
    warning: '#FF9800',
    selected: '#E3F2FD',
    selectedText: '#1976D2',
    icon: '#616161',
    headerBackground: '#2196F3',
    headerText: '#FFFFFF',
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
  },
  borderRadius: {
    sm: 4,
    md: 8,
    lg: 12,
  },
  fontSize: {
    xs: 12,
    sm: 14,
    md: 16,
    lg: 18,
    xl: 24,
  },
};

export const darkTheme: Theme = {
  colors: {
    primary: '#64B5F6',
    background: '#121212',
    surface: '#1E1E1E',
    text: '#FFFFFF',
    textSecondary: '#B0B0B0',
    border: '#333333',
    accent: '#FF4081',
    error: '#F44336',
    success: '#4CAF50',
    warning: '#FF9800',
    selected: '#1E3A8A',
    selectedText: '#64B5F6',
    icon: '#B0B0B0',
    headerBackground: '#1E1E1E',
    headerText: '#FFFFFF',
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
  },
  borderRadius: {
    sm: 4,
    md: 8,
    lg: 12,
  },
  fontSize: {
    xs: 12,
    sm: 14,
    md: 16,
    lg: 18,
    xl: 24,
  },
}; 