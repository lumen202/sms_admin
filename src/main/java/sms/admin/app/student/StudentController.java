package sms.admin.app.student;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import atlantafx.base.controls.ModalPane;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import sms.admin.app.student.enrollment.EnrollmentLoader;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.enrollment.CsvImporter;
import sms.admin.util.enrollment.CsvStudent;
import sms.admin.util.enrollment.EnrollmentUtils;
import sms.admin.util.exporter.StudentTableExporter;

/**
 * Controller for the student management view, handling the display, filtering,
 * and management of student records.
 * This class manages the UI elements and logic for viewing, adding, editing,
 * deleting, importing, and exporting student data.
 */
public class StudentController extends FXController {

    @FXML
    private TableView<Student> studentTableView; // Table displaying student records
    @FXML
    private TableColumn<Student, Integer> studentIDColumn; // Column for student ID
    @FXML
    private TableColumn<Student, String> firstNameColumn; // Column for student's first name
    @FXML
    private TableColumn<Student, String> middleNameColumn; // Column for student's middle name
    @FXML
    private TableColumn<Student, String> lastNameColumn; // Column for student's last name
    @FXML
    private TableColumn<Student, String> nameExtensionColumn; // Column for student's name extension
    @FXML
    private TableColumn<Student, String> clusterColumn; // Column for student's cluster
    @FXML
    private TableColumn<Student, String> contactColumn; // Column for student's contact
    @FXML
    private TableColumn<Student, String> emailColumn; // Column for student's email
    @FXML
    private TableColumn<Student, String> addressColumn; // Column for student's address
    @FXML
    private BorderPane contentPane; // Main content pane
    @FXML
    private ModalPane formodal; // Modal pane for displaying forms
    @FXML
    private StackPane modalContainer; // Container for modal content
    @FXML
    private MenuButton exportButton; // Button for export options
    @FXML
    private MenuItem exportExcel; // Menu item for exporting to Excel
    @FXML
    private MenuItem exportCsv; // Menu item for exporting to CSV
    @FXML
    private MenuItem exportPdf; // Menu item for exporting to PDF
    @FXML
    private MenuItem importCsv; // Menu item for importing from CSV
    @FXML
    private TextField searchField; // Field for searching students
    @FXML
    private Label totalLabel; // Label displaying total number of students
    @FXML
    private Label statusLabel; // Label displaying status messages
    @FXML
    private Button addStudentButton; // Button to add a new student

    // The master list is modifiable and backs our filtered view
    private ObservableList<Student> masterStudentList = FXCollections.observableArrayList(); // Master list of students
    private FilteredList<Student> filteredList; // Filtered list for year and search criteria
    private ContextMenu studentMenu; // Context menu for student table
    private String selectedYear; // Selected academic year (e.g., "2024-2025")
    private String searchText = ""; // Current search text

