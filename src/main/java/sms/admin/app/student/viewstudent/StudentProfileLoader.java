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
            Stage stage = createAndConfigureStage();
            Scene scene = createAndConfigureScene();
            stage.setScene(scene);

            initializeController(stage);

            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load student profile", e);
        }
    }

    private Stage createAndConfigureStage() {
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner((Stage) getParameter("OWNER_STAGE"));
        return stage;
    }

    private Scene createAndConfigureScene() {
        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(
                getClass().getResource("/sms/admin/app/styles/main.css").toExternalForm(),
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm());
        return scene;
    }

    private void initializeController(Stage stage) {
        StudentProfileController controller = loader.getController();
        controller.setStage(stage);
        controller.load();
        controller.setStudent((Student) getParameter("SELECTED_STUDENT"));
    }
}