package sms.admin.app.student;

import atlantafx.base.controls.ModalPane;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.exporter.StudentTableExporter;
import sms.admin.util.YearData;
import dev.finalproject.App;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import java.time.LocalDate;
import java.io.File;
import sms.admin.util.enrollment.CsvStudent;
import sms.admin.util.enrollment.EnrollmentUtils;
import sms.admin.util.enrollment.CsvImporter;
import java.io.IOException;
import java.util.List;

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
    private BorderPane contentPane; // Changed from StackPane to BorderPane to match FXML
    @FXML
    private ModalPane formodal; // ModalPane from AtlantaFX (defined in FXML without children)
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
    private MenuItem importCsv; // Add new import menu item field
    @FXML
    private TextField searchField;
    @FXML
    private Label totalLabel;
    @FXML
    private Label statusLabel;

    private FilteredList<Student> yearFilteredList;
    private FilteredList<Student> searchFilteredList;
    private ContextMenu studentMenu;

    @Override
    protected void load_fields() {
        try {
            // Get filtered list from parameters
            yearFilteredList = (FilteredList<Student>) getParameter("filteredStudentList");
            searchFilteredList = new FilteredList<>(yearFilteredList);
            studentTableView.setItems(searchFilteredList);
            String selectedYear = (String) getParameter("selectedYear");
            if (selectedYear == null) {
                selectedYear = YearData.getCurrentAcademicYear();
            }

            // No need to filter by year anymore since we're using the filtered list
            formodal.setAlignment(Pos.TOP_CENTER);
            formodal.usePredefinedTransitionFactories(Side.TOP);
            formodal.setPersistent(true);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to load student data");
            alert.setContentText("An error occurred while loading student data from the database.");
            alert.showAndWait();
        }
    }

    @Override
    protected void load_bindings() {
        // Setup sample data for the cluster table.
        studentIDColumn.setCellValueFactory(cell -> cell.getValue().studentIDProperty().asObject());
        firstNameColumn.setCellValueFactory(cell -> cell.getValue().firstNameProperty());
        middleNameColumn.setCellValueFactory(cell -> cell.getValue().middleNameProperty());
        lastNameColumn.setCellValueFactory(cell -> cell.getValue().lastNameProperty());
        nameExtensionColumn.setCellValueFactory(cell -> cell.getValue().nameExtensionProperty());
        clusterColumn.setCellValueFactory(cell -> cell.getValue().clusterIDProperty().getValue().clusterNameProperty());
        contactColumn.setCellValueFactory(cell -> cell.getValue().contactProperty());
        emailColumn.setCellValueFactory(cell -> cell.getValue().emailProperty());
        studentTableView.setItems(searchFilteredList);

        studentMenu = new ContextMenu();
        MenuItem viewMenu = new MenuItem("View Student Profile");
        viewMenu.setOnAction(e -> openStudentProfile());
        studentMenu.getItems().add(viewMenu);
        studentTableView.setContextMenu(studentMenu);

        // Configure the ModalPane (its content will be set when opening the form).
        formodal.setAlignment(Pos.TOP_CENTER);
        formodal.setVisible(false);
    }

    @Override
    protected void load_listeners() {
        // Add export menu item handlers
        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));
        
        // Add import handler
        importCsv.setOnAction(event -> handleImport());

        // Modified search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String searchText = newValue.toLowerCase();
            searchFilteredList.setPredicate(student -> {
                if (searchText == null || searchText.isEmpty()) {
                    return true;
                }
                // Search in multiple fields
                return student.getFirstName().toLowerCase().contains(searchText)
                        || student.getLastName().toLowerCase().contains(searchText)
                        || student.getMiddleName().toLowerCase().contains(searchText)
                        || student.getEmail().toLowerCase().contains(searchText)
                        || student.getContact().toLowerCase().contains(searchText)
                        || String.valueOf(student.getStudentID()).contains(searchText);
            });
            updateStatusLabel();
        });
    }

    public void updateYear(String year) {
        // No need to filter by year anymore
        initializeWithYear(year);
        updateStatusLabel();
    }

    public void initializeWithYear(String year) {
        // No need to filter by year anymore
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        int totalStudents = searchFilteredList.size();
        totalLabel.setText("Total Students: " + totalStudents);
    }

    @FXML
    private void openStudentProfile() {
        try {
            Student selectedStudent = studentTableView.getSelectionModel().getSelectedItem();
            if (selectedStudent == null) {
                return;
            }
            openStudentProfile(selectedStudent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openStudentProfile(Student student) {
        try {
            StudentProfileLoader loader = new StudentProfileLoader();
            loader.addParameter("SELECTED_STUDENT", student);
            loader.addParameter("OWNER_STAGE", studentTableView.getScene().getWindow());
            loader.load();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open student profile");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open student profile");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void closeModal() {
        formodal.hide();
    }

    private String getExportPath(String extension) {
        return System.getProperty("user.home") + "/Downloads/students_" +
                LocalDate.now().toString() + "." + extension;
    }

    private void handleExport(String type) {
        try {
            String title = "Student List Report";
            String outputPath = getExportPath(type.equals("excel") ? "xlsx" : type.toLowerCase());
            StudentTableExporter exporter = new StudentTableExporter();
            switch (type) {
                case "excel" -> exporter.exportToExcel(studentTableView, title, outputPath);
                case "pdf" -> exporter.exportToPdf(studentTableView, title, outputPath);
                case "csv" -> exporter.exportToCsv(studentTableView, title, outputPath);
            }
            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleImport() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select CSV File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

            File selectedFile = fileChooser.showOpenDialog(studentTableView.getScene().getWindow());
            if (selectedFile != null) {
                statusLabel.setText("Processing: " + selectedFile.getName());
                try {
                    List<CsvStudent> students = CsvImporter.importCsv(selectedFile);
                    int successCount = 0;

                    SchoolYear currentSchoolYear = (SchoolYear) App.COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR").stream()
                        .filter(sy -> sy instanceof SchoolYear)
                        .findFirst()
                        .orElse(null);

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

                    statusLabel.setText("Successfully imported " + successCount + " students");

                } catch (IOException e) {
                    statusLabel.setText("Error: Failed to read CSV file");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Import Error");
            alert.setHeaderText("Failed to import students");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}