package sms.admin.app.schoolyear;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import dev.finalproject.data.SchoolYearDAO;
import dev.finalproject.models.SchoolYear;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import sms.admin.util.dialog.DialogManager;

/**
 * Controller for the school year dialog, handling both creation and editing of
 * school years. This class manages the UI elements and logic for selecting
 * start and end years and months, ensuring that the selected dates form a valid
 * school year period.
 */
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

    /**
     * Returns the property holding the current school year being edited or
     * created.
     *
     * @return the school year property
     */
    public ObjectProperty<SchoolYear> schoolYearProperty() {
        return schoolYearProperty;
    }

    // Define months as a class field for reuse
    private final String[] months = { "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };

    /**
     * Initializes the controller, setting up the UI components and listeners.
     */
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

    /**
     * Updates the end year options based on the selected start year. Ensures
     * that the end year is at least the start year.
     */
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

    /**
     * Updates the end month options based on the selected start year, start
     * month, and end year. Ensures that the end month is after the start month
     * if the years are the same.
     */
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

    /**
     * Sets the dialog stage and applies overlay effects.
     *
     * @param dialogStage The stage of the dialog.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), true);
        dialogStage.setOnCloseRequest(e -> DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), false));
    }

    /**
     * Sets the dialog reference.
     *
     * @param dialog The dialog instance.
     */
    public void setDialog(SchoolYearDialog dialog) {
        this.dialog = dialog;
    }

    /**
     * Sets the existing school year for editing.
     *
     * @param schoolYear The school year to edit, or null for a new one.
     */
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

    /**
     * Handles the save action, validating input and saving the school year.
     */
    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                SchoolYear schoolYear = createSchoolYear();

                // Double check yearID is valid
                if (schoolYear.getYearID() <= 0) {
                    schoolYear = new SchoolYear(
                            getNextYearId(),
                            schoolYear.getYearStart(),
                            schoolYear.getYearEnd(),
                            schoolYear.getMonthStart(),
                            schoolYear.getMonthEnd(),
                            schoolYear.getDayStart(),
                            schoolYear.getDayEnd());
                }

                System.out.println("Attempting to save school year with ID: " + schoolYear.getYearID());

                if (existingSchoolYear == null) {
                    // Ensure we're passing the yearID in the insert
                    SchoolYearDAO.insert(schoolYear);
                    System.out.println("Successfully inserted new school year with ID: " + schoolYear.getYearID());
                } else {
                    SchoolYearDAO.update(schoolYear);
                    System.out.println("Successfully updated school year with ID: " + schoolYear.getYearID());
                }

                // Rest of the existing code...
                schoolYearProperty.set(schoolYear);
                isSaveClicked = true;
                DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), false);
                dialogStage.close();

                if (dialog != null) {
                    dialog.resultProperty().set(schoolYear);
                }
            } catch (Exception e) {
                e.printStackTrace();
                schoolYearProperty.set(null);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Database Error");
                alert.setContentText("Could not save school year: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Handles the cancel action, closing the dialog.
     */
    @FXML
    private void handleCancel() {
        DialogManager.setOverlayEffect((Stage) dialogStage.getOwner(), false);
        dialogStage.close();
    }

    /**
     * Creates a SchoolYear object based on the selected values.
     *
     * @return The created SchoolYear object.
     */
    public SchoolYear createSchoolYear() {
        int yearId;
        if (existingSchoolYear != null) {
            yearId = existingSchoolYear.getYearID();
        } else {
            yearId = getNextYearId();
            if (yearId <= 0) {
                throw new IllegalStateException("Invalid yearID generated");
            }
        }

        // Get the last day of the end month
        int endDay = getLastDayOfMonth(
                endYearCombo.getValue(),
                endMonthCombo.getValue());

        SchoolYear newYear = new SchoolYear(
                yearId,
                startYearCombo.getValue(),
                endYearCombo.getValue(),
                startMonthCombo.getValue(),
                endMonthCombo.getValue(),
                1, // First day of start month
                endDay // Last day of end month
        );

        System.out.println("Created school year with ID: " + yearId);
        return newYear;
    }

    /**
     * Gets the next available year ID for a new school year.
     *
     * @return The next year ID.
     */
    private int getNextYearId() {
        try {
            // Get the maximum yearID from the database
            List<SchoolYear> allYears = SchoolYearDAO.getSchoolYearList();
            if (allYears == null || allYears.isEmpty()) {
                return 1; // Start with 1 if no existing years
            }

            // Find the maximum yearID and add 1
            int maxId = allYears.stream()
                    .mapToInt(SchoolYear::getYearID)
                    .max()
                    .orElse(0);

            // Ensure we never return 0 or negative
            return Math.max(1, maxId + 1);
        } catch (Exception e) {
            e.printStackTrace();
            // If there's an error, try to get a timestamp-based ID to avoid conflicts
            return (int) (System.currentTimeMillis() % 100000);
        }
    }

    /**
     * Converts a month name to its numerical value.
     *
     * @param monthName The name of the month.
     * @return The numerical value of the month (1-12).
     */
    private int getMonthNumber(String monthName) {
        return Month.valueOf(monthName.toUpperCase()).getValue();
    }

    /**
     * Gets the last day of the specified month and year.
     *
     * @param year      The year.
     * @param monthName The name of the month.
     * @return The last day of the month.
     */
    private int getLastDayOfMonth(int year, String monthName) {
        Month month = Month.valueOf(monthName.toUpperCase());
        return YearMonth.of(year, month).lengthOfMonth();
    }

    /**
     * Validates the input fields to ensure a valid school year can be created.
     *
     * @return true if input is valid, false otherwise.
     */
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
                errorMessage += "End year must be greater than org equal to start year!\n";
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
