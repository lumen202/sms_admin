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
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.exporter.StudentTableExporter;
import sms.admin.util.YearData;
import sms.admin.util.mock.DataUtil;

import java.time.LocalDate;

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
    // @FXML
    // private TableColumn<Student, String> addressColumn;

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
    private TextField searchField;
    @FXML
    private Label totalLabel;
    @FXML
    private Label statusLabel;

    private ObservableList<Student> originalMasterList; // Add this field to store original list
    private FilteredList<Student> yearFilteredList;
    private FilteredList<Student> searchFilteredList;
    private ContextMenu studentMenu;

    @Override
    protected void load_fields() {
        // Initialize data
        originalMasterList = DataUtil.createStudentList();
        yearFilteredList = new FilteredList<>(originalMasterList);
        searchFilteredList = new FilteredList<>(yearFilteredList);

        // Set filtered items to table
        studentTableView.setItems(searchFilteredList);

        // Get selected year
        String selectedYear = (String) getParameter("selectedYear");
        if (selectedYear == null) {
            selectedYear = YearData.getCurrentAcademicYear();
        }

        // Apply initial filter
        initializeWithYear(selectedYear);

        // Configure modal panes
        formodal.setAlignment(Pos.TOP_CENTER);
        formodal.usePredefinedTransitionFactories(Side.TOP);
        formodal.setPersistent(true);
    }

    @Override
    protected void load_bindings() {
        // Setup sample data for the cluster table.

        studentIDColumn.setCellValueFactory(cell -> cell.getValue().studentIDProperty().asObject());
        firstNameColumn.setCellValueFactory(cell -> cell.getValue().firstNameProperty());
        middleNameColumn.setCellValueFactory(cell -> cell.getValue().middleNameProperty());
        lastNameColumn.setCellValueFactory(cell -> cell.getValue().lastNameProperty());
        nameExtensionColumn.setCellValueFactory(cell -> cell.getValue().nameExtensionProperty());
        contactColumn.setCellValueFactory(cell -> cell.getValue().contactProperty());
        emailColumn.setCellValueFactory(cell -> cell.getValue().emailProperty());
        clusterColumn.setCellValueFactory(cell -> cell.getValue().clusterIDProperty().getValue().clusterNameProperty());

        studentTableView.setItems(searchFilteredList);
        studentMenu = new ContextMenu();
        MenuItem editMenu = new MenuItem("Edit Student Profile");
        editMenu.setOnAction(e -> openStudentProfile());
        studentMenu.getItems().add(editMenu);
        studentTableView.setContextMenu(studentMenu);

        // Configure the ModalPane (its content will be set when opening the form).
        formodal.setAlignment(Pos.TOP_CENTER);
        formodal.setVisible(false);
    }

    @Override
    protected void load_listeners() {
        // When testButton is clicked, open the student form modal.

        // Add export menu item handlers
        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));

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
        initializeWithYear(year);
    }

    public void initializeWithYear(String year) {
        if (year == null || originalMasterList == null)
            return;

        // Convert year string to yearID (assuming year format is "2023-2024")
        final int startYear = Integer.parseInt(year.split("-")[0]);
        final int endYear = Integer.parseInt(year.split("-")[1]);
        System.out.println("Filtering students for years: " + startYear + "-" + endYear);

        // Update the year filter predicate
        yearFilteredList.setPredicate(student -> {
            if (student.getYearID() == null) {
                return false;
            }
            SchoolYear schoolYear = student.getYearID();
            return schoolYear != null &&
                    schoolYear.getYearStart() == startYear &&
                    schoolYear.getYearEnd() == endYear;
        });

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

            // Create new stage for profile
            Stage profileStage = new Stage(StageStyle.UNDECORATED);
            profileStage.initOwner(studentTableView.getScene().getWindow());

            // Load the profile with corrected resource path
            StudentProfileLoader loader = (StudentProfileLoader) FXLoaderFactory
                    .createInstance(StudentProfileLoader.class,
                            StudentProfileLoader.class.getResource("STUDENT_PROFILE.fxml"))
                    .addParameter("OWNER_STAGE", studentTableView.getScene().getWindow())
                    .addParameter("SELECTED_STUDENT", selectedStudent)
                    .initialize();

            // Let the loader handle scene creation and showing
            loader.load();

        } catch (Exception ex) {
            ex.printStackTrace();
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

}