package sms.admin.util.dialog;

import javafx.animation.FadeTransition;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class DialogManager {
    private static final double BLUR_AMOUNT = 5.0; // Reduced blur amount
    
    public static void setOverlayEffect(Window owner, boolean enable) {
        if (owner instanceof Stage ownerStage && ownerStage.getScene() != null) {
            if (enable) {
                ownerStage.getScene().getRoot().setEffect(new GaussianBlur(BLUR_AMOUNT));
            } else {
                ownerStage.getScene().getRoot().setEffect(null);
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
