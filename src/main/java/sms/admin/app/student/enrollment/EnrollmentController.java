package sms.admin.app.student.enrollment;

import java.sql.Date;
import java.util.OptionalInt;

import dev.finalproject.App;
import dev.finalproject.data.StudentGuardianDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.Guardian;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.finalproject.models.StudentGuardian;
import dev.sol.core.application.FXController;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import sms.admin.util.DialogUtils;
import sms.admin.util.dialog.ValidationUtils;
import sms.admin.util.enrollment.EnrollmentUtils;
import sms.admin.util.profile.ProfileDataManager;

/**
 * Controller for the student enrollment form, managing the input and submission
 * of student data. This class handles the UI elements and logic for entering
 * student details, validating input fields, and enrolling students into the
 * system for a specific school year.
 */
public class EnrollmentController extends FXController {

    @FXML
    private TextField firstNameField; // Field for student's first name
    @FXML
    private TextField lastNameField; // Field for student's last name
    @FXML
    private TextField middleNameField; // Field for student's middle name
    @FXML
    private TextField nameExtField; // Field for student's name extension (e.g., Jr., Sr.)
    @FXML
    private TextField streetField; // Field for student's street address
    @FXML
    private TextField cityField; // Field for student's city
    @FXML
    private TextField municipalityField; // Field for student's municipality
    @FXML
    private TextField postalCodeField; // Field for student's postal code
    @FXML
    private TextField emailField; // Field for student's email address
    @FXML
    private TextField contactNumberField; // Field for student's contact number
    @FXML
    private TextField guardianFirstNameField; // Field for guardian's first name
    @FXML
    private TextField guardianMiddleNameField; // Field for guardian's middle name
    @FXML
    private TextField guardianLastNameField; // Field for guardian's last name
    @FXML
    private TextField guardianRelationshipField; // Field for guardian's relationship
    @FXML
    private TextField guardianContactInfoField; // Field for guardian's contact information
    @FXML
    private GridPane guardianEditGrid; // GridPane for guardian fields
    @FXML
    private DatePicker dateOfBirthPicker; // Picker for student's date of birth
    @FXML
    private ComboBox<String> statusComboBox; // ComboBox for student's status (Active, Inactive, Graduate)
    @FXML
    private TextField fareField; // Field for student's fare amount
    @FXML
    private Button submitButton; // Button to submit the enrollment form
    @FXML
    private Button clearButton; // Button to clear the form
    @FXML
    private TextField barangayField; // Field for student's barangay

    private SchoolYear currentSchoolYear; // The current school year for enrollment
    private Stage dialogStage; // The stage for the dialog, if opened as a dialog

    /**
     * Loads the initial fields and configurations for the enrollment form.
     */
    @Override
    protected void load_fields() {
        try {
            // Initialize status options
            statusComboBox.getItems().addAll("Single", "Married", "Widow");
            statusComboBox.setValue("Single"); // Set default value

            String selectedYear = (String) getParameter("selectedYear");
            if (selectedYear != null) {
                initializeWithYear(selectedYear);
            }

            // Add listeners for real-time validation
            addValidationListeners();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Error initializing form");
        }
    }

