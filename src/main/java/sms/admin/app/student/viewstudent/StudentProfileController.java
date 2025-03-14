package sms.admin.app.student.viewstudent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import dev.finalproject.models.Address;
import dev.finalproject.models.Guardian;
import dev.finalproject.models.Student;
import dev.finalproject.models.StudentGuardian;
import dev.sol.core.application.FXController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
import sms.admin.util.dialog.DialogManager;
import dev.finalproject.App;
import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.GuardianDAO;
import dev.finalproject.data.StudentGuardianDAO;
import java.io.IOException;
import java.nio.file.StandardCopyOption;

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
    private ObservableList<Guardian> guardianMasterlist;
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
        initializeEditableFields(); // Make sure fields are initialized first
        initializeKeyHandler();

        // Initialize UI state
        isEditMode = false;
        updateUIForEditMode();
        changePhotoButton.setVisible(false);
    }

    private void initializeMasterLists() {
        try {
            // Initialize address list
            addressMasterList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
            
            // Get the master lists directly from the registry
            studentGuardianMasterList = FXCollections.observableArrayList(StudentGuardianDAO.getStudentGuardianList());
            guardianMasterlist = App.COLLECTIONS_REGISTRY.getList("GUARDIAN");

            System.out.println("Student-Guardian relationships loaded: " + studentGuardianMasterList.size());
            studentGuardianMasterList.forEach(sg -> 
                System.out.println("StudentGuardian - Student: " + sg.getStudentId().getStudentID() 
                    + " Guardian: " + sg.getGuardianId().getGuardianID()));
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to load data");
            alert.setContentText("An error occurred while loading data from the database.");
            alert.showAndWait();
        }
    }

    private void initializeEditableFields() {
        if (firstNameField == null) { // Add FXML injection check
            return; // Return if FXML fields haven't been injected yet
        }
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
    }

    // Public setters
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStudent(Student student) {
        this.student = student;
        if (student != null && editableFields != null) { // Add null check
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

    private void toggleFieldsVisibility(boolean showAll) {
        if (editableFields == null) return;

        // Always show cluster field
        if (clusterField != null) {
            clusterField.setVisible(true);
            clusterField.setManaged(true);
            // Make cluster field parent elements visible
            VBox clusterSection = (VBox) clusterField.getParent().getParent();
            if (clusterSection != null) {
                clusterSection.setVisible(true);
                clusterSection.setManaged(true);
            }
        }

        editableFields.forEach(field -> {
            if (field == null || field == clusterField) return; // Skip cluster field

            String fieldText = field.getText();
            VBox section = field.getParent() != null ? (VBox) field.getParent().getParent() : null;
            GridPane grid = field.getParent() != null ? (GridPane) field.getParent() : null;
            
            if (section == null || grid == null) return;

            int rowIndex = GridPane.getRowIndex(field) != null ? GridPane.getRowIndex(field) : 0;
            boolean shouldShow = showAll || (fieldText != null && !fieldText.trim().isEmpty() && !isEditMode);

            // Set visibility for field and its components
            field.setVisible(shouldShow);
            field.setManaged(shouldShow);
            section.setVisible(shouldShow);
            section.setManaged(shouldShow);

            // Handle section header
            if (section.getChildren() != null) {
                section.getChildren().stream()
                        .filter(node -> node instanceof Label && node.getStyleClass() != null
                                && node.getStyleClass().contains("section-header"))
                        .forEach(header -> {
                            header.setVisible(shouldShow);
                            header.setManaged(shouldShow);
                        });
            }

            // Handle field labels
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
        });
    }

    private void updateUIForEditMode() {
        editableFields.forEach(field -> {
            field.setEditable(isEditMode);
            // Make cluster field always visible but not editable
            if (field == clusterField) {
                field.setVisible(true);
                field.setManaged(true);
                field.setEditable(false);
            }
        });

        // Show change photo button only in edit mode
        changePhotoButton.setVisible(isEditMode);
        changePhotoButton.setManaged(isEditMode);
        changePhotoButton.setDisable(!isEditMode); // Ensure button is enabled/disabled properly

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
            // Save student basic info
            student.setFirstName(firstNameField.getText());
            student.setMiddleName(middleNameField.getText());
            student.setLastName(lastNameField.getText());
            student.setNameExtension(nameExtField.getText());
            student.setContact(contactField.getText());
            student.setEmail(emailField.getText());
            String fareText = fareField.getText().trim();
            if (!fareText.isEmpty()) {
                student.setFare(Double.valueOf(fareText));
            } else {
                student.setFare(0.0);
            }

            // Update address using AddressDAO
            Address studentAddress = null;
            if (addressMasterList != null) {
                studentAddress = addressMasterList.stream()
                    .filter(addr -> addr.getStudentID().getStudentID() == student.getStudentID())
                    .findFirst()
                    .orElse(new Address(student, 0, "", "", "", "", 0));
            } else {
                studentAddress = new Address(student, 0, "", "", "", "", 0);
            }

            // Update address fields
            studentAddress.setStudentID(student);
            studentAddress.setStreet(streetAddressField.getText());
            studentAddress.setBarangay(barangayField.getText());
            studentAddress.setCity(cityField.getText());
            studentAddress.setMunipality(municipalityField.getText());
            try {
                studentAddress.setZipCode(Integer.parseInt(zipCodeField.getText().trim()));
            } catch (NumberFormatException e) {
                studentAddress.setZipCode(0);
            }

            // Save or update using AddressDAO
            if (studentAddress.getAddressID() == 0) {
                AddressDAO.insert(studentAddress);
                if (addressMasterList == null) {
                    addressMasterList = FXCollections.observableArrayList();
                }
                addressMasterList.add(studentAddress);
            } else {
                AddressDAO.update(studentAddress);
            }

            // Save student changes to database
            dev.finalproject.data.StudentDAO.update(student);

            isEditMode = false;
            updateUIForEditMode();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to save changes");
            alert.setContentText("An error occurred while saving changes to the database.");
            alert.showAndWait();
        }
    }

    private void loadStudentData() {
        if (student == null || editableFields == null) return;
        
        // Initialize all text fields with empty string to prevent null values
        editableFields.forEach(field -> {
            if (field != null) {
                field.setText("");
                field.setVisible(true);
                field.setManaged(true);
            }
        });

        // Load basic student info
        if (student != null) {
            // Set all fields initially visible
            editableFields.forEach(field -> {
                field.setVisible(true);
                field.setManaged(true);
            });

            // Load basic student info
            firstNameField.setText(student.getFirstName());
            middleNameField.setText(student.getMiddleName());
            lastNameField.setText(student.getLastName());
            nameExtField.setText(student.getNameExtension());
            contactField.setText(student.getContact());
            emailField.setText(student.getEmail());
            // Fix fare field handling
            Double fareValue = student.getFare();
            fareField.setText(fareValue != null ? fareValue.toString() : "");

            // Reload address list if needed
            if (addressMasterList == null || addressMasterList.isEmpty()) {
                AddressDAO.initialize(App.COLLECTIONS_REGISTRY.getList("STUDENT"));
                addressMasterList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
            }

            // Load address info
            Address studentAddress = addressMasterList.stream()
                    .filter(addr -> addr.getStudentID() != null && 
                                  addr.getStudentID().getStudentID() == student.getStudentID())
                    .findFirst()
                    .orElse(null);

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
                clearGuardianFields();
            }

            // Load cluster info if available
            if (student.getClusterID() != null) {
                clusterField.setText(student.getClusterID().getClusterName());
            }

            // Load student photo
            loadStudentPhoto();

            // Show/hide fields based on content
            toggleFieldsVisibility(false);
        }
    }

    private void clearGuardianFields() {
        guardianNameField.setText("");
        guardianContactField.setText("");
    }

    private void loadStudentPhoto() {
        try {
            // Check custom photo first
            String photoPath = findStudentPhoto();
            if (photoPath != null) {
                // Load custom photo
                Image image = new Image(new File(photoPath).toURI().toString(), true);
                if (!image.isError()) {
                    profileImageView.setImage(image);
                    return;
                }
            }

            // If no custom photo or loading failed, try to load default
            loadDefaultPhoto();
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
            loadDefaultPhoto();
        }
    }

    private String findStudentPhoto() {
        // Check for different possible extensions
        String[] extensions = { ".jpg", ".jpeg", ".png" };
        String baseFileName = "student_" + student.getStudentID();

        for (String ext : extensions) {
            Path photoPath = Paths.get(STUDENT_PHOTOS_DIR, baseFileName + ext);
            if (Files.exists(photoPath)) {
                return photoPath.toString();
            }
        }
        return null;
    }

    private void loadDefaultPhoto() {
        // Try multiple possible paths for the default image
        String[] possiblePaths = {
                DEFAULT_PHOTO_PATH,
                BACKUP_PHOTO_PATH,
                "/img/default-profile.png",
                "/default-profile.png"
        };

        for (String path : possiblePaths) {
            try {
                var resourceUrl = getClass().getResource(path);
                if (resourceUrl != null) {
                    Image defaultImage = new Image(resourceUrl.toExternalForm());
                    if (!defaultImage.isError()) {
                        profileImageView.setImage(defaultImage);
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load image from path: " + path);
            }
        }

        // If all attempts fail, create a placeholder image
        createEmptyProfileImage();
    }

    private void createEmptyProfileImage() {
        try {
            // Create a new empty image with a light gray background
            profileImageView.setImage(null);
            profileImageView.setStyle(
                    "-fx-background-color: #f0f0f0;" +
                            "-fx-background-radius: 75;" +
                            "-fx-border-color: #cccccc;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 75;");
        } catch (Exception e) {
            System.err.println("Failed to create empty profile image: " + e.getMessage());
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

    private String saveStudentPhoto(File sourceFile) {
        try {
            // Get the absolute path to resources directory
            String projectDir = System.getProperty("user.dir");
            Path photosDir = Paths.get(projectDir, STUDENT_PHOTOS_DIR);

            // Create directories if they don't exist
            if (!Files.exists(photosDir)) {
                Files.createDirectories(photosDir);
            }

            // Get file extension
            String extension = sourceFile.getName().substring(sourceFile.getName().lastIndexOf('.'));

            // Create destination path with student ID
            String fileName = "student_" + student.getStudentID() + extension;
            Path destinationPath = photosDir.resolve(fileName);

            // Copy file to destination
            Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            return destinationPath.toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save photo: " + e.getMessage());
            return null;
        }
    }

    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (selectedFile != null) {
            String savedPath = saveStudentPhoto(selectedFile);
            if (savedPath != null) {
                try {
                    Image image = new Image(new File(savedPath).toURI().toString());
                    profileImageView.setImage(image);
                    System.out.println("Photo saved successfully to: " + savedPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Failed to load saved image: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    public void initialize() {
        initializeEditableFields();
        changePhotoButton.setVisible(false);
        changePhotoButton.setManaged(false);

        // Initialize default profile image
        loadDefaultPhoto();

        // Add click handler for change photo button
        changePhotoButton.setOnAction(e -> handleChangePhoto());
    }
}