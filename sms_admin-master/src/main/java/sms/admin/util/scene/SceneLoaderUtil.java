package sms.admin.util.scene;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoader;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import sms.admin.util.loader.BaseLoader;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SceneLoaderUtil {
    private static final double FADE_DURATION = 100;
    private static final double MIN_WIDTH = 800;  // Reduced from 1024
    private static final double MIN_HEIGHT = 600; // Reduced from 768
    private static final Map<String, FXController> controllerCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <C extends FXController> C getControllerFromLoader(FXLoader loader) {
        if (!(loader instanceof BaseLoader)) {
            throw new IllegalArgumentException("Loader must extend BaseLoader");
        }
        return (C) ((BaseLoader) loader).getControllerFromLoader();
    }

    public static <C extends FXController> C loadScene(
            String fxmlPath,
            Class<?> resourceBase,
            Class<? extends FXLoader> loaderClass,
            Map<String, Object> parameters,
            StackPane contentPane) {

        try {
            // Clear cache to force refresh
            clearCache();

            URL resource = resourceBase.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalArgumentException("FXML resource not found: " + fxmlPath);
            }

            FXLoader loader = FXLoaderFactory.createInstance(loaderClass, resource);
            if (parameters != null) {
                parameters.forEach(loader::addParameter);
            }

            loader.initialize();
            Parent rootNode = loader.getRoot();

            if (rootNode != null) {
                // Update bindings immediately
                if (rootNode instanceof Region) {
                    Region region = (Region) rootNode;
                    region.prefWidthProperty().unbind();
                    region.prefHeightProperty().unbind();
                    region.prefWidthProperty().bind(contentPane.widthProperty());
                    region.prefHeightProperty().bind(contentPane.heightProperty());
                }
                setContentPane(rootNode, contentPane);
            }

            loader.load();
            C controller = getControllerFromLoader(loader);
            if (controller != null) {
                controllerCache.put(fxmlPath, controller);
            }

            return controller;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }

    public static <C extends FXController> C loadSceneWithYear(
            String fxmlPath,
            Class<?> baseClass,
            Class<? extends FXLoader> loaderClass,
            String selectedYear,
            StackPane contentPane) {
        return loadScene(
                fxmlPath,
                baseClass,
                loaderClass,
                Map.of("selectedYear", selectedYear),
                contentPane);
    }

    private static void applyFadeTransition(Parent rootNode) {
        rootNode.setOpacity(0.0);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(FADE_DURATION), rootNode);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    private static void setContentPane(Parent rootNode, StackPane contentPane) {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(rootNode);
        
        if (rootNode instanceof Region) {
            Region region = (Region) rootNode;
            // Set minimum size constraints
            region.setMinWidth(MIN_WIDTH);
            region.setMinHeight(MIN_HEIGHT);
            
            // Set preferred size bindings
            region.prefWidthProperty().bind(contentPane.widthProperty());
            region.prefHeightProperty().bind(contentPane.heightProperty());
            
            // Set max size bindings
            region.maxWidthProperty().bind(contentPane.widthProperty());
            region.maxHeightProperty().bind(contentPane.heightProperty());
        }
        
        applyFadeTransition(rootNode);
    }

    @SuppressWarnings("unchecked")
    public static <C extends FXController> C getCachedController(String fxmlPath) {
        return (C) controllerCache.get(fxmlPath);
    }

    public static void clearCache() {
        controllerCache.clear();
    }
}
