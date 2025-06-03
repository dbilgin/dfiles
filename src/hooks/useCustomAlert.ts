import {useState} from 'react';

interface AlertButton {
  text: string;
  onPress?: () => void;
  style?: 'default' | 'cancel' | 'destructive';
}

interface AlertState {
  visible: boolean;
  title: string;
  message?: string;
  buttons: AlertButton[];
}

export const useCustomAlert = () => {
  const [alertState, setAlertState] = useState<AlertState>({
    visible: false,
    title: '',
    message: undefined,
    buttons: [],
  });

  const showAlert = (
    title: string,
    message?: string,
    buttons: AlertButton[] = [{text: 'OK'}],
  ) => {
    setAlertState({
      visible: true,
      title,
      message,
      buttons,
    });
  };

  const hideAlert = () => {
    setAlertState(prev => ({
      ...prev,
      visible: false,
    }));
  };

  return {
    alertState,
    showAlert,
    hideAlert,
  };
};
