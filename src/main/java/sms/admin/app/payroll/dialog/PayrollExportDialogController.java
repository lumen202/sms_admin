package sms.admin.app.payroll.dialog;

import java.time.YearMonth;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import sms.admin.util.datetime.DateTimeUtils;

/**
 * Controller for the payroll export dialog, managing the selection of date
 * ranges and export types for payroll data.
 * This class handles the UI elements and logic for selecting start and end
 * months, validating the date range,
 * and confirming the export action.
 */
public class PayrollExportDialogController {
    @FXML
    private ComboBox<String> startMonthCombo; // ComboBox for selecting the start month
    @FXML
    private ComboBox<String> endMonthCombo; // ComboBox for selecting the end month
    @FXML
    private Button exportButton; // Button to confirm the export action
    @FXML
    private Button cancelButton; // Button to cancel and close the dialog
    @FXML
    private Label errorLabel; // Label to display error messages

    private Stage stage; // The stage for this dialog
    private String selectedExportType; // The selected export type (e.g., "excel", "csv", "xlsx")
    private YearMonth startMonth; // The selected start month
    private YearMonth endMonth; // The selected end month
    private boolean confirmed = false; // Flag indicating if the export was confirmed

    /**
     * Initializes the controller, setting up the UI components and event listeners.
     */
    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        setupListeners();
    }

    /**
     * Sets up event listeners for the combo boxes and buttons.
     */
    private void setupListeners() {
        startMonthCombo.setOnAction(e -> validateDateRange());
        endMonthCombo.setOnAction(e -> validateDateRange());

        exportButton.setOnAction(e -> handleExport());
        cancelButton.setOnAction(e -> stage.close());
    }

    /**
     * Initializes the dialog with the current year, month, and export type.
     *
     * @param currentYear  The current academic year (e.g., "2023-2024").
     * @param currentMonth The current month to set as default (e.g., "September
     *                     2023").
     * @param exportType   The type of export (e.g., "excel", "csv", "xlsx").
     */
    public void initData(String currentYear, String currentMonth, String exportType) {
        this.selectedExportType = exportType;

        // Validate export type
        if (!exportType.equals("excel") && !exportType.equals("csv") && !exportType.equals("xlsx")) {
            errorLabel.setText("Invalid export type selected");
            errorLabel.setVisible(true);
            exportButton.setDisable(true);
            return;
        }

        // Populate month combo boxes for the school year
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

        // Perform initial validation
        validateDateRange();
    }

    /**
     * Validates the selected date range to ensure the end month is not before the
     * start month.
     */
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

    /**
     * Handles the export action, validating the selection and closing the dialog if
     * valid.
     */
    private void handleExport() {
        if (validateSelection()) {
            confirmed = true;
            stage.close();
        }
    }

    /**
     * Validates the current selection of start and end months.
     *
     * @return true if the selection is valid, false otherwise.
     */
    private boolean validateSelection() {
        if (startMonthCombo.getValue() == null || endMonthCombo.getValue() == null) {
            errorLabel.setText("Please select both start and end months");
            errorLabel.setVisible(true);
            return false;
        }

        validateDateRange();
        return !errorLabel.isVisible();
    }

    /**
     * Sets the stage for this dialog.
     *
     * @param stage The stage to set.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Checks if the export was confirmed.
     *
     * @return true if the export was confirmed, false otherwise.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Gets the selected start month.
     *
     * @return The selected start YearMonth.
     */
    public YearMonth getStartMonth() {
        return startMonth;
    }

    /**
     * Gets the selected end month.
     *
     * @return The selected end YearMonth.
     */
    public YearMonth getEndMonth() {
        return endMonth;
    }

    /**
     * Gets the selected export type.
     *
     * @return The selected export type (e.g., "excel", "csv", "xlsx").
     */
    public String getSelectedExportType() {
        return selectedExportType;
    }
}