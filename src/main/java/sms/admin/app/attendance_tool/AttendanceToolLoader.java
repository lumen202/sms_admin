package sms.admin.app.attendance_tool;

import dev.sol.core.application.loader.FXLoader;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sms.admin.app.attendance.AttendanceController;

import java.net.URL;

public class AttendanceToolLoader extends FXLoader {

    public AttendanceToolLoader() {
        createInstance(getClass().getResource("ATTENDANCE_TOOL.fxml"));
        initialize();
    }

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
            stage.setTitle("Attendance Tool");
            stage.setMinWidth(800);
            stage.setMinHeight(600);

            // Add on close handler to refresh attendance scene
            stage.setOnHiding(e -> {
                Object attendanceController = getParameter("ATTENDANCE_CONTROLLER");
                if (attendanceController != null) {
                    Platform.runLater(() -> {
                        ((AttendanceController) attendanceController).refreshView();
                    });
                }
            });

            // Position window relative to owner
            stage.setOnShown(e -> {
                stage.setX(ownerStage.getX() + (ownerStage.getWidth() - stage.getWidth()) / 2);
                stage.setY(ownerStage.getY() + (ownerStage.getHeight() - stage.getHeight()) / 2);
            });

            // Initialize controller
            AttendanceToolController controller = loader.getController();
            if (controller != null) {
                String selectedYear = (String) getParameter("selectedYear");
                controller.addParameter("selectedYear", selectedYear)
                        .addParameter("SCENE", scene)
                        .addParameter("OWNER", stage)
                        .load();
            }

            stage.show();

        } catch (Exception e) {
            System.err.println("AttendanceToolLoader Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Scene createAndConfigureScene() {
        Scene scene = new Scene(root);

        // Add required stylesheets
        String[] stylesheets = {
                "/sms/admin/assets/styles/skins/primer_light.css",
                "/sms/admin/app/attendance_tool/attendance-tool.css"
        };

        for (String stylesheet : stylesheets) {
            URL resource = getClass().getResource(stylesheet);
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            } else {
                System.err.println("Could not find stylesheet: " + stylesheet);
            }
        }

        return scene;
    }
}
