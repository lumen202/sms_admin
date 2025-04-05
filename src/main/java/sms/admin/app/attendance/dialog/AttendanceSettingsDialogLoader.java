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
import dev.sol.core.application.loader.FXLoader;
import sms.admin.app.attendance.model.AttendanceSettings;

public class AttendanceSettingsDialogLoader extends FXLoader {
    private static final int ANIMATION_DURATION_MS = 200;
    private AttendanceSettingsDialogController controller;
    private final AttendanceSettings settings;

    public AttendanceSettingsDialogLoader(AttendanceSettings settings) {
        String fxmlPath = "/sms/admin/app/attendance/dialog/ATTENDANCE_SETTINGS_DIALOG.fxml";
        createInstance(getClass().getResource(fxmlPath));
        this.settings = settings;
        initialize();
    }

    @Override
    public void load() {
        try {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            Window owner = (Window) getParameter("OWNER_STAGE");
            if (owner != null) {
                stage.initOwner(owner);
                applyOwnerStageEffects((Stage)owner, stage);
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            
            controller = loader.getController();
            controller.setStage(stage);
            String currentMonth = (String) getParameter("CURRENT_MONTH");
            controller.setSettings(settings, currentMonth);

            positionStageRelativeToOwner(stage, (Stage)owner);
            applyEnterAnimation(stage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load settings dialog", e);
        }
    }

    private void applyOwnerStageEffects(Stage ownerStage, Stage dialogStage) {
        GaussianBlur blur = new GaussianBlur(0);
        ColorAdjust colorAdjust = new ColorAdjust();
        ownerStage.getScene().getRoot().setEffect(blur);

        dialogStage.setOnHiding(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), ownerStage.getScene().getRoot());
            fade.setFromValue(0.7);
            fade.setToValue(1.0);
            fade.setOnFinished(event -> ownerStage.getScene().getRoot().setEffect(null));
            fade.play();
        });

        FadeTransition fade = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), ownerStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.7);
        fade.play();
    }

    private void applyEnterAnimation(Stage stage) {
        root.setScaleX(0.9);
        root.setScaleY(0.9);
        root.setOpacity(0);

        ParallelTransition showAnimation = new ParallelTransition(
                createFadeTransition(root),
                createScaleTransition(root));
        showAnimation.setInterpolator(Interpolator.EASE_OUT);

        stage.show();
        showAnimation.play();
    }

    private FadeTransition createFadeTransition(javafx.scene.Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(ANIMATION_DURATION_MS), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        return fade;
    }

    private ScaleTransition createScaleTransition(javafx.scene.Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(ANIMATION_DURATION_MS), node);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1.0);
        scale.setToY(1.0);
        return scale;
    }

    private void positionStageRelativeToOwner(Stage stage, Stage ownerStage) {
        if (ownerStage != null) {
            stage.setX(ownerStage.getX() + (ownerStage.getWidth() - root.prefWidth(-1)) / 2);
            stage.setY(ownerStage.getY() + (ownerStage.getHeight() - root.prefHeight(-1)) / 2);
        }
    }

    public AttendanceSettingsDialogController getController() {
        return controller;
    }
}
