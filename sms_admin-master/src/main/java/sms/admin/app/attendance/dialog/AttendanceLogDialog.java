package sms.admin.app.attendance.dialog;

import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;
import javafx.stage.Modality;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

public class AttendanceLogDialog extends Dialog<Void> {
    
    public AttendanceLogDialog() {
        initStyle(StageStyle.TRANSPARENT);
        initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ATTENDANCE_LOG_DIALOG.fxml"));
            getDialogPane().setContent(loader.load());

            getDialogPane().getStylesheets().add(
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm()
            );
            getDialogPane().setStyle("-fx-background-color: transparent;");
            
            // Remove default button types and result converter
            getDialogPane().getButtonTypes().clear();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load attendance log dialog", e);
        }
    }
}
