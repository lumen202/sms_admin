package sms.admin.app.student;

import dev.finalproject.models.Cluster;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.YearData;
import atlantafx.base.controls.ModalPane;

public class StudentController extends FXController {

    @FXML
    private StackPane contentPane; // Container where scenes are loaded
    @FXML
    private ModalPane formodal; // ModalPane from AtlantaFX (defined in FXML without children)

    @FXML
    private TableView<Student> studentTableView;
    // @FXML
    // private TableColumn<Cluster, Integer> clusterID;
    // @FXML
    // private TableColumn<Cluster, String> clusterName;

    private ObservableList<Cluster> clusterMasterList;
    private ContextMenu studentMenu;

    @Override
    protected void load_fields() {
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
        clusterMasterList = FXCollections.observableArrayList();
        clusterMasterList.add(new Cluster(1, "Cluster A"));
        clusterMasterList.add(new Cluster(2, "Cluster B"));

        // clusterID.setCellValueFactory(cell ->
        // cell.getValue().clusterIDProperty().asObject());
        // clusterName.setCellValueFactory(cell ->
        // cell.getValue().clusterNameProperty());
        // clusterTableView.setItems(clusterMasterList);

        // Setup a sample context menu.
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
            // Create an instance of StudentFormLoader.
            StudentProfileLoader loader = (StudentProfileLoader) FXLoaderFactory
                    .createInstance(StudentProfileLoader.class,
                            getClass().getResource("/sms/admin/app/student/viewstudent/STUDENT_PROFILE.fxml"))
                    .addParameter("selectedYear", getParameter("selectedYear"))
                    .initialize(); // Loads the FXML into the loader
            loader.load();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Failed to open student profle modal");
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
