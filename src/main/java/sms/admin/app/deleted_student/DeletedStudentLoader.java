package sms.admin.app.deleted_student;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;

public class DeletedStudentLoader extends FXLoader {

    public DeletedStudentLoader() {
        createInstance(getClass().getResource("/sms/admin/app/deleted_student/DELETED_STUDENT.fxml"));
        initialize();
    }

    @Override
    public void load() {
        try {
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/sms/admin/app/deleted_student/deleted_student.css").toExternalForm());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Deleted Students");
            stage.setScene(scene);

            // Set size constraints
            stage.setMinWidth(600);
            stage.setMinHeight(400);

            // Center the stage relative to owner if specified
            if (params != null && params.containsKey("OWNER_WINDOW")) {
                stage.initOwner((Stage) params.get("OWNER_WINDOW"));
                stage.centerOnScreen();
            }

            DeletedStudentController controller = loader.getController();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load deleted students window", e);
        }
    }
}
