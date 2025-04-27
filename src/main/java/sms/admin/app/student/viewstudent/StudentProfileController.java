
package sms.admin.app.student.viewstudent;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

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

/**
 * Controller for the student profile view, managing the display and editing of
 * student information. This class handles the UI elements and logic for viewing
 * and updating student details, including personal information, address,
 * guardian, cluster, and profile photo.
 */

public class StudentProfileController extends FXController {

    // Core fields
    private Stage stage; // The stage for this dialog
    private Student student; // The student whose profile is being viewed
    private static final String STUDENT_PHOTOS_DIR = "src/main/resources/sms/admin/assets/img/profile"; // Directory for
    // student
    // photos
    private static final String DEFAULT_PHOTO_PATH = "/assets/img/default-profile.png"; // Default profile photo path
    private static final String BACKUP_PHOTO_PATH = "/sms/admin/assets/img/default-profile.png"; // Backup default photo
    // path

    // Data lists
    private ObservableList<Address> addressMasterList; // List of all addresses
    private ObservableList<StudentGuardian> studentGuardianMasterList; // List of student-guardian relationships
    private ObservableList<Guardian> guardianMasterList; // List of all guardians
    private ObservableList<Cluster> clusterMasterList; // List of all clusters

    // FXML injected fields
    @FXML
    private ImageView profileImageView; // Image view for the student's profile photo
    @FXML
    private Button changePhotoButton; // Button to change the profile photo
    @FXML
    private Button backCancelButton; // Button to cancel and close the dialog
    @FXML
    private Button editSaveButton; // Button to save changes
    // Personal Info Fields
    @FXML
    private TextField firstNameField; // Field for student's first name
    @FXML
    private TextField middleNameField; // Field for student's middle name
    @FXML
    private TextField lastNameField; // Field for student's last name
    @FXML
    private TextField nameExtField; // Field for student's name extension
    // Contact Fields
    @FXML
    private TextField contactField; // Field for student's contact number
    @FXML
    private TextField emailField; // Field for student's email
    @FXML
    private TextField fareField; // Field for student's fare
    // Address Fields
    @FXML
    private TextField streetAddressField; // Field for street address
    @FXML
    private TextField barangayField; // Field for barangay
    @FXML
    private TextField cityField; // Field for city
    @FXML
    private TextField municipalityField; // Field for municipality
    @FXML
    private TextField zipCodeField; // Field for zip code
    // Academic Fields
    @FXML
    private TextField clusterField; // Field for cluster name
    @FXML
    private TextField clusterDetailsField; // Field for cluster details
    // Guardian Fields
    @FXML
    private TextField guardianFirstNameField; // Field for guardian's first name
    @FXML
    private TextField guardianMiddleNameField; // Field for guardian's middle name
    @FXML
    private TextField guardianLastNameField; // Field for guardian's last name
    @FXML
    private TextField guardianRelationshipField; // Field for guardian's relationship to student
    @FXML
    private TextField guardianContactInfoField; // Field for guardian's contact information
    @FXML
    private GridPane guardianViewGrid; // Grid for viewing guardian information
    @FXML
    private GridPane guardianEditGrid; // Grid for editing guardian information

    private ObservableList<Address> addresses; // Shared address collection
    private ObservableList<Guardian> guardians; // Shared guardian collection
    private ObservableList<StudentGuardian> studentGuardians; // Shared student-guardian collection
    private ObservableList<Student> students; // Shared student collection

    /**
     * Loads the initial fields and configurations for the student profile view.
     */
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

    /**
     * Loads event listeners for UI interactions.
     */
    @Override
    protected void load_listeners() {
        editSaveButton.setOnAction(e -> saveChanges());
        backCancelButton.setOnAction(e -> closeDialog());
        changePhotoButton.setOnAction(e -> handleChangePhoto());
    }

    /**
     * Loads bindings for UI components. Currently empty as no bindings are
     * needed.
     */
    @Override
    protected void load_bindings() {
        // No bindings needed currently
    }

    /**
     * Initializes the controller after FXML injection, setting the initial
     * state.
     */
    @FXML
    public void initialize() {
        // Hide change photo button initially
        changePhotoButton.setVisible(false);
        changePhotoButton.setManaged(false);
        ProfilePhotoManager.loadPhoto(profileImageView, -1); // Load default photo when no student is set
    }

    /**
     * Sets the stage for this dialog.
     *
     * @param stage The stage to set.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        initializeKeyHandler(); // Reinitialize key handler when stage is set
    }

    /**
     * Sets the student whose profile is being viewed.
     *
     * @param student The student to set.
     */
    public void setStudent(Student student) {
        this.student = student;
        if (student != null) {
            loadStudentData();
        }
    }

