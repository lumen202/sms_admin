package sms.admin.app.payroll.dialog;

import java.time.YearMonth;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import sms.admin.util.datetime.DateTimeUtils;

public class PayrollExportDialogController {
    @FXML
    private ComboBox<String> startMonthCombo;
    @FXML
    private ComboBox<String> endMonthCombo;
    @FXML
    private Button exportButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label errorLabel;

    private Stage stage;
    private String selectedExportType;
    private YearMonth startMonth;
    private YearMonth endMonth;
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        setupListeners();
    }

    private void setupListeners() {
        startMonthCombo.setOnAction(e -> validateDateRange());
        endMonthCombo.setOnAction(e -> validateDateRange());

        exportButton.setOnAction(e -> handleExport());
        cancelButton.setOnAction(e -> stage.close());
    }

    public void initData(String currentYear, String currentMonth, String exportType) {
        this.selectedExportType = exportType;
        
        // Only allow Excel, CSV, and Detailed Excel export types
        if (!exportType.equals("excel") && !exportType.equals("csv") && !exportType.equals("xlsx")) {
            errorLabel.setText("Invalid export type selected");
            errorLabel.setVisible(true);
            exportButton.setDisable(true);
            return;
        }

        // Populate month combos for the school year
        DateTimeUtils.updateMonthYearComboBox(startMonthCombo, currentYear);
        DateTimeUtils.updateMonthYearComboBox(endMonthCombo, currentYear);

        // Set current month as default selection
        if (currentMonth != null && startMonthCombo.getItems().contains(currentMonth)) {
            startMonthCombo.setValue(currentMonth);
            endMonthCombo.setValue(currentMonth);
        } else {
            startMonthCombo.setValue(startMonthCombo.getItems().get(0));
            endMonthCombo.setValue(endMonthCombo.getItems().get(0));
        }

        // Initial validation
        validateDateRange();
    }

    private void validateDateRange() {
        try {
            if (startMonthCombo.getValue() == null || endMonthCombo.getValue() == null) {
                errorLabel.setText("Please select both start and end months");
                errorLabel.setVisible(true);
                exportButton.setDisable(true);
                return;
            }

            startMonth = DateTimeUtils.parseMonthYear(startMonthCombo.getValue());
            endMonth = DateTimeUtils.parseMonthYear(endMonthCombo.getValue());

            if (endMonth.isBefore(startMonth)) {
                errorLabel.setText("End month cannot be before start month");
                errorLabel.setVisible(true);
                exportButton.setDisable(true);
            } else {
                errorLabel.setVisible(false);
                exportButton.setDisable(false);
                // Store the valid date range
                this.startMonth = startMonth;
                this.endMonth = endMonth;
            }
        } catch (Exception e) {
            errorLabel.setText("Invalid date format");
            errorLabel.setVisible(true);
            exportButton.setDisable(true);
        }
    }

    private void handleExport() {
        if (validateSelection()) {
            confirmed = true;
            stage.close();
        }
    }

    private boolean validateSelection() {
        if (startMonthCombo.getValue() == null || endMonthCombo.getValue() == null) {
            errorLabel.setText("Please select both start and end months");
            errorLabel.setVisible(true);
            return false;
        }

        validateDateRange();
        return !errorLabel.isVisible();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public YearMonth getStartMonth() {
        return startMonth;
    }

    public YearMonth getEndMonth() {
        return endMonth;
    }

    public String getSelectedExportType() {
        return selectedExportType;
    }
}