    /**
     * Sets up real-time validation listeners for input fields to provide
     * immediate feedback.
     */
    private void addValidationListeners() {
        // Email validation
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidEmail(newValue)) {
                ValidationUtils.setErrorStyle(emailField);
            } else {
                ValidationUtils.resetStyle(emailField);
            }
        });

        // Phone number validation for student
        contactNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidPhoneNumber(newValue)) {
                ValidationUtils.setErrorStyle(contactNumberField);
            } else {
                ValidationUtils.resetStyle(contactNumberField);
            }
        });

        // Phone number validation for guardian
        guardianContactInfoField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !ValidationUtils.isValidPhoneNumber(newValue)) {
                ValidationUtils.setErrorStyle(guardianContactInfoField);
            } else {
                ValidationUtils.resetStyle(guardianContactInfoField);
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

    /**
     * Generates the next available student ID based on existing students in the
     * system.
     *
     * @return A string representing the next student ID.
     */
    private String generateNextStudentId() {
        OptionalInt maxId = App.COLLECTIONS_REGISTRY.getList("STUDENT").stream()
                .filter(s -> s instanceof Student)
                .map(s -> ((Student) s).getStudentID())
                .mapToInt(id -> id)
                .max();
        return String.valueOf(maxId.isPresent() ? maxId.getAsInt() + 1 : 1);
    }

    /**
     * Handles the submission of the enrollment form, validating fields and
     * enrolling the student.
     */
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
                Date dateOfBirth = dateOfBirthPicker.getValue() != null
                        ? Date.valueOf(dateOfBirthPicker.getValue())
                        : null;

                // Get the master list from DataManager
                ObservableList<Guardian> guardianList = DataManager.getInstance()
                        .getCollectionsRegistry().getList("GUARDIAN");

                // Create new guardian using ProfileDataManager
                Guardian guardian = ProfileDataManager.createOrUpdateGuardian(
                        guardianFirstNameField.getText(),
                        guardianContactInfoField.getText(),
                        guardianFirstNameField,
                        guardianMiddleNameField,
                        guardianLastNameField,
                        guardianRelationshipField,
                        guardianContactInfoField,
                        null, // passing null to create new guardian
                        guardianList
                );

                // Enroll the student using the provided details
                Student student = EnrollmentUtils.enrollStudent(
                        nextStudentId,
                        firstNameField.getText(),
                        middleNameField.getText(),
                        lastNameField.getText(),
                        nameExtField.getText(),
                        emailField.getText(),
                        statusComboBox.getValue(),
                        contactNumberField.getText(),
                        dateOfBirth,
                        Double.parseDouble(fareField.getText()),
                        streetField.getText(),
                        barangayField.getText(),
                        cityField.getText(),
                        municipalityField.getText(),
                        postalCodeField.getText(),
                        guardian,
                        null,
                        currentSchoolYear);

                // Create student-guardian relationship
                StudentGuardian studentGuardian = new StudentGuardian(student, guardian);
                StudentGuardianDAO.insert(studentGuardian);

                // Update Collections Registry
                ObservableList<StudentGuardian> studentGuardians = DataManager.getInstance()
                        .getCollectionsRegistry().getList("STUDENT_GUARDIAN");
                if (studentGuardians != null) {
                    studentGuardians.add(studentGuardian);
                }

                handleClear();

                if (dialogStage != null) {
                    dialogStage.close();
                }

                showSuccessMessage("Student enrolled successfully!");

            } catch (Exception e) {
                showErrorMessage("Failed to enroll student: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Validates all input fields to ensure required fields are filled and
     * formats are correct.
     *
     * @return true if all validations pass, false otherwise.
     */
    private boolean validateFields() {
        boolean isValid = true;

        // Validate required fields
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
        if (ValidationUtils.isTextFieldEmpty(barangayField)) {
            ValidationUtils.setErrorStyle(barangayField);
            isValid = false;
        }

        // Validate optional fields with specific formats
        if (!ValidationUtils.isTextFieldEmpty(emailField) && !ValidationUtils.isValidEmail(emailField.getText())) {
            ValidationUtils.setErrorStyle(emailField);
            isValid = false;
        }
        if (!ValidationUtils.isTextFieldEmpty(contactNumberField)
                && !ValidationUtils.isValidPhoneNumber(contactNumberField.getText())) {
            ValidationUtils.setErrorStyle(contactNumberField);
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

        // Validate guardian fields
        if (ValidationUtils.isTextFieldEmpty(guardianFirstNameField)) {
            ValidationUtils.setErrorStyle(guardianFirstNameField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(guardianLastNameField)) {
            ValidationUtils.setErrorStyle(guardianLastNameField);
            isValid = false;
        }
        if (ValidationUtils.isTextFieldEmpty(guardianRelationshipField)) {
            ValidationUtils.setErrorStyle(guardianRelationshipField);
            isValid = false;
        }
        if (!ValidationUtils.isTextFieldEmpty(guardianContactInfoField)
                && !ValidationUtils.isValidPhoneNumber(guardianContactInfoField.getText())) {
            ValidationUtils.setErrorStyle(guardianContactInfoField);
            isValid = false;
        }

        // Validate date of birth
        if (dateOfBirthPicker.getValue() == null) {
            ValidationUtils.setErrorStyle(dateOfBirthPicker);
            isValid = false;
        }

        // Validate status
        if (statusComboBox.getValue() == null) {
            ValidationUtils.setErrorStyle(statusComboBox);
            isValid = false;
        }

        return isValid;
    }

    /**
     * Clears all input fields and resets their styles.
     */
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
        barangayField.clear();

        // Clear guardian fields
        guardianFirstNameField.clear();
        guardianMiddleNameField.clear();
        guardianLastNameField.clear();
        guardianRelationshipField.clear();
        guardianContactInfoField.clear();

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
        ValidationUtils.resetStyle(barangayField);

        // Reset guardian field styles
        ValidationUtils.resetStyle(guardianFirstNameField);
        ValidationUtils.resetStyle(guardianMiddleNameField);
        ValidationUtils.resetStyle(guardianLastNameField);
        ValidationUtils.resetStyle(guardianRelationshipField);
        ValidationUtils.resetStyle(guardianContactInfoField);
    }

    /**
     * Loads bindings for UI components. Currently empty as no bindings are
     * needed.
     */
    @Override
    protected void load_bindings() {
        System.out.println();
    }

    /**
     * Loads event listeners for UI interactions. Currently empty as listeners
     * are handled elsewhere.
     */
    @Override
    protected void load_listeners() {
        System.out.println();
    }

    /**
     * Updates the school year for the enrollment form.
     *
     * @param year The academic year to set (e.g., "2023-2024").
     */
    public void updateYear(String year) {
        initializeWithYear(year);
    }

    /**
     * Initializes the controller with the specified school year.
     *
     * @param year The academic year to initialize with (e.g., "2023-2024").
     */
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

    /**
     * Displays a success message to the user.
     *
     * @param message The message to display.
     */
    private void showSuccessMessage(String message) {
        DialogUtils.showSuccess(message);
    }

    /**
     * Displays an error message to the user.
     *
     * @param message The message to display.
     */
    private void showErrorMessage(String message) {
        DialogUtils.showError(message);
    }

    /**
     * Sets the dialog stage for this controller, used when opened as a dialog.
     *
     * @param dialogStage The stage to set.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Handles the cancel action, closing the dialog if in dialog mode.
     */
    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
