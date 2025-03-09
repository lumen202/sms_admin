package sms.admin.app.attendance.attendancelog;

import sms.admin.util.loader.BaseLoader;

public class AttendanceLogLoader extends BaseLoader {
    @Override
    public void load() {
        AttendanceLogController controller = (AttendanceLogController) getControllerFromLoader();
        if (controller != null) {
            controller.setParameters(getParameters());
            controller.load();
        }
    }
}
