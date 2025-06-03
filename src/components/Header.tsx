import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  StatusBar,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {useAppContext} from '../context/AppContext';
import {lightTheme, darkTheme} from '../utils/theme';

interface HeaderProps {
  title: string;
  onBack?: () => void;
  onSelectAll?: () => void;
  onClearSelection?: () => void;
  onDelete?: () => void;
  onDeletePermanently?: () => void;
  onCopy?: () => void;
  onCut?: () => void;
  onShare?: () => void;
  onSettings?: () => void;
  canGoBack?: boolean;
  isInTrash?: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  onBack,
  onSelectAll,
  onClearSelection,
  onDelete,
  onDeletePermanently,
  onCopy,
  onCut,
  onShare,
  onSettings,
  canGoBack = false,
  isInTrash = false,
}) => {
  const {state} = useAppContext();
  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const styles = createStyles(theme);

  const selectedCount = state.selectedFiles.length;

  if (state.isSelectionMode) {
    return (
      <View style={styles.container}>
        <StatusBar
          backgroundColor={theme.colors.headerBackground}
          barStyle={state.isDarkMode ? 'light-content' : 'dark-content'}
        />
        <View style={styles.selectionHeader}>
          <TouchableOpacity onPress={onClearSelection} style={styles.iconButton}>
            <Icon name="close" size={24} color={theme.colors.headerText} />
          </TouchableOpacity>
          <Text style={styles.selectionTitle}>
            {selectedCount} selected
          </Text>
          <View style={styles.selectionActions}>
            <TouchableOpacity onPress={onSelectAll} style={styles.iconButton}>
              <Icon name="select-all" size={24} color={theme.colors.headerText} />
            </TouchableOpacity>
            <TouchableOpacity onPress={onShare} style={styles.iconButton}>
              <Icon name="share" size={24} color={theme.colors.headerText} />
            </TouchableOpacity>
            <TouchableOpacity onPress={onCopy} style={styles.iconButton}>
              <Icon name="content-copy" size={24} color={theme.colors.headerText} />
            </TouchableOpacity>
            <TouchableOpacity onPress={onCut} style={styles.iconButton}>
              <Icon name="content-cut" size={24} color={theme.colors.headerText} />
            </TouchableOpacity>
            {isInTrash ? (
              <TouchableOpacity onPress={onDeletePermanently} style={styles.iconButton}>
                <Icon name="clear" size={24} color="#F44336" />
              </TouchableOpacity>
            ) : (
              <TouchableOpacity onPress={onDelete} style={styles.iconButton}>
                <Icon name="delete" size={24} color={theme.colors.headerText} />
              </TouchableOpacity>
            )}
          </View>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <StatusBar
        backgroundColor={theme.colors.headerBackground}
        barStyle={state.isDarkMode ? 'light-content' : 'dark-content'}
      />
      <View style={styles.normalHeader}>
        <View style={styles.leftSection}>
          {canGoBack && (
            <TouchableOpacity onPress={onBack} style={styles.iconButton}>
              <Icon name="arrow-back" size={24} color={theme.colors.headerText} />
            </TouchableOpacity>
          )}
          <Text style={styles.title} numberOfLines={1} ellipsizeMode="middle">
            {title}
          </Text>
        </View>
        <View style={styles.rightSection}>
          <TouchableOpacity onPress={onSettings} style={styles.iconButton}>
            <Icon name="settings" size={24} color={theme.colors.headerText} />
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const createStyles = (theme: any) =>
  StyleSheet.create({
    container: {
      backgroundColor: theme.colors.headerBackground,
      elevation: 4,
      shadowColor: '#000',
      shadowOffset: {width: 0, height: 2},
      shadowOpacity: 0.2,
      shadowRadius: 4,
    },
    normalHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: theme.spacing.md,
      paddingVertical: theme.spacing.sm,
      minHeight: 56,
    },
    selectionHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: theme.spacing.md,
      paddingVertical: theme.spacing.sm,
      minHeight: 56,
    },
    leftSection: {
      flex: 1,
      flexDirection: 'row',
      alignItems: 'center',
    },
    rightSection: {
      flexDirection: 'row',
      alignItems: 'center',
    },
    title: {
      fontSize: theme.fontSize.lg,
      fontWeight: '500',
      color: theme.colors.headerText,
      marginLeft: theme.spacing.sm,
      flex: 1,
    },
    selectionTitle: {
      fontSize: theme.fontSize.lg,
      fontWeight: '500',
      color: theme.colors.headerText,
      marginLeft: theme.spacing.md,
      flex: 1,
    },
    selectionActions: {
      flexDirection: 'row',
      alignItems: 'center',
    },
    iconButton: {
      padding: theme.spacing.sm,
      borderRadius: theme.borderRadius.md,
    },
  }); 