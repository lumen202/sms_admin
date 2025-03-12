package sms.admin.app.enrollment;

import sms.admin.util.loader.BaseLoader;

public class EnrollmentLoader extends BaseLoader {
    @Override
    public void load() {
        EnrollmentController controller = (EnrollmentController) getControllerFromLoader();
        if (controller != null) {
            controller.setParameters(getParameters());
            controller.load();
        }
    }
}
