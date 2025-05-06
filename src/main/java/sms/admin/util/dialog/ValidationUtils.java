package sms.admin.util.dialog;

import javafx.geometry.Point2D;
import javafx.scene.control.TextField;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import javafx.stage.Window;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing validation methods for common user input fields.
 * 
 * <p>
 * This class includes methods for validating email, phone number, postal code,
 * fare input, and general text field content. It also provides utilities to
 * visually indicate validation errors using JavaFX control styling.
 * </p>
 */
public class ValidationUtils {
    // Error message constants
    public static final String EMAIL_ERROR = "Please enter a valid email address";
    public static final String PHONE_ERROR = "Phone number must be 11 digits";
    public static final String POSTAL_ERROR = "Postal code must be 4 digits";
    public static final String FARE_ERROR = "Please enter a valid fare amount";
    public static final String REQUIRED_FIELD = "This field is required";

    // Add static map to track tooltips
    private static final Map<Node, Tooltip> activeTooltips = new HashMap<>();

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

        // Remove from tracking and hide tooltip
        if (activeTooltips.containsKey(control)) {
            activeTooltips.get(control).hide();
            activeTooltips.remove(control);
        }

        if (control instanceof TextField) {
            ((TextField) control).setTooltip(null);
        } else {
            Tooltip.uninstall(control, control.getTooltip());
        }
    }

    /**
     * Applies an error style and immediately shows a tooltip for a given JavaFX
     * {@link Node}.
     *
     * @param node    the node to style
     * @param message the error message to display
     */
    public static void setErrorStyleAndShowTooltip(Node node, String message) {
        node.setStyle("-fx-border-color: red;");
        
        // Remove any existing tooltip
        if (activeTooltips.containsKey(node)) {
            activeTooltips.get(node).hide();
            activeTooltips.remove(node);
        }

        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-background-color: #FFE0E0; -fx-text-fill: #CC0000;");

        // Set tooltip and track it
        if (node instanceof TextField) {
            ((TextField) node).setTooltip(tooltip);
        } else if (node instanceof ComboBox) {
            ((ComboBox<?>) node).setTooltip(tooltip);
        } else if (node instanceof DatePicker) {
            ((DatePicker) node).setTooltip(tooltip);
        }
        
        activeTooltips.put(node, tooltip);
        
        // Add window focus listener
        if (node.getScene() != null && node.getScene().getWindow() != null) {
            node.getScene().getWindow().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    hideAllTooltips();
                }
            });
        }
        
        // Update tooltip position
        updateTooltipPosition(node, tooltip);
    }

    public static void hideAllTooltips() {
        activeTooltips.forEach((node, tooltip) -> tooltip.hide());
    }

    private static void updateTooltipPosition(Node node, Tooltip tooltip) {
        if (node.getScene() != null && node.getScene().getWindow() != null) {
            // Find the ScrollPane ancestor
            ScrollPane scrollPane = findScrollPaneAncestor(node);
            if (scrollPane == null) return;

            Point2D nodePoint = node.localToScreen(node.getBoundsInLocal().getMaxX(), 
                                                 node.getBoundsInLocal().getMinY());
            
            if (nodePoint != null) {
                // Get coordinates relative to the ScrollPane's viewport
                Point2D scrollPoint = node.localToScreen(0, 0);
                Point2D viewportPoint = scrollPane.localToScreen(0, 0);
                
                double relativeY = scrollPoint.getY() - viewportPoint.getY();
                
                // Check if node is within viewport bounds
                if (relativeY < 0 || relativeY > scrollPane.getViewportBounds().getHeight()) {
                    tooltip.hide();
                } else {
                    double xOffset = 5;
                    double yOffset = -5;
                    tooltip.show(node.getScene().getWindow(), 
                               nodePoint.getX() + xOffset, 
                               nodePoint.getY() + yOffset);
                }
            }
        }
    }

    private static ScrollPane findScrollPaneAncestor(Node node) {
        Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof ScrollPane) {
                return (ScrollPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Updates the positions of all active tooltips.
     * Called when the scroll position changes to keep tooltips aligned with their fields.
     */
    public static void updateAllTooltipPositions() {
        for (Map.Entry<Node, Tooltip> entry : activeTooltips.entrySet()) {
            Node node = entry.getKey();
            Tooltip tooltip = entry.getValue();
            updateTooltipPosition(node, tooltip);
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

    public static boolean validateRequiredWithTooltip(TextField field) {
        if (field.getText().trim().isEmpty()) {
            setErrorStyleAndShowTooltip(field, "This field is required");
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

    public static boolean validateEmailWithTooltip(TextField field) {
        if (!field.getText().trim().isEmpty() && !field.getText().matches("[^@]+@[^@]+\\.[^@]+")) {
            setErrorStyleAndShowTooltip(field, "Invalid email format");
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

    public static boolean validatePhoneWithTooltip(TextField field) {
        if (!field.getText().trim().isEmpty() && !field.getText().matches("\\d{11}")) {
            setErrorStyleAndShowTooltip(field, "Phone number must be 11 digits");
            return false;
        }
        resetStyle(field);
        return true;
    }

    // Helper method to validate and show error for postal code
    public static boolean validatePostal(TextField field) {
        if (!field.getText().trim().isEmpty() && !isValidPostalCode(field.getText())) {
            setErrorStyleAndShowTooltip(field, POSTAL_ERROR);
            return false;
        }
        resetStyle(field);
        return true;
    }

    public static boolean validatePostalWithTooltip(TextField field) {
        if (!field.getText().trim().isEmpty() && !field.getText().matches("\\d{4}")) {
            setErrorStyleAndShowTooltip(field, "Postal code must be 4 digits");
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

    public static boolean validateFareWithTooltip(TextField field) {
        if (!field.getText().trim().isEmpty() && !isValidFare(field.getText())) {
            setErrorStyleAndShowTooltip(field, "Please enter a valid fare amount");
            return false;
        }
        resetStyle(field);
        return true;
    }

    public static boolean validateDateOfBirthWithTooltip(DatePicker datePicker) {
        if (datePicker.getValue() == null) {
            setErrorStyleAndShowTooltip(datePicker, "Date of birth is required");
            return false;
        }
        resetStyle(datePicker);
        return true;
    }
}
