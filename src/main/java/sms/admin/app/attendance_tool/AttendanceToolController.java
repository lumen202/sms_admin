package sms.admin.app.attendance_tool;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import sms.admin.util.datetime.SchoolYearUtil;

public class AttendanceToolController extends FXController {

    public AttendanceToolController() {
        System.out.println("AttendanceToolController constructor called");
    }

    @FXML
    private TableView<StudentAttendance> tableView;
    @FXML
    private TableColumn<StudentAttendance, String> idColumn;
    @FXML
    private TableColumn<StudentAttendance, String> nameColumn;
    @FXML
    private TableColumn<StudentAttendance, String> timeColumn;
    @FXML
    private TableColumn<StudentAttendance, Void> actionColumn;
    @FXML
    private Label dateLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private ComboBox<String> yearComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button timeOutAllButton;

    private Timeline timeline;
    private ObservableList<StudentAttendance> studentList;
    private ObservableList<SchoolYear> schoolYearList;
    private FilteredList<StudentAttendance> filteredList;
    private ObservableList<AttendanceRecord> attendanceRecords;
    private ObservableList<AttendanceLog> attendanceLogs;

    @Override
    protected void load_fields() {
        try {
            System.out.println("Loading attendance tool fields...");
            // Set up timeline cleanup
            if (dateLabel.getScene() != null) {
                dateLabel.getScene().getWindow().setOnCloseRequest(e -> {
                    if (timeline != null) {
                        timeline.stop();
                    }
                });
            }

            // Initialize collections in correct order
            schoolYearList = DataManager.getInstance().getCollectionsRegistry().getList("SCHOOL_YEAR");
            System.out.println("School years loaded: " + (schoolYearList != null ? schoolYearList.size() : "null"));

            attendanceRecords = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_RECORD");
            System.out.println(
                    "Attendance records loaded: " + (attendanceRecords != null ? attendanceRecords.size() : "null"));

            attendanceLogs = DataManager.getInstance().getCollectionsRegistry().getList("ATTENDANCE_LOG");
            System.out.println("Attendance logs loaded: " + (attendanceLogs != null ? attendanceLogs.size() : "null"));

            // Initialize lists
            studentList = FXCollections.observableArrayList();
            filteredList = new FilteredList<>(studentList);

            // Setup UI
            initializeColumns();
            tableView.setItems(filteredList);
            yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

            // Initialize time display
            initializeTimeUpdates();

            // Set initial year if provided
            String selectedYear = (String) getParameter("selectedYear");
            if (selectedYear != null) {
                yearComboBox.setValue(selectedYear);
            } else {
                SchoolYear currentYear = SchoolYearUtil.findCurrentYear(schoolYearList);
                if (currentYear != null) {
                    yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(currentYear));
                }
            }

            // Load initial data
            loadStudentData();

            System.out.println("Attendance tool fields loaded successfully");
        } catch (Exception e) {
            System.err.println("Error loading attendance tool fields: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Error: Failed to load data - " + e.getMessage());
        }
    }

    @Override
    protected void load_bindings() {
        // Remove the existing binding and create a new one that updates automatically
        totalStudentsLabel.textProperty().unbind(); // Unbind first to be safe
        totalStudentsLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> String.format("Showing %d of %d students",
                                filteredList != null ? filteredList.size() : 0,
                                studentList != null ? studentList.size() : 0),
                        filteredList, studentList));
    }

    @Override
    protected void load_listeners() {
        yearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadStudentData();
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(student -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase();
                return student.getStudentId().toLowerCase().contains(lowerCaseFilter)
                        || student.getStudentName().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }

    private void initializeColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        idColumn.setStyle("-fx-alignment: CENTER;");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("lastActionTime"));

        actionColumn.setPrefWidth(120);
        actionColumn.setStyle("-fx-alignment: CENTER;");
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button();

            {
                button.setMaxWidth(Double.MAX_VALUE);
                button.getStyleClass().add("action-button");
                button.setOnAction(evt -> {
                    StudentAttendance student = getTableView().getItems().get(getIndex());
                    handleAttendanceAction(student);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    StudentAttendance student = getTableView().getItems().get(getIndex());
                    button.setText(student.isLoggedIn() ? "Time-out" : "Time-in");
                    button.getStyleClass().removeAll("login-button", "logout-button");
                    button.getStyleClass().add(student.isLoggedIn() ? "logout-button" : "login-button");
                    setGraphic(button);
                }
            }
        });
    }

    private void loadStudentData() {
        try {
            System.out.println("Starting loadStudentData...");
            ObservableList<Student> students = DataManager.getInstance().getCollectionsRegistry().getList("STUDENT");
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            boolean isPM = now.getHour() >= 12;

            studentList.clear();
            String selectedYear = yearComboBox.getValue();

            if (selectedYear != null) {
                int startYear = Integer.parseInt(selectedYear.split("-")[0]);

                students.stream()
                        .filter(student -> student != null
                                && student.getYearID() != null
                                && student.getYearID().getYearStart() == startYear
                                && student.isDeleted() == 0)
                        .forEach(student -> {
                            // Create StudentAttendance object
                            StudentAttendance sa = new StudentAttendance(
                                    String.valueOf(student.getStudentID()),
                                    student.getFirstName() + " " + student.getLastName());

                            // Check for existing log today
                            AttendanceLog todayLog = attendanceLogs.stream()
                                    .filter(log -> log.getStudentID().getStudentID() == student.getStudentID()
                                            && log.getRecordID().getYear() == today.getYear()
                                            && log.getRecordID().getMonth() == today.getMonthValue()
                                            && log.getRecordID().getDay() == today.getDayOfMonth())
                                    .findFirst()
                                    .orElse(null);

                            // Update logged in status based on existing log
                            if (todayLog != null) {
                                if (isPM) {
                                    boolean hasTimeIn = todayLog.getTimeInPM() > 0;
                                    boolean hasTimeOut = todayLog.getTimeOutPM() > 0;
                                    sa.setLoggedIn(hasTimeIn && !hasTimeOut);
                                    if (hasTimeIn) {
                                        int time = hasTimeOut ? todayLog.getTimeOutPM() : todayLog.getTimeInPM();
                                        sa.setLastActionTime(formatTime(time));
                                    }
                                } else {
                                    boolean hasTimeIn = todayLog.getTimeInAM() > 0;
                                    boolean hasTimeOut = todayLog.getTimeOutAM() > 0;
                                    sa.setLoggedIn(hasTimeIn && !hasTimeOut);
                                    if (hasTimeIn) {
                                        int time = hasTimeOut ? todayLog.getTimeOutAM() : todayLog.getTimeInAM();
                                        sa.setLastActionTime(formatTime(time));
                                    }
                                }
                            }

                            studentList.add(sa);
                        });
            }

            System.out.println("Final student list size: " + studentList.size());
            tableView.refresh();
        } catch (Exception e) {
            System.err.println("Error in loadStudentData: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Error loading students: " + e.getMessage());
        }
    }

    private String formatTime(int time) {
        int hours = time / 100;
        int minutes = time % 100;
        return String.format("%02d:%02d:%02d %s",
                hours > 12 ? hours - 12 : hours,
                minutes,
                0,
                hours >= 12 ? "PM" : "AM");
    }

    private void initializeTimeUpdates() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            Platform.runLater(() -> {
                dateLabel.setText(now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                timeLabel.setText(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
            });
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Set initial values
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        timeLabel.setText(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
    }

    private void handleAttendanceAction(StudentAttendance studentAttendance) {
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isPM = now.getHour() >= 13; // Changed from 12 to 13 (1 PM)
            int currentTime = now.getHour() * 100 + now.getMinute();

            Student student = findStudentById(studentAttendance.getStudentId());
            if (student == null) {
                statusLabel.setText("Error: Student not found");
                return;
            }

            AttendanceRecord todayRecord = getOrCreateDayRecord();

            if (!studentAttendance.isLoggedIn()) { // Time In
                AttendanceLog log = findTodayAttendanceLog(student, todayRecord);
                if (log == null) {
                    // Create new log only if one doesn't exist
                    int nextLogId = attendanceLogs.isEmpty() ? 1
                            : attendanceLogs.stream()
                                    .mapToInt(AttendanceLog::getLogID)
                                    .max()
                                    .getAsInt() + 1;
                    log = new AttendanceLog(nextLogId, todayRecord, student,
                            isPM ? 0 : currentTime, // timeInAM
                            0, // timeOutAM
                            isPM ? currentTime : 0, // timeInPM
                            0 // timeOutPM
                    );
                    AttendanceLogDAO.insert(log);
                    attendanceLogs.add(log);
                } else {
                    // Update existing log with time in only
                    if (isPM) {
                        log.setTimeInPM(currentTime);
                        log.setTimeOutAM(0); // Clear any incorrect AM timeout
                    } else {
                        log.setTimeInAM(currentTime);
                    }
                    AttendanceLogDAO.update(log);
                }
                studentAttendance.setLoggedIn(true);
                studentAttendance.setLastActionTime(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
                statusLabel.setText("Time In recorded for " + student.getFirstName());
            } else { // Time Out
                AttendanceLog log = findTodayAttendanceLog(student, todayRecord);
                if (log != null) {
                    if (isPM) {
                        log.setTimeOutPM(currentTime);
                    } else {
                        log.setTimeOutAM(currentTime);
                    }
                    AttendanceLogDAO.update(log);
                    studentAttendance.setLoggedIn(false);
                    studentAttendance.setLastActionTime(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
                    statusLabel.setText("Time Out recorded for " + student.getFirstName());
                }
            }

            tableView.refresh();
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleTimeOutAll() {
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isPM = now.getHour() >= 12;
            int currentTime = now.getHour() * 100 + now.getMinute();
            AttendanceRecord todayRecord = getOrCreateDayRecord();
            int timeOutCount = 0;

            for (StudentAttendance studentAttendance : studentList) {
                if (studentAttendance.isLoggedIn()) {
                    Student student = findStudentById(studentAttendance.getStudentId());
                    if (student != null) {
                        AttendanceLog log = findTodayAttendanceLog(student, todayRecord);
                        if (log != null) {
                            if (isPM) {
                                log.setTimeOutPM(currentTime);
                            } else {
                                log.setTimeOutAM(currentTime);
                            }
                            AttendanceLogDAO.update(log);
                            studentAttendance.setLoggedIn(false);
                            studentAttendance.setLastActionTime(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
                            timeOutCount++;
                        }
                    }
                }
            }

            tableView.refresh();
            statusLabel.setText(String.format("Timed out %d students", timeOutCount));
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private AttendanceRecord getOrCreateDayRecord() {
        LocalDate today = LocalDate.now();
        return attendanceRecords.stream()
                .filter(record -> record.getMonth() == today.getMonthValue()
                        && record.getDay() == today.getDayOfMonth()
                        && record.getYear() == today.getYear())
                .findFirst()
                .orElseGet(() -> {
                    int maxId = attendanceRecords.stream()
                            .mapToInt(AttendanceRecord::getRecordID)
                            .max()
                            .orElse(0);
                    AttendanceRecord newRecord = new AttendanceRecord(
                            maxId + 1, today.getMonthValue(),
                            today.getDayOfMonth(), today.getYear());
                    AttendanceRecordDAO.insert(newRecord);
                    attendanceRecords.add(newRecord);
                    return newRecord;
                });
    }

    private AttendanceLog findOrCreateAttendanceLog(Student student, AttendanceRecord record,
            boolean isPM, int currentTime) {
        AttendanceLog log = findTodayAttendanceLog(student, record);
        if (log == null) {
            int nextLogId = attendanceLogs.isEmpty() ? 1
                    : attendanceLogs.stream()
                            .mapToInt(AttendanceLog::getLogID)
                            .max()
                            .getAsInt() + 1;
            log = new AttendanceLog(nextLogId, record, student,
                    isPM ? 0 : currentTime, // timeInAM
                    0, // timeOutAM
                    isPM ? currentTime : 0, // timeInPM
                    0 // timeOutPM
            );
            AttendanceLogDAO.insert(log);
            attendanceLogs.add(log);
        } else {
            if (isPM && log.getTimeInPM() == 0) {
                log.setTimeInPM(currentTime);
                AttendanceLogDAO.update(log);
            } else if (!isPM && log.getTimeInAM() == 0) {
                log.setTimeInAM(currentTime);
                AttendanceLogDAO.update(log);
            }
        }
        return log;
    }

    private AttendanceLog findTodayAttendanceLog(Student student, AttendanceRecord record) {
        return attendanceLogs.stream()
                .filter(log -> log.getStudentID().getStudentID() == student.getStudentID()
                        && log.getRecordID().getRecordID() == record.getRecordID())
                .findFirst()
                .orElse(null);
    }

    private Student findStudentById(String studentId) {
        try {
            int id = Integer.parseInt(studentId);
            ObservableList<Student> students = DataManager.getInstance()
                    .getCollectionsRegistry()
                    .getList("STUDENT");

            return students.stream()
                    .filter(student -> student.getStudentID() == id)
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void initializeWithYear(String selectedYear) {
        if (selectedYear != null) {
            Platform.runLater(() -> {
                if (yearComboBox != null) {
                    yearComboBox.setValue(selectedYear);
                }
                if (studentList != null) {
                    loadStudentData();
                }
            });
        }
    }
}
