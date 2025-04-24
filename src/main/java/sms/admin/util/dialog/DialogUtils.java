package sms.admin.util.dialog;

import javafx.scene.control.Alert;

/**
 * Utility class for showing common dialog messages in the application.
 * 
 * <p>
 * This class simplifies the process of displaying standard alert dialogs
 * such as error and success messages using JavaFX {@link Alert} dialogs.
 * </p>
 */
public class DialogUtils {

    /**
     * Displays an error dialog with the specified title, header, and content.
     *
     * <p>
     * Uses {@link Alert.AlertType#ERROR} and waits for user confirmation before
     * closing.
     * </p>
     *
     * @param title   the title of the dialog window
     * @param header  the header text displayed above the content
     * @param content the main message content of the dialog
     */
    public static void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Displays a success (information) dialog with the specified title, header, and
     * content.
     *
     * <p>
     * Uses {@link Alert.AlertType#INFORMATION} and waits for user confirmation
     * before closing.
     * </p>
     *
     * @param title   the title of the dialog window
     * @param header  the header text displayed above the content
     * @param content the main message content of the dialog
     */
    public static void showSuccessDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
