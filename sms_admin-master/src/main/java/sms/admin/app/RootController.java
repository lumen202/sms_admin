package sms.admin.app;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import dev.finalproject.models.SchoolYear;
import dev.sol.core.application.FXController;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import sms.admin.app.attendance.AttendanceController;
import sms.admin.app.attendance.AttendanceLoader;
import sms.admin.app.enrollment.EnrollmentController;
import sms.admin.app.enrollment.EnrollmentLoader;
import sms.admin.app.payroll.PayrollController;
import sms.admin.app.payroll.PayrollLoader;
import sms.admin.app.schoolyear.SchoolYearDialog;
import sms.admin.app.student.StudentController;
import sms.admin.app.student.StudentLoader;
import sms.admin.util.SchoolYearUtil;
import sms.admin.util.mock.DataUtil;
import sms.admin.util.scene.SceneLoaderUtil;

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

    @FXML
    private void handleStudentButton() {
        highlightButton(studentButton);
        currentController = SceneLoaderUtil.loadScene(
            "/sms/admin/app/student/STUDENT.fxml",
            getClass(),
            StudentLoader.class,
            Map.of("selectedYear", yearComboBox.getValue()),
            contentPane
        );
    }

    @FXML
    private void handlePayrollButton() {  // Changed from handlesPayrollButton
        highlightButton(payrollButton);
        currentController = SceneLoaderUtil.loadScene(
            "/sms/admin/app/payroll/PAYROLL.fxml",
            getClass(),
            PayrollLoader.class,
            Map.of("selectedYear", yearComboBox.getValue()),
            contentPane
        );
    }

    @FXML
    private void handleAttendanceButton() {
        highlightButton(attendanceButton);
        try {
            currentController = SceneLoaderUtil.loadScene(
                "/sms/admin/app/attendance/ATTENDANCE.fxml",
                getClass(),
                AttendanceLoader.class,
                Map.of("selectedYear", yearComboBox.getValue()),
                contentPane
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleEnrollmentButton() {
        highlightButton(enrollmentButton);
        currentController = SceneLoaderUtil.loadScene(
            "/sms/admin/app/enrollment/ENROLLMENT.fxml",
            getClass(),
            EnrollmentLoader.class,
            Map.of("selectedYear", yearComboBox.getValue()),
            contentPane
        );
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
        // schoolYearList = App.COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR");
        schoolYearList = DataUtil.createSchoolYearList();
        yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

        // Set current year as default using findCurrentYear
        SchoolYear currentYear = SchoolYearUtil.findCurrentYear(schoolYearList);
        if (currentYear != null) {
            yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(currentYear));
        }

        // Load initial scene
        handleStudentButton();
    }

    @Override
    protected void load_listeners() {
        // Use setOnAction consistently for all buttons
        payrollButton.setOnAction(event -> handlePayrollButton());
        attendanceButton.setOnAction(event -> handleAttendanceButton());
        studentButton.setOnAction(event -> handleStudentButton());
        enrollmentButton.setOnAction(event -> handleEnrollmentButton());
        generateKeyMenuItem.setOnAction(event -> handleGenerateKeyMenuItem());
        // Remove the lookup calls since we're using FXML onAction
    }

    private void highlightButton(Button button) {
        // Reset all buttons first
        String defaultStyle = "-fx-background-color: #800000; -fx-text-fill: white;";
        Arrays.asList(attendanceButton, payrollButton, studentButton, enrollmentButton)
              .forEach(btn -> btn.setStyle(defaultStyle));
        // Highlight selected button
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

    private void setOverlayVisible(boolean visible) {
        if (contentPane.getScene() != null) {
            BorderPane root = (BorderPane) contentPane.getScene().getRoot();
            if (visible) {
                root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-opacity: 0.3;");
                contentPane.setDisable(true);
            } else {
                root.setStyle("");
                contentPane.setDisable(false);
            }
        }
    }

    @FXML
    private void handleNewSchoolYear() {
        setOverlayVisible(true);
        SchoolYearDialog dialog = new SchoolYearDialog(null);
        dialog.showAndWait().ifPresent(newSchoolYear -> {
            schoolYearList.add(newSchoolYear);
            yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(newSchoolYear));
        });
        setOverlayVisible(false);
    }

    @FXML
    private void handleEditSchoolYear() {
        setOverlayVisible(true);
        String currentYear = yearComboBox.getValue();
        SchoolYear selectedYear = schoolYearList.stream()
            .filter(sy -> SchoolYearUtil.formatSchoolYear(sy).equals(currentYear))
            .findFirst()
            .orElse(null);

        if (selectedYear != null) {
            SchoolYearDialog dialog = new SchoolYearDialog(selectedYear);
            dialog.showAndWait().ifPresent(updatedSchoolYear -> {
                int index = schoolYearList.indexOf(selectedYear);
                schoolYearList.set(index, updatedSchoolYear);
                yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(updatedSchoolYear));
            });
        }
        setOverlayVisible(false);
    }

    private void updateCurrentController(String newYear) {
        if (currentController == null) return;

        try {
            if (currentController instanceof AttendanceController) {
                ((AttendanceController) currentController).updateYear(newYear);
            } else if (currentController instanceof PayrollController) {
                ((PayrollController) currentController).updateYear(newYear);
            } else if (currentController instanceof StudentController) {
                ((StudentController) currentController).updateYear(newYear);
            } else if (currentController instanceof EnrollmentController) {
                ((EnrollmentController) currentController).updateYear(newYear);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
