package sms.admin.app.student.viewstudent;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.finalproject.App;
import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.data.StudentGuardianDAO;
import dev.finalproject.models.Address;
import dev.finalproject.models.Cluster;
import dev.finalproject.models.Guardian;
import dev.finalproject.models.Student;
import dev.finalproject.models.StudentGuardian;
import dev.sol.core.application.FXController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sms.admin.util.dialog.DialogManager;
import sms.admin.util.dialog.DialogUtils;
import sms.admin.util.profile.ProfileDataManager;
import sms.admin.util.profile.ProfilePhotoManager;

public class StudentProfileController extends FXController {

    // Core fields
    private Stage stage;
    private Student student;
    private boolean isEditMode = false;
    private static final String STUDENT_PHOTOS_DIR = "src/main/resources/sms/admin/assets/img/profile";
    private static final String DEFAULT_PHOTO_PATH = "/assets/img/default-profile.png";
    private static final String BACKUP_PHOTO_PATH = "/sms/admin/assets/img/default-profile.png";

    // Data lists
    private ObservableList<Address> addressMasterList;
    private ObservableList<StudentGuardian> studentGuardianMasterList;
    private ObservableList<Guardian> guardianMasterList;
    private ObservableList<Cluster> clusterMasterList;
    private List<TextField> editableFields;

    // FXML injected fields
    @FXML
    private ImageView profileImageView;
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
    private TextField streetAddressField, barangayField, cityField, municipalityField, zipCodeField;
    // Academic Fields
    @FXML
    private TextField clusterField, clusterDetailsField;
    // Guardian Fields
    @FXML
    private TextField guardianNameField, guardianContactField;
    @FXML
    private TextField guardianFirstNameField, guardianMiddleNameField, guardianLastNameField,
            guardianRelationshipField, guardianContactInfoField;
    @FXML
    private GridPane guardianViewGrid, guardianEditGrid;

    @Override
    protected void load_fields() {
        initializeMasterLists();
        initializeEditableFields();
        initializeKeyHandler();
        
        // Check if edit mode parameter is set
        Boolean editMode = (Boolean) getParameter("EDIT_MODE");
        if (Boolean.TRUE.equals(editMode)) {
            isEditMode = true;
        }
        
        resetUIState();
        
        // Update UI for edit mode if needed
        if (isEditMode) {
            prepareGuardianEditFields();
            updateUIForEditMode();
        }
    }

    @Override
    protected void load_listeners() {
        editSaveButton.setOnAction(e -> handleEditOrSave());
        backCancelButton.setOnAction(e -> handleBackOrCancel());
        changePhotoButton.setOnAction(e -> handleChangePhoto());
    }

    @Override
    protected void load_bindings() {
        // No bindings needed currently
    }

    @FXML
    public void initialize() {
        // Called after FXML injection; ensure initial state
        initializeEditableFields();
        changePhotoButton.setVisible(false);
        changePhotoButton.setManaged(false);
        ProfilePhotoManager.loadPhoto(profileImageView, -1); // Load default photo when no student is set
    }

    // Setters
    public void setStage(Stage stage) {
        this.stage = stage;
        initializeKeyHandler(); // Reinitialize key handler when stage is set
    }

    public void setStudent(Student student) {
        this.student = student;
        if (student != null && editableFields != null) {
            loadStudentData();
        }
    }

    // Initialization Helpers
    private void resetUIState() {
        if (!isEditMode) {  // Only reset edit mode if not explicitly set
            isEditMode = false;
        }
        updateUIForEditMode();
        changePhotoButton.setVisible(isEditMode);  // Show change photo button in edit mode
    }

    private void initializeMasterLists() {
        try {
            addressMasterList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
            studentGuardianMasterList = FXCollections.observableArrayList(StudentGuardianDAO.getStudentGuardianList());
            guardianMasterList = App.COLLECTIONS_REGISTRY.getList("GUARDIAN");
            clusterMasterList = App.COLLECTIONS_REGISTRY.getList("CLUSTER");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog("Database Error", "Failed to load data",
                    "An error occurred while loading data from the database.");
        }
    }

    private void initializeEditableFields() {
        // Ensure FXML fields are injected before proceeding
        if (firstNameField == null) {
            return;
        }
        editableFields = List.of(
                firstNameField, middleNameField, lastNameField, nameExtField,
                streetAddressField, barangayField, cityField, municipalityField, zipCodeField,
                contactField, emailField, fareField, clusterField, clusterDetailsField
        );
    }

