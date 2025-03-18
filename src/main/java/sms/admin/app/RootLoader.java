package sms.admin.app;

import dev.sol.core.application.loader.FXLoader;
import javafx.scene.Scene;
import sms.admin.App;

public class RootLoader extends FXLoader {

    @Override
    public void load() {
        Scene scene = (Scene) params.get("scene");
        scene.setRoot(root);
        
        RootController controller = loader.getController();
        App.CONTROLLER_REGISTRY.register("ROOT", controller);
        
        controller.addParameter("SCENE", scene)
                .addParameter("OWNER", params.get("OWNER"))
                .load();
    }
}