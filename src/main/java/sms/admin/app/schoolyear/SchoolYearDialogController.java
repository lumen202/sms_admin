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
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.time.YearMonth;

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

    private final ObjectProperty<SchoolYear> schoolYearProperty = new SimpleObjectProperty<>();

    public ObjectProperty<SchoolYear> schoolYearProperty() {
        return schoolYearProperty;
    }

    // Define months as a class field for reuse
    private final String[] months = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

    public void initialize() {
        int currentYear = LocalDate.now().getYear();

        // Setup initial year options (current year +/- 5 years)
        Integer[] years = new Integer[11];
        for (int i = 0; i < 11; i++) {
            years[i] = currentYear - 5 + i;
        }
        startYearCombo.setItems(FXCollections.observableArrayList(years));
        endYearCombo.setItems(FXCollections.observableArrayList(years));

        // Setup month options
        startMonthCombo.setItems(FXCollections.observableArrayList(months));
        endMonthCombo.setItems(FXCollections.observableArrayList(months));

        // Add listeners for dynamic updates
        startYearCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateEndYearOptions();
                updateEndMonthOptions();
            }
        });

        startMonthCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateEndMonthOptions();
        });

        endYearCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateEndMonthOptions();
        });

        // Set default values for a new academic year
        startYearCombo.setValue(currentYear);
        startMonthCombo.setValue("SEPTEMBER");
        endMonthCombo.setValue("JUNE");
        // Listener will set endYearCombo to currentYear + 1
    }

    // Update end year options based on start year
    private void updateEndYearOptions() {
        Integer startYear = startYearCombo.getValue();
        if (startYear != null) {
            List<Integer> endYears = new ArrayList<>();
            for (int i = startYear; i <= startYear + 10; i++) {
                endYears.add(i);
            }
            endYearCombo.setItems(FXCollections.observableArrayList(endYears));
            // Set default end year to startYear + 1 if current value is invalid
            if (endYearCombo.getValue() == null || endYearCombo.getValue() < startYear) {
                endYearCombo.setValue(startYear + 1);
            }
        }
    }

    // Update end month options based on start year, start month, and end year
    private void updateEndMonthOptions() {
        Integer startYear = startYearCombo.getValue();
        String startMonth = startMonthCombo.getValue();
        Integer endYear = endYearCombo.getValue();

        if (startYear == null || startMonth == null || endYear == null) {
            endMonthCombo.setItems(FXCollections.observableArrayList(months));
            return;
        }

        if (endYear > startYear) {
            endMonthCombo.setItems(FXCollections.observableArrayList(months));
        } else if (endYear.equals(startYear)) {
            int startMonthIndex = getMonthNumber(startMonth);
            List<String> availableMonths = new ArrayList<>();
            for (String month : months) {
                if (getMonthNumber(month) > startMonthIndex) {
                    availableMonths.add(month);
                }
            }
            endMonthCombo.setItems(FXCollections.observableArrayList(availableMonths));
            // Adjust end month if current selection is invalid
            if (!availableMonths.contains(endMonthCombo.getValue()) && !availableMonths.isEmpty()) {
                endMonthCombo.setValue(availableMonths.get(0));
            }
        } else {
            // endYear < startYear (shouldn't happen due to updateEndYearOptions)
            endMonthCombo.setItems(FXCollections.emptyObservableList());
        }
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
            updateEndYearOptions();
            updateEndMonthOptions();
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            SchoolYear schoolYear = createSchoolYear();
            if (schoolYear != null) {
                try {
                    System.out.println("Created school year: " + schoolYear);

                    if (existingSchoolYear == null) {
                        SchoolYearDAO.insert(schoolYear);
                        System.out.println("Inserted new school year");
                    } else {
                        SchoolYearDAO.update(schoolYear);
                        System.out.println("Updated existing school year");
                    }

                    // Set property and prepare to close dialog
                    schoolYearProperty.set(schoolYear);
                    System.out.println("Set school year property: " + schoolYear);

                    isSaveClicked = true;
                    DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), false);
                    // Just close the dialog, the result will be handled by the converter
                    dialogStage.close();

                    // Fire dialog close event with OK button type
                    if (dialog != null) {
                        dialog.resultProperty().set(schoolYear);
                    }
                } catch (Exception e) {
                    schoolYearProperty.set(null); // Reset on error
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
        int yearId = existingSchoolYear != null ? existingSchoolYear.getYearID() : getNextYearId();

        // Get the last day of the end month
        int endDay = getLastDayOfMonth(
                endYearCombo.getValue(),
                endMonthCombo.getValue());

        return new SchoolYear(
                yearId,
                startYearCombo.getValue(),
                endYearCombo.getValue(),
                startMonthCombo.getValue(),
                endMonthCombo.getValue(),
                1, // First day of start month
                endDay // Last day of end month
        );
    }

    private int getNextYearId() {
        List<SchoolYear> allYears = SchoolYearDAO.getSchoolYearList();
        return allYears.stream()
                .mapToInt(SchoolYear::getYearID)
                .max()
                .orElse(0) + 1;
    }

    private int getMonthNumber(String monthName) {
        return Month.valueOf(monthName.toUpperCase()).getValue();
    }

    private int getLastDayOfMonth(int year, String monthName) {
        Month month = Month.valueOf(monthName.toUpperCase());
        return YearMonth.of(year, month).lengthOfMonth();
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
            } else if (endYearCombo.getValue().equals(startYearCombo.getValue())
                    && Month.valueOf(endMonthCombo.getValue()).getValue() <= Month.valueOf(startMonthCombo.getValue())
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
