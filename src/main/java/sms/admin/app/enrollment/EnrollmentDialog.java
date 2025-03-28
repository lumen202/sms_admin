package sms.admin.app.enrollment;

import dev.finalproject.models.SchoolYear;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class EnrollmentDialog extends Dialog<Void> {
    private EnrollmentController controller;

    public EnrollmentDialog(SchoolYear schoolYear) {
        // initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ENROLLMENT.fxml"));
            getDialogPane().setContent(loader.load());
            
            controller = loader.getController();
            controller.setDialogStage((Stage)getDialogPane().getScene().getWindow());
            if (schoolYear != null) {
                controller.initializeWithYear(schoolYear.getYearStart() + "-" + schoolYear.getYearEnd());
            }

            getDialogPane().getStylesheets().add(
                getClass().getResource("/sms/admin/app/styles/dialog.css").toExternalForm()
            );
            getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 20;");

            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load enrollment dialog", e);
        }
    }
}
