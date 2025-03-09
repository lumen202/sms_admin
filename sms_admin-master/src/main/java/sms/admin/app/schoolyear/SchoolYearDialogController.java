package sms.admin.app.schoolyear;

import dev.finalproject.models.SchoolYear;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SchoolYearDialogController {
    @FXML private Label headerLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Dialog<SchoolYear> dialog;
    private SchoolYear existingSchoolYear;

    public void initialize() {
        // Set custom converter to handle dates and clean unwanted characters (like backticks)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        StringConverter<java.time.LocalDate> converter = new StringConverter<>() {
            @Override
            public String toString(java.time.LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }
            @Override
            public java.time.LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                String cleaned = string.trim().replace("`", "");
                return java.time.LocalDate.parse(cleaned, formatter);
            }
        };
        startDatePicker.setConverter(converter);
        endDatePicker.setConverter(converter);
        // Set initial values
        startDatePicker.setValue(java.time.LocalDate.now());
        endDatePicker.setValue(java.time.LocalDate.now().plusMonths(10));
        
        // Remove this line since we're using FXML onAction
        // cancelButton.setOnAction(event -> handleCancel());
    }

    // Revert to accepting Dialog<SchoolYear> as before
    public void setDialog(Dialog<SchoolYear> dialog) {
        this.dialog = dialog;
    }

    public void setExistingSchoolYear(SchoolYear schoolYear) {
        this.existingSchoolYear = schoolYear;
        headerLabel.setText(schoolYear == null ? "Create New School Year" : "Edit School Year");

        if (schoolYear != null) {
            startDatePicker.setValue(LocalDate.of(
                schoolYear.getYearStart(),
                getMonthNumber(schoolYear.getMonthStart()),
                schoolYear.getDayStart()
            ));
            endDatePicker.setValue(LocalDate.of(
                schoolYear.getYearEnd(),
                getMonthNumber(schoolYear.getMonthEnd()),
                schoolYear.getDayEnd()
            ));
        }
    }

    @FXML
    private void handleSave() {
        SchoolYear result = createSchoolYear();
        if (result != null) {
            dialog.setResult(result);
            dialog.close();
        }
    }

    @FXML
    private void handleCancel() {
        if (dialog != null) {
            dialog.setResult(null);
            dialog.close();
        }
    }

    public SchoolYear createSchoolYear() {  // Changed from private to public
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate != null && endDate != null) {
            return new SchoolYear(
                existingSchoolYear != null ? existingSchoolYear.getYearID() : 0,
                startDate.getYear(),
                endDate.getYear(),
                startDate.getMonth().toString(),
                endDate.getMonth().toString(),
                startDate.getDayOfMonth(),
                endDate.getDayOfMonth()
            );
        }
        return null;
    }

    private int getMonthNumber(String monthName) {
        return java.time.Month.valueOf(monthName.toUpperCase()).getValue();
    }
}
