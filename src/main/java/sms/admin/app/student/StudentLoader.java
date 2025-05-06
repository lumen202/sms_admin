package sms.admin.app.student;

import sms.admin.util.loader.BaseLoader;

@SuppressWarnings("rawtypes")
public class StudentLoader extends BaseLoader {
    @Override
    public void load() {
        StudentController controller = (StudentController) getControllerFromLoader();
        if (controller != null) {
            controller.setParameters(getParameters());
            controller.load();
        }
    }
}
