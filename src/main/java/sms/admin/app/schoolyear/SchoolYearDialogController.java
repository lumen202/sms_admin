package sms.admin.app.schoolyear;

import dev.finalproject.models.SchoolYear;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;

public class SchoolYearDialogController {
    @FXML private Label headerLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Dialog<SchoolYear> dialog;
    private SchoolYear existingSchoolYear;

    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusMonths(10));
    }

    public void setDialog(Dialog<SchoolYear> dialog) {
        this.dialog = dialog;
        
        // Add handler for ESC key and window close button
        dialog.setOnCloseRequest(event -> handleCancel());
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelButtonType);
        
        // Set the result converter to handle window closing properly
        dialog.setResultConverter(buttonType -> {
            if (buttonType != null && buttonType.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return null;
            }
            return dialog.getResult();
        });
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

    private SchoolYear createSchoolYear() {
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
