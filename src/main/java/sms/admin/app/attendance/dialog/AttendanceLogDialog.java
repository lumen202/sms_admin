package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.util.List;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Node;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.util.Duration;
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

        // Set dialog content
        Parent root = loader.getRoot();
        if (root == null) {
            throw new RuntimeException("Failed to load FXML root");
        }

        // Remove inline styling and rely on CSS
        this.setTitle("Attendance Log Details");
        this.getDialogPane().getStyleClass().add("attendance-log-dialog");
        this.getDialogPane().setContent(root);
        
        // Add stylesheets
        this.getDialogPane().getStylesheets().addAll(
            getClass().getResource("/sms/admin/app/attendance/dialog/attendance-log-dialog.css").toExternalForm(),
            getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm()
        );
                
        // Center on screen with proper size
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        this.setX((screenBounds.getWidth() - 600) / 2);
        this.setY((screenBounds.getHeight() - 400) / 2);

        // Get the owner window
        Window owner = Stage.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
                
        if (owner instanceof Stage) {
            this.initOwner(owner);
            this.initModality(Modality.APPLICATION_MODAL);
            this.initStyle(StageStyle.TRANSPARENT);
            
            Node ownerRoot = owner.getScene().getRoot();
            GaussianBlur blur = new GaussianBlur(0);
            
            // Add blur effect with smooth transition
            this.setOnShowing(event -> {
                ownerRoot.setEffect(blur);
                Timeline timeline = new Timeline();
                timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
                    new KeyFrame(Duration.millis(200), new KeyValue(blur.radiusProperty(), 5))
                );
                timeline.play();
            });

            // Remove blur effect with smooth transition
            this.setOnHiding(event -> {
                Timeline timeline = new Timeline();
                timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 5)),
                    new KeyFrame(Duration.millis(200), new KeyValue(blur.radiusProperty(), 0))
                );
                timeline.setOnFinished(e -> {
                    ownerRoot.setEffect(null);
                    // Remove the reference to the blur effect
                    blur.setRadius(0);
                });
                timeline.play();
            });

            // Ensure blur is removed on close
            this.setOnCloseRequest(event -> {
                ownerRoot.setEffect(null);
            });
        }

        // Set close request handling
        this.setOnCloseRequest(event -> {
            // Clean up any resources if needed
            if (loader != null) {
                loader = null;
            }
            controller = null;
        });

        // Set close handler for the dialog
        this.setOnCloseRequest(event -> {
            Platform.runLater(() -> {
                if (loader != null) {
                    loader = null;
                }
                controller = null;
                this.close();
            });
        });

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
            Platform.runLater(() -> {
                AttendanceLogDialog.this.setResultConverter(dialogButton -> null);
                AttendanceLogDialog.this.initStyle(StageStyle.TRANSPARENT);
                AttendanceLogDialog.this.showAndWait();
            });
        }
    }
}