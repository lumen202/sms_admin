package sms.admin.app.schoolyear;

import java.io.IOException;

import dev.finalproject.models.SchoolYear;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

public class SchoolYearDialog extends Dialog<SchoolYear> {

    public SchoolYearDialog(SchoolYear schoolYear) {
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SCHOOL_YEAR_DIALOG.fxml"));
            getDialogPane().setContent(loader.load());
            
            SchoolYearDialogController controller = loader.getController();
            controller.setDialog(this);
            controller.setExistingSchoolYear(schoolYear);

            getDialogPane().getStylesheets().add(
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm()
            );
            getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            getDialogPane().getScene().setFill(null);

            // Add dialog button types
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            // Set result converter
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    return controller.createSchoolYear();
                }
                return null;
            });

            // Hide the default buttons since we're using custom ones
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            okButton.setVisible(false);
            cancelButton.setVisible(false);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load school year dialog", e);
        }
    }
}
