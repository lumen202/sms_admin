package sms.admin.app.student;

import dev.sol.core.application.loader.FXLoader;
import sms.admin.util.ControllerLoader;

public class StudentLoader extends FXLoader implements ControllerLoader<StudentController> {

    @Override
    public void load() {
        StudentController controller = (StudentController) loader.getController();
        controller.load();
    }

    @Override
    public StudentController getController() {
        return (StudentController) loader.getController();
    }
}
