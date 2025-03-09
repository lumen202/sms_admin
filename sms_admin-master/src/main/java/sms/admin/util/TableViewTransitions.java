package sms.admin.util;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.TableView;
import javafx.util.Duration;

public class TableViewTransitions {
    
    public static void fadeTransition(TableView<?> tableView) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), tableView);
        fadeTransition.setFromValue(0.7);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }
    
    public static void slideTransition(TableView<?> tableView) {
        TranslateTransition slideTransition = new TranslateTransition(Duration.millis(500), tableView);
        slideTransition.setFromX(-20);
        slideTransition.setToX(0);
        slideTransition.play();
    }
    
    public static void applyUpdateTransition(TableView<?> tableView) {
        fadeTransition(tableView);
        slideTransition(tableView);
    }
}
