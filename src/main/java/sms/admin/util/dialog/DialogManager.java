package sms.admin.util.dialog;

import javafx.animation.FadeTransition;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Utility class for managing dialog-related visual effects and transitions.
 * 
 * <p>
 * Provides functionality to apply blur effects to background windows
 * when a dialog is active, and to close dialogs with a fade-out transition.
 * </p>
 */
public class DialogManager {
    private static final double BLUR_AMOUNT = 5.0; // Reduced blur amount

    /**
     * Applies or removes a Gaussian blur effect to the specified owner window.
     * 
     * <p>
     * This is typically used to visually distinguish modal dialogs by blurring the
     * background.
     * </p>
     *
     * @param owner  the window to apply the effect to
     * @param enable {@code true} to apply the blur effect, {@code false} to remove
     *               it
     */
    public static void setOverlayEffect(Window owner, boolean enable) {
        if (owner instanceof Stage ownerStage && ownerStage.getScene() != null) {
            if (enable) {
                ownerStage.getScene().getRoot().setEffect(new GaussianBlur(BLUR_AMOUNT));
            } else {
                ownerStage.getScene().getRoot().setEffect(null);
            }
        }
    }

    /**
     * Closes the specified stage with a fade-out animation.
     *
     * <p>
     * Also removes any overlay effect (such as blur) from the stage's owner window
     * before closing. An optional callback can be executed after the stage is
     * closed.
     * </p>
     *
     * @param stage      the stage to close
     * @param onFinished an optional {@link Runnable} to execute after the fade-out
     *                   completes and the stage is closed;
     *                   may be {@code null}
     */
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
                if (onFinished != null)
                    onFinished.run();
            });
            fade.play();
        }
    }
}
