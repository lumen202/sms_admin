package sms.admin.util.loader;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoader;

public abstract class BaseLoader extends FXLoader {
    public FXController getControllerFromLoader() {
        if (loader == null) {
            throw new IllegalStateException("FXMLLoader is not initialized");
        }
        return loader.getController();
    }
}
