package sms.admin.util;

import javafx.scene.control.TextField;
import javafx.scene.control.Control;

public class ValidationUtils {

    public static boolean isTextFieldEmpty(TextField textField) {
        return textField.getText() == null || textField.getText().trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public static boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\d{11}");
    }

    public static boolean isValidPostalCode(String postalCode) {
        return postalCode.matches("\\d{4}");
    }

    public static boolean isValidFare(String fare) {
        try {
            double value = Double.parseDouble(fare);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void setErrorStyle(Control control) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
    }

    public static void resetStyle(Control control) {
        control.setStyle("");
    }
}
