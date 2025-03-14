package sms.admin.app.schoolyear;

import dev.finalproject.data.SchoolYearDAO;
import dev.finalproject.models.SchoolYear;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import sms.admin.util.dialog.DialogManager;
import java.time.Month;
import java.time.LocalDate;
import javafx.collections.FXCollections;

public class SchoolYearDialogController {
    @FXML
    private Label headerLabel;
    @FXML
    private ComboBox<Integer> startYearCombo;
    @FXML
    private ComboBox<String> startMonthCombo;
    @FXML
    private ComboBox<Integer> endYearCombo;
    @FXML
    private ComboBox<String> endMonthCombo;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private Stage dialogStage;
    private boolean isSaveClicked = false;
    private SchoolYear existingSchoolYear;
    private SchoolYearDialog dialog;

    public void initialize() {
        // Setup year options (current year +/- 5 years)
        int currentYear = LocalDate.now().getYear();
        Integer[] years = new Integer[11];
        for (int i = 0; i < 11; i++) {
            years[i] = currentYear - 5 + i;
        }
        startYearCombo.setItems(FXCollections.observableArrayList(years));
        endYearCombo.setItems(FXCollections.observableArrayList(years));

        // Setup month options
        String[] months = { "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
                "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };
        startMonthCombo.setItems(FXCollections.observableArrayList(months));
        endMonthCombo.setItems(FXCollections.observableArrayList(months));

        // Set default values
        startYearCombo.setValue(currentYear);
        endYearCombo.setValue(currentYear);
        startMonthCombo.setValue("JUNE");
        endMonthCombo.setValue("MARCH");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), true);
        dialogStage.setOnCloseRequest(e -> DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), false));
    }

    public void setDialog(SchoolYearDialog dialog) {
        this.dialog = dialog;
    }

    public void setExistingSchoolYear(SchoolYear schoolYear) {
        this.existingSchoolYear = schoolYear;
        headerLabel.setText(schoolYear == null ? "Create New School Year" : "Edit School Year");

        if (schoolYear != null) {
            startYearCombo.setValue(schoolYear.getYearStart());
            endYearCombo.setValue(schoolYear.getYearEnd());
            startMonthCombo.setValue(schoolYear.getMonthStart());
            endMonthCombo.setValue(schoolYear.getMonthEnd());
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            SchoolYear schoolYear = createSchoolYear();
            if (schoolYear != null) {
                try {
                    if (existingSchoolYear == null) {
                        SchoolYearDAO.insert(schoolYear);
                    } else {
                        SchoolYearDAO.update(schoolYear);
                    }
                    isSaveClicked = true;
                    DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), false);
                    dialogStage.close();
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Database Error");
                    alert.setContentText("Could not save school year: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }

    @FXML
    private void handleCancel() {
        DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), false);
        dialogStage.close();
    }

    public SchoolYear createSchoolYear() {
        return new SchoolYear(
                existingSchoolYear != null ? existingSchoolYear.getYearID() : 0,
                startYearCombo.getValue(),
                endYearCombo.getValue(),
                startMonthCombo.getValue(),
                endMonthCombo.getValue(),
                1, // Default to first day of month
                1 // Default to first day of month
        );
    }

    private int getMonthNumber(String monthName) {
        return java.time.Month.valueOf(monthName.toUpperCase()).getValue();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (startYearCombo.getValue() == null) {
            errorMessage += "Start year is required!\n";
        }
        if (startMonthCombo.getValue() == null) {
            errorMessage += "Start month is required!\n";
        }
        if (endYearCombo.getValue() == null) {
            errorMessage += "End year is required!\n";
        }
        if (endMonthCombo.getValue() == null) {
            errorMessage += "End month is required!\n";
        }

        if (startYearCombo.getValue() != null && endYearCombo.getValue() != null) {
            if (endYearCombo.getValue() < startYearCombo.getValue()) {
                errorMessage += "End year must be greater than or equal to start year!\n";
            } else if (endYearCombo.getValue().equals(startYearCombo.getValue()) &&
                    Month.valueOf(endMonthCombo.getValue()).getValue() < Month.valueOf(startMonthCombo.getValue())
                            .getValue()) {
                errorMessage += "End month must be after start month for the same year!\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct the invalid fields");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}
