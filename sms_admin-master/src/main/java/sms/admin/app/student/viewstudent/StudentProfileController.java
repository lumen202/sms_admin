package sms.admin.app.student.viewstudent;

import java.io.File;
import java.util.List;

import dev.finalproject.models.Address;
import dev.finalproject.models.Guardian;
import dev.finalproject.models.Student;
import dev.finalproject.models.StudentGuardian;
import dev.sol.core.application.FXController;
import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import sms.admin.util.mock.DataUtil;

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
    private Button changePhotoButton, backCancelButton, editSaveButton;

    // Personal Info Fields
    @FXML
    private TextField firstNameField, middleNameField, lastNameField, nameExtField;

    // Contact Fields
    @FXML
    private TextField contactField, emailField, fareField;

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
        initializeMasterLists();
        initializeEditableFields();
        initializeKeyHandler();

        // Initialize UI state
        isEditMode = false;
        updateUIForEditMode();
        changePhotoButton.setVisible(false);
    }

    private void initializeMasterLists() {
        // addressMasterList = App.COLLECTIONS_REGISTRY.getList("ADDRESS");
        // studentGuardianMasterList = App.COLLECTIONS_REGISTRY.getList("STUDENT_GUARDIAN");
        // guardianMasterlist = App.COLLECTIONS_REGISTRY.getList("GUARDIAN");

        addressMasterList = DataUtil.createAddressList();
        studentGuardianMasterList = DataUtil.createStudentGuardianList();
        guardianMasterlist = DataUtil.createGuardianList();
    }

    private void initializeEditableFields() {
        editableFields = List.of(
                firstNameField, middleNameField, lastNameField, nameExtField,
                streetAddressField, barangayField, cityField, municipalityField, zipCodeField,
                contactField, emailField, fareField, clusterField, clusterDetailsField,
                guardianNameField, guardianContactField);
    }

    private void initializeKeyHandler() {
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(this::handleKeyPress);
        }
    }

    @Override
    protected void load_listeners() {
        // Update button listeners
        editSaveButton.setOnAction(e -> handleEditOrSave());
        backCancelButton.setOnAction(e -> handleBackOrCancel());
        changePhotoButton.setOnAction(e -> handleChangePhoto());

        // Profile image hover effects remain the same
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
            if (isEditMode) {
                handleBackOrCancel(); 
            }else {
                closeDialog();
            }
            event.consume();
        }
    }

    @FXML
    private void closeDialog() {
        if (stage != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(200), stage.getScene().getRoot());
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> stage.close());
            fade.play();
        }
    }

    private void toggleFieldsVisibility(boolean showAll) {
        editableFields.forEach(field -> {
            VBox section = (VBox) field.getParent().getParent();
            GridPane grid = (GridPane) field.getParent();
            int rowIndex = GridPane.getRowIndex(field) != null ? GridPane.getRowIndex(field) : 0;

            boolean shouldShow = showAll || (!field.getText().trim().isEmpty() && !isEditMode);

            // Set visibility for field and its components
            field.setVisible(shouldShow);
            field.setManaged(shouldShow);
            section.setVisible(shouldShow);
            section.setManaged(shouldShow);

            // Handle section header
            section.getChildren().stream()
                    .filter(node -> node instanceof Label
                    && node.getStyleClass().contains("section-header"))
                    .forEach(header -> {
                        header.setVisible(shouldShow);
                        header.setManaged(shouldShow);
                    });

            // Handle field labels
            grid.getChildren().stream()
                    .filter(node -> node instanceof Label
                    && GridPane.getRowIndex(node) != null
                    && GridPane.getRowIndex(node) == rowIndex)
                    .forEach(label -> {
                        label.setVisible(shouldShow);
                        label.setManaged(shouldShow);
                    });
        });
    }

    private void updateUIForEditMode() {
        editableFields.forEach(field -> field.setEditable(isEditMode));
        changePhotoButton.setVisible(isEditMode);

        // Update button text based on mode
        if (isEditMode) {
            backCancelButton.setText("Cancel");
            editSaveButton.setText("Save Changes");
        } else {
            backCancelButton.setText("Back");
            editSaveButton.setText("Edit Profile");
        }

        toggleFieldsVisibility(isEditMode);
    }

    private void saveStudentChanges() {
        try {
            student.setFirstName(firstNameField.getText());
            student.setMiddleName(middleNameField.getText());
            student.setLastName(lastNameField.getText());
            student.setNameExtension(nameExtField.getText());
            student.setContact(contactField.getText());
            student.setEmail(emailField.getText());
            // Fix fare field handling
            String fareText = fareField.getText().trim();
            if (!fareText.isEmpty()) {
                student.setFare(Double.valueOf(fareText));
            } else {
                student.setFare(0.0);
            }
            studentNameLabel.setText(firstNameField.getText() + " " + lastNameField.getText());

            isEditMode = false;
            updateUIForEditMode();
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
            // Fix fare field handling
            Double fareValue = student.getFare();
            fareField.setText(fareValue != null ? fareValue.toString() : "");

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
            toggleFieldsVisibility(false);
        }
    }

    @Override
    protected void load_bindings() {
        // No bindings needed currently
    }

    @FXML
    private void handleBackOrCancel() {
        if (isEditMode) {
            isEditMode = false;
            loadStudentData();
            updateUIForEditMode();
        } else {
            closeDialog();
        }
    }

    @FXML
    private void handleEditOrSave() {
        if (isEditMode) {
            saveStudentChanges();
        } else {
            isEditMode = true;
            updateUIForEditMode();
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
}
