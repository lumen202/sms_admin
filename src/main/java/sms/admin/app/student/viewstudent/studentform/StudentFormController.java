package sms.admin.app.student.viewstudent.studentform;

import dev.sol.core.application.FXController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class StudentFormController extends FXController {

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @Override
    protected void load_bindings() {
        initializeButtons();
        System.out.println("Studentorm isb called");
    }

    @Override
    protected void load_fields() {
        // Initialize form fields here
    }

    @Override
    protected void load_listeners() {
        setupButtonListeners();
    }

    private void initializeButtons() {
        if (saveButton != null) {
            saveButton.getStyleClass().add("button-primary");
        }
    }

    private void setupButtonListeners() {
        if (saveButton != null) {
            saveButton.setOnAction(e -> handleSave());
        }
        if (cancelButton != null) {
            cancelButton.setOnAction(e -> handleCancel());
        }
    }

    @FXML
    private void handleSave() {
        // Add save logic here
        closeModal();
    }

    @FXML
    private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        if (saveButton != null && saveButton.getScene() != null) {
            ((Stage) saveButton.getScene().getWindow()).close();
        }
    }
}
