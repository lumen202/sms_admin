package sms.admin.util.dialog;

import javafx.animation.FadeTransition;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DialogManager {
    
    public static void setOverlayEffect(Stage ownerStage, boolean enable) {
        if (ownerStage != null && ownerStage.getScene() != null) {
            ownerStage.getScene().getRoot().setStyle(
                enable ? "-fx-opacity: 0.5; -fx-background-color: rgba(0, 0, 0, 0.5);" : ""
            );
        }
    }
    
    public static void closeWithFade(Stage stage, Runnable onFinished) {
        if (stage != null) {
            setOverlayEffect((Stage) stage.getOwner(), false);
            
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
