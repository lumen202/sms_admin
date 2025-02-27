package sms.admin.app.student.viewstudent;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sms.admin.App;

public class StudentProfileLoader extends FXLoader {

    @Override
    public void load() {

        Stage ownerStage = (Stage) params.get("OWNER_STAGE");
        Stage stage = new Stage();
        Scene scene = new Scene(root);
        stage.setTitle("Student Profile");
        stage.setResizable(false);
        stage.setScene(scene);
        scene.getStylesheets().add(
                getClass().getResource("/sms.admin/assets/styles/skins/primer_light.css").toExternalForm());
        stage.initOwner(ownerStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();

        StudentProfileController controller = loader.getController();
        App.CONTROLLER_REGISTRY.register("STUDENT_PROFILE", controller);
        controller.load();
        controller
                .addParameter("MODAL", getParameter("MODAL"))
                .addParameter("SELECTED_STUDENT", getParameter("SELECTED_STUDENT"))
                .load();

    }
}
