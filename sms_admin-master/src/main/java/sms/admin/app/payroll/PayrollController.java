package sms.admin.app.payroll;

import dev.sol.core.application.FXController;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import sms.admin.util.YearData;
import sms.admin.util.datetime.DateTimeUtils;

public class PayrollController extends FXController {

    @FXML
    private ComboBox<String> yearMonthComboBox;

    @FXML
    private AnchorPane rootPane;

    @Override
    protected void load_fields() {
        if (yearMonthComboBox != null) {
            String selectedYear = (String) getParameter("selectedYear");
            if (selectedYear == null) {
                selectedYear = YearData.getCurrentAcademicYear();
            }
            DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, selectedYear);
            System.out.println("PayrollController.load_fields: selectedYear = " + selectedYear);
        }
    }

    public void updateYear(String newYear) {
        if (yearMonthComboBox != null) {
            System.out.println("PayrollController.updateYear called with newYear = " + newYear);
            DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, newYear);
        }
    }

    @Override
    protected void load_bindings() {
        System.out.println("PayrollController.load_bindings called");
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    rootPane.prefWidthProperty().bind(newScene.widthProperty());
                    rootPane.prefHeightProperty().bind(newScene.heightProperty());
                }
            });
        }
    }

    @Override
    protected void load_listeners() {
        // Add any additional listeners if needed.
    }

    @FXML
    public void initialize() {
    }
}
