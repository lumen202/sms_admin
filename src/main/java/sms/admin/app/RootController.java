package sms.admin.app;

import dev.finalproject.App;
import dev.finalproject.models.SchoolYear;
import dev.sol.core.application.FXController;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import sms.admin.app.attendance.AttendanceController;
import sms.admin.app.attendance.AttendanceLoader;
import sms.admin.app.enrollment.EnrollmentController;
import sms.admin.app.enrollment.EnrollmentLoader;
import sms.admin.app.payroll.PayrollController;
import sms.admin.app.payroll.PayrollLoader;
import sms.admin.app.schoolyear.SchoolYearDialog;
import sms.admin.app.student.StudentController;
import sms.admin.app.student.StudentLoader;
import sms.admin.util.ControllerLoader;
import sms.admin.util.SceneLoaderUtil;
import sms.admin.util.SchoolYearUtil;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;
import dev.sol.core.application.loader.FXLoader;

// Add these imports

public class RootController extends FXController {

    @FXML
    private Button attendanceButton;
    @FXML
    private Button payrollButton;
    @FXML
    private Button studentButton;
    @FXML
    private Button enrollmentButton;
    @FXML
    private MenuItem generateKeyMenuItem;
    @FXML
    private StackPane contentPane;

    @FXML
    private ComboBox<String> yearComboBox;
    @FXML
    private Scene scene;

    private ObservableList<SchoolYear> schoolYearList;

    // Store the active controller (e.g., PayrollController or AttendanceController)
    private FXController currentController;

    // Generic method that wraps the utility call.
    private <L extends FXLoader & ControllerLoader<C>, C extends FXController> void loadSceneWithYear(
            String fxmlPath, Class<L> loaderClass, Class<C> controllerClass) {
        // Use this class (RootController.class) as the base to load the resource.
        C controller = SceneLoaderUtil.loadSceneWithYear(fxmlPath, getClass(), loaderClass, yearComboBox.getValue(),
                contentPane);
        currentController = controller;
    }

    @FXML
    private void handleStudentButton() {
        highlightButton(studentButton);
        loadSceneWithYear("/sms/admin/app/student/STUDENT.fxml",
                StudentLoader.class,
                StudentController.class);
    }

    @FXML
    private void handlesPayrollButton() {
        highlightButton(payrollButton);
        loadSceneWithYear("/sms/admin/app/payroll/PAYROLL.fxml",
                PayrollLoader.class,
                PayrollController.class);
    }

    @FXML
    private void handleAttendanceButton() {
        highlightButton(attendanceButton);
        loadSceneWithYear("/sms/admin/app/attendance/ATTENDANCE.fxml",
                AttendanceLoader.class,
                AttendanceController.class);
    }

    @FXML
    private void handleEnrollmentButton() {
        highlightButton(enrollmentButton);
        loadSceneWithYear("/sms/admin/app/enrollment/ENROLLMENT.fxml",
                EnrollmentLoader.class,
                EnrollmentController.class);
    }

    @Override
    protected void load_bindings() {
        scene = (Scene) getParameter("scene");

        // Update year listener
        yearComboBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (newYear != null) {
                System.out.println("RootController: Year changed to " + newYear);
                updateCurrentController(newYear);
            }
        });
    }

    @Override
    protected void load_fields() {
        schoolYearList = App.COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR");
        yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

        // Set current year as default using findCurrentYear
        SchoolYear currentYear = SchoolYearUtil.findCurrentYear(schoolYearList);
        if (currentYear != null) {
            yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(currentYear));
        }

        loadSceneWithYear("/sms/admin/app/student/STUDENT.fxml",
                StudentLoader.class,
                StudentController.class);
    }

    @Override
    protected void load_listeners() {
        payrollButton.setOnAction(event -> handlesPayrollButton());
        attendanceButton.setOnAction(event -> handleAttendanceButton());
        studentButton.setOnMouseClicked(event -> handleStudentButton());
        enrollmentButton.setOnAction(event -> handleEnrollmentButton());
        generateKeyMenuItem.setOnAction(event -> handleGenerateKeyMenuItem());
    }

    private void highlightButton(Button button) {
        String defaultStyle = "-fx-background-color: #800000; -fx-text-fill: white;";
        attendanceButton.setStyle(defaultStyle);
        payrollButton.setStyle(defaultStyle);
        studentButton.setStyle(defaultStyle);
        enrollmentButton.setStyle(defaultStyle);
        button.setStyle("-fx-background-color: #ADD8E6; -fx-text-fill: black;");
    }

    @FXML
    private void handleGenerateKeyMenuItem() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();
            byte[] encodedKey = secretKey.getEncoded();
            String encryptedKey = Base64.getEncoder().encodeToString(encodedKey);
            System.out.println("Generated Encrypted Key: " + encryptedKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewSchoolYear() {
        SchoolYearDialog dialog = new SchoolYearDialog(null);
        dialog.showAndWait().ifPresent(newSchoolYear -> {
            // Add to database
            // Add to schoolYearList
            schoolYearList.add(newSchoolYear);
            yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(newSchoolYear));
        });
    }

    @FXML
    private void handleEditSchoolYear() {
        String currentYear = yearComboBox.getValue();
        SchoolYear selectedYear = schoolYearList.stream()
            .filter(sy -> SchoolYearUtil.formatSchoolYear(sy).equals(currentYear))
            .findFirst()
            .orElse(null);

        if (selectedYear != null) {
            SchoolYearDialog dialog = new SchoolYearDialog(selectedYear);
            dialog.showAndWait().ifPresent(updatedSchoolYear -> {
                // Update in database
                // Update in list
                int index = schoolYearList.indexOf(selectedYear);
                schoolYearList.set(index, updatedSchoolYear);
                yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(updatedSchoolYear));
            });
        }
    }

    private void updateCurrentController(String newYear) {
        if (currentController instanceof AttendanceController) {
            ((AttendanceController) currentController).updateYear(newYear);
        } else if (currentController instanceof PayrollController) {
            ((PayrollController) currentController).updateYear(newYear);
        } else if (currentController instanceof StudentController) {
            ((StudentController) currentController).updateYear(newYear);
        } else if (currentController instanceof EnrollmentController) {
            ((EnrollmentController) currentController).updateYear(newYear);
        }
    }

}
