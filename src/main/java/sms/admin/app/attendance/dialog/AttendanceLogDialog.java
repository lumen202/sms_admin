package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.util.List;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;
import sms.admin.util.loader.BaseLoader;

public class AttendanceLogDialog extends Dialog<Void> {
    private static final String PARAM_STUDENT = "student";
    private static final String PARAM_DATE = "date";
    private static final String PARAM_LOGS = "logs";

    private BaseLoader loader;
    private Student student;
    private LocalDate date;
    private List<AttendanceLog> logs;
    private AttendanceLogDialogController controller;

    public AttendanceLogDialog(Student student, LocalDate date, List<AttendanceLog> allLogs) {
        this.student = student;
        this.date = date;
        this.logs = allLogs;

        initializeLoader();
        // loadDialog() is now handled in CustomBaseLoader.load()
    }

    private void initializeLoader() {
        loader = new CustomBaseLoader();
        loader.createInstance(getClass().getResource("/sms/admin/app/attendance/dialog/ATTENDANCE_LOG_DIALOG.fxml"));
        loader.addParameter(PARAM_STUDENT, student)
                .addParameter(PARAM_DATE, date)
                .addParameter(PARAM_LOGS, logs);

        if (loader.initialize() == null) {
            throw new RuntimeException("Failed to initialize FXML loader");
        }

        controller = (AttendanceLogDialogController) loader.getControllerFromLoader();
        if (controller == null) {
            throw new RuntimeException("Controller not initialized by loader");
        }

        controller.initData(
                (Student) loader.getParameter(PARAM_STUDENT),
                (LocalDate) loader.getParameter(PARAM_DATE),
                (List<AttendanceLog>) loader.getParameter(PARAM_LOGS));

        // Set dialog content
        Parent root = loader.getRoot();
        if (root == null) {
            throw new RuntimeException("Failed to load FXML root");
        }

        // Make dialog completely transparent
        this.getDialogPane().setStyle("-fx-background-color: transparent;");
        this.getDialogPane().getScene().setFill(null);
        this.getDialogPane().setContent(root);

        // Add stylesheets
        this.getDialogPane().getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm());

        // Call load() to show the dialog
        loader.load();
    }

    public Parent getRoot() {
        return loader.getRoot();
    }

    // Local concrete subclass of BaseLoader
    private class CustomBaseLoader extends BaseLoader {
        public CustomBaseLoader() {
            // No-args constructor
        }

        @Override
        public void load() {
            // Show the dialog
            AttendanceLogDialog.this.setResultConverter(dialogButton -> null);
            AttendanceLogDialog.this.initStyle(StageStyle.TRANSPARENT); // Move initStyle to dialog
            AttendanceLogDialog.this.showAndWait();
        }
    }
}