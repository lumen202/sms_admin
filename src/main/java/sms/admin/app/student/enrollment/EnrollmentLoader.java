package sms.admin.app.student.enrollment;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Loader for the student enrollment dialog, responsible for initializing and
 * displaying the enrollment form.
 * This class sets up the stage, configures the scene with styles, and manages
 * the positioning of multiple open windows.
 */
public class EnrollmentLoader extends FXLoader {

    private static int openWindowCount = 0; // Tracks the number of open enrollment windows
    private static final int WINDOW_OFFSET = 30; // Offset for positioning multiple windows

    /**
     * Constructor for the EnrollmentLoader.
     * Initializes the loader with the FXML resource for the enrollment form.
     */
    public EnrollmentLoader() {
        createInstance(getClass().getResource("/sms/admin/app/enrollment/ENROLLMENT.fxml"));
        initialize();
    }

    /**
     * Loads and displays the enrollment dialog, setting up the stage and
     * controller.
     */
    @Override
    public void load() {
        try {
            // Create and configure the scene
            Scene scene = createAndConfigureScene();
            Stage ownerStage = (Stage) getParameter("OWNER_WINDOW");
            Stage stage = new Stage();

            // Configure the stage
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(ownerStage);
            stage.setScene(scene);
            stage.setTitle("Add New Student");

            // Set size and position
            stage.setHeight(ownerStage.getHeight() * 0.9);
            ownerStage.heightProperty()
                    .addListener((obs, oldVal, newVal) -> stage.setHeight(newVal.doubleValue() * 0.9));

            // Center the first window relative to the owner, offset others
            if (openWindowCount == 0) {
                stage.setOnShown(e -> {
                    stage.setX(ownerStage.getX() + (ownerStage.getWidth() - stage.getWidth()) / 2);
                    stage.setY(ownerStage.getY() + (ownerStage.getHeight() - stage.getHeight()) / 2);
                });
            } else {
                stage.setX(ownerStage.getX() + WINDOW_OFFSET * (openWindowCount + 1));
                stage.setY(ownerStage.getY() + WINDOW_OFFSET * (openWindowCount + 1));
            }
            openWindowCount++;

            // Decrement counter when the window is closed
            stage.setOnHiding(e -> openWindowCount--);

            // Initialize the controller
            EnrollmentController controller = loader.getController();
            if (controller != null) {
                controller.setDialogStage(stage);
                controller.setParameters(params)
                        .addParameter("SCENE", scene)
                        .addParameter("OWNER", stage)
                        .load();
            }

            // Show the dialog and wait for it to close
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load enrollment form", e);
        }
    }

    /**
     * Creates and configures the scene for the enrollment dialog, applying
     * necessary styles.
     *
     * @return The configured Scene object.
     */
    private Scene createAndConfigureScene() {
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/enrollment/enrollment.css").toExternalForm());
        return scene;
    }
}