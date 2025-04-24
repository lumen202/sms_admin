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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class StudentController extends FXController {

    @FXML
    private TableView<Student> studentTableView;
    @FXML
    private TableColumn<Student, Integer> studentIDColumn;
    @FXML
    private TableColumn<Student, String> firstNameColumn;
    @FXML
    private TableColumn<Student, String> middleNameColumn;
    @FXML
    private TableColumn<Student, String> lastNameColumn;
    @FXML
    private TableColumn<Student, String> nameExtensionColumn;
    @FXML
    private TableColumn<Student, String> clusterColumn;
    @FXML
    private TableColumn<Student, String> contactColumn;
    @FXML
    private TableColumn<Student, String> emailColumn;
    @FXML
    private TableColumn<Student, String> addressColumn;
    @FXML
    private BorderPane contentPane;
    @FXML
    private ModalPane formodal;
    @FXML
    private StackPane modalContainer;
    @FXML
    private MenuButton exportButton;
    @FXML
    private MenuItem exportExcel;
    @FXML
    private MenuItem exportCsv;
    @FXML
    private MenuItem exportPdf;
    @FXML
    private MenuItem importCsv;
    @FXML
    private TextField searchField;
    @FXML
    private Label totalLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button addStudentButton;

    // The master list is modifiable and backs our filtered view.
    private ObservableList<Student> masterStudentList = FXCollections.observableArrayList();
    // One filtered list to handle both year and search criteria.
    private FilteredList<Student> filteredList;
    private ContextMenu studentMenu;
    private String selectedYear; // e.g., "2024-2025"
    private String searchText = "";

    @Override
    protected void load_fields() {
        try {
            contentPane.getProperties().put("controller", this);
            selectedYear = (String) getParameter("selectedYear");
            if (selectedYear == null) {
                selectedYear = getDefaultYear();
            }
            // Load the master list for the selected year using the DataManager
            initializeStudentList(selectedYear);
            // Create a single FilteredList using the master list.
            filteredList = new FilteredList<>(masterStudentList);
            studentTableView.setItems(filteredList);

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

        studentMenu = new ContextMenu();
        MenuItem viewMenu = new MenuItem("Edit Student");
        viewMenu.setOnAction(e -> openStudentProfileInEditMode());
        studentMenu.getItems().add(viewMenu);
        MenuItem deleteMenu = new MenuItem("Delete Student");
        deleteMenu.setOnAction(e -> deleteStudent(studentTableView.getSelectionModel().getSelectedItem()));
        studentMenu.getItems().add(deleteMenu);
        studentTableView.setContextMenu(studentMenu);
    }

    @Override
    protected void load_listeners() {
        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));
        importCsv.setOnAction(event -> handleImport());

        // Update the search text and filter predicate as the user types.
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue.toLowerCase();
            updateFilter();
            updateStatusLabel();
        });
    }

    public void updateYear(String year) {
        if (year != null && !year.equals(selectedYear)) {
            selectedYear = year;
            initializeStudentList(selectedYear);
            updateFilter();
            studentTableView.refresh();
            updateStatusLabel();
        }
    }

    public void initializeWithYear(String year) {
        if (year == null) {
            return;
        }
        updateYear(year); // Delegate to updateYear for consistency
    }

    /**
     * Populate the masterStudentList with students matching the selected year
     * and that are not archived.
     */
    private void initializeStudentList(String year) {
        int startYear = Integer.parseInt(year.split("-")[0]);
        // Use the new DataManager to get fresh data.
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
     * Update the filter predicate to apply both the year and search filters.
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

    private void updateStatusLabel() {
        int totalStudents = filteredList.size();
        totalLabel.setText("Total Students: " + totalStudents);
    }

    @FXML
    private void openStudentProfile() {
        Student selectedStudent = studentTableView.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            openStudentProfile(selectedStudent);
        }
    }

    /**
     * Marks the student as archived (i.e. deleted) and updates the underlying
     * master list.
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

    @FXML
    private void closeModal() {
        formodal.hide();
    }

    private String getExportPath(String extension) {
        return System.getProperty("user.home") + "/Downloads/students_" + LocalDate.now().toString() + "." + extension;
    }

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

    private void handleAddStudent() {
        try {
            SchoolYear currentSchoolYear = getCurrentSchoolYear();
            if (currentSchoolYear != null) {
                EnrollmentLoader loader = new EnrollmentLoader();
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

    private String getDefaultYear() {
        int currentYear = LocalDate.now().getYear();
        return (LocalDate.now().getMonthValue() >= 6 ? currentYear : currentYear - 1) + "-"
                + (LocalDate.now().getMonthValue() >= 6 ? currentYear + 1 : currentYear);
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
