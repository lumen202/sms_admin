package sms.admin.app.attendance.attendancev2;

import javafx.application.Platform;
import sms.admin.util.loader.BaseLoader;

public class AttendanceV2Loader extends BaseLoader {

    @Override
    public void load() {
        Platform.runLater(() -> {
            AttendanceV2Controller controller = (AttendanceV2Controller) getControllerFromLoader();
            if (controller != null) {
                try {
                    controller.load();
                } catch (Exception e) {
                    System.err.println("Error loading AttendanceV2Controller: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("AttendanceV2Controller is null");
            }
        });
    }
}
