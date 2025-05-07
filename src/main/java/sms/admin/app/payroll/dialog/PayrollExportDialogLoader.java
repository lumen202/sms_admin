package sms.admin.app.payroll.dialog;

import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import dev.sol.core.application.loader.FXLoader;

import java.net.URL;

/**
 * Loader for the payroll export dialog, responsible for initializing and
 * displaying the dialog.
 * This class sets up the stage, applies visual effects, and configures the
 * controller with the provided parameters.
 */
public class PayrollExportDialogLoader extends FXLoader {
    private PayrollExportDialogController controller; // Controller for the dialog
    private final String currentYear; // The current academic year (e.g., "2023-2024")
    private final String currentMonth; // The current month (e.g., "September 2023")
    private final String exportType; // The type of export (e.g., "excel", "csv", "xlsx")

    /**
     * Constructor for the PayrollExportDialogLoader.
     *
     * @param currentYear  The current academic year.
     * @param currentMonth The current month.
     * @param exportType   The type of export to perform.
     */
    public PayrollExportDialogLoader(String currentYear, String currentMonth, String exportType) {
        String fxmlPath = "/sms/admin/app/payroll/dialog/PAYROLL_EXPORT_DIALOG.fxml";
        createInstance(getClass().getResource(fxmlPath));
        this.currentYear = currentYear;
        this.currentMonth = currentMonth;
        this.exportType = exportType;
        initialize();
    }

    /**
     * Loads and displays the payroll export dialog, setting up the stage and
     * controller.
     */
    public void load() {
        try {
            // Create a transparent stage for the dialog
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            Window owner = (Window) getParameter("OWNER_STAGE");
            if (owner != null) {
                stage.initOwner(owner);
                applyOwnerStageEffects((Stage) owner, stage);
            }

            // Configure scene with both stylesheets
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            // Add required stylesheets
            String[] stylesheets = {
                    "/sms/admin/assets/styles/skins/primer_light.css",
                    "/sms/admin/app/payroll/dialog/payroll-export-dialog.css"
            };

            for (String stylesheet : stylesheets) {
                URL resource = getClass().getResource(stylesheet);
                if (resource != null) {
                    scene.getStylesheets().add(resource.toExternalForm());
                } else {
                    System.err.println("Could not find stylesheet: " + stylesheet);
                }
            }

            stage.setScene(scene);
            stage.sizeToScene();

            // Initialize controller
            controller = loader.getController();
            controller.setStage(stage);
            controller.initData(currentYear, currentMonth, exportType);

            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Failed to load export dialog: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load export dialog", e);
        }
    }

    /**
     * Applies a blur effect to the owner stage when the dialog is shown and removes
     * it when the dialog is closed.
     *
     * @param ownerStage  The owner stage to apply effects to.
     * @param dialogStage The dialog stage being shown.
     */
    private void applyOwnerStageEffects(Stage ownerStage, Stage dialogStage) {
        GaussianBlur blur = new GaussianBlur(5);
        ownerStage.getScene().getRoot().setEffect(blur);

        // Remove the effect when the dialog is closed
        dialogStage.setOnHiding(event -> {
            ownerStage.getScene().getRoot().setEffect(null);
        });
    }

    /**
     * Gets the controller for the payroll export dialog.
     *
     * @return The current PayrollExportDialogController instance.
     */
    public PayrollExportDialogController getController() {
        return controller;
    }
}