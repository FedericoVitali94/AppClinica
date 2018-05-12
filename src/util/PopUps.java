package util;

import javafx.scene.control.Alert;

/**
 *
 * @author den
 */
public class PopUps {
   private static void showPopUp(String title, String message, Alert.AlertType type) {
      Alert popup = new Alert(type);
      popup.setTitle(title);
      popup.setContentText(message);
      popup.showAndWait();
   }
   
   public static void showError(String title, String message) {
      PopUps.showPopUp(title, message, Alert.AlertType.ERROR);
   }
   
   public static void showInfo(String title, String message) {
      PopUps.showPopUp(title, message, Alert.AlertType.INFORMATION);
   }
   
   private PopUps() { }
   
}
