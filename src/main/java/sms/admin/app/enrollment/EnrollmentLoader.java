package sms.admin.app.enrollment;

import dev.sol.core.application.loader.FXLoader;
import sms.admin.util.ControllerLoader;

public class EnrollmentLoader extends FXLoader implements ControllerLoader<EnrollmentController> {

    @Override
    public void load() {
        EnrollmentController controller = (EnrollmentController) loader.getController();
        controller.load();

    }

    @Override
    public EnrollmentController getController() {
        return (EnrollmentController) loader.getController();
    }

}
