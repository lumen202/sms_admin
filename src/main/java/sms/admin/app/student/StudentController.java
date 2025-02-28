package sms.admin.app.student;

import dev.finalproject.App;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.YearData;
import atlantafx.base.controls.ModalPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
    private StackPane contentPane; // Container where scenes are loaded
    @FXML
    private ModalPane formodal; // ModalPane from AtlantaFX (defined in FXML without children)

    private ObservableList<Student> studentMasterList;
    private ContextMenu studentMenu;
    private ModalPane studentProfileModal;

    @Override
    protected void load_fields() {
        studentMasterList = App.COLLECTIONS_REGISTRY.getList("STUDENT");

        // Retrieve the "selectedYear" parameter passed from RootController.
        // If not set, use the current year (from YearData.getYears()).
        String selectedYear = (String) getParameter("selectedYear");
        if (selectedYear == null) {
            // YearData.getYears() returns a list with current year at index 0.
            selectedYear = YearData.getYears().get(0);
        }
        System.out.println("HomeController.load_fields: selectedYear = " + selectedYear);
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

    @FXML
    private void openStudentProfile() {
        try {
            Student selectedStudent = studentTableView.getSelectionModel().getSelectedItem();
            if (selectedStudent == null) {
                return;
            }

            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.initOwner(studentTableView.getScene().getWindow());

            // Use FXLoader as intended
            StudentProfileLoader loader = new StudentProfileLoader();
            loader.createInstance(getClass().getResource("/sms/admin/app/student/viewstudent/STUDENT_PROFILE.fxml"));
            loader.addParameter("OWNER_STAGE", stage);
            loader.addParameter("SELECTED_STUDENT", selectedStudent);

            // Let the loader handle everything else
            loader.initialize();
            loader.load();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Failed to open student profile: " + ex.getMessage());
        }
    }

    /**
     * Called via FXML (for example, from a "Close" button inside the modal) to hide
     * the student form.
     */
    @FXML
    private void closeModal() {
        formodal.hide();
    }

    /**
     * Example handler for editing a student from the context menu.
     */
}
