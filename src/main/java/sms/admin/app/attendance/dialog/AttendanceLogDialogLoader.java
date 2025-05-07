package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.net.URL;
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

/**
 * Loader for the attendance log dialog, responsible for initializing and
 * displaying the dialog.
 * This class sets up the stage, applies visual effects, and handles animations
 * for the dialog.
 */
public class AttendanceLogDialogLoader extends FXLoader {
    private static final double DIALOG_WIDTH = 700; // Default width of the dialog
    private static final double DIALOG_HEIGHT = 600; // Default height of the dialog
    private static final double MIN_WIDTH = 700; // Minimum width of the dialog
    private static final double MIN_HEIGHT = 400; // Minimum height of the dialog
    private static final int ANIMATION_DURATION_MS = 200; // Duration of animations in milliseconds

    private static AttendanceLogDialogController lastController; // Reference to the last controller for state
                                                                 // restoration
    private AttendanceLogDialogController controller; // Current dialog controller

    /**
     * Constructor for the AttendanceLogDialogLoader.
     *
     * @param student The student whose attendance logs are to be displayed.
     * @param date    The date (month and year) for the attendance logs.
     * @param logs    The list of attendance logs to display.
     */
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

    /**
     * Gets the controller for the attendance log dialog.
     *
     * @return The current AttendanceLogDialogController instance.
     */
    public AttendanceLogDialogController getController() {
        return controller;
    }

    /**
     * Loads and displays the attendance log dialog, setting up the stage and
     * animations.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void load() {
        try {
            // Create a transparent stage for the dialog
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            Stage ownerStage = (Stage) getParameter("OWNER_STAGE");
            if (ownerStage != null) {
                stage.initOwner(ownerStage);
                applyOwnerStageEffects(ownerStage, stage);
            }

            // Configure stage dimensions
            configureStageDimensions(stage);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            // Add required stylesheets
            String[] stylesheets = {
                "/sms/admin/assets/styles/skins/primer_light.css",
                "/sms/admin/app/attendance/dialog/attendance-log-dialog.css"
            };

            for (String stylesheet : stylesheets) {
                URL resource = getClass().getResource(stylesheet);
                if (resource != null) {
                    scene.getStylesheets().add(resource.toExternalForm());
                } else {
                    System.err.println("Could not find stylesheet: " + stylesheet);
                }
            }

            stage.setScene(scene);

            // Get and validate the controller
            controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Failed to load dialog controller");
            }

            // Transfer previous state if available
            if (lastController != null) {
                controller.restoreState(lastController);
            }
            lastController = controller;

            // Initialize controller with parameters
            controller.setStage(stage);
            controller.initData(
                    (Student) getParameter("STUDENT"),
                    (LocalDate) getParameter("DATE"),
                    (List<AttendanceLog>) getParameter("LOGS"));

            // Make the dialog draggable
            makeDialogDraggable(stage);
            // Position the dialog relative to the owner stage
            positionStageRelativeToOwner(stage, ownerStage);

            // Apply entrance animation and show the stage
            applyEnterAnimation(stage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load attendance log dialog", e);
        }
    }

    /**
     * Configures the dimensions of the dialog stage.
     *
     * @param stage The stage to configure.
     */
    private void configureStageDimensions(Stage stage) {
        stage.setWidth(DIALOG_WIDTH);
        stage.setHeight(DIALOG_HEIGHT);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
    }

    /**
     * Applies visual effects (blur and brightness) to the owner stage when the
     * dialog is shown.
     *
     * @param ownerStage  The owner stage to apply effects to.
     * @param dialogStage The dialog stage being shown.
     */
    private void applyOwnerStageEffects(Stage ownerStage, Stage dialogStage) {
        GaussianBlur blur = new GaussianBlur(0);
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(0);
        ownerStage.getScene().getRoot().setEffect(blur);

        // Animate blur and brightness effects
        Timeline blurAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(blur.radiusProperty(), 0),
                        new KeyValue(colorAdjust.brightnessProperty(), 0)),
                new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                        new KeyValue(blur.radiusProperty(), 3),
                        new KeyValue(colorAdjust.brightnessProperty(), -0.1)));
        blurAnimation.play();

        // Reverse effects when the dialog is closed
        dialogStage.setOnHiding(e -> {
            Timeline reverseBlur = new Timeline(
                    new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                            new KeyValue(blur.radiusProperty(), 0),
                            new KeyValue(colorAdjust.brightnessProperty(), 0)));
            reverseBlur.setOnFinished(event -> ownerStage.getScene().getRoot().setEffect(null));
            reverseBlur.play();
        });
    }

    /**
     * Makes the dialog draggable by allowing mouse drag events to move the stage.
     *
     * @param stage The stage to make draggable.
     */
    private void makeDialogDraggable(Stage stage) {
        root.setOnMousePressed(event -> {
            root.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - event.getSceneX());
                stage.setY(e.getScreenY() - event.getSceneY());
            });
        });
    }

    /**
     * Positions the dialog stage relative to the owner stage for centered
     * placement.
     *
     * @param stage      The dialog stage to position.
     * @param ownerStage The owner stage to position relative to.
     */
    private void positionStageRelativeToOwner(Stage stage, Stage ownerStage) {
        if (ownerStage != null) {
            stage.setX(ownerStage.getX() + (ownerStage.getWidth() - DIALOG_WIDTH) / 2);
            stage.setY(ownerStage.getY() + (ownerStage.getHeight() - DIALOG_HEIGHT) / 2);
        }
    }

    /**
     * Applies an entrance animation (fade and scale) to the dialog when it is
     * shown.
     *
     * @param stage The stage to animate.
     */
    private void applyEnterAnimation(Stage stage) {
        // Set initial properties for animation
        root.setScaleX(0.9);
        root.setScaleY(0.9);
        root.setOpacity(0);

        // Create parallel fade and scale animations
        ParallelTransition showAnimation = new ParallelTransition(
                createFadeTransition(root, 0, 1, ANIMATION_DURATION_MS),
                createScaleTransition(root, 0.9, 1.0, ANIMATION_DURATION_MS));
        showAnimation.setInterpolator(Interpolator.EASE_OUT);

        // Show the stage and play the animation
        stage.show();
        showAnimation.play();
    }

    /**
     * Creates a fade transition for a node.
     *
     * @param node       The node to apply the fade to.
     * @param from       The starting opacity.
     * @param to         The ending opacity.
     * @param durationMs The duration of the animation in milliseconds.
     * @return The configured FadeTransition.
     */
    private FadeTransition createFadeTransition(Node node, double from, double to, int durationMs) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(from);
        fade.setToValue(to);
        return fade;
    }

    /**
     * Creates a scale transition for a node.
     *
     * @param node       The node to apply the scale to.
     * @param from       The starting scale factor.
     * @param to         The ending scale factor.
     * @param durationMs The duration of the animation in milliseconds.
     * @return The configured ScaleTransition.
     */
    private ScaleTransition createScaleTransition(Node node, double from, double to, int durationMs) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMs), node);
        scale.setFromX(from);
        scale.setFromY(from);
        scale.setToX(to);
        scale.setToY(to);
        return scale;
    }
}