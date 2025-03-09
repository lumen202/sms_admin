package sms.admin.app.student;

import atlantafx.base.controls.ModalPane;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.YearData;
import sms.admin.util.mock.DataUtil;

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

    private ObservableList<Student> studentMasterList;
    private ObservableList<Student> originalMasterList; // Add this field to store original list
    private ContextMenu studentMenu;

    @Override
    protected void load_fields() {
        // Store original master list
        // originalMasterList = App.COLLECTIONS_REGISTRY.getList("STUDENT");
        originalMasterList = DataUtil.createStudentList();
        studentMasterList = originalMasterList; // Initial reference

        // Retrieve the "selectedYear" parameter passed from RootController.
        // If not set, use the current year (from YearData.getYears()).
        String selectedYear = (String) getParameter("selectedYear");
        if (selectedYear == null) {
            // YearData.getYears() returns a list with current year at index 0.
            selectedYear = YearData.getYears().get(0);
        }
        System.out.println("HomeController.load_fields: selectedYear = " + selectedYear);

        // Apply initial filter based on selected year
        updateYear(selectedYear);

        // Configure modal panes like the example
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

        studentTableView.setItems(studentMasterList);
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

    }

    public void updateYear(String year) {
        if (year == null || originalMasterList == null)
            return;

        // Convert year string to yearID (assuming year format is "2023-2024")
        final int yearID = Integer.parseInt(year.split("-")[0]);

        // Filter students based on yearID
        studentTableView.setItems(originalMasterList.filtered(student -> {
            if (student.getYearID() == null)
                return false;
            SchoolYear schoolYear = student.getYearID();
            return schoolYear != null && schoolYear.getYearID() == yearID;
        }));
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

}