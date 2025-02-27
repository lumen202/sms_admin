package sms.admin.app.student.viewstudent;

import atlantafx.base.controls.ModalPane;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import sms.admin.app.student.viewstudent.studentform.StudentFormLoader;

import java.io.File;
import java.util.List;

public class StudentProfileController extends FXController {

    @FXML
    private ImageView profileImageView;
    @FXML
    private Button changePhotoButton;
    @FXML
    private Button editButton;
    @FXML
    private Button saveButton;
    @FXML
    private Label studentNameLabel;
    @FXML
    private Label studentIdLabel;

    // Text fields
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField middleNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField nameExtField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField contactField;
    @FXML
    private TextField clusterField;
    @FXML
    private TextField clusterDetailsField;
    @FXML
    private TextField guardianNameField;
    @FXML
    private TextField guardianContactField;

    // Add this field declaration
    private ModalPane forModal;
    private List<TextField> editableFields;
    private boolean isEditMode = false;

    @Override
    protected void load_fields() {
        editableFields = List.of(
                firstNameField, middleNameField, lastNameField, nameExtField,
                addressField, contactField, clusterField, clusterDetailsField,
                guardianNameField, guardianContactField);

        // Initialize with dummy data (replace with actual data loading)
        loadStudentData();
    }

    @Override
    protected void load_listeners() {
        editButton.setOnAction(event -> toggleEditMode());
        saveButton.setOnAction(event -> saveChanges());
        changePhotoButton.setOnAction(event -> handleChangePhoto());

        // Show change photo button on hover
        profileImageView.setOnMouseEntered(e -> changePhotoButton.setVisible(true));
        profileImageView.setOnMouseExited(e -> {
            if (!isEditMode) {
                changePhotoButton.setVisible(false);
            }
        });
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        editableFields.forEach(field -> field.setEditable(isEditMode));

        if (isEditMode) {
            editButton.setVisible(false);
            saveButton.setVisible(true);
            changePhotoButton.setVisible(true);
        } else {
            editButton.setVisible(true);
            saveButton.setVisible(false);
            changePhotoButton.setVisible(false);
        }
    }

    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            profileImageView.setImage(image);
        }
    }

    private void saveChanges() {
        // TODO: Implement save logic
        toggleEditMode();
        // Update student name label
        studentNameLabel.setText(firstNameField.getText() + " " + lastNameField.getText());
    }

    private void loadStudentData() {
        // TODO: Replace with actual data loading
        studentNameLabel.setText("John Doe");
        studentIdLabel.setText("ID: 2024-0001");
        firstNameField.setText("John");
        lastNameField.setText("Doe");
        // ... set other fields
    }

    @Override
    protected void load_bindings() {
        // Add any necessary bindings here
    }

    @FXML
    private void handleEditButton() {
        System.out.println("edit button is clicked");
        initializeEdit();
    }

    private void initializeEdit() {
        if (forModal == null) {
            forModal = new ModalPane();
            forModal.setId("studentFormModal");
            forModal.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        }

        // Get the current scene root
        if (editButton.getScene() != null) {
            // Create a new loader instance
            StudentFormLoader loader = (StudentFormLoader) FXLoaderFactory
                    .createInstance(StudentFormLoader.class,
                            getClass().getResource(
                                    "/sms.admin/app/viewstudent/studentform/STUDENT_FORM.fxml"));

            loader.addParameter("MODAL", forModal);
            loader.addParameter("SCENE_ROOT", editButton.getScene().getRoot());
            loader.initialize();
            loader.load();
        }
    }

}
