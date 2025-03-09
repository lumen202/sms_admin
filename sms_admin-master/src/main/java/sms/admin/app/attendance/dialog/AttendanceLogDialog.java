package sms.admin.app.attendance.dialog;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

public class AttendanceLogDialog extends Dialog<Void> {
    public AttendanceLogDialog(Student student, LocalDate date, List<AttendanceLog> allLogs) {
        initStyle(StageStyle.TRANSPARENT);
        initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sms/admin/app/attendance/dialog/ATTENDANCE_LOG_DIALOG.fxml"));
            Parent root = loader.load();
            AttendanceLogDialogController controller = loader.getController();
            controller.initData(student, date, allLogs);
            
            getDialogPane().setContent(root);
            getDialogPane().getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm()
            );
            
            // Make both the dialog pane and its background transparent
            getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            getDialogPane().getScene().setFill(null);
            
            getDialogPane().getButtonTypes().clear();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to load attendance log dialog", ex);
        }
    }
}
