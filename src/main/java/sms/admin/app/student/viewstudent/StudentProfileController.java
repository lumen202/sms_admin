package sms.admin.app.student.viewstudent;

import java.io.File;
import java.util.Optional;

import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.data.StudentGuardianDAO;
import dev.finalproject.database.DataManager;
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
    private static final String STUDENT_PHOTOS_DIR = "src/main/resources/sms/admin/assets/img/profile";
    private static final String DEFAULT_PHOTO_PATH = "/assets/img/default-profile.png";
    private static final String BACKUP_PHOTO_PATH = "/sms/admin/assets/img/default-profile.png";

    // Data lists
    private ObservableList<Address> addressMasterList;
    private ObservableList<StudentGuardian> studentGuardianMasterList;
    private ObservableList<Guardian> guardianMasterList;
    private ObservableList<Cluster> clusterMasterList;

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
    private TextField guardianFirstNameField, guardianMiddleNameField, guardianLastNameField,
            guardianRelationshipField, guardianContactInfoField;
    @FXML
    private GridPane guardianViewGrid, guardianEditGrid;

    private ObservableList<Address> addresses;
    private ObservableList<Guardian> guardians;
    private ObservableList<StudentGuardian> studentGuardians;
    private ObservableList<Student> students;

    @Override
    protected void load_fields() {
        // Initialize collections via the shared DataManager
        addresses = DataManager.getInstance().getCollectionsRegistry().getList("ADDRESS");
        guardians = DataManager.getInstance().getCollectionsRegistry().getList("GUARDIAN");
        studentGuardians = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT_GUARDIAN");
        students = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT");

        initializeMasterLists();
        initializeKeyHandler();
        updateUI();
    }

    @Override
    protected void load_listeners() {
        editSaveButton.setOnAction(e -> saveChanges());
        backCancelButton.setOnAction(e -> closeDialog());
        changePhotoButton.setOnAction(e -> handleChangePhoto());
    }

    @Override
    protected void load_bindings() {
        // No bindings needed currently
    }

    @FXML
    public void initialize() {
        // Called after FXML injection; ensure initial state
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
        if (student != null) {
            loadStudentData();
        }
    }

    // Initialization Helpers
    private void initializeMasterLists() {
        try {
            addressMasterList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
            studentGuardianMasterList = FXCollections.observableArrayList(StudentGuardianDAO.getStudentGuardianList());
            // Use DataManager to retrieve shared collections
            guardianMasterList = DataManager.getInstance().getCollectionsRegistry().getList("GUARDIAN");
            clusterMasterList = DataManager.getInstance().getCollectionsRegistry().getList("CLUSTER");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog("Database Error", "Failed to load data",
                    "An error occurred while loading data from the database.");
        }
    }

    private void initializeKeyHandler() {
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(this::handleKeyPress);
        }
    }

    // Event Handlers
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            closeDialog();
            event.consume();
        }
    }

    @FXML
    private void closeDialog() {
        DialogManager.closeWithFade(stage, null);
    }

    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (selectedFile != null) {
            String savedPath = ProfilePhotoManager.savePhoto(selectedFile, student.getStudentID());
            if (savedPath != null) {
                loadStudentPhoto();
            }
        }
    }

    @FXML
    private void handleClose() {
        closeDialog();
    }

    @FXML
    private void handleSave() {
        saveChanges();
    }

    // UI Update Helpers
    private void updateUI() {
        changePhotoButton.setVisible(true);
        changePhotoButton.setManaged(true);
    }

    // Data Loading Methods
    private void loadStudentData() {
        if (student == null) {
            return;
        }
        loadBasicStudentInfo();
        loadAddressInfo();
        loadGuardianInfo();
        loadClusterInfo();
        loadStudentPhoto();
        updateUI();
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
            AddressDAO.initialize(DataManager.getInstance().getCollectionsRegistry().getList("STUDENT"));
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
                            guardianFirstNameField.setText(guardian.getFirstName());
                            guardianMiddleNameField.setText(guardian.getMiddleName());
                            guardianLastNameField.setText(guardian.getLastName());
                            guardianRelationshipField.setText(guardian.getRelationship());
                            guardianContactInfoField.setText(guardian.getContact());
                        }));
    }

    private void clearGuardianFields() {
        guardianFirstNameField.clear();
        guardianMiddleNameField.clear();
        guardianLastNameField.clear();
        guardianRelationshipField.clear();
        guardianContactInfoField.clear();
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
    private void saveChanges() {
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

            // Update the student in the registry list
            int index = students.indexOf(student);
            if (index >= 0) {
                students.set(index, student);
            }

            closeDialog();

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog("Error", "Failed to save changes", e.getMessage());
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
                guardianFirstNameField.getText(),
                guardianContactInfoField.getText(),
                guardianFirstNameField,
                guardianMiddleNameField,
                guardianLastNameField,
                guardianRelationshipField,
                guardianContactInfoField,
                currentGuardian,
                guardianMasterList);

        updateStudentGuardianRelationship(updatedGuardian);
        return updatedGuardian;
    }

    private void handleClusterUpdate() {
        ProfileDataManager.handleClusterUpdate(
                clusterField.getText(),
                student.getClusterID() != null ? String.valueOf(student.getClusterID().getClusterID()) : "",
                clusterMasterList).ifPresent(student::setClusterID);
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
}
