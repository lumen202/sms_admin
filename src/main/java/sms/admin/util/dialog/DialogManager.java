package sms.admin.util.dialog;

import javafx.animation.FadeTransition;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class DialogManager {
    private static final double BLUR_AMOUNT = 10.0; // Increased blur
    private static final double DARKEN_AMOUNT = 0.4; // Adjusted darkness
    
    public static void setOverlayEffect(Window owner, boolean enable) {
        if (owner instanceof Stage ownerStage && ownerStage.getScene() != null) {
            if (enable) {
                ownerStage.getScene().getRoot().setEffect(new GaussianBlur(BLUR_AMOUNT));
                ownerStage.getScene().getRoot().setStyle(
                    String.format("-fx-opacity: %.1f; -fx-background-color: rgba(0, 0, 0, %.1f);", 
                    DARKEN_AMOUNT, 0.5)
                );
            } else {
                ownerStage.getScene().getRoot().setEffect(null);
                ownerStage.getScene().getRoot().setStyle("");
            }
        }
    }

    public static void closeWithFade(Stage stage, Runnable onFinished) {
        if (stage != null) {
            // Always ensure blur is cleared from owner
            Window owner = stage.getOwner();
            if (owner instanceof Stage) {
                setOverlayEffect(owner, false);
            }
            
            FadeTransition fade = new FadeTransition(Duration.millis(200), stage.getScene().getRoot());
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> {
                stage.close();
                if (onFinished != null) onFinished.run();
            });
            fade.play();
        }
    }
}
