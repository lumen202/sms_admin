package sms.admin.app.payroll.dialog;

import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import dev.sol.core.application.loader.FXLoader;

public class PayrollExportDialogLoader extends FXLoader {
    private PayrollExportDialogController controller;
    private final String currentYear;
    private final String currentMonth;
    private final String exportType;

    public PayrollExportDialogLoader(String currentYear, String currentMonth, String exportType) {
        String fxmlPath = "/sms/admin/app/payroll/dialog/PAYROLL_EXPORT_DIALOG.fxml";
        createInstance(getClass().getResource(fxmlPath));
        this.currentYear = currentYear;
        this.currentMonth = currentMonth;
        this.exportType = exportType;
        initialize();
    }

    public void load() {
        try {
            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            Window owner = (Window) getParameter("OWNER_STAGE");
            if (owner != null) {
                stage.initOwner(owner);
                applyOwnerStageEffects((Stage) owner, stage);
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.sizeToScene(); // Ensure stage dimensions are set based on content

            controller = loader.getController();
            controller.setStage(stage);
            controller.initData(currentYear, currentMonth, exportType);

            System.out.println("Export dialog loaded successfully for export type: " + exportType);

            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Failed to load export dialog: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load export dialog", e);
        }
    }

    private void applyOwnerStageEffects(Stage ownerStage, Stage dialogStage) {
        GaussianBlur blur = new GaussianBlur(5);
        ownerStage.getScene().getRoot().setEffect(blur);

        dialogStage.setOnHiding(event -> {
            ownerStage.getScene().getRoot().setEffect(null);
        });
    }

    public PayrollExportDialogController getController() {
        return controller;
    }
}