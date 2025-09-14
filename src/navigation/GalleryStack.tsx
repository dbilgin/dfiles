import { createStackNavigator } from "@react-navigation/stack"
import { GalleryScreen } from "../screens/GalleryScreen";
import { VideoPlayerScreen } from "../screens/VideoPlayerScreen";

export type GalleryStackParamList = {
    Gallery: undefined;
    VideoPlayer: {video: string};
}

const Stack = createStackNavigator<GalleryStackParamList>();

export const GalleryStack = () => {
    return (
        <Stack.Navigator initialRouteName="Gallery">
            <Stack.Screen name="Gallery" component={GalleryScreen} options={{ headerShown: false }} />
            <Stack.Screen name="VideoPlayer" component={VideoPlayerScreen} options={{ headerShown: false }} />
        </Stack.Navigator>
    );
}
