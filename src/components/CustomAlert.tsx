import React from 'react';
import {View, Text, Modal, TouchableOpacity, StyleSheet} from 'react-native';
import {useAppContext} from '../context/AppContext';
import {lightTheme, darkTheme} from '../utils/theme';

interface AlertButton {
  text: string;
  onPress?: () => void;
  style?: 'default' | 'cancel' | 'destructive';
}

interface CustomAlertProps {
  visible: boolean;
  title: string;
  message?: string;
  buttons: AlertButton[];
  onDismiss?: () => void;
}

export const CustomAlert: React.FC<CustomAlertProps> = ({
  visible,
  title,
  message,
  buttons,
  onDismiss,
}) => {
  const {state} = useAppContext();
  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const styles = createStyles(theme);

  const handleButtonPress = (button: AlertButton) => {
    if (button.onPress) {
      button.onPress();
    }
    if (onDismiss) {
      onDismiss();
    }
  };

  const getButtonStyle = (buttonStyle?: string) => {
    switch (buttonStyle) {
      case 'cancel':
        return [styles.button, styles.cancelButton];
      case 'destructive':
        return [styles.button, styles.destructiveButton];
      default:
        return [styles.button, styles.defaultButton];
    }
  };

  const getButtonTextStyle = (buttonStyle?: string) => {
    switch (buttonStyle) {
      case 'cancel':
        return [styles.buttonText, styles.cancelButtonText];
      case 'destructive':
        return [styles.buttonText, styles.destructiveButtonText];
      default:
        return [styles.buttonText, styles.defaultButtonText];
    }
  };

  return (
    <Modal
      visible={visible}
      transparent={true}
      animationType="fade"
      onRequestClose={onDismiss}>
      <TouchableOpacity
        style={styles.overlay}
        activeOpacity={1}
        onPress={onDismiss}>
        <TouchableOpacity
          style={styles.alertContainer}
          activeOpacity={1}
          onPress={() => {}} // Prevent modal from closing when tapping content
        >
          <Text style={styles.title}>{title}</Text>
          {message && <Text style={styles.message}>{message}</Text>}

          <View style={styles.buttonContainer}>
            {buttons.map((button, index) => (
              <TouchableOpacity
                key={index}
                style={getButtonStyle(button.style)}
                onPress={() => handleButtonPress(button)}>
                <Text style={getButtonTextStyle(button.style)}>
                  {button.text}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
};

const createStyles = (theme: any) =>
  StyleSheet.create({
    overlay: {
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      justifyContent: 'center',
      alignItems: 'center',
      padding: theme.spacing.lg,
    },
    alertContainer: {
      backgroundColor: theme.colors.surface,
      borderRadius: theme.spacing.sm,
      padding: theme.spacing.lg,
      minWidth: 280,
      maxWidth: '90%',
    },
    title: {
      fontSize: theme.fontSize.lg,
      fontWeight: 'bold',
      color: theme.colors.text,
      marginBottom: theme.spacing.sm,
      textAlign: 'center',
    },
    message: {
      fontSize: theme.fontSize.md,
      color: theme.colors.textSecondary,
      marginBottom: theme.spacing.lg,
      textAlign: 'center',
      lineHeight: 20,
    },
    buttonContainer: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      gap: theme.spacing.sm,
    },
    button: {
      flex: 1,
      paddingVertical: theme.spacing.sm,
      paddingHorizontal: theme.spacing.md,
      borderRadius: theme.spacing.xs,
      alignItems: 'center',
      minHeight: 44,
      justifyContent: 'center',
    },
    defaultButton: {
      backgroundColor: theme.colors.primary,
    },
    cancelButton: {
      backgroundColor: theme.colors.background,
      borderWidth: 1,
      borderColor: theme.colors.border,
    },
    destructiveButton: {
      backgroundColor: '#F44336',
    },
    buttonText: {
      fontSize: theme.fontSize.md,
      fontWeight: '500',
    },
    defaultButtonText: {
      color: theme.colors.background,
    },
    cancelButtonText: {
      color: theme.colors.text,
    },
    destructiveButtonText: {
      color: '#FFFFFF',
    },
  });
