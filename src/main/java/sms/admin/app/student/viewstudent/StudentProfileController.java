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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sms.admin.app.student.viewstudent.studentform.StudentFormLoader;
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
    private Stage stage;
    private ModalPane modalPane;
    private Student student;
    private ObservableList<Address> addressMasterList;
    private ObservableList<StudentGuardian> studentGuardianMasterList;
    private ObservableList<Guardian> guardianMasterlist;
    private ModalPane forModal;
    private List<TextField> editableFields;
    private boolean isEditMode = false;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setModalPane(ModalPane modalPane) {
        this.modalPane = modalPane;
    }

    public void setStudent(Student student) {
        this.student = student;
        if (student != null) {
            loadStudentData();
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

    @FXML
    private ImageView profileImageView;
    @FXML
    private Button changePhotoButton;
    @FXML
    private Button editButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label studentNameLabel;
    @FXML
    private Label studentIdLabel;
    @FXML
    private Button backButton;
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
    @FXML
    private TextField streetAddressField;
    @FXML
    private TextField barangayField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField municipalityField;
    @FXML
    private TextField zipCodeField;
    @FXML
    private TextField emailField;

    @Override
    protected void load_fields() {
        // Initialize lists first
        addressMasterList = App.COLLECTIONS_REGISTRY.getList("ADDRESS");
        studentGuardianMasterList = App.COLLECTIONS_REGISTRY.getList("STUDENT_GUARDIAN");
        guardianMasterlist = App.COLLECTIONS_REGISTRY.getList("GUARDIAN");

        // Then initialize fields
        editableFields = List.of(
                firstNameField, middleNameField, lastNameField, nameExtField,
                streetAddressField, barangayField, cityField, municipalityField, zipCodeField,
                contactField, emailField,
                clusterField, clusterDetailsField,
                guardianNameField, guardianContactField);

        // Set initial button states
        backButton.setVisible(true);
        editButton.setVisible(true);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);
        changePhotoButton.setVisible(false);

        // Add scene key handler
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(this::handleKeyPress);
        }

        // Note: loadStudentData() will be called by setStudent()
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

    @Override
    protected void load_listeners() {
        editButton.setOnAction(event -> toggleEditMode());
        saveButton.setOnAction(event -> saveChanges());
        changePhotoButton.setOnAction(event -> handleChangePhoto());
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
        // Add any necessary bindings here
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

    private void initializeEdit() {
        if (forModal == null) {
            forModal = new ModalPane();
            forModal.setId("studentFormModal");
            forModal.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        }
        if (editButton.getScene() != null) {
            StudentFormLoader loader = (StudentFormLoader) FXLoaderFactory
                    .createInstance(StudentFormLoader.class,
                            getClass().getResource("/sms.admin/app/viewstudent/studentform/STUDENT_FORM.fxml"));
            loader.addParameter("MODAL", forModal);
            loader.addParameter("SCENE_ROOT", editButton.getScene().getRoot());
            loader.initialize();
            loader.load();
        }
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

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            if (isEditMode) {
                handleCancel();
            } else {
                closeDialog();
            }
            event.consume();
        }
    }
}
