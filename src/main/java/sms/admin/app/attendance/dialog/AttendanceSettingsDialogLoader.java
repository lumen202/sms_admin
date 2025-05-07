package sms.admin.app.attendance.dialog;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;

import dev.sol.core.application.loader.FXLoader;
import sms.admin.app.attendance.model.AttendanceSettings;

/**
 * Loader for the attendance settings dialog, responsible for initializing and
 * displaying the dialog.
 * This class sets up the stage, applies visual effects, and handles animations
 * for the dialog.
 */
public class AttendanceSettingsDialogLoader extends FXLoader {
    private static final int ANIMATION_DURATION_MS = 200; // Duration of animations in milliseconds
    private AttendanceSettingsDialogController controller; // Controller for the dialog
    private final AttendanceSettings settings; // Attendance settings to configure

    /**
     * Constructor for the AttendanceSettingsDialogLoader.
     *
     * @param settings The attendance settings to be configured in the dialog.
     */
    @SuppressWarnings("exports")
    public AttendanceSettingsDialogLoader(AttendanceSettings settings) {
        String fxmlPath = "/sms/admin/app/attendance/dialog/ATTENDANCE_SETTINGS_DIALOG.fxml";
        createInstance(getClass().getResource(fxmlPath));
        this.settings = settings;
        initialize();
    }

    /**
     * Loads and displays the attendance settings dialog, setting up the stage and
     * animations.
     */
    @Override
    public void load() {
        try {
            // Create and configure scene
            Scene scene = createAndConfigureScene();

            // Create and configure stage
            Stage stage = createAndConfigureStage();
            stage.setScene(scene);

            // Initialize controller
            initializeController(stage);

            // Position and show dialog
            positionAndShowDialog(stage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load settings dialog", e);
        }
    }

    /**
     * Creates and configures the scene for the dialog.
     *
     * @return The configured Scene instance.
     */
    private Scene createAndConfigureScene() {
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        // Add required stylesheets
        String[] stylesheets = {
                "/sms/admin/assets/styles/skins/primer_light.css",
                "/sms/admin/app/attendance/dialog/attendance-settings-dialog.css"
        };

        for (String stylesheet : stylesheets) {
            URL resource = getClass().getResource(stylesheet);
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            } else {
                System.err.println("Could not find stylesheet: " + stylesheet);
            }
        }

        return scene;
    }

    /**
     * Creates and configures the stage for the dialog.
     *
     * @return The configured Stage instance.
     */
    private Stage createAndConfigureStage() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        Window owner = (Window) getParameter("OWNER_STAGE");
        if (owner != null) {
            stage.initOwner(owner);
            applyOwnerStageEffects((Stage) owner, stage);
        }
        return stage;
    }

    /**
     * Initializes the controller for the dialog.
     *
     * @param stage The stage associated with the dialog.
     */
    private void initializeController(Stage stage) {
        controller = loader.getController();
        if (controller != null) {
            controller.setStage(stage);
            String currentMonth = (String) getParameter("CURRENT_MONTH");
            controller.setSettings(settings, currentMonth);
        }
    }

    /**
     * Positions the dialog stage relative to the owner stage and shows it.
     *
     * @param stage The dialog stage to position and show.
     */
    private void positionAndShowDialog(Stage stage) {
        Window owner = stage.getOwner();
        if (owner instanceof Stage ownerStage) {
            positionStageRelativeToOwner(stage, ownerStage);
        }
        applyEnterAnimation(stage);
        stage.show();
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
        ownerStage.getScene().getRoot().setEffect(blur);

        // Apply fade effect to owner stage
        FadeTransition fade = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS),
                ownerStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.7);
        fade.play();

        // Reverse effects when the dialog is closed
        dialogStage.setOnHiding(e -> {
            FadeTransition reverseFade = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS),
                    ownerStage.getScene().getRoot());
            reverseFade.setFromValue(0.7);
            reverseFade.setToValue(1.0);
            reverseFade.setOnFinished(event -> ownerStage.getScene().getRoot().setEffect(null));
            reverseFade.play();
        });
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
                createFadeTransition(root),
                createScaleTransition(root));
        showAnimation.setInterpolator(Interpolator.EASE_OUT);

        // Show the stage and play the animation
        stage.show();
        showAnimation.play();
    }

    /**
     * Creates a fade transition for a node.
     *
     * @param node The node to apply the fade to.
     * @return The configured FadeTransition.
     */
    private FadeTransition createFadeTransition(javafx.scene.Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        return fade;
    }

    /**
     * Creates a scale transition for a node.
     *
     * @param node The node to apply the scale to.
     * @return The configured ScaleTransition.
     */
    private ScaleTransition createScaleTransition(javafx.scene.Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(ANIMATION_DURATION_MS), node);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1.0);
        scale.setToY(1.0);
        return scale;
    }

    /**
     * Positions the dialog stage relative to the owner stage for centered
     * placement.
     *
     * @param stage      The dialog stage to position.
     * @param ownerStage The owner stage to position relative to.
     */
    private void positionStageRelativeToOwner(Stage stage, Stage ownerStage) {
        // Get the button coordinates from parameters
        Double buttonX = (Double) getParameter("BUTTON_X");
        Double buttonY = (Double) getParameter("BUTTON_Y");

        if (buttonX != null && buttonY != null && ownerStage != null) {
            // Convert button coordinates to screen coordinates
            double screenX = ownerStage.getX() + buttonX;
            double screenY = ownerStage.getY() + buttonY;

            // Position the dialog slightly below and to the right of the button
            stage.setX(screenX - 20); // Offset to the left slightly
            stage.setY(screenY + 40); // Position below the button

            // Ensure the dialog stays within the screen bounds
            if (stage.getX() + root.prefWidth(-1) > ownerStage.getX() + ownerStage.getWidth()) {
                stage.setX(ownerStage.getX() + ownerStage.getWidth() - root.prefWidth(-1) - 20);
            }
        }
    }

    /**
     * Gets the controller for the attendance settings dialog.
     *
     * @return The current AttendanceSettingsDialogController instance.
     */
    public AttendanceSettingsDialogController getController() {
        return controller;
    }
}