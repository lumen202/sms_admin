package sms.admin.app.schoolyear;

import java.io.IOException;

import dev.finalproject.models.SchoolYear;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.beans.property.ObjectProperty;

public class SchoolYearDialog extends Dialog<SchoolYear> {

    private SchoolYearDialogController controller;

    public SchoolYearDialog(SchoolYear schoolYear) {
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SCHOOL_YEAR_DIALOG.fxml"));
            getDialogPane().setContent(loader.load());

            controller = loader.getController();
            controller.setDialog(this);
            controller.setDialogStage((Stage) getDialogPane().getScene().getWindow());
            controller.setExistingSchoolYear(schoolYear);

            getDialogPane().getStylesheets().add(
                    getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm()
            );
            getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            getDialogPane().getScene().setFill(null);

            // Add dialog button types
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Set result converter with logging
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    SchoolYear result = controller.schoolYearProperty().get();
                    System.out.println("Dialog converting result: " + result);
                    return result;
                }
                System.out.println("Dialog returning null (cancelled)");
                return null;
            });

            // Hide the default buttons since we're using custom ones
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            okButton.setVisible(false);
            cancelButton.setVisible(false);

            // Hook into the dialog's show event to ensure window is available
            setOnShowing(event -> {
                Stage stage = (Stage) getDialogPane().getScene().getWindow();
                controller.setDialogStage(stage);
            });

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load school year dialog", e);
        }
    }

    public ObjectProperty<SchoolYear> schoolYearProperty() {
        return controller.schoolYearProperty();
    }
}
