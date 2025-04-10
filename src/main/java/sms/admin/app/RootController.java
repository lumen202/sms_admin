package sms.admin.app;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import dev.finalproject.database.DataManager;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
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
import javafx.stage.DirectoryChooser;
import sms.admin.app.attendance.AttendanceController;
import sms.admin.app.attendance.AttendanceLoader;
import sms.admin.app.enrollment.EnrollmentController;
import sms.admin.app.payroll.PayrollController;
import sms.admin.app.payroll.PayrollLoader;
import sms.admin.app.schoolyear.SchoolYearDialog;
import sms.admin.app.student.StudentController;
import sms.admin.app.student.StudentLoader;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.datetime.SchoolYearUtil;
import sms.admin.util.scene.SceneLoaderUtil;

public class RootController extends FXController {

    @FXML
    private Button attendanceButton;
    @FXML
    private Button payrollButton;
    @FXML
    private Button studentButton;
    @FXML
    private MenuItem generateKeyMenuItem;
    @FXML
    private StackPane contentPane;
    @FXML
    private ComboBox<String> yearComboBox;
    @FXML
    private Scene scene;
    @FXML
    private MenuItem newSchoolYearMenuItem;
    @FXML
    private MenuItem editSchoolYearMenuItem;

    private ObservableList<SchoolYear> schoolYearList;
    private String selectedMonth;
    private FXController currentController;

    @Override
    protected void load_fields() {
        // Use DataManager to obtain the shared SCHOOL_YEAR collection
        schoolYearList = FXCollections.observableArrayList(
                DataManager.getInstance().getCollectionsRegistry().getList("SCHOOL_YEAR"));
        yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

        selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth == null) {
            YearMonth current = YearMonth.now();
            selectedMonth = current.format(DateTimeUtils.MONTH_YEAR_FORMATTER);
        }
        System.out.println("RootController initialized with month: " + selectedMonth);

