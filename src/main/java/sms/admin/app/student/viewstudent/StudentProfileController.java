package sms.admin.app.student.viewstudent;

import dev.sol.core.application.FXController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import dev.finalproject.App;
import dev.finalproject.models.Address;
import dev.finalproject.models.Guardian;
import dev.finalproject.models.Student;
import dev.finalproject.models.StudentGuardian;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentProfileController extends FXController {

    // Core fields
    private Stage stage;
    private Student student;
    private boolean isEditMode = false;

    // Data lists
    private ObservableList<Address> addressMasterList;
    private ObservableList<StudentGuardian> studentGuardianMasterList;
    private ObservableList<Guardian> guardianMasterlist;
    private List<TextField> editableFields;

    // FXML injected fields - grouped by section
    @FXML
    private ImageView profileImageView;
    @FXML
    private Label studentNameLabel, studentIdLabel;

    // Buttons
    @FXML
    private Button changePhotoButton, editButton, saveButton, cancelButton, backButton;

    // Personal Info Fields
    @FXML
    private TextField firstNameField, middleNameField, lastNameField, nameExtField;

    // Contact Fields
    @FXML
    private TextField contactField, emailField;

    // Address Fields
    @FXML
    private TextField streetAddressField, barangayField, cityField,
            municipalityField, zipCodeField;

    // Academic Fields
    @FXML
    private TextField clusterField, clusterDetailsField;

    // Guardian Fields
    @FXML
    private TextField guardianNameField, guardianContactField;

    @Override
    protected void load_fields() {
        // Initialize master lists
        initializeMasterLists();

        // Initialize editable fields
        initializeEditableFields();

        // Set initial UI state
        initializeKeyHandler();
    }

    private void initializeMasterLists() {
        addressMasterList = App.COLLECTIONS_REGISTRY.getList("ADDRESS");
        studentGuardianMasterList = App.COLLECTIONS_REGISTRY.getList("STUDENT_GUARDIAN");
        guardianMasterlist = App.COLLECTIONS_REGISTRY.getList("GUARDIAN");
    }

    private void initializeEditableFields() {
        editableFields = List.of(
                firstNameField, middleNameField, lastNameField, nameExtField,
                streetAddressField, barangayField, cityField, municipalityField, zipCodeField,
                contactField, emailField, clusterField, clusterDetailsField,
                guardianNameField, guardianContactField);
    }

    private void initializeKeyHandler() {
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(this::handleKeyPress);
        }
    }

    @Override
    protected void load_listeners() {
        // Button actions
        editButton.setOnAction(e -> handleEdit());
        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> handleCancel());
        backButton.setOnAction(e -> handleBack());
        changePhotoButton.setOnAction(e -> handleChangePhoto());

        // Profile image hover effects
        profileImageView.setOnMouseEntered(e -> changePhotoButton.setVisible(isEditMode));
        profileImageView.setOnMouseExited(e -> changePhotoButton.setVisible(false));
    }

    // Public setters
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStudent(@SuppressWarnings("exports") Student student) {
        this.student = student;
        if (student != null) {
            loadStudentData();
        }
    }

    // Event handlers
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            if (isEditMode)
                handleCancel();
            else
                closeDialog();
            event.consume();
        }
    }

    @FXML
    private void closeDialog() {
        if (stage != null) {
            TranslateTransition slideUp = new TranslateTransition(Duration.millis(200), stage.getScene().getRoot());
            slideUp.setToY(-stage.getScene().getRoot().getBoundsInLocal().getHeight());
            slideUp.setOnFinished(e -> stage.close());
            slideUp.play();
        }
    }

    private void showNonEmptyFields() {
        // First, collect fields by section
        Map<VBox, List<TextField>> sectionFields = editableFields.stream()
                .collect(Collectors.groupingBy(field -> (VBox) field.getParent().getParent()));

        // Check each section
        sectionFields.forEach((section, fields) -> {
            // Check if any field in this section has content
            boolean hasContent = fields.stream()
                    .anyMatch(field -> field.getText() != null && !field.getText().trim().isEmpty());

            // Show/hide section and its header based on content or edit mode
            boolean shouldShowSection = hasContent || isEditMode;
            section.setVisible(shouldShowSection);
            section.setManaged(shouldShowSection);

            // Find and set header visibility (first Label with section-header style class)
            section.getChildren().stream()
                    .filter(node -> node instanceof Label &&
                            node.getStyleClass().contains("section-header"))
                    .findFirst()
                    .ifPresent(header -> {
                        header.setVisible(shouldShowSection);
                        header.setManaged(shouldShowSection);
                    });

            // Handle fields and their labels
            fields.forEach(field -> {
                boolean fieldHasValue = field.getText() != null && !field.getText().trim().isEmpty();
                boolean shouldShow = fieldHasValue || isEditMode;

                // Show/hide field
                field.setVisible(shouldShow);
                field.setManaged(shouldShow);

                // Show/hide corresponding label
                GridPane grid = (GridPane) field.getParent();
                int rowIndex = GridPane.getRowIndex(field) != null ? GridPane.getRowIndex(field) : 0;
                grid.getChildren().stream()
                        .filter(node -> node instanceof Label &&
                                GridPane.getRowIndex(node) != null &&
                                GridPane.getRowIndex(node) == rowIndex)
                        .forEach(label -> {
                            label.setVisible(shouldShow);
                            label.setManaged(shouldShow);
                        });
            });
        });
    }

    private void showAllFields() {
        editableFields.forEach(field -> {
            field.setVisible(true);
            field.setManaged(true);
            VBox section = (VBox) field.getParent().getParent();
            section.setVisible(true);
            section.setManaged(true);
            GridPane grid = (GridPane) field.getParent();
            int rowIndex = GridPane.getRowIndex(field) != null ? GridPane.getRowIndex(field) : 0;
            grid.getChildren().stream()
                    .filter(node -> node instanceof Label && GridPane.getRowIndex(node) != null &&
                            GridPane.getRowIndex(node) == rowIndex)
                    .forEach(label -> {
                        label.setVisible(true);
                        label.setManaged(true);
                    });
        });
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        editableFields.forEach(field -> field.setEditable(isEditMode));
        if (isEditMode) {
            editButton.setVisible(false);
            saveButton.setVisible(true);
            cancelButton.setVisible(true);
            changePhotoButton.setVisible(true);
            backButton.setVisible(false);
        } else {
            editButton.setVisible(true);
            saveButton.setVisible(false);
            cancelButton.setVisible(false);
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
        try {
            student.setFirstName(firstNameField.getText());
            student.setMiddleName(middleNameField.getText());
            student.setLastName(lastNameField.getText());
            student.setNameExtension(nameExtField.getText());
            student.setContact(contactField.getText());
            student.setEmail(emailField.getText());
            studentNameLabel.setText(firstNameField.getText() + " " + lastNameField.getText());
            editableFields.forEach(field -> field.setEditable(false));
            isEditMode = false;
            showNonEmptyFields();
            backButton.setVisible(true);
            editButton.setVisible(true);
            saveButton.setVisible(false);
            cancelButton.setVisible(false);
            changePhotoButton.setVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStudentData() {
        if (student != null) {
            // Set all fields initially visible
            editableFields.forEach(field -> {
                field.setVisible(true);
                field.setManaged(true);
            });

            // Load basic student info
            studentNameLabel.setText(student.getFirstName() + " " + student.getLastName());
            studentIdLabel.setText("ID: " + student.getStudentID());
            firstNameField.setText(student.getFirstName());
            middleNameField.setText(student.getMiddleName());
            lastNameField.setText(student.getLastName());
            nameExtField.setText(student.getNameExtension());
            contactField.setText(student.getContact());
            emailField.setText(student.getEmail());

            // Load address info using stream
            if (addressMasterList != null) {
                addressMasterList.stream()
                        .filter(addr -> addr.getStudentID().getStudentID() == student.getStudentID())
                        .findFirst()
                        .ifPresentOrElse(
                                // If address found
                                address -> {
                                    streetAddressField.setText(address.getStreet());
                                    barangayField.setText(address.getBarangay());
                                    cityField.setText(address.getCity());
                                    municipalityField.setText(address.getMunicipality());
                                    zipCodeField.setText(String.valueOf(address.getZipCode()));
                                },
                                // If no address found
                                () -> {
                                    streetAddressField.setText("");
                                    barangayField.setText("");
                                    cityField.setText("");
                                    municipalityField.setText("");
                                    zipCodeField.setText("");
                                });
            }

            // Load guardian info using streams
            if (studentGuardianMasterList != null && guardianMasterlist != null) {
                studentGuardianMasterList.stream()
                        .filter(sg -> sg.getStudentId().getStudentID() == student.getStudentID())
                        .findFirst()
                        .ifPresent(studentGuardian -> {
                            guardianMasterlist.stream()
                                    .filter(g -> g.getGuardianID() == studentGuardian.getGuardianId().getGuardianID())
                                    .findFirst()
                                    .ifPresent(guardian -> {
                                        guardianNameField.setText(guardian.getGuardianFullName());
                                        guardianContactField.setText(guardian.getContact());
                                    });
                        });
            } else {
                // Clear guardian fields if no data found
                guardianNameField.setText("");
                guardianContactField.setText("");
            }

            // Load cluster info if available
            if (student.getClusterID() != null) {
                clusterField.setText(student.getClusterID().getClusterName());
            }

            // Show/hide fields based on content
            showNonEmptyFields();
        }
    }

    @Override
    protected void load_bindings() {
        // No bindings needed currently
    }

    @FXML
    private void handleEditButton() {
        System.out.println("edit button is clicked");
    }

    @FXML
    private void handleCancel() {
        isEditMode = false;
        loadStudentData();
        editableFields.forEach(field -> field.setEditable(false));
        showNonEmptyFields();
        backButton.setVisible(true);
        editButton.setVisible(true);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);
        changePhotoButton.setVisible(false);
    }

    @FXML
    private void handleEdit() {
        isEditMode = true;
        editableFields.forEach(field -> {
            // Make field visible and editable
            field.setVisible(true);
            field.setManaged(true);
            field.setEditable(true);

            // Make parent section visible
            VBox section = (VBox) field.getParent().getParent();
            section.setVisible(true);
            section.setManaged(true);

            // Make section header visible
            section.getChildren().stream()
                    .filter(node -> node instanceof Label &&
                            node.getStyleClass().contains("section-header"))
                    .forEach(header -> {
                        header.setVisible(true);
                        header.setManaged(true);
                    });

            // Make field label visible
            GridPane grid = (GridPane) field.getParent();
            int rowIndex = GridPane.getRowIndex(field) != null ? GridPane.getRowIndex(field) : 0;
            grid.getChildren().stream()
                    .filter(node -> node instanceof Label &&
                            GridPane.getRowIndex(node) != null &&
                            GridPane.getRowIndex(node) == rowIndex)
                    .forEach(label -> {
                        label.setVisible(true);
                        label.setManaged(true);
                    });
        });

        // Update button visibility
        backButton.setVisible(false);
        editButton.setVisible(false);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);
        changePhotoButton.setVisible(true);
    }

    @FXML
    private void handleSave() {
        try {
            student.setFirstName(firstNameField.getText());
            student.setMiddleName(middleNameField.getText());
            student.setLastName(lastNameField.getText());
            student.setNameExtension(nameExtField.getText());
            student.setContact(contactField.getText());
            student.setEmail(emailField.getText());
            studentNameLabel.setText(firstNameField.getText() + " " + lastNameField.getText());
            editableFields.forEach(field -> field.setEditable(false));
            isEditMode = false;
            showNonEmptyFields();
            backButton.setVisible(true);
            editButton.setVisible(true);
            saveButton.setVisible(false);
            cancelButton.setVisible(false);
            changePhotoButton.setVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        closeDialog();
    }
}
