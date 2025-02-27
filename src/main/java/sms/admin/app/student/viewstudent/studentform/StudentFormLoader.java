package sms.admin.app.student.viewstudent.studentform;

import dev.finalproject.App;
import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import sms.admin.util.ControllerLoader;
import javafx.stage.StageStyle;

public class StudentFormLoader extends FXLoader implements ControllerLoader<StudentFormController> {
    private static final double FORM_WIDTH = 400;
    private static final double FORM_HEIGHT = 500;

    @Override
    public void load() {
        try {
            System.out.println("Loading student form...");

            // Initialize root and controller only once
            if (root == null) {
                initialize();
                System.out.println("Root initialized: " + (root != null));

                // Initialize the controller immediately after root
                StudentFormController controller = getController();
                if (controller != null) {
                    App.CONTROLLER_REGISTRY.register("STUDENT_FORM", controller);
                    controller.load();
                    System.out.println("Controller loaded");
                }
            }

            // Configure the form size
            if (root instanceof Region) {
                Region region = (Region) root;
                region.setPrefSize(FORM_WIDTH, FORM_HEIGHT);
                region.setMinSize(FORM_WIDTH, FORM_HEIGHT);
                System.out.println("Form size configured");
            }

            // Create and setup the stage
            Stage stage = new Stage();
            Scene scene = new Scene((Region) root);

            // Add stylesheet
            String cssPath = "/sms.admin/assets/styles/skins/primer_light.css";
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            // Configure the stage
            stage.setScene(scene);
            stage.setTitle("Student Form");
            stage.setResizable(false);

            // Setup modality and position
            Window ownerWindow = (Window) getParameter("OWNER_WINDOW");
            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.initStyle(StageStyle.UNIFIED);

                // Position the dialog above the owner window
                stage.setX(ownerWindow.getX() + (ownerWindow.getWidth() - FORM_WIDTH) / 2);
                stage.setY(ownerWindow.getY() + 50); // 50 pixels from top of owner window
            }

            // Show the stage
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
            System.out.println("Stage shown");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading student form: " + e.getMessage());
        }
    }

    @Override
    public StudentFormController getController() {
        return loader != null ? (StudentFormController) loader.getController() : null;
    }
}