    private void initializeKeyHandler() {
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(this::handleKeyPress);
        }
    }

    // Event Handlers
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            if (isEditMode) {
                handleBackOrCancel();
            } else {
                closeDialog();
            }
            event.consume();
        }
    }

    @FXML
    private void closeDialog() {
        DialogManager.closeWithFade(stage, null);
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
            prepareGuardianEditFields();
            updateUIForEditMode();
        }
    }

    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (selectedFile != null) {
            String savedPath = ProfilePhotoManager.savePhoto(selectedFile, student.getStudentID());
            if (savedPath != null) {
                loadStudentPhoto();
            }
        }
    }

    // UI Update Helpers
    private void updateUIForEditMode() {
        // Handle field editability and visibility
        editableFields.forEach(field -> {
            if (field != null) {
                field.setEditable(isEditMode);
                boolean hasValue = field.getText() != null && !field.getText().trim().isEmpty();
                boolean shouldShow = isEditMode || hasValue;
                field.setVisible(shouldShow);
                field.setManaged(shouldShow);

                // Find and set visibility of corresponding label
                Label label = findFieldLabel(field);
                if (label != null) {
                    label.setVisible(shouldShow);
                    label.setManaged(shouldShow);
                }
            }
        });

        // Toggle guardian grids
        toggleGuardianGrids();

        // Update buttons
        changePhotoButton.setVisible(isEditMode);
        changePhotoButton.setManaged(isEditMode);
        backCancelButton.setText(isEditMode ? "Cancel" : "Back");
        editSaveButton.setText(isEditMode ? "Save Changes" : "Edit Profile");
    }

    private Label findFieldLabel(TextField field) {
        if (field.getParent() instanceof GridPane) {
            GridPane grid = (GridPane) field.getParent();
            final Integer fieldRow = GridPane.getRowIndex(field);
            final int rowIndex = fieldRow != null ? fieldRow : 0;

            // Find label in the same row
            return grid.getChildren().stream()
                    .filter(node -> node instanceof Label
                    && Objects.equals(GridPane.getRowIndex(node), rowIndex))
                    .map(node -> (Label) node)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void toggleGuardianGrids() {
        if (guardianViewGrid != null && guardianEditGrid != null) {
            guardianViewGrid.setVisible(!isEditMode);
            guardianViewGrid.setManaged(!isEditMode);
            guardianEditGrid.setVisible(isEditMode);
            guardianEditGrid.setManaged(isEditMode);
        }
    }

    // Data Loading Methods
    private void loadStudentData() {
        if (student == null || editableFields == null) {
            return;
        }
        clearAllFields();
        loadBasicStudentInfo();
        loadAddressInfo();
        loadGuardianInfo();
        loadClusterInfo();
        loadStudentPhoto();
        updateUIForEditMode();
    }

    private void clearAllFields() {
        editableFields.forEach(field -> {
            if (field != null) {
                field.clear();
                field.setVisible(false);
                field.setManaged(false);

                // Also hide the corresponding label
                Label label = findFieldLabel(field);
                if (label != null) {
                    label.setVisible(false);
                    label.setManaged(false);
                }
            }
        });
    }

    private void loadBasicStudentInfo() {
        firstNameField.setText(student.getFirstName());
        middleNameField.setText(student.getMiddleName());
        lastNameField.setText(student.getLastName());
        nameExtField.setText(student.getNameExtension());
        contactField.setText(student.getContact());
        emailField.setText(student.getEmail());
        fareField.setText(String.valueOf(student.getFare()));
    }

    private void loadAddressInfo() {
        // Reload address list if needed
        if (addressMasterList == null || addressMasterList.isEmpty()) {
            AddressDAO.initialize(App.COLLECTIONS_REGISTRY.getList("STUDENT"));
            addressMasterList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
        }
        Optional<Address> studentAddress = addressMasterList.stream()
                .filter(addr -> addr.getStudentID() != null
                && addr.getStudentID().getStudentID() == student.getStudentID())
                .findFirst();

        studentAddress.ifPresentOrElse(addr -> {
            streetAddressField.setText(addr.getStreet());
            barangayField.setText(addr.getBarangay());
            cityField.setText(addr.getCity());
            municipalityField.setText(addr.getMunicipality());
            zipCodeField.setText(String.valueOf(addr.getZipCode()));
        }, () -> {
            streetAddressField.clear();
            barangayField.clear();
            cityField.clear();
            municipalityField.clear();
            zipCodeField.clear();
        });
    }

    private void loadGuardianInfo() {
        if (studentGuardianMasterList == null || guardianMasterList == null) {
            clearGuardianFields();
            return;
        }
        studentGuardianMasterList.stream()
                .filter(sg -> sg.getStudentId().getStudentID() == student.getStudentID())
                .findFirst()
                .ifPresent(sg -> guardianMasterList.stream()
                .filter(g -> g.getGuardianID() == sg.getGuardianId().getGuardianID())
                .findFirst()
                .ifPresent(guardian -> {
                    guardianNameField.setText(guardian.getGuardianFullName());
                    guardianContactField.setText(guardian.getContact());
                })
                );
    }

    private void clearGuardianFields() {
        guardianNameField.clear();
        guardianContactField.clear();
    }

    private void loadClusterInfo() {
        if (student.getClusterID() != null) {
            clusterField.setText(student.getClusterID().getClusterName());
        } else {
            clusterField.clear();
        }
    }

    private void loadStudentPhoto() {
        ProfilePhotoManager.loadPhoto(profileImageView, student.getStudentID());
    }

    // Save Changes
    private void saveStudentChanges() {
        try {
            // Update basic student info
            ProfileDataManager.updateBasicStudentInfo(student,
                    firstNameField, middleNameField, lastNameField, nameExtField,
                    contactField, emailField, fareField);

            // Update address
            handleAddressUpdate();

            // Update guardian
            Guardian updatedGuardian = handleGuardianUpdate();

            // Update cluster
            handleClusterUpdate();

            // Update student in database
            StudentDAO.update(student);

            isEditMode = false;
            updateUIForEditMode();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private void handleAddressUpdate() {
        Address studentAddress = findOrCreateStudentAddress();
        ProfileDataManager.updateAddressInfo(studentAddress, student,
                streetAddressField, barangayField, cityField, municipalityField, zipCodeField);
        if (studentAddress.getAddressID() == 0) {
            AddressDAO.insert(studentAddress);
            addressMasterList.add(studentAddress);
        } else {
            AddressDAO.update(studentAddress);
        }
    }

    private Guardian handleGuardianUpdate() {
        Guardian currentGuardian = findCurrentGuardian();
        Guardian updatedGuardian = ProfileDataManager.createOrUpdateGuardian(
                guardianNameField.getText(),
                guardianContactField.getText(),
                guardianFirstNameField,
                guardianMiddleNameField,
                guardianLastNameField,
                guardianRelationshipField,
                guardianContactInfoField,
                currentGuardian,
                guardianMasterList
        );

        updateStudentGuardianRelationship(updatedGuardian);
        return updatedGuardian;
    }

    private void handleClusterUpdate() {
        ProfileDataManager.handleClusterUpdate(
                clusterField.getText(),
                student.getClusterID() != null ? String.valueOf(student.getClusterID().getClusterID()) : "",
                clusterMasterList
        ).ifPresent(student::setClusterID);
    }

    // Helper Methods for Data Retrieval
    private Address findOrCreateStudentAddress() {
        if (addressMasterList == null) {
            return new Address(student, 0, "", "", "", "", 0);
        }
        return addressMasterList.stream()
                .filter(addr -> addr.getStudentID().getStudentID() == student.getStudentID())
                .findFirst()
                .orElse(new Address(student, 0, "", "", "", "", 0));
    }

    private Guardian findCurrentGuardian() {
        if (studentGuardianMasterList == null || guardianMasterList == null) {
            return null;
        }
        return studentGuardianMasterList.stream()
                .filter(sg -> sg.getStudentId().getStudentID() == student.getStudentID())
                .findFirst()
                .flatMap(sg -> guardianMasterList.stream()
                .filter(g -> g.getGuardianID() == sg.getGuardianId().getGuardianID())
                .findFirst())
                .orElse(null);
    }

    private void updateStudentGuardianRelationship(Guardian guardian) {
        boolean relationshipExists = studentGuardianMasterList.stream()
                .anyMatch(sg -> sg.getStudentId().getStudentID() == student.getStudentID());
        if (!relationshipExists) {
            StudentGuardian newRelation = new StudentGuardian(student, guardian);
            StudentGuardianDAO.insert(newRelation);
            studentGuardianMasterList.add(newRelation);
        }
    }

    private void prepareGuardianEditFields() {
        if (!guardianNameField.getText().isEmpty()) {
            String[] nameParts = guardianNameField.getText().split(" ");
            if (nameParts.length >= 2) {
                guardianFirstNameField.setText(nameParts[0]);
                guardianLastNameField.setText(nameParts[nameParts.length - 1]);
                if (nameParts.length > 2) {
                    guardianMiddleNameField.setText(nameParts[1]);
                }
            }
            guardianContactInfoField.setText(guardianContactField.getText());
        }
    }
}
