package sms.admin.app.payroll;

import sms.admin.util.loader.BaseLoader;

@SuppressWarnings("rawtypes")
public class PayrollLoader extends BaseLoader {
    @Override
    public void load() {
        PayrollController controller = (PayrollController) getControllerFromLoader();
        if (controller != null) {
            controller.setParameters(getParameters());
            controller.load();
        }
    }
}
