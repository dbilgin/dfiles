import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useAppContext } from "../context/AppContext";
import { darkTheme, lightTheme } from "../utils/theme";
import Share from 'react-native-share';
import { useCustomAlert } from "../hooks/useCustomAlert";
import { CustomAlert } from "./CustomAlert";
import { moveToTrash } from "../utils/fileUtils";
import Icon from 'react-native-vector-icons/MaterialIcons';

interface Props {
  name?: string;
  path?: string;
  onDeleteSuccess?: () => void;
}

export const MediaFooter = ({name, path, onDeleteSuccess}: Props) => {
  const { state } = useAppContext();
  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const styles = createStyles(theme);

  const { showAlert, alertState, hideAlert } = useCustomAlert();

  const handleShareImage = async () => {
    if (!name || !path) {
      return;
    }

    await Share.open({
      url: `file://${path}`,
      title: name,
    }).catch(err => {
      console.log(err);
    });
  };

    const handleDeleteImage = async () => {
    if (!name || !path) {
        return;
      }
  
      showAlert('Delete Image', `Move "${name}" to trash?`, [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              const success = await moveToTrash(path);
              if (success) {
                onDeleteSuccess?.();
              } else {
                showAlert('Error', 'Failed to delete image');
              }
            } catch (error) {
              console.error('Delete error:', error);
              showAlert('Error', 'Failed to delete image');
            }
          },
        },
      ]);
    };
  
  return (
    <>
      <View style={styles.imageModalActions}>
        <TouchableOpacity
          style={styles.imageActionButton}
          onPress={handleShareImage}>
          <Icon name="share" size={24} color={theme.colors.background} />
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.imageActionButton, styles.deleteButton]}
          onPress={handleDeleteImage}>
          <Icon name="delete" size={24} color="#fff" />
        </TouchableOpacity>
      </View>
      <CustomAlert
        visible={alertState.visible}
        title={alertState.title}
        message={alertState.message}
        buttons={alertState.buttons}
        onDismiss={hideAlert}
      />
    </>
  );
}

const createStyles = (theme: any) =>
  StyleSheet.create({
    imageModalActions: {
      position: 'absolute',
      top: 50,
      left: 0,
      right: 0,
      flexDirection: 'row',
      justifyContent: 'center',
      paddingHorizontal: 32,
    },
    imageActionButton: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: theme.colors.primary,
      paddingHorizontal: 24,
      paddingVertical: 12,
      borderRadius: 25,
      marginHorizontal: 8,
    },
    deleteButton: {
      backgroundColor: '#F44336',
    },
    imageActionText: {
      color: theme.colors.background,
      fontSize: 16,
      fontWeight: '600',
      marginLeft: 8,
    },
    deleteButtonText: {
      color: '#fff',
    },
  });