    /**
     * Initializes the master lists for addresses, guardians, and clusters.
     */
    private void initializeMasterLists() {
        try {
            addressMasterList = FXCollections.observableArrayList(AddressDAO.getAddressesList());

            // Handle empty student-guardian list gracefully
            List<StudentGuardian> sgList;
            try {
                sgList = StudentGuardianDAO.getStudentGuardianList();
            } catch (NoSuchElementException e) {
                // If no student-guardian relationships exist yet, create empty list
                sgList = FXCollections.observableArrayList();
                System.out.println("No student-guardian relationships found, creating empty list");
            }
            studentGuardianMasterList = FXCollections.observableArrayList(sgList);

            // Use DataManager to retrieve shared collections
            guardianMasterList = DataManager.getInstance().getCollectionsRegistry().getList("GUARDIAN");
            clusterMasterList = DataManager.getInstance().getCollectionsRegistry().getList("CLUSTER");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog("Database Error", "Failed to load data",
                    "An error occurred while loading data from the database.");
        }
    }

    /**
     * Initializes the key handler for the stage to handle keyboard events.
     */
    private void initializeKeyHandler() {
        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(this::handleKeyPress);
        }
    }

    /**
     * Handles key press events, closing the dialog on ESCAPE key press.
     *
     * @param event The key event to handle.
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            closeDialog();
            event.consume();
        }
    }

    /**
     * Closes the dialog with a fade animation.
     */
    @FXML
    private void closeDialog() {
        DialogManager.closeWithFade(stage, null);
    }

    /**
     * Handles the action to change the student's profile photo.
     */
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

    /**
     * Closes the dialog (alias for closeDialog).
     */
    @FXML
    private void handleClose() {
        closeDialog();
    }

    /**
     * Saves the changes made to the student's profile.
     */
    @FXML
    private void handleSave() {
        saveChanges();
    }

    /**
     * Updates the UI to show the change photo button when a student is loaded.
     */
    private void updateUI() {
        changePhotoButton.setVisible(true);
        changePhotoButton.setManaged(true);
    }

    /**
     * Loads all student data into the UI fields.
     */
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

    /**
     * Loads the student's basic information into the UI fields.
     */
    private void loadBasicStudentInfo() {
        firstNameField.setText(student.getFirstName());
        middleNameField.setText(student.getMiddleName());
        lastNameField.setText(student.getLastName());
        nameExtField.setText(student.getNameExtension());
        contactField.setText(student.getContact());
        emailField.setText(student.getEmail());
        fareField.setText(String.valueOf(student.getFare()));
    }

    /**
     * Loads the student's address information into the UI fields.
     */
    private void loadAddressInfo() {
        // Reload address list if needed
        if (addressMasterList == null || addressMasterList.isEmpty()) {
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

    /**
     * Loads the student's guardian information into the UI fields.
     */
    private void loadGuardianInfo() {
        if (studentGuardianMasterList == null || guardianMasterList == null) {
            try {
                // Try to refresh the lists from DataManager
                studentGuardianMasterList = DataManager.getInstance()
                        .getCollectionsRegistry().getList("STUDENT_GUARDIAN");
                guardianMasterList = DataManager.getInstance()
                        .getCollectionsRegistry().getList("GUARDIAN");
                
                if (studentGuardianMasterList == null || guardianMasterList == null) {
                    clearGuardianFields();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                clearGuardianFields();
                return;
            }
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

    /**
     * Clears all guardian-related fields in the UI.
     */
    private void clearGuardianFields() {
        guardianFirstNameField.clear();
        guardianMiddleNameField.clear();
        guardianLastNameField.clear();
        guardianRelationshipField.clear();
        guardianContactInfoField.clear();
    }

    /**
     * Loads the student's cluster information into the UI fields.
     */
    private void loadClusterInfo() {
        if (student.getClusterID() != null) {
            clusterField.setText(student.getClusterID().getClusterName());
        } else {
            clusterField.clear();
        }
    }

    /**
     * Loads the student's profile photo into the image view.
     */
    private void loadStudentPhoto() {
        ProfilePhotoManager.loadPhoto(profileImageView, student.getStudentID());
    }

    /**
     * Saves changes made to the student's profile, updating the database and
     * collections.
     */
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

    /**
     * Handles the update of the student's address, creating a new address if
     * none exists.
     */
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

    /**
     * Handles the update of the student's guardian, creating or updating as
     * needed.
     *
     * @return The updated Guardian object.
     */
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

    /**
     * Handles the update of the student's cluster information.
     */
    private void handleClusterUpdate() {
        ProfileDataManager.handleClusterUpdate(
                clusterField.getText(),
                student.getClusterID() != null ? String.valueOf(student.getClusterID().getClusterID()) : "",
                clusterMasterList).ifPresent(student::setClusterID);
    }

    /**
     * Finds or creates an address for the student.
     *
     * @return The student's Address object.
     */
    private Address findOrCreateStudentAddress() {
        if (addressMasterList == null) {
            return new Address(student, 0, "", "", "", "", 0);
        }
        return addressMasterList.stream()
                .filter(addr -> addr.getStudentID().getStudentID() == student.getStudentID())
                .findFirst()
                .orElse(new Address(student, 0, "", "", "", "", 0));
    }

    /**
     * Finds the current guardian for the student.
     *
     * @return The Guardian object, or null if not found.
     */
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

    /**
     * Updates the student-guardian relationship in the database and master
     * list.
     *
     * @param guardian The guardian to associate with the student.
     */
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
