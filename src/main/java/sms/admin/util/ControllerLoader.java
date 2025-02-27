package sms.admin.util;

import dev.sol.core.application.FXController;

public interface ControllerLoader<C extends FXController> {
    C getController();
}
