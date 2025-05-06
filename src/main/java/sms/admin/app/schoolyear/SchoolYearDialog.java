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

/**
 * A custom dialog for creating or editing a school year.
 * If a {@link SchoolYear} is provided, the dialog will be prefilled for
 * editing;
 * otherwise, it will be for creating a new school year.
 * The dialog returns a {@link SchoolYear} object upon confirmation.
 */
public class SchoolYearDialog extends Dialog<SchoolYear> {

    private SchoolYearDialogController controller;

    /**
     * Constructs a new SchoolYearDialog.
     *
     * @param schoolYear The school year to edit, or null for creating a new one.
     */
    public SchoolYearDialog(SchoolYear schoolYear) {
        // Set dialog style to undecorated (no window borders)
        initStyle(StageStyle.UNDECORATED);
        // Make the dialog modal to block other windows
        initModality(Modality.APPLICATION_MODAL);

        try {
            // Load the FXML file for the dialog's UI
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SCHOOL_YEAR_DIALOG.fxml"));
            getDialogPane().setContent(loader.load());

            // Get the controller and set dialog references
            controller = loader.getController();
            controller.setDialog(this);
            controller.setDialogStage((Stage) getDialogPane().getScene().getWindow());
            controller.setExistingSchoolYear(schoolYear);

            // Apply stylesheet for custom styling
            getDialogPane().getStylesheets().add(
                    getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm());
            // Set transparent background and no padding
            getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            getDialogPane().getScene().setFill(null);

            // Add OK and CANCEL button types
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Set result converter to return SchoolYear on OK, null on CANCEL
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    SchoolYear result = controller.schoolYearProperty().get();
                    // Debug: print the result being returned
                    System.out.println("Dialog converting result: " + result);
                    return result;
                }
                // Debug: indicate cancellation
                System.out.println("Dialog returning null (cancelled)");
                return null;
            });

            // Hide default buttons as custom buttons are used in FXML
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            okButton.setVisible(false);
            cancelButton.setVisible(false);

            // Ensure the stage is set when the dialog is shown
            setOnShowing(event -> {
                Stage stage = (Stage) getDialogPane().getScene().getWindow();
                controller.setDialogStage(stage);
            });

        } catch (IOException e) {
            // Log the exception and rethrow as unchecked exception
            e.printStackTrace();
            throw new RuntimeException("Failed to load school year dialog", e);
        }
    }

    /**
     * Returns the property holding the current school year being edited or created.
     *
     * @return the school year property
     */
    public ObjectProperty<SchoolYear> schoolYearProperty() {
        return controller.schoolYearProperty();
    }
}