package sms.admin.app.enrollment;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EnrollmentV2Loader extends FXLoader {

    public EnrollmentV2Loader() {
        createInstance(getClass().getResource("/sms/admin/app/enrollment/ENROLLMENT_V2.fxml"));
        initialize();
    }
    
    @Override
    public void load() {
        Scene scene = (Scene) params.get("scene");
        if (scene == null) {
            scene = new Scene(root);
            scene.getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm()
            );
        }
        
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner((javafx.stage.Window) params.get("OWNER_WINDOW"));
        dialogStage.setTitle("Add New Student");
        dialogStage.setScene(scene);
        
        EnrollmentControllerV2 controller = loader.getController();
        if (controller != null) {
            controller.setParameters(params)
                     .addParameter("SCENE", scene)
                     .addParameter("OWNER", dialogStage)
                     .load();
        }
        
        dialogStage.showAndWait();
    }
}
