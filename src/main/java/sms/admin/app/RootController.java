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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import sms.admin.app.attendance.AttendanceController;
import sms.admin.app.attendance.AttendanceLoader;
import sms.admin.app.deleted_student.DeletedStudentLoader;
import sms.admin.app.payroll.PayrollController;
import sms.admin.app.payroll.PayrollLoader;
import sms.admin.app.schoolyear.SchoolYearDialog;
import sms.admin.app.student.StudentController;
import sms.admin.app.student.StudentLoader;
import sms.admin.app.student.enrollment.EnrollmentController;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.datetime.SchoolYearUtil;
import sms.admin.util.scene.SceneLoaderUtil;

/**
 * Controller for the root view of the application, managing navigation and
 * school year selection. This class handles the UI elements and logic for
 * switching between attendance, payroll, and student views, generating QR codes
 * for students, and managing school year data.
 */
public class RootController extends FXController {

    @FXML
    private Button attendanceButton; // Button to navigate to attendance view
    @FXML
    private Button payrollButton; // Button to navigate to payroll view
    @FXML
    private Button studentButton; // Button to navigate to student view
    @FXML
    private MenuItem generateKeyMenuItem; // Menu item to generate QR codes
    @FXML
    private StackPane contentPane; // Container for loading different views
    @FXML
    private ComboBox<String> yearComboBox; // ComboBox for selecting school year
    @FXML
    private Scene scene; // The main application scene
    @FXML
    private MenuItem newSchoolYearMenuItem; // Menu item to create a new school year
    @FXML
    private MenuItem editSchoolYearMenuItem; // Menu item to edit the current school year
    @FXML
    private MenuItem deletedStudentMenuItem; // Menu item to view deleted students

    private ObservableList<SchoolYear> schoolYearList; // List of school years
    private String selectedMonth; // Selected month for filtering data
    private FXController currentController; // The currently active controller

