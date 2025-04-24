package sms.admin.util.scene;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoader;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import sms.admin.util.loader.BaseLoader;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading JavaFX scenes with FXML controllers, caching
 * controllers,
 * and applying fade-in transitions and responsive layout bindings.
 * <p>
 * This class leverages the dev.sol.core FXLoader framework along with a custom
 * {@link BaseLoader}
 * to type-safely retrieve controllers. Controllers are cached per FXML path,
 * and scenes are resized to fit a {@link StackPane} container with minimum size
 * constraints.
 * Fade-in transitions improve the user experience when switching views.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> Controller cache operations clear and insert
 * entries
 * synchronously, and should be invoked on the JavaFX Application Thread to
 * avoid
 * concurrency issues.
 * </p>
 *
 * @author YourName
 * @since 1.0
 */
public class SceneLoaderUtil {

    /** Duration of the fade-in animation in milliseconds. */
    private static final double FADE_DURATION = 200;

    /** Default minimum width for loaded scenes in pixels. */
    private static final double MIN_WIDTH = 800;

    /** Default minimum height for loaded scenes in pixels. */
    private static final double MIN_HEIGHT = 500;

    /**
     * Cache of controllers keyed by FXML resource path.
     * Cleared on each new load to ensure fresh instances, or can be manually
     * cleared via {@link #clearCache()}.
     */
    private static final Map<String, FXController> controllerCache = new HashMap<>();

    /**
     * Retrieves the typed controller from a given FXLoader instance.
     * <p>
     * Ensures the loader extends {@link BaseLoader} to allow safe parameter
     * injection
     * and controller retrieval.
     * </p>
     *
     * @param loader the FXLoader instance used to load the FXML
     * @param <C>    the expected controller type extending {@link FXController}
     * @return the initialized controller cast to {@code C}
     * @throws IllegalArgumentException if the provided loader is not an instance of
     *                                  {@link BaseLoader}
     */
    @SuppressWarnings("unchecked")
    private static <C extends FXController> C getControllerFromLoader(FXLoader loader) {
        if (!(loader instanceof BaseLoader)) {
            throw new IllegalArgumentException("Loader must extend BaseLoader");
        }
        return (C) ((BaseLoader<?>) loader).getControllerFromLoader();
    }

    /**
     * Loads an FXML scene into the specified {@link StackPane} container, with
     * optional parameters.
     * <p>
     * Clears the controller cache, injects non-null parameters into the loader,
     * initializes and loads the FXML,
     * binds the scene's sizing to match the container, applies a fade-in
     * transition, and caches the controller.
     * </p>
     *
     * @param fxmlPath     the path to the FXML resource (e.g.,
     *                     "/views/MainView.fxml")
     * @param resourceBase the class used to resolve the resource via
     *                     {@code getResource(fxmlPath)}
     * @param loaderClass  the specific FXLoader implementation to instantiate
     * @param parameters   map of parameter names to values; entries with null
     *                     values are ignored
     * @param contentPane  the {@link StackPane} that will host the loaded scene
     * @param <C>          the controller type extending {@link FXController}
     * @return the initialized controller of type {@code C}
     * @throws RuntimeException         if the FXML fails to load or the resource is
     *                                  not found
     * @throws IllegalArgumentException if the resource cannot be located
     */
    public static <C extends FXController> C loadScene(
            String fxmlPath,
            Class<?> resourceBase,
            Class<? extends FXLoader> loaderClass,
            Map<String, Object> parameters,
            StackPane contentPane) {
        try {
            clearCache();
            URL resource = resourceBase.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalArgumentException("FXML resource not found: " + fxmlPath);
            }
            FXLoader loader = FXLoaderFactory.createInstance(loaderClass, resource);
            if (parameters != null) {
                parameters.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .forEach(e -> loader.addParameter(e.getKey(), e.getValue()));
            }
            loader.initialize();
            Parent rootNode = loader.getRoot();
            if (rootNode instanceof Region) {
                Region region = (Region) rootNode;
                region.prefWidthProperty().unbind();
                region.prefHeightProperty().unbind();
                region.prefWidthProperty().bind(contentPane.widthProperty());
                region.prefHeightProperty().bind(contentPane.heightProperty());
            }
            setContentPane(rootNode, contentPane);
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

    /**
     * Convenience method to load a scene with a single string parameter named
     * "selectedYear".
     *
     * @param fxmlPath     the path to the FXML resource
     * @param baseClass    the class used for resource resolution
     * @param loaderClass  the FXLoader implementation class
     * @param selectedYear the academic year or similar context parameter
     * @param contentPane  the container {@link StackPane} to host the scene
     * @param <C>          the controller type extending {@link FXController}
     * @return the initialized controller of type {@code C}
     */
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

    /**
     * Loads an FXML scene with a map of parameters, delegating to
     * {@link #loadScene(String, Class, Class, Map, StackPane)}.
     *
     * @param fxmlPath     the path to the FXML resource
     * @param resourceBase the class used for resolving the resource
     * @param loaderClass  the FXLoader implementation class
     * @param parameters   map of parameter names to values; must not be null
     * @param contentPane  the container {@link StackPane} for the scene
     * @param <C>          the controller type extending {@link FXController}
     * @return the initialized controller of type {@code C}
     */
    public static <C extends FXController> C loadSceneWithParameters(
            String fxmlPath,
            Class<?> resourceBase,
            Class<? extends FXLoader> loaderClass,
            Map<String, Object> parameters,
            StackPane contentPane) {
        return loadScene(fxmlPath, resourceBase, loaderClass, parameters, contentPane);
    }

    /**
     * Applies a fade-in transition to the provided scene graph root.
     *
     * @param rootNode the {@link Parent} node to animate
     */
    private static void applyFadeTransition(Parent rootNode) {
        rootNode.setOpacity(0.0);
        Platform.runLater(() -> {
            FadeTransition fade = new FadeTransition(Duration.millis(FADE_DURATION), rootNode);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        });
    }

    /**
     * Replaces the children of the content pane with the new root node,
     * applies min/max size constraints, binds sizing properties, and fades in.
     *
     * @param rootNode    the new scene root to display
     * @param contentPane the {@link StackPane} container
     */
    private static void setContentPane(Parent rootNode, StackPane contentPane) {
        contentPane.getChildren().setAll(rootNode);
        if (rootNode instanceof Region) {
            Region region = (Region) rootNode;
            region.setMinWidth(MIN_WIDTH);
            region.setMinHeight(MIN_HEIGHT);
            region.prefWidthProperty().bind(contentPane.widthProperty());
            region.prefHeightProperty().bind(contentPane.heightProperty());
            region.maxWidthProperty().bind(contentPane.widthProperty());
            region.maxHeightProperty().bind(contentPane.heightProperty());
        }
        applyFadeTransition(rootNode);
    }

    /**
     * Retrieves a previously cached controller by its FXML path key.
     *
     * @param fxmlPath the FXML path used as the cache key
     * @param <C>      the controller type extending {@link FXController}
     * @return the cached controller instance, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public static <C extends FXController> C getCachedController(String fxmlPath) {
        return (C) controllerCache.get(fxmlPath);
    }

    /**
     * Clears all cached controllers, forcing fresh loads on subsequent invocations.
     * Call this method if you need to release references and reload controllers.
     */
    public static void clearCache() {
        controllerCache.clear();
    }
}
