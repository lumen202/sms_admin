package sms.admin.app;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import dev.finalproject.App;
import dev.finalproject.models.SchoolYear;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
    private String selectedMonth;
    private FXController currentController; // Add this field

    @FXML
    private MenuItem newSchoolYearMenuItem;
    @FXML
    private MenuItem editSchoolYearMenuItem;
  

    @FXML
    private void handleStudentButton() {
        highlightButton(studentButton);
        Map<String, Object> params = new HashMap<>();
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", selectedMonth);

        currentController = SceneLoaderUtil.loadScene(
                "/sms/admin/app/student/STUDENT.fxml",
                getClass(),
                StudentLoader.class,
                params,
                contentPane);
        if (currentController instanceof StudentController controller) {
            controller.initializeWithYear(yearComboBox.getValue());
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
        String currentMonth = getCurrentControllerMonth();
        
        if (currentMonth == null) {
            currentMonth = selectedMonth;
        }
        
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", currentMonth);
        
        // Get attendance logs from current attendance controller if it exists
        if (currentController instanceof AttendanceController attendanceController) {
            params.put("attendanceLogs", attendanceController.getAttendanceLogs());
        }

        currentController = SceneLoaderUtil.loadScene(
                "/sms/admin/app/payroll/PAYROLL.fxml",
                getClass(),
                PayrollLoader.class,
                params,
                contentPane);
                
        if (currentController instanceof PayrollController controller) {
            controller.initializeWithYear(yearComboBox.getValue());
            if (currentMonth != null) {
                controller.setSelectedMonth(currentMonth);
            }
        }
    }

    @FXML
    private void handleAttendanceButton() {
        highlightButton(attendanceButton);
        Map<String, Object> params = new HashMap<>();
        String currentMonth = getCurrentControllerMonth();
        
        if (currentMonth == null) {
            currentMonth = selectedMonth;
        }
        
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", currentMonth);

        try {
            currentController = SceneLoaderUtil.loadScene(
                    "/sms/admin/app/attendance/ATTENDANCE.fxml",
                    getClass(),
                    AttendanceLoader.class,
                    params,
                    contentPane);
                    
            if (currentController instanceof AttendanceController controller) {
                String finalMonth = currentMonth;
                Platform.runLater(() -> {
                    controller.initializeWithYear(yearComboBox.getValue());
                    controller.setSelectedMonth(finalMonth);
                });
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

        yearComboBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (newYear != null && !newYear.equals(oldYear)) {
                System.out.println("RootController: Year changed to " + newYear);
                if (currentController != null) {
                    updateCurrentController(newYear);
                }
            }
        });
    }

    @Override
    protected void load_fields() {
        schoolYearList = FXCollections.observableArrayList(App.COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR"));
        
        yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

        selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth == null) {
            YearMonth current = YearMonth.now();
            selectedMonth = current.format(DateTimeUtils.MONTH_YEAR_FORMATTER);
        }
        System.out.println("RootController initialized with month: " + selectedMonth);

        SchoolYear currentYear = SchoolYearUtil.findCurrentYear(schoolYearList);
        if (currentYear != null) {
            String yearString = SchoolYearUtil.formatSchoolYear(currentYear);
            yearComboBox.setValue(yearString);
            handleStudentButton();
        }
    }

    @Override
    protected void load_listeners() {
        yearComboBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (newYear != null && !newYear.equals(oldYear)) {
                System.out.println("RootController: Year changed to " + newYear);
                updateCurrentController(newYear);
            }
        });

        payrollButton.setOnAction(event -> handlePayrollButton());
        attendanceButton.setOnAction(event -> handleAttendanceButton());
        studentButton.setOnAction(event -> handleStudentButton());
        enrollmentButton.setOnAction(event -> handleEnrollmentButton());
        generateKeyMenuItem.setOnAction(event -> handleGenerateKeyMenuItem());

        // Use FXML injected menu items directly
        if (newSchoolYearMenuItem != null) {
            newSchoolYearMenuItem.setOnAction(event -> handleNewSchoolYear());
        }
        
        if (editSchoolYearMenuItem != null) {
            editSchoolYearMenuItem.setOnAction(event -> handleEditSchoolYear());
        }
    }

    private void highlightButton(Button button) {
        String defaultStyle = "-fx-background-color: #800000; -fx-text-fill: white;";
        Arrays.asList(attendanceButton, payrollButton, studentButton, enrollmentButton)
                .forEach(btn -> btn.setStyle(defaultStyle));
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
    public void handleNewSchoolYear() {
        setOverlayVisible(true);
        try {
            SchoolYearDialog dialog = new SchoolYearDialog();
            dialog.showAndWait().ifPresent(newSchoolYear -> {
                if (newSchoolYear != null) {
                    schoolYearList.add(newSchoolYear);
                    App.COLLECTIONS_REGISTRY.register("SCHOOL_YEAR", schoolYearList);
                    yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(newSchoolYear));
                }
            });
        } finally {
            setOverlayVisible(false);
        }
    }

    @FXML
    public void handleEditSchoolYear() {
        setOverlayVisible(true);
        try {
            String currentYear = yearComboBox.getValue();
            SchoolYear selectedYear = schoolYearList.stream()
                    .filter(sy -> SchoolYearUtil.formatSchoolYear(sy).equals(currentYear))
                    .findFirst()
                    .orElse(null);

            if (selectedYear != null) {
                SchoolYearDialog dialog = new SchoolYearDialog(selectedYear);
                dialog.showAndWait().ifPresent(updatedSchoolYear -> {
                    if (updatedSchoolYear != null) {
                        int index = schoolYearList.indexOf(selectedYear);
                        schoolYearList.set(index, updatedSchoolYear);
                        App.COLLECTIONS_REGISTRY.register("SCHOOL_YEAR", schoolYearList);
                        yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(updatedSchoolYear));
                    }
                });
            }
        } finally {
            setOverlayVisible(false);
        }
    }

    private void updateCurrentController(String newYear) {
        if (currentController instanceof PayrollController controller) {
            controller.initializeWithYear(newYear);
        } else if (currentController instanceof AttendanceController controller) {
            controller.initializeWithYear(newYear);
        }
    }

    public void setSelectedMonth(String monthYear) {
        if (monthYear != null && !monthYear.equals(this.selectedMonth)) {
            this.selectedMonth = monthYear;
            
            Platform.runLater(() -> {
                if (currentController instanceof AttendanceController controller) {
                    controller.setSelectedMonth(monthYear);
                } else if (currentController instanceof PayrollController controller) {
                    controller.setSelectedMonth(monthYear);
                }
            });
        }
    }
}
