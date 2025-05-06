/**
 * Abstract loader class that extends the FXLoader framework to provide
 * type-safe access to the controller managed by the FXMLLoader.
 *
 * <p>This base loader handles casting of the controller to the specified
 * {@link dev.sol.core.application.FXController} subtype and ensures the
 * FXMLLoader has been properly initialized.</p>
 *
 * @param <T> the concrete controller type extending {@link FXController}
 */
package sms.admin.util.loader;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoader;

public abstract class BaseLoader<T extends FXController> extends FXLoader {

    /**
     * Retrieves the controller instance from the underlying FXMLLoader and casts it
     * to the generic type {@code T}.
     *
     * @return the controller instance of type {@code T}
     * @throws IllegalStateException if the FXMLLoader has not been initialized
     */
    @SuppressWarnings("unchecked")
    public T getControllerFromLoader() {
        if (loader == null) {
            throw new IllegalStateException("FXMLLoader is not initialized");
        }
        return (T) loader.getController();
    }
}
