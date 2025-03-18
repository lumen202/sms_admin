package sms.admin.app;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.time.YearMonth;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import dev.finalproject.App;
import dev.finalproject.models.SchoolYear;
import dev.sol.core.application.FXController;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
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
import sms.admin.util.SchoolYearUtil;
import sms.admin.util.datetime.DateTimeUtils;
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

    private String selectedMonth; // Change field name

    @FXML
    private void handleStudentButton() {
        highlightButton(studentButton);
        String selectedYear = yearComboBox.getValue();
        currentController = SceneLoaderUtil.loadScene(
                "/sms/admin/app/student/STUDENT.fxml",
                getClass(),
                StudentLoader.class,
                Map.of("selectedYear", selectedYear),
                contentPane);
        if (currentController instanceof StudentController controller) {
            controller.initializeWithYear(selectedYear);
        }
    }

    private String getCurrentControllerMonth() {
        if (currentController instanceof AttendanceController controller) {
            return controller.getSelectedMonth();
        } else if (currentController instanceof PayrollController controller) {
            return controller.getSelectedMonth();
        }
        return selectedMonth;
    }

    @FXML
    private void handlePayrollButton() {
        highlightButton(payrollButton);
        Map<String, Object> params = new HashMap<>();
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", getCurrentControllerMonth());

        currentController = SceneLoaderUtil.loadScene(
                "/sms/admin/app/payroll/PAYROLL.fxml",
                getClass(),
                PayrollLoader.class,
                params,
                contentPane);
                
        if (currentController instanceof PayrollController controller) {
            controller.initializeWithYear(yearComboBox.getValue());
        }
    }

    @FXML
    private void handleAttendanceButton() {
        highlightButton(attendanceButton);
        Map<String, Object> params = new HashMap<>();
        String currentMonth = getCurrentControllerMonth();
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", currentMonth != null ? currentMonth : selectedMonth);

        try {
            currentController = SceneLoaderUtil.loadScene(
                    "/sms/admin/app/attendance/ATTENDANCE.fxml",
                    getClass(),
                    AttendanceLoader.class,
                    params,
                    contentPane);
                    
            if (currentController instanceof AttendanceController controller) {
                controller.initializeWithYear(yearComboBox.getValue());
                if (currentMonth != null) {
                    controller.setSelectedMonth(currentMonth);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleEnrollmentButton() {
        highlightButton(enrollmentButton);
        String selectedYear = yearComboBox.getValue();
        currentController = SceneLoaderUtil.loadScene(
                "/sms/admin/app/enrollment/ENROLLMENT.fxml",
                getClass(),
                EnrollmentLoader.class,
                Map.of("selectedYear", selectedYear),
                contentPane);
        if (currentController instanceof EnrollmentController controller) {
            controller.initializeWithYear(selectedYear);
        }
    }

    @Override
    protected void load_bindings() {
        scene = (Scene) getParameter("scene");

        // Update year listener
        yearComboBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (newYear != null && !newYear.equals(oldYear)) {
                System.out.println("RootController: Year changed to " + newYear);
                reloadCurrentScene();
            }
        });
    }

    @Override
    protected void load_fields() {
        // Initialize school year list
        schoolYearList =App.COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR");
        yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

        // Set initial selected month
        selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth == null) {
            selectedMonth = YearMonth.now().format(DateTimeUtils.MONTH_YEAR_FORMATTER);
        }

        // Set current year as default
        SchoolYear currentYear = SchoolYearUtil.findCurrentYear(schoolYearList);
        if (currentYear != null) {
            String yearString = SchoolYearUtil.formatSchoolYear(currentYear);
            yearComboBox.setValue(yearString);

            // Load initial scene with selected year
            handleStudentButton();
        }
    }

    @Override
    protected void load_listeners() {
        // Update year listener with immediate refresh
        yearComboBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (newYear != null && !newYear.equals(oldYear)) {
                System.out.println("RootController: Year changed to " + newYear);
                updateCurrentController(newYear);
            }
        });

        // Rest of the listeners...
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
        if (currentController != null) {
            if (currentController instanceof StudentController controller) {
                controller.initializeWithYear(newYear);
            } else if (currentController instanceof PayrollController controller) {
                controller.initializeWithYear(newYear);
            } else if (currentController instanceof AttendanceController controller) {
                controller.initializeWithYear(newYear);
            } else if (currentController instanceof EnrollmentController controller) {
                controller.initializeWithYear(newYear);
            }
        }
    }

    private void reloadCurrentScene() {
        if (currentController == null)
            return;

        switch (currentController) {
            case StudentController ignored -> handleStudentButton();
            case PayrollController ignored -> handlePayrollButton();
            case AttendanceController ignored -> handleAttendanceButton();
            case EnrollmentController ignored -> handleEnrollmentButton();
            default -> {
            }
        }
    }

    // Add method with correct name to match what's being called
    public void setSelectedMonth(String monthYear) {
        this.selectedMonth = monthYear;
        // Update current controller if it's attendance or payroll
        if (currentController instanceof AttendanceController controller) {
            controller.setSelectedMonth(monthYear);
        } else if (currentController instanceof PayrollController controller) {
            controller.setSelectedMonth(monthYear);
        }
    }

}
