/**
 * Utility class for managing the visibility of profile-related input fields in the UI.
 * <p>
 * Provides methods to toggle the visibility and managed state of TextField controls,
 * their associated labels, and section headers within a JavaFX GridPane/VBox layout,
 * based on whether they contain text or a global visibility flag.
 * </p>
 */
package sms.admin.util.profile;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import java.util.List;

public class ProfileFieldManager {

    /**
     * Toggles visibility of a list of TextField controls and their containing
     * sections.
     * <p>
     * If {@code showAll} is {@code true}, all fields and their associated labels
     * and headers
     * are made visible. Otherwise, only fields with non-empty text are shown.
     * </p>
     *
     * @param fields  List of TextField instances to process (may be null)
     * @param showAll if {@code true}, display all fields regardless of content; if
     *                {@code false},
     *                display only non-empty fields
     */
    public static void toggleFieldsVisibility(List<TextField> fields, boolean showAll) {
        if (fields == null)
            return;

        fields.forEach(field -> {
            if (field == null)
                return;

            String fieldText = field.getText();
            // Assume layout hierarchy: VBox -> GridPane -> TextField
            VBox section = field.getParent() instanceof GridPane ? (VBox) field.getParent().getParent() : null;
            GridPane grid = field.getParent() instanceof GridPane ? (GridPane) field.getParent() : null;

            if (section == null || grid == null)
                return;

            int rowIndex = GridPane.getRowIndex(field) != null ? GridPane.getRowIndex(field) : 0;
            boolean shouldShow = showAll || (fieldText != null && !fieldText.trim().isEmpty());

            updateFieldVisibility(field, section, grid, rowIndex, shouldShow);
        });
    }

    /**
     * Updates visibility and managed states for a single field, its section,
     * header, and label.
     *
     * @param field      the TextField to show or hide
     * @param section    the VBox container wrapping the field group
     * @param grid       the GridPane that holds the field and label
     * @param rowIndex   the row index of the field in the grid
     * @param shouldShow {@code true} to make visible and managed; {@code false}
     *                   otherwise
     */
    private static void updateFieldVisibility(TextField field,
            VBox section,
            GridPane grid,
            int rowIndex,
            boolean shouldShow) {
        field.setVisible(shouldShow);
        field.setManaged(shouldShow);
        section.setVisible(shouldShow);
        section.setManaged(shouldShow);

        updateSectionHeaderVisibility(section, shouldShow);
        updateFieldLabelVisibility(grid, rowIndex, shouldShow);
    }

    /**
     * Toggles visibility of section header labels within a section VBox.
     * <p>
     * Section headers are identified by the style class "section-header".
     * </p>
     *
     * @param section    the VBox containing the header label and fields
     * @param shouldShow {@code true} to show headers; {@code false} to hide
     */
    private static void updateSectionHeaderVisibility(VBox section, boolean shouldShow) {
        section.getChildren().stream()
                .filter(node -> node instanceof Label
                        && node.getStyleClass().contains("section-header"))
                .forEach(header -> {
                    header.setVisible(shouldShow);
                    header.setManaged(shouldShow);
                });
    }

    /**
     * Toggles visibility of the label associated with a field in a GridPane row.
     *
     * @param grid       the GridPane containing field and label nodes
     * @param rowIndex   the row index of the field/label pair
     * @param shouldShow {@code true} to show labels; {@code false} to hide
     */
    private static void updateFieldLabelVisibility(GridPane grid,
            int rowIndex,
            boolean shouldShow) {
        grid.getChildren().stream()
                .filter(node -> node instanceof Label
                        && GridPane.getRowIndex(node) != null
                        && GridPane.getRowIndex(node) == rowIndex)
                .forEach(label -> {
                    label.setVisible(shouldShow);
                    label.setManaged(shouldShow);
                });
    }
}
