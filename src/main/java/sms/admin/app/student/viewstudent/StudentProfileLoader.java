package sms.admin.app.student.viewstudent;

import dev.sol.core.application.loader.FXLoader;
import dev.finalproject.models.Student;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class StudentProfileLoader extends FXLoader {

    @Override
    public void load() {
        try {
            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            Stage ownerStage = (Stage) getParameter("OWNER_STAGE");
            stage.initOwner(ownerStage);

            // Setup controller first
            StudentProfileController controller = (StudentProfileController) loader.getController();
            controller.setStage(stage);

            // Create scene after controller is ready
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm());
            stage.setScene(scene);
            
            // Now initialize controller and set student
            controller.load();
            controller.setStudent((Student) getParameter("SELECTED_STUDENT"));

            // Show stage last
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load student profile", e);
        }
    }
}