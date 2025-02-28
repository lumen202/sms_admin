package sms.admin.util;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoader;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SceneLoaderUtil {
    private static final double FADE_DURATION = 100; // Reduced duration for smoother transition
    private static final Map<String, FXController> controllerCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <L extends FXLoader & ControllerLoader<C>, C extends FXController> C loadScene(
            String fxmlPath,
            Class<?> resourceBase,
            Class<L> loaderClass,
            Map<String, Object> parameters,
            StackPane contentPane) {

        try {
            URL resource = resourceBase.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalArgumentException("FXML resource not found: " + fxmlPath);
            }

            L loader = (L) FXLoaderFactory.createInstance(loaderClass, resource);
            if (parameters != null) {
                parameters.forEach(loader::addParameter);
            }

            loader.initialize();
            Parent rootNode = loader.getRoot();
            applyFadeTransition(rootNode);
            setContentPane(rootNode, contentPane);

            loader.load();
            C controller = (C) loader.getController();
            controllerCache.put(fxmlPath, controller);

            return controller;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }

    public static <L extends FXLoader & ControllerLoader<C>, C extends FXController> C loadSceneWithYear(
            String fxmlPath,
            Class<?> baseClass,
            Class<L> loaderClass,
            String selectedYear,
            StackPane contentPane) {
        try {
            L loader = loaderClass.getDeclaredConstructor().newInstance();
            loader.createInstance(baseClass.getResource(fxmlPath));
            loader.addParameter("selectedYear", selectedYear);
            loader.initialize();

            Parent root = loader.getRoot();
            if (root == null) {
                throw new RuntimeException("Failed to load FXML: " + fxmlPath);
            }

            // Create fade out transition for current content
            if (!contentPane.getChildren().isEmpty()) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(FADE_DURATION),
                        contentPane.getChildren().get(0));
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                // Create fade in transition for new content
                root.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_DURATION), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                fadeOut.setOnFinished(e -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(root);
                    fadeIn.play();
                });

                fadeOut.play();
            } else {
                // If no current content, just fade in the new content
                root.setOpacity(0.0);
                contentPane.getChildren().add(root);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_DURATION), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }

            C controller = (C) loader.getController();
            if (controller != null) {
                controller.load();
            }

            return controller;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }

    private static void applyFadeTransition(Parent rootNode) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(FADE_DURATION), rootNode);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    private static void setContentPane(Parent rootNode, StackPane contentPane) {
        contentPane.getChildren().setAll(rootNode);
        if (rootNode instanceof Region) {
            Region region = (Region) rootNode;
            region.prefWidthProperty().bind(contentPane.widthProperty());
            region.prefHeightProperty().bind(contentPane.heightProperty());
        }
    }

    @SuppressWarnings("unchecked")
    public static <C extends FXController> C getCachedController(String fxmlPath) {
        return (C) controllerCache.get(fxmlPath);
    }

    public static void clearCache() {
        controllerCache.clear();
    }
}
