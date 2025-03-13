package sms.admin.app.student.viewstudent;

import dev.sol.core.application.loader.FXLoader;
import dev.finalproject.models.Student;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sms.admin.util.dialog.DialogManager;

public class StudentProfileLoader extends FXLoader {

    @Override
    public void load() {
        try {
            Stage stage = createAndConfigureStage();
            Scene scene = createAndConfigureScene();
            stage.setScene(scene);

            Stage ownerStage = (Stage) getParameter("OWNER_STAGE");
            DialogManager.setOverlayEffect(ownerStage, true);

            initializeController(stage);

            stage.centerOnScreen();
            stage.show();

            stage.setOnHiding(e -> DialogManager.setOverlayEffect(ownerStage, false));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load student profile", e);
        }
    }

    private Stage createAndConfigureStage() {
        Stage ownerStage = (Stage) getParameter("OWNER_STAGE");
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(ownerStage);
       
        stage.setHeight(ownerStage.getHeight() * 0.9);

        
            
        ownerStage.heightProperty().addListener((obs, oldVal, newVal) -> 
            stage.setHeight(newVal.doubleValue() * 0.9));
        
        return stage;
    }

    private Scene createAndConfigureScene() {
        Scene scene = new Scene(root);
        scene.setFill(null);
        root.getStyleClass().addAll("custom-dialog", "modal-dialog");
        scene.getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm());
        return scene;
    }

    private void initializeController(Stage stage) {
        StudentProfileController controller = loader.getController();
        controller.setStage(stage);
        controller.load(); // Call load() before setStudent()
        controller.setStudent((Student) getParameter("SELECTED_STUDENT"));
    }
}