    /**
     * Loads the initial fields and configurations for the student management view.
     */
    @Override
    protected void load_fields() {
        try {
            contentPane.getProperties().put("controller", this);
            selectedYear = (String) getParameter("selectedYear");
            if (selectedYear == null) {
                selectedYear = getDefaultYear();
            }
            // Load the master list for the selected year
            initializeStudentList(selectedYear);
            // Create a filtered list using the master list
            filteredList = new FilteredList<>(masterStudentList);
            studentTableView.setItems(filteredList);

            // Configure modal pane
            formodal.setAlignment(Pos.TOP_CENTER);
            formodal.usePredefinedTransitionFactories(Side.TOP);
            formodal.setPersistent(true);

            updateFilter();
            updateStatusLabel();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Database Error", "Failed to load student data",
                    "An error occurred while loading student data from the database.");
        }
        addStudentButton.setOnAction(e -> handleAddStudent());
    }

    /**
     * Loads bindings for the table columns to display student properties.
     */
    @Override
    protected void load_bindings() {
        studentIDColumn.setCellValueFactory(cell -> cell.getValue().studentIDProperty().asObject());
        firstNameColumn.setCellValueFactory(cell -> cell.getValue().firstNameProperty());
        middleNameColumn.setCellValueFactory(cell -> cell.getValue().middleNameProperty());
        lastNameColumn.setCellValueFactory(cell -> cell.getValue().lastNameProperty());
        nameExtensionColumn.setCellValueFactory(cell -> cell.getValue().nameExtensionProperty());
        clusterColumn.setCellValueFactory(cell -> cell.getValue().clusterIDProperty().getValue().clusterNameProperty());
        contactColumn.setCellValueFactory(cell -> cell.getValue().contactProperty());
        emailColumn.setCellValueFactory(cell -> cell.getValue().emailProperty());
        studentTableView.setItems(filteredList);

        // Configure context menu for the table
        studentMenu = new ContextMenu();
        MenuItem viewMenu = new MenuItem("Edit Student");
        viewMenu.setOnAction(e -> openStudentProfileInEditMode());
        studentMenu.getItems().add(viewMenu);
        MenuItem deleteMenu = new MenuItem("Delete Student");
        deleteMenu.setOnAction(e -> deleteStudent(studentTableView.getSelectionModel().getSelectedItem()));
        studentMenu.getItems().add(deleteMenu);
        studentTableView.setContextMenu(studentMenu);
    }

    /**
     * Loads event listeners for UI interactions.
     */
    @Override
    protected void load_listeners() {
        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));
        importCsv.setOnAction(event -> handleImport());

        // Update search filter as the user types
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue.toLowerCase();
            updateFilter();
            updateStatusLabel();
        });
    }

    /**
     * Updates the selected academic year and refreshes the student list.
     *
     * @param year The academic year to set (e.g., "2024-2025").
     */
    public void updateYear(String year) {
        if (year != null && !year.equals(selectedYear)) {
            selectedYear = year;
            initializeStudentList(selectedYear);
            updateFilter();
            studentTableView.refresh();
            updateStatusLabel();
        }
    }

    /**
     * Initializes the controller with the specified academic year.
     *
     * @param year The academic year to initialize with (e.g., "2024-2025").
     */
    public void initializeWithYear(String year) {
        if (year == null) {
            return;
        }
        updateYear(year); // Delegate to updateYear for consistency
    }

    /**
     * Populates the master student list with students matching the selected year
     * and not marked as deleted.
     *
     * @param year The academic year to filter students by.
     */
    private void initializeStudentList(String year) {
        int startYear = Integer.parseInt(year.split("-")[0]);
        // Use DataManager to get fresh data
        ObservableList<Student> students = DataManager.getInstance()
                .getCollectionsRegistry().getList("STUDENT");
        masterStudentList.setAll(
                students.stream()
                        .filter(s -> s != null && s.getYearID() != null
                                && s.getYearID().getYearStart() == startYear
                                && s.isDeleted() == 0)
                        .collect(Collectors.toList()));
    }

    /**
     * Updates the filter predicate to apply both year and search criteria.
     */
    private void updateFilter() {
        filteredList.setPredicate(student -> {
            boolean matchesYear = student.getYearID() != null
                    && student.getYearID().getYearStart() == Integer.parseInt(selectedYear.split("-")[0]);
            boolean matchesSearch = searchText == null || searchText.isEmpty()
                    || student.getFirstName().toLowerCase().contains(searchText)
                    || student.getLastName().toLowerCase().contains(searchText)
                    || student.getMiddleName().toLowerCase().contains(searchText)
                    || student.getEmail().toLowerCase().contains(searchText)
                    || student.getContact().toLowerCase().contains(searchText)
                    || String.valueOf(student.getStudentID()).contains(searchText);
            return matchesYear && matchesSearch;
        });
    }

    /**
     * Updates the status label with the total number of students in the filtered
     * list.
     */
    private void updateStatusLabel() {
        int totalStudents = filteredList.size();
        totalLabel.setText("Total Students: " + totalStudents);
    }

    /**
     * Opens the profile view for the selected student.
     */
    @FXML
    private void openStudentProfile() {
        Student selectedStudent = studentTableView.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            openStudentProfile(selectedStudent);
        }
    }

    /**
     * Marks a student as deleted and updates the master list and database.
     *
     * @param student The student to delete.
     */
    private void deleteStudent(Student student) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Student Deletion");
        confirmDialog.setHeaderText("Are you sure you want to delete this student?");
        confirmDialog.setContentText(
                "Student Details:\n" +
                        "ID: " + student.getStudentID() + "\n" +
                        "Name: " + student.getFullName() + "\n");

        // Style the dialog buttons
        Button okButton = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Delete");
        okButton.setStyle("-fx-background-color: #800000; -fx-text-fill: white;");

        Button cancelButton = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #003366; -FX-text-fill: white;");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    student.setDeleted(1);
                    StudentDAO.update(student);
                    masterStudentList.remove(student);
                    studentTableView.refresh();
                    updateStatusLabel();

                    // Show success message
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Student has been successfully deleted.");
                    successAlert.show();

                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorAlert("Error", "Failed to delete student", e.getMessage());
                }
            }
        });
    }

    /**
     * Opens the profile view for a specific student.
     *
     * @param student The student whose profile to open.
     */
    private void openStudentProfile(Student student) {
        try {
            StudentProfileLoader loader = new StudentProfileLoader();
            loader.addParameter("SELECTED_STUDENT", student);
            loader.addParameter("OWNER_STAGE", studentTableView.getScene().getWindow());
            loader.load();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to open student profile", e.getMessage());
        }
    }

    /**
     * Opens the profile view for the selected student in edit mode.
     */
    private void openStudentProfileInEditMode() {
        try {
            Student selectedStudent = studentTableView.getSelectionModel().getSelectedItem();
            if (selectedStudent == null) {
                return;
            }

            StudentProfileLoader loader = new StudentProfileLoader();
            loader.addParameter("SELECTED_STUDENT", selectedStudent);
            loader.addParameter("OWNER_STAGE", studentTableView.getScene().getWindow());
            loader.addParameter("EDIT_MODE", true);
            loader.load();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to open student profile in edit mode", e.getMessage());
        }
    }

    /**
     * Closes the modal pane.
     */
    @FXML
    private void closeModal() {
        formodal.hide();
    }

    /**
     * Generates the file path for exporting student data.
     *
     * @param extension The file extension (e.g., "xlsx", "csv", "pdf").
     * @return The file path for the export.
     */
    private String getExportPath(String extension) {
        return System.getProperty("user.home") + "/Downloads/students_" + LocalDate.now().toString() + "." + extension;
    }

    /**
     * Handles the export of student data to the specified format.
     *
     * @param type The export type ("excel", "csv", or "pdf").
     */
    private void handleExport(String type) {
        try {
            String title = "Student List Report";
            String outputPath = getExportPath(type.equals("excel") ? "xlsx" : type.toLowerCase());
            StudentTableExporter exporter = new StudentTableExporter();
            switch (type) {
                case "excel" ->
                    exporter.exportToExcel(studentTableView, title, outputPath);
                case "pdf" ->
                    exporter.exportToPdf(studentTableView, title, outputPath);
                case "csv" ->
                    exporter.exportToCsv(studentTableView, title, outputPath);
            }
            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Export Error", "Failed to export students", e.getMessage());
        }
    }

    /**
     * Handles the import of student data from a CSV file.
     */
    private void handleImport() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select CSV File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File selectedFile = fileChooser.showOpenDialog(studentTableView.getScene().getWindow());
            if (selectedFile != null) {
                statusLabel.setText("Processing: " + selectedFile.getName());
                List<CsvStudent> students = CsvImporter.importCsv(selectedFile);
                int successCount = 0;

                SchoolYear currentSchoolYear = getCurrentSchoolYear();
                if (currentSchoolYear == null) {
                    statusLabel.setText("Error: No school year selected");
                    return;
                }

                for (CsvStudent csvStudent : students) {
                    try {
                        EnrollmentUtils.enrollStudentFromCsv(csvStudent, currentSchoolYear);
                        successCount++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Refresh the table after import
                initializeStudentList(selectedYear);
                updateFilter();
                studentTableView.refresh();
                statusLabel.setText("Successfully imported " + successCount + " students");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Import Error", "Failed to import students", e.getMessage());
        }
    }

    /**
     * Opens the enrollment form to add a new student.
     */
    private void handleAddStudent() {
        try {
            SchoolYear currentSchoolYear = getCurrentSchoolYear();
            if (currentSchoolYear != null) {
                EnrollmentLoader loader = new EnrollmentLoader();
                // Pass the selectedYear parameter
                loader.addParameter("selectedYear", selectedYear);
                loader.addParameter("OWNER_WINDOW", studentTableView.getScene().getWindow());
                loader.load();

                // Refresh the table after adding a student
                initializeStudentList(selectedYear);
                updateFilter();
                studentTableView.refresh();
                updateStatusLabel();
            } else {
                showErrorAlert("Error", "No school year selected",
                        "Please select a school year before adding a student.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to open enrollment form", e.getMessage());
        }
    }

    /**
     * Retrieves the current school year based on the selected year.
     *
     * @return The current SchoolYear object, or null if not found.
     */
    private SchoolYear getCurrentSchoolYear() {
        if (selectedYear == null) {
            return null;
        }
        String[] years = selectedYear.split("-");
        int startYear = Integer.parseInt(years[0].trim());
        int endYear = Integer.parseInt(years[1].trim());

        return DataManager.getInstance().getCollectionsRegistry().getList("SCHOOL_YEAR")
                .stream()
                .filter(sy -> sy instanceof SchoolYear)
                .map(sy -> (SchoolYear) sy)
                .filter(sy -> sy.getYearStart() == startYear && sy.getYearEnd() == endYear)
                .findFirst()
                .orElse(null);
    }

    /**
     * Determines the default academic year based on the current date.
     *
     * @return The default academic year string (e.g., "2024-2025").
     */
    private String getDefaultYear() {
        int currentYear = LocalDate.now().getYear();
        return (LocalDate.now().getMonthValue() >= 6 ? currentYear : currentYear - 1) + "-"
                + (LocalDate.now().getMonthValue() >= 6 ? currentYear + 1 : currentYear);
    }

    /**
     * Displays an error alert with the specified details.
     *
     * @param title   The title of the alert.
     * @param header  The header text of the alert.
     * @param content The content text of the alert.
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}