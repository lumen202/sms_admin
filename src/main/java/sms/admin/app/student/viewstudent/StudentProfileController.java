package sms.admin.app.student.viewstudent;

import java.io.File;
import java.util.List;

import dev.finalproject.App;
import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.ClusterDAO;
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
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sms.admin.util.dialog.DialogManager;
import sms.admin.util.dialog.DialogUtils;
import sms.admin.util.profile.ProfilePhotoManager;
import sms.admin.util.profile.ProfileFieldManager;
import sms.admin.util.profile.ProfileDataManager;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

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
    // FXML injected fields - grouped by section
    @FXML
    private ImageView profileImageView;
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
        initializeEditableFields(); // Make sure fields are injected
        initializeKeyHandler();
        resetUIState();
    }

    private void resetUIState() {
        isEditMode = false;
        updateUIForEditMode();
        changePhotoButton.setVisible(false);
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
        if (firstNameField == null) {
            return; // FXML fields not injected

                }editableFields = List.of(
                firstNameField, middleNameField, lastNameField, nameExtField,
                streetAddressField, barangayField, cityField, municipalityField, zipCodeField,
                contactField, emailField, fareField, clusterField, clusterDetailsField,
                guardianNameField, guardianContactField, guardianFirstNameField, guardianMiddleNameField, 
                guardianLastNameField, guardianRelationshipField, guardianContactInfoField);
    }

    private void initializeKeyHandler() {
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(this::handleKeyPress);
        }
    }

    @Override
    protected void load_listeners() {
        editSaveButton.setOnAction(e -> handleEditOrSave());
        backCancelButton.setOnAction(e -> handleBackOrCancel());
        changePhotoButton.setOnAction(e -> handleChangePhoto());
    }

    // Public setters
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStudent(Student student) {
        this.student = student;
        if (student != null && editableFields != null) {
            loadStudentData();
        }
    }

    // Event handlers
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

    /**
     * Adjust visibility of fields. For the cluster field, it is shown if: - The
     * student’s cluster exists, OR - The cluster field text is non-empty, OR - 
     * We are in edit mode (showAll is true)
     */
    private void toggleFieldsVisibility(boolean showAll) {
        if (editableFields == null) return;
        
        // Handle guardian grids visibility
        if (guardianViewGrid != null && guardianEditGrid != null) {
            guardianViewGrid.setVisible(!isEditMode);
            guardianViewGrid.setManaged(!isEditMode);
            guardianEditGrid.setVisible(isEditMode);
            guardianEditGrid.setManaged(isEditMode);
        }

        // Process all fields
        editableFields.forEach(field -> {
            if (field == null) return;
            
            String fieldText = field.getText();
            boolean hasValue = fieldText != null && !fieldText.trim().isEmpty();
            boolean shouldShow = showAll || (hasValue && !isEditMode);
            
            // Special handling for fields that should always be visible
            if (isAlwaysVisibleField(field)) {
                shouldShow = true;
            }
            
            field.setVisible(shouldShow);
            field.setManaged(shouldShow);
            
            // Handle parent container and label
            Node fieldContainer = field.getParent();
            if (fieldContainer != null && fieldContainer instanceof GridPane) {
                Node label = getFieldLabel(field);
                if (label != null) {
                    label.setVisible(shouldShow);
                    label.setManaged(shouldShow);
                }
            }
        });
    }

    private boolean isAlwaysVisibleField(TextField field) {
        return field == clusterField || 
               field == firstNameField || 
               field == lastNameField;
    }

    private Node getFieldLabel(TextField field) {
        if (field == null || field.getParent() == null || !(field.getParent() instanceof GridPane)) {
            return null;
        }
        
        GridPane grid = (GridPane) field.getParent();
        Integer colIndex = GridPane.getColumnIndex(field);
        Integer rowIndex = GridPane.getRowIndex(field);
        
        // Default to column 1 if not set
        colIndex = (colIndex != null) ? colIndex : 1;
        // Default to row 0 if not set
        rowIndex = (rowIndex != null) ? rowIndex : 0;
        
        // Find the label in the same row
        for (Node node : grid.getChildren()) {
            if (node instanceof Label) {
                Integer nodeCol = GridPane.getColumnIndex(node);
                Integer nodeRow = GridPane.getRowIndex(node);
                
                // Default to column 0 for label if not set
                nodeCol = (nodeCol != null) ? nodeCol : 0;
                // Default to row 0 if not set
                nodeRow = (nodeRow != null) ? nodeRow : 0;
                
                if (nodeRow.equals(rowIndex) && nodeCol == 0) {
                    return node;
                }
            }
        }
        return null;
    }

    private void updateUIForEditMode() {
        editableFields.forEach(field -> {
            field.setEditable(isEditMode);
            // Force cluster field to be visible in either mode
            if (field == clusterField) {
                field.setVisible(true);
                field.setManaged(true);
            }
        });

        changePhotoButton.setVisible(isEditMode);
        changePhotoButton.setManaged(isEditMode);
        changePhotoButton.setDisable(!isEditMode);
        backCancelButton.setText(isEditMode ? "Cancel" : "Back");
        editSaveButton.setText(isEditMode ? "Save Changes" : "Edit Profile");

        toggleFieldsVisibility(isEditMode);
    }

    private void saveStudentChanges() {
        try {
            // Update basic student info
            ProfileDataManager.updateBasicStudentInfo(student, 
                firstNameField, middleNameField, lastNameField, nameExtField,
                contactField, emailField, fareField);

            // Update address info
            Address studentAddress = findOrCreateStudentAddress();
            ProfileDataManager.updateAddressInfo(studentAddress, student,
                streetAddressField, barangayField, cityField,
                municipalityField, zipCodeField);

            // Save address changes
            if (studentAddress.getAddressID() == 0) {
                AddressDAO.insert(studentAddress);
                addressMasterList.add(studentAddress);
            } else {
                AddressDAO.update(studentAddress);
            }

            // Update cluster
            ProfileDataManager.handleClusterUpdate(clusterField.getText(), 
                student.getClusterID() != null ? String.valueOf(student.getClusterID().getClusterID()) : "",
                clusterMasterList)
                .ifPresent(cluster -> student.setClusterID(cluster));

            // Update student in database
            StudentDAO.update(student);

            isEditMode = false;
            updateUIForEditMode();
            DialogUtils.showSuccessDialog("Success", "Profile Updated", 
                "Student profile has been updated successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog("Save Error", "Failed to save changes",
                "An error occurred while saving changes to the database.");
        }
    }

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
        toggleFieldsVisibility(false);
    }

    private void clearAllFields() {
        editableFields.forEach(field -> {
            if (field != null) {
                field.setText("");
                field.setVisible(false);
                field.setManaged(false);
            }
        });
    }

    private void loadBasicStudentInfo() {
        // Load basic student info
        firstNameField.setText(student.getFirstName());
        middleNameField.setText(student.getMiddleName());
        lastNameField.setText(student.getLastName());
        nameExtField.setText(student.getNameExtension());
        contactField.setText(student.getContact());
        emailField.setText(student.getEmail());
        Double fareValue = student.getFare();
        fareField.setText(fareValue != null ? fareValue.toString() : "");
    }

    private void loadAddressInfo() {
        if (addressMasterList == null || addressMasterList.isEmpty()) {
            AddressDAO.initialize(App.COLLECTIONS_REGISTRY.getList("STUDENT"));
            addressMasterList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
        }

        Address studentAddress = addressMasterList.stream()
                .filter(addr -> addr.getStudentID() != null
                && addr.getStudentID().getStudentID() == student.getStudentID())
                .findFirst().orElse(null);
        if (studentAddress != null) {
            streetAddressField.setText(studentAddress.getStreet());
            barangayField.setText(studentAddress.getBarangay());
            cityField.setText(studentAddress.getCity());
            municipalityField.setText(studentAddress.getMunicipality());
            zipCodeField.setText(String.valueOf(studentAddress.getZipCode()));
        } else {
            streetAddressField.setText("");
            barangayField.setText("");
            cityField.setText("");
            municipalityField.setText("");
            zipCodeField.setText("");
        }
    }

    private void loadGuardianInfo() {
        if (studentGuardianMasterList != null && guardianMasterList != null) {
            studentGuardianMasterList.stream()
                    .filter(sg -> sg.getStudentId().getStudentID() == student.getStudentID())
                    .findFirst()
                    .ifPresent(studentGuardian
                            -> guardianMasterList.stream()
                            .filter(g -> g.getGuardianID() == studentGuardian.getGuardianId().getGuardianID())
                            .findFirst()
                            .ifPresent(guardian -> {
                                guardianNameField.setText(guardian.getGuardianFullName());
                                guardianContactField.setText(guardian.getContact());
                            })
                    );
        } else {
            clearGuardianFields();
        }
    }

    private void loadClusterInfo() {
        if (student.getClusterID() != null) {
            clusterField.setText(student.getClusterID().getClusterName());
        } else {
            clusterField.setText("");
        }
    }

    private void clearGuardianFields() {
        guardianNameField.setText("");
        guardianContactField.setText("");
    }

    private void loadStudentPhoto() {
        ProfilePhotoManager.loadPhoto(profileImageView, student.getStudentID());
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
            // Transfer guardian view data to edit fields when entering edit mode
            if (!guardianNameField.getText().isEmpty()) {
                String[] nameParts = guardianNameField.getText().split(" ");
                if (nameParts.length >= 2) {
                    guardianFirstNameField.setText(nameParts[0]);
                    guardianLastNameField.setText(nameParts[nameParts.length - 1]);
                    if (nameParts.length > 2) {
                        guardianMiddleNameField.setText(nameParts[1]);
                    }
                }
            }
            guardianContactInfoField.setText(guardianContactField.getText());
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

    @FXML
    public void initialize() {
        initializeEditableFields();
        changePhotoButton.setVisible(false);
        changePhotoButton.setManaged(false);
        // Load default photo using ProfilePhotoManager instead
        ProfilePhotoManager.loadPhoto(profileImageView, -1); // -1 will load default photo
        changePhotoButton.setOnAction(e -> handleChangePhoto());
    }

    private Address findOrCreateStudentAddress() {
        if (addressMasterList == null) {
            return new Address(student, 0, "", "", "", "", 0);
        }
        return addressMasterList.stream()
                .filter(addr -> addr.getStudentID().getStudentID() == student.getStudentID())
                .findFirst()
                .orElse(new Address(student, 0, "", "", "", "", 0));
    }
}
