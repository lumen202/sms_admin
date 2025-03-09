package sms.admin.app.attendance;

import sms.admin.util.loader.BaseLoader;

public class AttendanceLoader extends BaseLoader {
    @Override
    public void load() {
        AttendanceController controller = (AttendanceController) getControllerFromLoader();
        if (controller != null) {
            controller.load();
        }
    }
}
