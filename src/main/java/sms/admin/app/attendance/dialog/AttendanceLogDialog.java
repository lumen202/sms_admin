package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.util.List;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import sms.admin.util.dialog.DialogManager;
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
    private Stage ownerStage;

    public AttendanceLogDialog(Student student, LocalDate date, List<AttendanceLog> allLogs) {
        this.student = student;
        this.date = date;
        this.logs = allLogs;

        initializeLoader();
    }

    private void initializeLoader() {
        loader = new CustomBaseLoader();
        loader.createInstance(getClass().getResource("/sms/admin/app/attendance/dialog/ATTENDANCE_LOG_DIALOG.fxml"));
        
        // Validate and add parameters
        if (student == null || date == null || logs == null) {
            throw new IllegalArgumentException("Required parameters cannot be null");
        }

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

        this.setTitle(null);
        this.initStyle(StageStyle.TRANSPARENT);
        this.initModality(Modality.APPLICATION_MODAL);

        // Get the owner window and setup
        Window owner = Stage.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
                
        if (owner instanceof Stage stage) {
            ownerStage = stage;
            this.initOwner(ownerStage);
            this.getDialogPane().setContent(loader.getRoot());
            this.getDialogPane().getStylesheets().add(
                getClass().getResource("/sms/admin/app/styles/attendance-log-dialog.css").toExternalForm()
            );

            // Apply blur on show
            this.setOnShowing(event -> {
                DialogManager.setOverlayEffect(ownerStage, true);
            });

            // Clear blur and close with fade
            this.setOnCloseRequest(event -> {
                event.consume(); // Prevent immediate close
                handleClose();
            });

            // Handle escape key
            this.getDialogPane().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    event.consume();
                    handleClose();
                }
            });

            // Handle dialog hide
            this.setOnHidden(event -> {
                DialogManager.setOverlayEffect(ownerStage, false);
                cleanup();
            });
        }

        // Call load() to show the dialog
        loader.load();
    }

    private void handleClose() {
        Stage dialogStage = (Stage) this.getDialogPane().getScene().getWindow();
        DialogManager.closeWithFade(dialogStage, () -> {
            DialogManager.setOverlayEffect(ownerStage, false);
            cleanup();
            super.hide(); // Use hide instead of close
        });
    }

    private void centerDialog() {
        Window window = this.getDialogPane().getScene().getWindow();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        window.setX((screenBounds.getWidth() - window.getWidth()) / 2);
        window.setY((screenBounds.getHeight() - window.getHeight()) / 2);
    }

    private void cleanup() {
        if (ownerStage != null) {
            DialogManager.setOverlayEffect(ownerStage, false);
            ownerStage = null;
        }
        loader = null;
        controller = null;
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
            Platform.runLater(() -> {
                AttendanceLogDialog.this.showAndWait();
            });
        }
    }
}