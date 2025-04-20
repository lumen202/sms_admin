package sms.admin.app.enrollment;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EnrollmentV2Loader extends FXLoader {

    private static int openWindowCount = 0;
    private static final int WINDOW_OFFSET = 30;

    public EnrollmentV2Loader() {
        createInstance(getClass().getResource("/sms/admin/app/enrollment/ENROLLMENT_V2.fxml"));
        initialize();
    }
    
    @Override
    public void load() {
        try {
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
            ownerStage.heightProperty().addListener((obs, oldVal, newVal) 
                -> stage.setHeight(newVal.doubleValue() * 0.9));

            // Center the first window relative to the owner
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

            // Add close handler to decrement counter
            stage.setOnHiding(e -> openWindowCount--);
            
            EnrollmentControllerV2 controller = loader.getController();
            if (controller != null) {
                controller.setDialogStage(stage);
                controller.setParameters(params)
                         .addParameter("SCENE", scene)
                         .addParameter("OWNER", stage)
                         .load();
            }
            
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load enrollment form", e);
        }
    }

    private Scene createAndConfigureScene() {
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
            getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm(),
            getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm(),
            getClass().getResource("/sms/admin/app/enrollment/enrollment.css").toExternalForm()
        );
        return scene;
    }
}
