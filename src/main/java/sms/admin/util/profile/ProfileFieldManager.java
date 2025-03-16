package sms.admin.util.profile;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import java.util.List;

public class ProfileFieldManager {
    public static void toggleFieldsVisibility(List<TextField> fields, boolean showAll) {
        if (fields == null) return;
        
        fields.forEach(field -> {
            if (field == null) return;
            
            String fieldText = field.getText();
            VBox section = field.getParent() != null ? (VBox) field.getParent().getParent() : null;
            GridPane grid = field.getParent() != null ? (GridPane) field.getParent() : null;
            
            if (section == null || grid == null) return;
            
            int rowIndex = GridPane.getRowIndex(field) != null ? GridPane.getRowIndex(field) : 0;
            boolean shouldShow = showAll || (fieldText != null && !fieldText.trim().isEmpty());
            
            updateFieldVisibility(field, section, grid, rowIndex, shouldShow);
        });
    }

    private static void updateFieldVisibility(TextField field, VBox section, GridPane grid, int rowIndex, boolean shouldShow) {
        field.setVisible(shouldShow);
        field.setManaged(shouldShow);
        section.setVisible(shouldShow);
        section.setManaged(shouldShow);

        updateSectionHeaderVisibility(section, shouldShow);
        updateFieldLabelVisibility(grid, rowIndex, shouldShow);
    }

    private static void updateSectionHeaderVisibility(VBox section, boolean shouldShow) {
        if (section.getChildren() != null) {
            section.getChildren().stream()
                .filter(node -> node instanceof Label && node.getStyleClass() != null
                        && node.getStyleClass().contains("section-header"))
                .forEach(header -> {
                    header.setVisible(shouldShow);
                    header.setManaged(shouldShow);
                });
        }
    }

    private static void updateFieldLabelVisibility(GridPane grid, int rowIndex, boolean shouldShow) {
        if (grid.getChildren() != null) {
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
}
