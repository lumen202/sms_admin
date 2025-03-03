package sms.admin.app.enrollment;

import dev.sol.core.application.FXController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import sms.admin.util.ValidationUtils;

import java.io.File;
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

    @FXML
    private void handleSubmit() {
        if (validateFields()) {
            // TODO: Implement save logic
            System.out.println("All fields are valid!");
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

    @FXML
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
            // TODO: Add CSV processing logic here
        }
    }

    public void updateYear(String year) {
        // Update enrollment data based on selected year
        // Add your year-specific enrollment logic here
        System.out.println("Updating enrollment data for year: " + year);
    }
}
