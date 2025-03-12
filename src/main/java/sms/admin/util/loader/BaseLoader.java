package sms.admin.util.loader;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoader;

public abstract class BaseLoader<T extends FXController> extends FXLoader {
    public T getControllerFromLoader() {
        if (loader == null) {
            throw new IllegalStateException("FXMLLoader is not initialized");
        }
        return (T) loader.getController();
    }
}
