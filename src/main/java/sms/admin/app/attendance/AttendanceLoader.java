package sms.admin.app.attendance;

import dev.sol.core.application.loader.FXLoader;
import sms.admin.util.ControllerLoader;

public class AttendanceLoader extends FXLoader implements ControllerLoader<AttendanceController> {

    @Override
    public void load() {
        // Call the controller's load method after loading the FXML.
        AttendanceController controller = (AttendanceController) loader.getController();
        controller.load();
    }

    // Expose the controller to other classes.
    public AttendanceController getController() {
        return (AttendanceController) loader.getController();
    }
}
