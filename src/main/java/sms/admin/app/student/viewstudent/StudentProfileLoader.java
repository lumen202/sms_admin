package sms.admin.app.student.viewstudent;

import dev.finalproject.models.Student;
import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Loader for the student profile dialog, responsible for initializing and
 * displaying the student profile view.
 * This class sets up the stage, configures the scene with styles, and manages
 * the positioning of multiple open windows.
 */
public class StudentProfileLoader extends FXLoader {

    private static int openWindowCount = 0; // Tracks the number of open profile windows
    private static final int WINDOW_OFFSET = 30; // Offset for positioning multiple windows

    /**
     * Constructor for the StudentProfileLoader.
     * Initializes the loader with the FXML resource for the student profile view.
     */
    public StudentProfileLoader() {
        String fxmlPath = "/sms/admin/app/student/viewstudent/STUDENT_PROFILE.fxml";
        createInstance(getClass().getResource(fxmlPath));
        initialize();
    }

    /**
     * Loads and displays the student profile dialog, setting up the stage and
     * controller.
     */
    @Override
    public void load() {
        try {
            // Create and configure the scene
            Scene scene = createAndConfigureScene();
            Stage ownerStage = (Stage) getParameter("OWNER_STAGE");
            Stage stage = new Stage();

            // Configure the stage
            stage.initOwner(ownerStage);
            stage.setScene(scene);
            stage.setTitle("Student Profile");

            // Set size and position
            stage.setHeight(ownerStage.getHeight() * 0.9);
            ownerStage.heightProperty()
                    .addListener((obs, oldVal, newVal) -> stage.setHeight(newVal.doubleValue() * 0.9));

            // Center the first window relative to the owner
            if (openWindowCount == 0) {
                // Delay positioning until stage is shown to ensure correct width/height
                stage.setOnShown(e -> {
                    stage.setX(ownerStage.getX() + (ownerStage.getWidth() - stage.getWidth()) / 2);
                    stage.setY(ownerStage.getY() + (ownerStage.getHeight() - stage.getHeight()) / 2);
                });
            } else {
                // Offset window position for subsequent windows
                stage.setX(ownerStage.getX() + WINDOW_OFFSET * (openWindowCount + 1));
                stage.setY(ownerStage.getY() + WINDOW_OFFSET * (openWindowCount + 1));
            }
            openWindowCount++;

            // Decrement counter when the window is closed
            stage.setOnHiding(e -> openWindowCount--);

            // Initialize controller
            StudentProfileController controller = loader.getController();
            controller.setStage(stage);
            controller.load();
            controller.setStudent((Student) getParameter("SELECTED_STUDENT"));

            // Show the stage
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load student profile", e);
        }
    }

    /**
     * Creates and configures the scene for the student profile dialog, applying
     * necessary styles.
     *
     * @return The configured Scene object.
     */
    private Scene createAndConfigureScene() {
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm());
        return scene;
    }
}