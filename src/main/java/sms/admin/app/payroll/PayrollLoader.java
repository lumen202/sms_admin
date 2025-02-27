package sms.admin.app.payroll;

import dev.sol.core.application.loader.FXLoader;
import sms.admin.util.ControllerLoader;

public class PayrollLoader extends FXLoader implements ControllerLoader<PayrollController> {
    @Override
    public void load() {
        PayrollController controller = (PayrollController) loader.getController();
        controller.load();
    }

    @Override
    public PayrollController getController() {
        return (PayrollController) loader.getController();
    }
}
