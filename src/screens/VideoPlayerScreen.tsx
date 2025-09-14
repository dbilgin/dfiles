import { StyleSheet, View } from 'react-native';
import Video from 'react-native-video';
import { useAppContext } from '../context/AppContext';
import { darkTheme, lightTheme } from '../utils/theme';
import { GalleryStackParamList } from '../navigation/GalleryStack';
import { StackScreenProps } from '@react-navigation/stack';
import { MediaFooter } from '../components/MediaFooter';

type Props = StackScreenProps<GalleryStackParamList, 'VideoPlayer'> 

export const VideoPlayerScreen = ({ route, navigation }: Props) => {
  const { video } = route.params;
  const { state } = useAppContext();
  const theme = state.isDarkMode ? darkTheme : lightTheme;
  const styles = createStyles(theme);

  return (
    <View style={styles.container}>
      <Video
        source={{ uri: video }}
        style={{ flex: 1 }}
        controls
      />
      <MediaFooter
        name={video}
        path={video}
        onDeleteSuccess={() => navigation.goBack()}
      />
    </View>
  );
};

const createStyles = (theme: any) =>
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: theme.colors.background,
    },
  });