package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.util.List;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.effect.ColorAdjust;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.Node;

public class AttendanceLogDialogLoader extends FXLoader {
    private static AttendanceLogDialogController lastController;
    private AttendanceLogDialogController controller;

    public AttendanceLogDialogLoader(Student student, LocalDate date, List<AttendanceLog> logs) {
        String fxmlPath = "/sms/admin/app/attendance/dialog/ATTENDANCE_LOG_DIALOG.fxml";
        createInstance(getClass().getResource(fxmlPath));
        
        // Add parameters before initialization
        addParameter("STUDENT", student);
        addParameter("DATE", date);
        addParameter("LOGS", logs);
        addParameter("OWNER_STAGE", Stage.getWindows().stream()
            .filter(Window::isFocused)
            .findFirst()
            .orElse(null));
            
        initialize();
    }

    public AttendanceLogDialogController getController() {
        return controller;
    }

    @Override
    public void load() {
        try {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Get owner stage
            Stage ownerStage = (Stage) getParameter("OWNER_STAGE");
            if (ownerStage != null) {
                stage.initOwner(ownerStage);
                
                // Add blur and color adjust for better visual effect
                GaussianBlur blur = new GaussianBlur(0);
                ColorAdjust colorAdjust = new ColorAdjust();
                colorAdjust.setBrightness(0);
                
                ownerStage.getScene().getRoot().setEffect(blur);
                
                // Animate blur and dimming
                Timeline blurAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, 
                        new KeyValue(blur.radiusProperty(), 0),
                        new KeyValue(colorAdjust.brightnessProperty(), 0)),
                    new KeyFrame(Duration.millis(200), 
                        new KeyValue(blur.radiusProperty(), 3),
                        new KeyValue(colorAdjust.brightnessProperty(), -0.1))
                );
                
                stage.setOnHiding(e -> {
                    Timeline reverseBlur = new Timeline(
                        new KeyFrame(Duration.millis(200),
                            new KeyValue(blur.radiusProperty(), 0),
                            new KeyValue(colorAdjust.brightnessProperty(), 0))
                    );
                    reverseBlur.setOnFinished(event -> 
                        ownerStage.getScene().getRoot().setEffect(null));
                    reverseBlur.play();
                });
                
                blurAnimation.play();
            }

            // Set dimensions with minimums
            double dialogWidth = 700;
            double dialogHeight = 600;
            stage.setWidth(dialogWidth);
            stage.setHeight(dialogHeight);
            stage.setMinWidth(700);  // Minimum width to show all columns
            stage.setMinHeight(400); // Minimum height for usability

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            
            // Reapply CSS loading
            scene.getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/attendance-log-dialog.css").toExternalForm()
            );

            // Configure controller
            controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Failed to load dialog controller");
            }

            // Transfer state from previous instance if it exists
            if (lastController != null) {
                controller.restoreState(lastController);
            }
            lastController = controller;

            // Set stage and initialize data
            controller.setStage(stage);
            controller.initData(
                (Student) getParameter("STUDENT"),
                (LocalDate) getParameter("DATE"),
                (List<AttendanceLog>) getParameter("LOGS")
            );

            // Make dialog draggable
            root.setOnMousePressed(event -> {
                root.setOnMouseDragged(e -> {
                    stage.setX(e.getScreenX() - event.getSceneX());
                    stage.setY(e.getScreenY() - event.getScreenY());
                });
            });

            // Position relative to owner
            if (ownerStage != null) {
                stage.setX(ownerStage.getX() + (ownerStage.getWidth() - dialogWidth) / 2);
                stage.setY(ownerStage.getY() + (ownerStage.getHeight() - dialogHeight) / 2);
            }

            // Configure enter animation
            root.setScaleX(0.9);
            root.setScaleY(0.9);
            root.setOpacity(0);

            ParallelTransition showAnimation = new ParallelTransition(
                createFadeTransition(root, 0, 1, 200),
                createScaleTransition(root, 0.9, 1.0, 200)
            );
            showAnimation.setInterpolator(Interpolator.EASE_OUT);

            stage.show();
            showAnimation.play();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load attendance log dialog", e);
        }
    }

    private FadeTransition createFadeTransition(Node node, double from, double to, int durationMs) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(from);
        fade.setToValue(to);
        return fade;
    }

    private ScaleTransition createScaleTransition(Node node, double from, double to, int durationMs) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMs), node);
        scale.setFromX(from);
        scale.setFromY(from);
        scale.setToX(to);
        scale.setToY(to);
        return scale;
    }
}