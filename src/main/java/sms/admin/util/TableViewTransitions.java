package sms.admin.util;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.TableView;
import javafx.util.Duration;

public class TableViewTransitions {

    public static void applyUpdateTransition(TableView<?> tableView) {
        ParallelTransition parallelTransition = new ParallelTransition();
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), tableView);
        fadeTransition.setFromValue(0.7);
        fadeTransition.setToValue(1.0);
        TranslateTransition slideTransition = new TranslateTransition(Duration.millis(500), tableView);
        slideTransition.setFromX(-20);
        slideTransition.setToX(0);
        parallelTransition.getChildren().addAll(fadeTransition, slideTransition);
        parallelTransition.play();
    }
}