    /**
     * Loads the initial fields and configurations for the root view.
     */
    @Override
    protected void load_fields() {
        // Initialize school year list from DataManager and sort in descending order
        schoolYearList = FXCollections.observableArrayList(
                DataManager.getInstance().getCollectionsRegistry().getList("SCHOOL_YEAR"));

        // Sort by both yearStart and yearEnd in descending order
        FXCollections.sort(schoolYearList, (sy1, sy2) -> {
            int startYearCompare = Integer.compare(sy2.getYearStart(), sy1.getYearStart());
            if (startYearCompare != 0) {
                return startYearCompare;
            }
            return Integer.compare(sy2.getYearEnd(), sy1.getYearEnd());
        });

        yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

        // Set selected month, defaulting to current month if not provided
        selectedMonth = (String) getParameter("selectedMonth");
        if (selectedMonth == null) {
            YearMonth current = YearMonth.now();
            selectedMonth = current.format(DateTimeUtils.MONTH_YEAR_FORMATTER);
        }
        System.out.println("RootController initialized with month: " + selectedMonth);

        // Set initial school year if provided as parameter, otherwise set current year
        // if available
        String initialYear = (String) getParameter("selectedYear");
        if (initialYear != null) {
            yearComboBox.setValue(initialYear);
        } else if (!schoolYearList.isEmpty()) {
            // Find the current school year based on today's date
            int currentYear = YearMonth.now().getYear();
            SchoolYear currentSchoolYear = schoolYearList.stream()
                    .filter(sy -> sy.getYearStart() <= currentYear && sy.getYearEnd() >= currentYear)
                    .findFirst()
                    .orElseGet(() -> schoolYearList.get(0)); // Default to most recent since list is sorted
            yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(currentSchoolYear));
        }
        handleStudentButton(); // Load student view by default
    }

    /**
     * Loads bindings for UI components.
     */
    @Override
    protected void load_bindings() {
        scene = (Scene) getParameter("scene");
    }

    /**
     * Loads event listeners for UI interactions.
     */
    @Override
    protected void load_listeners() {
        // Update controller when school year changes
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
        deletedStudentMenuItem.setOnAction(event -> handleDeletedStudentMenuItem());
    }

    /**
     * Handles navigation to the student view.
     */
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

    /**
     * Handles navigation to the payroll view.
     */
    @FXML
    private void handlePayrollButton() {
        highlightButton(payrollButton);
        Map<String, Object> params = new HashMap<>();
        String currentMonth = getCurrentControllerMonth();
        params.put("selectedYear", yearComboBox.getValue());
        params.put("selectedMonth", currentMonth);
        DataManager.getInstance().refreshData();

        // Pass attendance logs if coming from attendance view
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

    /**
     * Handles navigation to the attendance view.
     */
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

    /**
     * Retrieves the currently selected month from the active controller.
     *
     * @return The selected month, or the default selected month if not
     *         available.
     */
    private String getCurrentControllerMonth() {
        if (currentController instanceof AttendanceController controller) {
            return controller.getSelectedMonth();
        } else if (currentController instanceof PayrollController controller) {
            return controller.getSelectedMonth();
        }
        return selectedMonth;
    }

    /**
     * Highlights the selected navigation button and resets others.
     *
     * @param button The button to highlight.
     */
    private void highlightButton(Button button) {
        String defaultStyle = "-fx-background-color: #800000; -fx-text-fill: white;";
        Arrays.asList(attendanceButton, payrollButton, studentButton)
                .forEach(btn -> btn.setStyle(defaultStyle));
        button.setStyle("-fx-background-color: #ADD8E6; -fx-text-fill: black;");
    }

    /**
     * Resets the highlighting of all navigation buttons to their default state.
     */
    private void resetButtonHighlights() {
        String defaultStyle = "-fx-background-color: #800000; -fx-text-fill: white;";
        Arrays.asList(attendanceButton, payrollButton, studentButton)
                .forEach(btn -> btn.setStyle(defaultStyle));
    }

    /**
     * Retrieves the list of students for the specified school year who are not
     * deleted.
     *
     * @param schoolYear The school year to filter students by (e.g.,
     *                   "2024-2025").
     * @return A list of students for the specified year.
     */
    private List<Student> getStudentsForYear(String schoolYear) {
        ObservableList<?> rawList = DataManager.getInstance()
                .getCollectionsRegistry()
                .getList("STUDENT");

        // Parse the year string
        int startYear = Integer.parseInt(schoolYear.split("-")[0]);

        return rawList.stream()
                .filter(obj -> obj instanceof Student)
                .map(obj -> (Student) obj)
                .filter(student -> student.getYearID() != null
                        && student.getYearID().getYearStart() == startYear
                        && student.isDeleted() == 0)
                .toList();
    }

    /**
     * Generates a QR code with the specified data and student name, saving it
     * to the given file path.
     *
     * @param data        The data to encode in the QR code.
     * @param filePath    The file path to save the QR code image.
     * @param width       The width of the QR code image.
     * @param height      The height of the QR code image.
     * @param studentName The student's name to include below the QR code.
     * @throws WriterException If QR code generation fails.
     * @throws IOException     If image writing fails.
     */
    private void generateQRCode(String data, String filePath, int width, int height, String studentName)
            throws WriterException, IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            // Create image with space for text
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

    /**
     * Resets highlights and restores the current view's button highlight
     */
    private void handleMenuItemCompletion() {
        resetButtonHighlights();
        // Restore highlight for current view
        if (currentController instanceof StudentController) {
            highlightButton(studentButton);
        } else if (currentController instanceof AttendanceController) {
            highlightButton(attendanceButton);
        } else if (currentController instanceof PayrollController) {
            highlightButton(payrollButton);
        }
    }

    @FXML
    private void handleGenerateKeyMenuItem() {
        try {
            // Show directory chooser
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose Directory to Save QR Codes");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDirectory = directoryChooser.showDialog(contentPane.getScene().getWindow());

            if (selectedDirectory == null) {
                return; // User cancelled
            }

            String currentYear = yearComboBox.getValue();
            List<Student> students = getStudentsForYear(currentYear);
            byte[] fixedKey = "MySuperSecretKey".getBytes();
            SecretKey secretKey = new SecretKeySpec(fixedKey, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Create QR codes directory
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

                System.out.println("Generated for: " + student.getFullName()
                        + " (" + qrFile.getAbsolutePath() + ")");
            }

            // Show success alert after generating all QR codes
            showSuccessAlert("Export Complete",
                    "Successfully generated QR codes",
                    "QR codes saved to:\n" + qrDir.getAbsolutePath());
            handleMenuItemCompletion();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSuccessAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Decrypts an encrypted student key and returns student information.
     *
     * @param encryptedKey The encrypted key to decrypt.
     * @return A string containing the student information or an error message.
     */
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

    /**
     * Finds a student by their student ID.
     *
     * @param studentId The student ID to search for.
     * @return The Student object, or null if not found.
     */
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

    /**
     * Toggles the visibility of the overlay for the content pane.
     *
     * @param visible True to show the overlay, false to hide it.
     */
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

    /**
     * Handles the creation of a new school year.
     */
    @FXML
    private void handleNewSchoolYear() {
        setOverlayVisible(true);
        try {
            SchoolYearDialog dialog = new SchoolYearDialog(null);
            dialog.showAndWait().ifPresent(newSchoolYear -> {
                if (newSchoolYear != null) {
                    Platform.runLater(() -> {
                        DataManager.getInstance().refreshData();
                        List<SchoolYear> updatedList = DataManager.getInstance().getCollectionsRegistry()
                                .getList("SCHOOL_YEAR");

                        schoolYearList.clear();
                        schoolYearList.addAll(updatedList);

                        // First update the items in the combo box
                        ObservableList<String> formattedYears = SchoolYearUtil.convertToStringList(schoolYearList);
                        yearComboBox.getItems().clear();
                        yearComboBox.getItems().addAll(formattedYears);

                        // Then set the value after a short delay to ensure the items are updated
                        String formattedNewYear = SchoolYearUtil.formatSchoolYear(newSchoolYear);
                        Platform.runLater(() -> {
                            yearComboBox.setValue(formattedNewYear);
                            updateCurrentController(formattedNewYear);
                        });
                    });
                }
            });
            handleMenuItemCompletion();
        } finally {
            setOverlayVisible(false);
        }
    }

    /**
     * Handles the editing of the current school year.
     */
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
                        // Update the shared collection
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

    @FXML
    public void handleDeletedStudentMenuItem() {
        try {
            DeletedStudentLoader loader = new DeletedStudentLoader();
            loader.addParameter("OWNER_WINDOW", contentPane.getScene().getWindow());
            loader.addParameter("SELECTED_YEAR", yearComboBox.getValue());
            loader.load();
            handleMenuItemCompletion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the current controller with the new school year.
     *
     * @param newYear The new school year to apply.
     */
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

    /**
     * Sets the selected month for the current controller.
     *
     * @param monthYear The month and year to set (e.g., "September 2024").
     */
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
