package sms.admin.app.schoolyear;

import dev.finalproject.models.SchoolYear;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;

public class SchoolYearDialog extends Dialog<SchoolYear> {
    private SchoolYearDialogController controller;
    private final Spinner<Integer> startYearSpinner;
    private final Spinner<Integer> endYearSpinner;
    private final SchoolYear existingSchoolYear;

    public SchoolYearDialog() {
        this(null);
    }

    public SchoolYearDialog(SchoolYear schoolYear) {
        this.existingSchoolYear = schoolYear;
        
        setTitle(schoolYear == null ? "New School Year" : "Edit School Year");
        setHeaderText(schoolYear == null ? "Create a new school year" : "Edit existing school year");

        // Create UI components
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        int currentYear = java.time.Year.now().getValue();
        startYearSpinner = new Spinner<>(currentYear - 10, currentYear + 10, 
            schoolYear != null ? schoolYear.getYearStart() : currentYear);
        endYearSpinner = new Spinner<>(currentYear - 10, currentYear + 10, 
            schoolYear != null ? schoolYear.getYearEnd() : currentYear + 1);

        startYearSpinner.setEditable(true);
        endYearSpinner.setEditable(true);

        grid.add(new Label("Start Year:"), 0, 0);
        grid.add(startYearSpinner, 1, 0);
        grid.add(new Label("End Year:"), 0, 1);
        grid.add(endYearSpinner, 1, 1);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Convert result to SchoolYear
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                int startYear = startYearSpinner.getValue();
                int endYear = endYearSpinner.getValue();
                
                if (existingSchoolYear != null) {
                    existingSchoolYear.setYearStart(startYear);
                    existingSchoolYear.setYearEnd(endYear);
                    return existingSchoolYear;
                } else {
                    // Create new SchoolYear with required parameters
                    return new SchoolYear(
                        startYear,        // yearStart
                        endYear,         // yearEnd
                        0,              // schoolYearID (database will assign)
                        "SY " + startYear + "-" + endYear,  // description
                        "Active",       // status
                        java.time.LocalDateTime.now().getMonthValue(),  // currentMonth
                        startYear       // currentYear
                    );
                }
            }
            return null;
        });

        // Add validation
        getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
        startYearSpinner.valueProperty().addListener((obs, oldVal, newVal) -> 
            validateInput());
        endYearSpinner.valueProperty().addListener((obs, oldVal, newVal) -> 
            validateInput());

        validateInput();
    }

    private void validateInput() {
        boolean valid = endYearSpinner.getValue() > startYearSpinner.getValue();
        getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
    }

    public ObjectProperty<SchoolYear> schoolYearProperty() {
        return controller.schoolYearProperty();
    }
}