        String initialYear = (String) getParameter("selectedYear");
        if (initialYear == null) {
            SchoolYear currentYear = SchoolYearUtil.findCurrentYear(schoolYearList);
            initialYear = currentYear != null ? SchoolYearUtil.formatSchoolYear(currentYear)
                    : yearComboBox.getItems().get(0);
        }
        yearComboBox.setValue(initialYear);
        handleStudentButton(); // Default view
    }

    @Override
    protected void load_bindings() {
        scene = (Scene) getParameter("scene");
    }

    @Override
    protected void load_listeners() {
        yearComboBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (newYear != null && !newYear.equals(oldYear)) {
                System.out.println("RootController: Year changed to " + newYear);
                updateCurrentController(newYear);
            }
        });

        attendanceButton.setOnAction(event -> handleAttendanceButton());
        payrollButton.setOnAction(event -> handlePayrollButton());
        studentButton.setOnAction(event -> handleStudentButton());
        generateKeyMenuItem.setOnAction(event -> handleGenerateKeyMenuItem());
        newSchoolYearMenuItem.setOnAction(event -> handleNewSchoolYear());
        editSchoolYearMenuItem.setOnAction(event -> handleEditSchoolYear());
    }

    @FXML
    private void handleStudentButton() {
        highlightButton(studentButton);
        Map<String, Object> params = new HashMap<>();
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", selectedMonth);
        DataManager.getInstance().refreshData();
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

    @FXML
    private void handlePayrollButton() {
        highlightButton(payrollButton);
        Map<String, Object> params = new HashMap<>();
        String currentMonth = getCurrentControllerMonth();
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", currentMonth);
        DataManager.getInstance().refreshData();

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
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", currentMonth);
        DataManager.getInstance().refreshData();

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
                    if (finalMonth != null) {
                        controller.setSelectedMonth(finalMonth);
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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

    private void highlightButton(Button button) {
        String defaultStyle = "-fx-background-color: #800000; -fx-text-fill: white;";
        Arrays.asList(attendanceButton, payrollButton, studentButton)
                .forEach(btn -> btn.setStyle(defaultStyle));
        button.setStyle("-fx-background-color: #ADD8E6; -fx-text-fill: black;");
    }

    private List<Student> getStudentsForYear(String schoolYear) {
        ObservableList<?> rawList = DataManager.getInstance()
                .getCollectionsRegistry()
                .getList("STUDENT");

        // Parse the year string (e.g., "2024-2025")
        int startYear = Integer.parseInt(schoolYear.split("-")[0]);

        return rawList.stream()
                .filter(obj -> obj instanceof Student)
                .map(obj -> (Student) obj)
                .filter(student -> student.getYearID() != null
                        && student.getYearID().getYearStart() == startYear
                        && student.isDeleted() == 0) // Add filter for non-deleted students
                .toList();
    }

    // Updated generateQRCode method with student name parameter
    private void generateQRCode(String data, String filePath, int width, int height, String studentName)
            throws WriterException, IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            // Create image with space for text (30px extra height)
            int textHeight = 40;
            BufferedImage qrImage = new BufferedImage(width, height + textHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = qrImage.createGraphics();

            // Draw white background
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height + textHeight);

            // Draw QR code
            graphics.setColor(Color.BLACK);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }

            // Draw student name
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics metrics = graphics.getFontMetrics();
            int x = (width - metrics.stringWidth(studentName)) / 2;
            int y = height + (textHeight - metrics.getHeight()) / 2 + metrics.getAscent();
            graphics.drawString(studentName, x, y);

            // Save image
            ImageIO.write(qrImage, "PNG", new File(filePath));
            graphics.dispose();
        } catch (IOException e) {
            throw new IOException("Failed to write QR code to file: " + e.getMessage(), e);
        }
    }

    // Updated handleGenerateKeyMenuItem with proper method call
    @FXML
    private void handleGenerateKeyMenuItem() {
        try {
            // Show directory chooser using contentPane's window
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose Directory to Save QR Codes");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDirectory = directoryChooser.showDialog(contentPane.getScene().getWindow());

            if (selectedDirectory == null) {
                return; // User cancelled directory selection
            }

            String currentYear = yearComboBox.getValue();
            List<Student> students = getStudentsForYear(currentYear);
            byte[] fixedKey = "MySuperSecretKey".getBytes();
            SecretKey secretKey = new SecretKeySpec(fixedKey, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Create QR codes directory inside selected directory
            File qrDir = new File(selectedDirectory, "qr_codes");
            qrDir.mkdirs();

            for (Student student : students) {
                String dataToEncrypt = student.getStudentID() + "|" + currentYear;
                byte[] encryptedBytes = cipher.doFinal(dataToEncrypt.getBytes());
                String encryptedKey = Base64.getEncoder().encodeToString(encryptedBytes);

                String cleanName = student.getFullName()
                        .replaceAll("[^a-zA-Z0-9]", "_")
                        .replaceAll("\\s+", "_");

                File qrFile = new File(qrDir, cleanName + ".png");
                generateQRCode(encryptedKey, qrFile.getAbsolutePath(), 300, 300, student.getFullName());

                System.out.println("Generated for: " + student.getFullName() +
                        " (" + qrFile.getAbsolutePath() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String decryptStudentKey(String encryptedKey) {
        try {
            byte[] fixedKey = "MySuperSecretKey".getBytes();
            SecretKey secretKey = new SecretKeySpec(fixedKey, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
            String decryptedString = new String(decryptedBytes);

            String[] parts = decryptedString.split("\\|");
            int studentId = Integer.parseInt(parts[0]);
            Student student = findStudentByStudentId(studentId);

            String studentInfo = student != null
                    ? String.format("Student: %s (Student ID: %d)", student.getFullName(), studentId)
                    : "Unknown Student (Student ID: " + studentId + ")";

            return studentInfo + ", School Year: " + parts[1];
        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid key";
        }
    }

    private static Student findStudentByStudentId(int studentId) {
        return DataManager.getInstance()
                .getCollectionsRegistry()
                .getList("STUDENT")
                .stream()
                .filter(obj -> obj instanceof Student)
                .map(obj -> (Student) obj)
                .filter(student -> student.getStudentID() == studentId)
                .findFirst()
                .orElse(null);
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

        System.out.println("Opening new school year dialog...");

        dialog.showAndWait().ifPresent(newSchoolYear -> {
            System.out.println("Dialog returned school year: " + newSchoolYear);
            if (newSchoolYear != null) {
                // Ensure UI updates happen on JavaFX thread
                Platform.runLater(() -> {
                    System.out.println("Refreshing data from database...");
                    DataManager.getInstance().refreshData();
                    List<SchoolYear> updatedList = DataManager.getInstance().getCollectionsRegistry()
                            .getList("SCHOOL_YEAR");
                    System.out.println("Found " + updatedList.size() + " school years");

                    schoolYearList.clear();
                    schoolYearList.addAll(updatedList);

                    ObservableList<String> formattedYears = SchoolYearUtil.convertToStringList(schoolYearList);
                    System.out.println("Formatted years: " + formattedYears);
                    yearComboBox.setItems(formattedYears);

                    String formattedNewYear = SchoolYearUtil.formatSchoolYear(newSchoolYear);
                    System.out.println("Setting combo box to: " + formattedNewYear);
                    yearComboBox.setValue(formattedNewYear);

                    // Verify the value was set
                    System.out.println("Current combo box value: " + yearComboBox.getValue());

                    updateCurrentController(formattedNewYear);
                });
            }
        });

        setOverlayVisible(false);
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
                        // Update the shared collection via DataManager
                        DataManager.getInstance().getCollectionsRegistry().register("SCHOOL_YEAR", schoolYearList);
                        yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(updatedSchoolYear));
                        updateCurrentController(yearComboBox.getValue());
                    }
                });
            }
        } finally {
            setOverlayVisible(false);
        }
    }

    private void updateCurrentController(String newYear) {
        if (currentController == null) {
            return;
        }
        if (currentController instanceof PayrollController controller) {
            controller.initializeWithYear(newYear);
            String currentMonth = getCurrentControllerMonth();
            if (currentMonth != null) {
                controller.setSelectedMonth(currentMonth);
            }
        } else if (currentController instanceof AttendanceController controller) {
            controller.initializeWithYear(newYear);
            String currentMonth = getCurrentControllerMonth();
            if (currentMonth != null) {
                controller.setSelectedMonth(currentMonth);
            }
        } else if (currentController instanceof StudentController controller) {
            controller.initializeWithYear(newYear);
        } else if (currentController instanceof EnrollmentController controller) {
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
