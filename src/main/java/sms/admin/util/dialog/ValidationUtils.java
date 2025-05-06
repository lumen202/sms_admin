package sms.admin.util.dialog;

import javafx.scene.control.TextField;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;

/**
 * Utility class providing validation methods for common user input fields.
 * 
 * <p>
 * This class includes methods for validating email, phone number, postal code,
 * fare input, and general text field content. It also provides utilities to
 * visually
 * indicate validation errors using JavaFX control styling.
 * </p>
 */
public class ValidationUtils {
    // Error message constants
    public static final String EMAIL_ERROR = "Please enter a valid email address";
    public static final String PHONE_ERROR = "Phone number must be 11 digits";
    public static final String POSTAL_ERROR = "Postal code must be 4 digits";
    public static final String FARE_ERROR = "Please enter a valid fare amount";
    public static final String REQUIRED_FIELD = "This field is required";

    /**
     * Checks whether a given {@link TextField} is empty.
     *
     * @param textField the TextField to check
     * @return true if the text field is null or contains only whitespace, false
     *         otherwise
     */
    public static boolean isTextFieldEmpty(TextField textField) {
        return textField.getText() == null || textField.getText().trim().isEmpty();
    }

    /**
     * Validates whether a given email string is in a correct email format.
     *
     * @param email the email string to validate
     * @return true if the email matches a basic pattern, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Validates whether a given phone number is composed of exactly 11 digits.
     *
     * @param phone the phone number string to validate
     * @return true if the phone number contains exactly 11 digits, false otherwise
     */
    public static boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\d{11}");
    }

    /**
     * Validates whether a given postal code is a 4-digit number.
     *
     * @param postalCode the postal code string to validate
     * @return true if the postal code consists of 4 digits, false otherwise
     */
    public static boolean isValidPostalCode(String postalCode) {
        return postalCode.matches("\\d{4}");
    }

    /**
     * Validates whether a fare input is a valid non-negative number.
     *
     * @param fare the fare input string
     * @return true if the string can be parsed to a non-negative double, false
     *         otherwise
     */
    public static boolean isValidFare(String fare) {
        try {
            double value = Double.parseDouble(fare);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Applies an error style to a given JavaFX {@link Control} to visually indicate
     * a validation issue.
     *
     * @param control the control to style
     */
    public static void setErrorStyle(Control control, String message) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

        // Create or update tooltip
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-background-color: #FFE0E0; -fx-text-fill: #CC0000;");

        if (control instanceof TextField) {
            ((TextField) control).setTooltip(tooltip);
        } else {
            Tooltip.install(control, tooltip);
        }
    }

    /**
     * Resets the style of a given JavaFX {@link Control} to default.
     *
     * @param control the control to reset
     */
    public static void resetStyle(Control control) {
        control.setStyle("");

        // Remove tooltip
        if (control instanceof TextField) {
            ((TextField) control).setTooltip(null);
        } else {
            Tooltip.uninstall(control, control.getTooltip());
        }
    }

    // Helper method to validate and show error for required fields
    public static boolean validateRequired(TextField field) {
        if (isTextFieldEmpty(field)) {
            setErrorStyle(field, REQUIRED_FIELD);
            return false;
        }
        resetStyle(field);
        return true;
    }

    // Helper method to validate and show error for email
    public static boolean validateEmail(TextField field) {
        if (!field.getText().isEmpty() && !isValidEmail(field.getText())) {
            setErrorStyle(field, EMAIL_ERROR);
            return false;
        }
        resetStyle(field);
        return true;
    }

    // Helper method to validate and show error for phone
    public static boolean validatePhone(TextField field) {
        if (!field.getText().isEmpty() && !isValidPhoneNumber(field.getText())) {
            setErrorStyle(field, PHONE_ERROR);
            return false;
        }
        resetStyle(field);
        return true;
    }

    // Helper method to validate and show error for postal code
    public static boolean validatePostal(TextField field) {
        if (!field.getText().isEmpty() && !isValidPostalCode(field.getText())) {
            setErrorStyle(field, POSTAL_ERROR);
            return false;
        }
        resetStyle(field);
        return true;
    }

    // Helper method to validate and show error for fare
    public static boolean validateFare(TextField field) {
        if (!field.getText().isEmpty() && !isValidFare(field.getText())) {
            setErrorStyle(field, FARE_ERROR);
            return false;
        }
        resetStyle(field);
        return true;
    }
}
