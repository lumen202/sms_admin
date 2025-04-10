package sms.admin.app.attendance;

import sms.admin.util.loader.BaseLoader;
import javafx.application.Platform;

@SuppressWarnings("rawtypes")
public class AttendanceLoader extends BaseLoader {
    @Override
    public void load() {
        Platform.runLater(() -> {
            AttendanceController controller = (AttendanceController) getControllerFromLoader();
            if (controller != null) {
                try {
                    controller.load();
                } catch (Exception e) {
                    System.err.println("Error loading AttendanceController: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("AttendanceController is null");
            }
        });
    }
}
