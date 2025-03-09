package sms.admin.app.attendance;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import sms.admin.app.attendance.dialog.AttendanceLogDialogController;

public class AttendanceLogDialog extends Dialog<Void> {
    public AttendanceLogDialog(Student student, LocalDate date, List<AttendanceLog> allLogs) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sms/admin/app/attendance/dialog/ATTENDANCE_LOG_DIALOG.fxml"));
            Parent root = loader.load();
            AttendanceLogDialogController controller = loader.getController();
            controller.initData(student, date, allLogs);
            getDialogPane().setContent(root);
            // Apply the main.css style sheet to enforce the app color scheme
            getDialogPane().getStylesheets().add(getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm());
            // Removed default button addition:
            // getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
