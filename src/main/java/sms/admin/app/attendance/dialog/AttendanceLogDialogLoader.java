package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.loader.FXLoader;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

public class AttendanceLogDialogLoader extends FXLoader {
    private static final double DIALOG_WIDTH = 700;
    private static final double DIALOG_HEIGHT = 600;
    private static final double MIN_WIDTH = 700;
    private static final double MIN_HEIGHT = 400;
    private static final int ANIMATION_DURATION_MS = 200;

    private static AttendanceLogDialogController lastController;
    private AttendanceLogDialogController controller;

    public AttendanceLogDialogLoader(Student student, LocalDate date, List<AttendanceLog> logs) {
        String fxmlPath = "/sms/admin/app/attendance/dialog/ATTENDANCE_LOG_DIALOG.fxml";
        createInstance(getClass().getResource(fxmlPath));

        // Add parameters before initialization
        addParameter("STUDENT", student);
        addParameter("DATE", date);
        addParameter("LOGS", logs);
        // Get the focused window as owner stage
        Optional<Window> owner = Stage.getWindows().stream().filter(Window::isFocused).findFirst();
        addParameter("OWNER_STAGE", owner.orElse(null));

        initialize();
    }

    public AttendanceLogDialogController getController() {
        return controller;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void load() {
        try {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            Stage ownerStage = (Stage) getParameter("OWNER_STAGE");
            if (ownerStage != null) {
                stage.initOwner(ownerStage);
                applyOwnerStageEffects(ownerStage, stage);
            }

            configureStageDimensions(stage);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            loadStyles(scene);

            controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Failed to load dialog controller");
            }

            // Transfer previous state if available
            if (lastController != null) {
                controller.restoreState(lastController);
            }
            lastController = controller;

            controller.setStage(stage);
            controller.initData(
                    (Student) getParameter("STUDENT"),
                    (LocalDate) getParameter("DATE"),
                    (List<AttendanceLog>) getParameter("LOGS"));

            makeDialogDraggable(stage);
            positionStageRelativeToOwner(stage, ownerStage);

            applyEnterAnimation(stage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load attendance log dialog", e);
        }
    }

    private void configureStageDimensions(Stage stage) {
        stage.setWidth(DIALOG_WIDTH);
        stage.setHeight(DIALOG_HEIGHT);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
    }

    private void loadStyles(Scene scene) {
        scene.getStylesheets().add(getClass()
                .getResource("/sms/admin/app/attendance/dialog/attendance-log-dialog.css")
                .toExternalForm());
    }

    private void applyOwnerStageEffects(Stage ownerStage, Stage dialogStage) {
        GaussianBlur blur = new GaussianBlur(0);
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(0);
        ownerStage.getScene().getRoot().setEffect(blur);

        Timeline blurAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(blur.radiusProperty(), 0),
                        new KeyValue(colorAdjust.brightnessProperty(), 0)),
                new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                        new KeyValue(blur.radiusProperty(), 3),
                        new KeyValue(colorAdjust.brightnessProperty(), -0.1)));
        blurAnimation.play();

        dialogStage.setOnHiding(e -> {
            Timeline reverseBlur = new Timeline(
                    new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                            new KeyValue(blur.radiusProperty(), 0),
                            new KeyValue(colorAdjust.brightnessProperty(), 0)));
            reverseBlur.setOnFinished(event -> ownerStage.getScene().getRoot().setEffect(null));
            reverseBlur.play();
        });
    }

    private void makeDialogDraggable(Stage stage) {
        root.setOnMousePressed(event -> {
            root.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - event.getSceneX());
                stage.setY(e.getScreenY() - event.getSceneY());
            });
        });
    }

    private void positionStageRelativeToOwner(Stage stage, Stage ownerStage) {
        if (ownerStage != null) {
            stage.setX(ownerStage.getX() + (ownerStage.getWidth() - DIALOG_WIDTH) / 2);
            stage.setY(ownerStage.getY() + (ownerStage.getHeight() - DIALOG_HEIGHT) / 2);
        }
    }

    private void applyEnterAnimation(Stage stage) {
        // Set initial properties for animation
        root.setScaleX(0.9);
        root.setScaleY(0.9);
        root.setOpacity(0);

        ParallelTransition showAnimation = new ParallelTransition(
                createFadeTransition(root, 0, 1, ANIMATION_DURATION_MS),
                createScaleTransition(root, 0.9, 1.0, ANIMATION_DURATION_MS));
        showAnimation.setInterpolator(Interpolator.EASE_OUT);

        stage.show();
        showAnimation.play();
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
