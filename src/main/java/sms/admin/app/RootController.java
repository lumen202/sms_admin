package sms.admin.app;

import dev.sol.core.application.FXController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import sms.admin.app.student.StudentController;
import sms.admin.app.student.StudentLoader;
import sms.admin.util.ControllerLoader;
import sms.admin.util.SceneLoaderUtil;
import sms.admin.util.YearData;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;
import dev.sol.core.application.loader.FXLoader;

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
        // Initially load student.

        // Listen for changes to the yearComboBox.
        yearComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldYear, String newYear) {
                System.out.println("RootController: Year changed to " + newYear);
                if (currentController instanceof AttendanceController) {
                    ((AttendanceController) currentController).updateYear(newYear);
                } else if (currentController instanceof PayrollController) {
                    ((PayrollController) currentController).updateYear(newYear);
                }
            }
        });
    }

    @Override
    protected void load_fields() {
        yearComboBox.setItems(YearData.getYears()); // Use YearData instead
        yearComboBox.setValue(YearData.getCurrentAcademicYear()); // Use YearData instead
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
        attendanceButton.setStyle("");
        payrollButton.setStyle("");
        studentButton.setStyle("");
        enrollmentButton.setStyle("");  // Add this line
        button.setStyle("-fx-background-color: #ADD8E6;");
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

}
