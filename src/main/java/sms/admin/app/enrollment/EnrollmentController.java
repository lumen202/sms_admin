package sms.admin.app.enrollment;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.OptionalInt;
import java.sql.Date;

import dev.sol.core.application.FXController;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import dev.finalproject.App;
import dev.finalproject.models.*;
import sms.admin.util.ValidationUtils;
import sms.admin.util.enrollment.CsvStudent;
import sms.admin.util.enrollment.EnrollmentUtils;
import sms.admin.util.DialogUtils;
import sms.admin.util.enrollment.CsvImporter;

public class EnrollmentController extends FXController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField middleNameField;
    @FXML
    private TextField nameExtField;
    @FXML
    private TextField streetField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField municipalityField;
    @FXML
    private TextField postalCodeField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField contactNumberField;
    @FXML
    private TextField guardianNameField;
    @FXML
    private TextField guardianContactField;
    @FXML
    private DatePicker dateOfBirthPicker;
    @FXML
    private ComboBox<String> statusComboBox;
    @FXML
    private TextField fareField;
    @FXML
    private Button submitButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button importCsvButton;
    @FXML
    private Label importStatusLabel;
    @FXML
    private TextField barangayField;

    private SchoolYear currentSchoolYear;

    @Override
    protected void load_fields() {
        System.out.println("Enrollment is called");
        // Initialize status options
        statusComboBox.getItems().addAll("Active", "Inactive", "Graduate");
        // Add listeners for real-time validation
        addValidationListeners();
    }

    private void addValidationListeners() {
        // Email validation
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidEmail(newValue)) {
                ValidationUtils.setErrorStyle(emailField);
            } else {
                ValidationUtils.resetStyle(emailField);
            }
        });

        // Phone number validation
        contactNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidPhoneNumber(newValue)) {
                ValidationUtils.setErrorStyle(contactNumberField);
            } else {
                ValidationUtils.resetStyle(contactNumberField);
            }
        });

        guardianContactField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidPhoneNumber(newValue)) {
                ValidationUtils.setErrorStyle(guardianContactField);
            } else {
                ValidationUtils.resetStyle(guardianContactField);
            }
        });

        // Postal code validation
        postalCodeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidPostalCode(newValue)) {
                ValidationUtils.setErrorStyle(postalCodeField);
            } else {
                ValidationUtils.resetStyle(postalCodeField);
            }
        });

        // Fare validation
        fareField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidFare(newValue)) {
                ValidationUtils.setErrorStyle(fareField);
            } else {
                ValidationUtils.resetStyle(fareField);
            }
        });
    }

    private String generateNextStudentId() {
        OptionalInt maxId = App.COLLECTIONS_REGISTRY.getList("STUDENT").stream()
                .filter(s -> s instanceof Student)
                .map(s -> ((Student) s).getStudentID())
                .mapToInt(id -> id)
                .max();
        return String.valueOf(maxId.isPresent() ? maxId.getAsInt() + 1 : 1);
    }

    @FXML
    private void handleSubmit() {
        if (validateFields()) {
            try {
                if (currentSchoolYear == null) {
                    showErrorMessage("No school year selected");
                    return;
                }

                String nextStudentId = generateNextStudentId();
                
                // Convert LocalDate to java.sql.Date
                Date dateOfBirth = dateOfBirthPicker.getValue() != null ?
                    Date.valueOf(dateOfBirthPicker.getValue()) :
                    null;

                Student student = EnrollmentUtils.enrollStudent(
                        nextStudentId,
                        firstNameField.getText(),
                        middleNameField.getText(),
                        lastNameField.getText(),
                        nameExtField.getText(),
                        emailField.getText(),
                        statusComboBox.getValue(),
                        contactNumberField.getText(),
                        dateOfBirth,  // Use the converted date
                        Double.parseDouble(fareField.getText()),
                        streetField.getText(),
                        barangayField.getText(),
                        cityField.getText(),
                        municipalityField.getText(),
                        postalCodeField.getText(),
                        guardianNameField.getText(),
                        guardianContactField.getText(),
                        null, // cluster name - using default
                        currentSchoolYear);

                handleClear();
                showSuccessMessage("Student enrolled successfully!");

            } catch (Exception e) {
                showErrorMessage("Failed to enroll student: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        // Required fields validation
        if (ValidationUtils.isTextFieldEmpty(firstNameField)) {
            ValidationUtils.setErrorStyle(firstNameField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(lastNameField)) {
            ValidationUtils.setErrorStyle(lastNameField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(streetField)) {
            ValidationUtils.setErrorStyle(streetField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(cityField)) {
            ValidationUtils.setErrorStyle(cityField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(municipalityField)) {
            ValidationUtils.setErrorStyle(municipalityField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(guardianNameField)) {
            ValidationUtils.setErrorStyle(guardianNameField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(barangayField)) {
            ValidationUtils.setErrorStyle(barangayField);
            isValid = false;
        }

        // Format validations
        if (!ValidationUtils.isTextFieldEmpty(emailField) && !ValidationUtils.isValidEmail(emailField.getText())) {
            ValidationUtils.setErrorStyle(emailField);
            isValid = false;
        }
        if (!ValidationUtils.isTextFieldEmpty(contactNumberField)
                && !ValidationUtils.isValidPhoneNumber(contactNumberField.getText())) {
            ValidationUtils.setErrorStyle(contactNumberField);
            isValid = false;
        }
        if (!ValidationUtils.isTextFieldEmpty(guardianContactField)
                && !ValidationUtils.isValidPhoneNumber(guardianContactField.getText())) {
            ValidationUtils.setErrorStyle(guardianContactField);
            isValid = false;
        }
        if (!ValidationUtils.isTextFieldEmpty(postalCodeField)
                && !ValidationUtils.isValidPostalCode(postalCodeField.getText())) {
            ValidationUtils.setErrorStyle(postalCodeField);
            isValid = false;
        }
        if (!ValidationUtils.isTextFieldEmpty(fareField) && !ValidationUtils.isValidFare(fareField.getText())) {
            ValidationUtils.setErrorStyle(fareField);
            isValid = false;
        }

        // Date validation
        if (dateOfBirthPicker.getValue() == null) {
            ValidationUtils.setErrorStyle(dateOfBirthPicker);
            isValid = false;
        }

        // Status validation
        if (statusComboBox.getValue() == null) {
            ValidationUtils.setErrorStyle(statusComboBox);
            isValid = false;
        }

        return isValid;
    }

    private void handleClear() {
        // Clear all fields
        firstNameField.clear();
        lastNameField.clear();
        middleNameField.clear();
        nameExtField.clear();
        streetField.clear();
        cityField.clear();
        municipalityField.clear();
        postalCodeField.clear();
        emailField.clear();
        contactNumberField.clear();
        guardianNameField.clear();
        guardianContactField.clear();
        dateOfBirthPicker.setValue(null);
        statusComboBox.setValue(null);
        fareField.clear();
        barangayField.clear();

        // Reset all styles
        ValidationUtils.resetStyle(firstNameField);
        ValidationUtils.resetStyle(lastNameField);
        ValidationUtils.resetStyle(middleNameField);
        ValidationUtils.resetStyle(nameExtField);
        ValidationUtils.resetStyle(streetField);
        ValidationUtils.resetStyle(cityField);
        ValidationUtils.resetStyle(municipalityField);
        ValidationUtils.resetStyle(postalCodeField);
        ValidationUtils.resetStyle(emailField);
        ValidationUtils.resetStyle(contactNumberField);
        ValidationUtils.resetStyle(guardianNameField);
        ValidationUtils.resetStyle(guardianContactField);
        ValidationUtils.resetStyle(dateOfBirthPicker);
        ValidationUtils.resetStyle(statusComboBox);
        ValidationUtils.resetStyle(fareField);
        ValidationUtils.resetStyle(barangayField);
    }

    @Override
    protected void load_bindings() {
        System.out.println();
    }

    @Override
    protected void load_listeners() {
        System.out.println();
    }

    @FXML
    private void handleImportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(importCsvButton.getScene().getWindow());
        if (selectedFile != null) {
            importStatusLabel.setText("Processing: " + selectedFile.getName());
            try {
                List<CsvStudent> students = CsvImporter.importCsv(selectedFile);
                int successCount = 0;

                if (currentSchoolYear == null) {
                    importStatusLabel.setText("Error: No school year selected");
                    return;
                }

                for (CsvStudent csvStudent : students) {
                    try {
                        EnrollmentUtils.enrollStudentFromCsv(csvStudent, currentSchoolYear);
                        successCount++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                importStatusLabel.setText("Successfully imported " + successCount + " students");

            } catch (IOException e) {
                importStatusLabel.setText("Error: Failed to read CSV file");
                e.printStackTrace();
            }
        }
    }

    public void updateYear(String year) {
        initializeWithYear(year);
    }

    public void initializeWithYear(String year) {
        System.out.println("Initializing enrollment data for year: " + year);
        // Parse the year string to get start and end years
        String[] years = year.split("-");
        if (years.length == 2) {
            try {
                int startYear = Integer.parseInt(years[0].trim());
                int endYear = Integer.parseInt(years[1].trim());
                
                // Find the school year from the collection
                this.currentSchoolYear = App.COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR").stream()
                    .filter(sy -> sy instanceof SchoolYear)
                    .map(sy -> (SchoolYear) sy)
                    .filter(sy -> sy.getYearStart() == startYear && sy.getYearEnd() == endYear)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("School year not found: " + year));

                System.out.println("Selected school year: " + startYear + "-" + endYear);
            } catch (NumberFormatException e) {
                showErrorMessage("Invalid year format: " + year);
            }
        }
    }

    private void showSuccessMessage(String message) {
        DialogUtils.showSuccess(message);
    }

    private void showErrorMessage(String message) {
        DialogUtils.showError(message);
    }
}
