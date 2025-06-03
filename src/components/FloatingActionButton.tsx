import React from 'react';
import {TouchableOpacity, StyleSheet} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {useAppContext} from '../context/AppContext';
import {lightTheme, darkTheme} from '../utils/theme';

interface FloatingActionButtonProps {
  onPress: () => void;
  icon?: string;
  size?: number;
}

export const FloatingActionButton: React.FC<FloatingActionButtonProps> = ({
  onPress,
  icon = 'add',
  size = 56,
}) => {
  const {state} = useAppContext();
  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const styles = createStyles(theme, size);

  return (
    <TouchableOpacity
      style={styles.container}
      onPress={onPress}
      activeOpacity={0.8}>
      <Icon name={icon} size={24} color={theme.colors.headerText} />
    </TouchableOpacity>
  );
};

const createStyles = (theme: any, size: number) =>
  StyleSheet.create({
    container: {
      position: 'absolute',
      bottom: 20,
      right: 20,
      width: size,
      height: size,
      borderRadius: size / 2,
      backgroundColor: theme.colors.primary,
      justifyContent: 'center',
      alignItems: 'center',
      elevation: 8,
      shadowColor: '#000',
      shadowOffset: {width: 0, height: 4},
      shadowOpacity: 0.3,
      shadowRadius: 8,
    },
  }); 