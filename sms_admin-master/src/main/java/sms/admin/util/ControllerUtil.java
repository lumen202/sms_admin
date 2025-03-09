package sms.admin.util;

import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoader;
import javafx.fxml.FXMLLoader;
import java.lang.reflect.Field;

public class ControllerUtil {
    @SuppressWarnings("unchecked")
    public static <C extends FXController> C getController(FXLoader fxLoader) {
        if (fxLoader == null) {
            throw new IllegalArgumentException("FXLoader cannot be null");
        }
        
        try {
            Field loaderField = fxLoader.getClass().getSuperclass().getDeclaredField("loader");
            loaderField.setAccessible(true);
            FXMLLoader loader = (FXMLLoader) loaderField.get(fxLoader);
            
            if (loader == null) {
                throw new IllegalStateException("FXMLLoader is not initialized");
            }
            
            C controller = (C) loader.getController();
            if (controller != null) {
                controller.setParameters(fxLoader.getParameters());
            }
            return controller;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get controller from loader: " + e.getMessage(), e);
        }
    }
}
