package sms.admin.app.attendance;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import atlantafx.base.controls.ModalPane;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.data.StudentDAO;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import sms.admin.app.RootController;
import sms.admin.app.attendance.dialog.AttendanceLogDialog;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.AttendanceDateUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;
import sms.admin.util.attendance.TableColumnUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;

public class AttendanceController extends FXController {

    private static final String STUDENT_PROFILE_FXML = "/sms.admin/app/management/viewstudent/STUDENT_PROFILE.fxml";

    @FXML
    private ComboBox<String> monthYearComboBox;
    @FXML
    private TableView<Student> attendanceTable;
    @FXML
    private TableColumn<Student, Integer> colNo;
    @FXML
    private TableColumn<Student, String> colFullName;
    @FXML
    private TableColumn<Student, String> monthAttendanceColumn;
    @FXML
    private BorderPane rootPane;
    @FXML
    private Label currentDateLabel;
    @FXML
    private Label selectedStudentsLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private ModalPane modalContainer;
    @FXML
    private StackPane dialogContainer;
    @FXML
    private MenuButton exportButton;
    @FXML
    private MenuItem exportExcel;
    @FXML
    private MenuItem exportCsv;
    @FXML
    private MenuItem exportPdf;

    private ObservableList<Student> studentList;
    private ObservableList<AttendanceLog> attendanceLog;

    private Map<String, AttendanceLog> logCache = new HashMap<>();
    private Map<LocalDate, AttendanceRecord> recordCache = new HashMap<>();

    private boolean isInitialSetup = true;

    @Override
    protected void load_bindings() {
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void load_fields() {
        System.out.println("load_fields: All parameters = " + getParameters());
        rootPane.getProperties().put("controller", this);
        
        // Get lists from parameters
        studentList = (ObservableList<Student>) getParameter("studentList");
        attendanceLog = (ObservableList<AttendanceLog>) getParameter("attendanceLogList");
        
        System.out.println("Loaded attendance logs: " + (attendanceLog != null ? attendanceLog.size() : "null"));
        
        // Load attendance logs if not provided
        if (attendanceLog == null || attendanceLog.isEmpty()) {
            loadAttendanceLogs();
        }
    
        setupTable();
        setupColumnWidths();
    
        String selectedYear = getSelectedYearOrDefault();
        initializeWithYear(selectedYear);
    
        // Remove setupMonthColumns() call from here since it's called in initializeWithYear
        updateStudentCountLabels();

        // Move the layout adjustment to after everything is initialized
        Platform.runLater(this::handleInitialLayout);
    }
    
    private void loadAttendanceLogs() {
        try {
            List<AttendanceLog> dbLogs = AttendanceLogDAO.getAttendanceLogList().stream()
                .filter(log -> {
                    if (log == null || log.getRecordID() == null) {
                        return false;
                    }
                    AttendanceRecord record = log.getRecordID();
                    // Validate record date components
                    try {
                        LocalDate.of(record.getYear(), record.getMonth(), record.getDay());
                        return true;
                    } catch (DateTimeException e) {
                        System.err.println(String.format(
                            "Invalid date in record %d: year=%d, month=%d, day=%d", 
                            record.getRecordID(), record.getYear(), record.getMonth(), record.getDay()));
                        return false;
                    }
                })
                .collect(Collectors.toList());

            attendanceLog = FXCollections.observableArrayList(dbLogs)
                .filtered(log -> !isFutureDate(log));
                
        } catch (Exception e) {
            System.err.println("Error loading attendance logs: " + e.getMessage());
            attendanceLog = FXCollections.observableArrayList();
        }
    }

    private void setupTable() {
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String fullName = String.format("%s, %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName());
            return new SimpleStringProperty(fullName);
        });

        // Add scrollbar configuration
        attendanceTable.setItems(studentList);
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attendanceTable.setTableMenuButtonVisible(true); // Enable column controls
        
        // Enable table scrolling
        attendanceTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Enable horizontal scrollbar
        ScrollBar horizontalScrollBar = findScrollBar(attendanceTable, true);
        if (horizontalScrollBar != null) {
            horizontalScrollBar.setVisible(true);
            horizontalScrollBar.setStyle("-fx-opacity: 1.0;");
        }
        
        // Make table fit viewport
        attendanceTable.prefHeightProperty().bind(rootPane.heightProperty());
        attendanceTable.prefWidthProperty().bind(rootPane.widthProperty());
        
        // Enable horizontal scrolling
        attendanceTable.setStyle("-fx-table-cell-border-color: transparent;");
    }

    private ScrollBar findScrollBar(TableView<?> table, boolean horizontal) {
        return (ScrollBar) table.lookupAll(".scroll-bar").stream()
            .filter(node -> node instanceof ScrollBar)
            .map(node -> (ScrollBar) node)
            .filter(bar -> horizontal ? 
                bar.getOrientation() == javafx.geometry.Orientation.HORIZONTAL :
                bar.getOrientation() == javafx.geometry.Orientation.VERTICAL)
            .findFirst()
            .orElse(null);
    }

    private void setupColumnWidths() {
        // Set fixed widths for ID and full name columns.
        colNo.setPrefWidth(44);
        colNo.setMinWidth(44);
        colNo.setMaxWidth(44);
        colNo.setResizable(false);

        colFullName.setPrefWidth(300);  // Increased from 250
        colFullName.setMinWidth(300);   // Increased from 250
        colFullName.setMaxWidth(400);   // Increased from 300
        colFullName.setResizable(true);

        // Let the monthly attendance column auto-calculate its width.
        monthAttendanceColumn.setPrefWidth(-1);
        monthAttendanceColumn.setMinWidth(400);  // Set minimum width
    }

    private void setupMonthColumns() {
        monthAttendanceColumn.getColumns().clear();
        WeeklyAttendanceUtil.clearCaches();

        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null) {
            return;
        }

        // Batch calculate dimensions
        double availableWidth = (attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth()) * 1.01;
        double minDayWidth = 52;
        
        LocalDate today = LocalDate.now();
        LocalDate startDate = WeeklyAttendanceUtil.getFirstDayOfMonth(selectedMonthYear);
        LocalDate endDate = (startDate.getMonth() == today.getMonth() && startDate.getYear() == today.getYear())
                ? today
                : startDate.withDayOfMonth(startDate.lengthOfMonth());

        // Pre-calculate all weeks and dimensions
        List<WeeklyAttendanceUtil.WeekDates> weeks = WeeklyAttendanceUtil.splitIntoWeeks(startDate, endDate);
        int totalDays = weeks.stream()
                .mapToInt(WeeklyAttendanceUtil::calculateWorkingDays)
                .sum();

        double optimalDayWidth = Math.max(minDayWidth, (availableWidth / Math.max(totalDays, 1)) - 5);
        double totalWidth = Math.max(availableWidth + 20, optimalDayWidth * totalDays) * 1.01;

        // Batch create columns
        List<TableColumn<Student, ?>> newColumns = new ArrayList<>();
        AtomicInteger weekCounter = new AtomicInteger(1);

        weeks.stream()
                .filter(WeeklyAttendanceUtil.WeekDates::hasWorkingDays)
                .forEach(week -> {
                    TableColumn<Student, String> weekColumn = createWeekColumn("Week " + weekCounter.getAndIncrement());
                    int workingDaysInWeek = WeeklyAttendanceUtil.calculateWorkingDays(week);
                    double weekWidth = WeeklyAttendanceUtil.calculateWeekWidth(workingDaysInWeek, totalWidth, totalDays);
                    
                    configureWeekColumn(weekColumn, weekWidth, week, workingDaysInWeek);
                    newColumns.add(weekColumn);
                });

        // Batch add columns
        Platform.runLater(() -> {
            monthAttendanceColumn.getColumns().addAll(newColumns);
            monthAttendanceColumn.setMinWidth(totalWidth);
            
            // Force layout pass
            attendanceTable.applyCss();
            attendanceTable.layout();
            
            adjustColumnWidths();
            
            // Add resize listener to handle window/table size changes
            monthAttendanceColumn.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                if (newWidth.doubleValue() > 0) {
                    adjustColumnWidths();
                }
            });
        });
    }

    private void configureWeekColumn(TableColumn<Student, String> weekColumn, double weekWidth, 
            WeeklyAttendanceUtil.WeekDates week, int workingDaysInWeek) {
        weekColumn.setPrefWidth(weekWidth);
        weekColumn.setMinWidth(weekWidth * 0.8);

        Map<DayOfWeek, TableColumn<Student, String>> dayColumns = 
            AttendanceDateUtil.createDayNameColumns(true, week.getDates());
        addDayColumns(weekColumn, dayColumns, week.getDates(), weekWidth / workingDaysInWeek);
    }

    private void addDayColumns(TableColumn<Student, String> weekColumn,
            Map<DayOfWeek, TableColumn<Student, String>> dayColumns,
            List<LocalDate> dates,
            double dayWidth) {
        dayColumns.forEach((dayOfWeek, dayCol) -> {
            weekColumn.getColumns().add(dayCol);
            List<TableColumn<Student, String>> daySubColumns = dates.stream()
                    .filter(date -> date.getDayOfWeek() == dayOfWeek)
                    .map(date -> {
                        TableColumn<Student, String> dayColumn = createDayColumn(date);
                        // Set equal widths for all day columns within the week
                        dayColumn.setPrefWidth(dayWidth);
                        dayColumn.setMinWidth(dayWidth * 0.8);
                        dayColumn.setMaxWidth(dayWidth * 1.2);
                        return dayColumn;
                    })
                    .collect(Collectors.toList());
            dayCol.getColumns().addAll(daySubColumns);
            
            // Set day name column width
            if (!daySubColumns.isEmpty()) {
                dayCol.setPrefWidth(dayWidth);
                dayCol.setMinWidth(dayWidth * 0.8);
            }
        });
    }

    private void adjustColumnWidths() {
        // Add width validation at start
        if (attendanceTable.getWidth() <= 0 || monthAttendanceColumn.getWidth() <= 0) {
            return;
        }
        
        // Calculate available width with 1% increase
        double availableWidth = (attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth()) * 1.01;
        
        // Get total number of visible days
        int totalDays = getTotalDays();
        if (totalDays == 0) return;
        
        // Calculate width per day with increased space
        double widthPerDay = Math.max(52, (availableWidth / totalDays) - 2);
        double totalNeededWidth = widthPerDay * totalDays * 1.01;
        
        // Ensure the monthly attendance column fills available space
        monthAttendanceColumn.setPrefWidth(Math.max(totalNeededWidth, availableWidth));
        
        // Adjust week column widths proportionally
        monthAttendanceColumn.getColumns().forEach(weekCol -> {
            int daysInWeek = weekCol.getColumns().stream()
                    .mapToInt(dayNameCol -> dayNameCol.getColumns().size())
                    .sum();
            if (daysInWeek > 0) {
                double weekWidth = widthPerDay * daysInWeek;
                weekCol.setPrefWidth(weekWidth);
                weekCol.setMinWidth(weekWidth * 0.8);
                
                // Adjust day columns within the week
                weekCol.getColumns().forEach(dayNameCol -> {
                    double dayWidth = weekWidth / daysInWeek;
                    dayNameCol.setPrefWidth(dayWidth);
                    dayNameCol.setMinWidth(dayWidth * 0.8);
                    
                    dayNameCol.getColumns().forEach(dayCol -> {
                        dayCol.setPrefWidth(dayWidth);
                        dayCol.setMinWidth(dayWidth * 0.8);
                    });
                });
            }
        });
    }

    @Override
    protected void load_listeners() {
        monthYearComboBox.setOnAction(event -> {
            if (!monthYearComboBox.isFocused()) {
                return;
            }
            String newValue = monthYearComboBox.getValue();
            setupMonthColumns();
            updateRootController(newValue);
        });

        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));

        attendanceTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateStudentCountLabels());

        attendanceTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                Platform.runLater(this::adjustColumnWidths);
            }
        });

        // Add scroll synchronization for nested columns
        attendanceTable.setOnScroll(event -> {
            if (event.isShiftDown()) {
                ScrollBar horizontalScrollBar = findScrollBar(attendanceTable, true);
                if (horizontalScrollBar != null) {
                    double deltaX = event.getDeltaY() / attendanceTable.getWidth();
                    horizontalScrollBar.setValue(horizontalScrollBar.getValue() - deltaX);
                }
                event.consume();
            }
        });
    }

    private void updateRootController(String monthYear) {
        Scene scene = rootPane.getScene();
        if (scene != null) {
            Parent root = scene.getRoot();
            if (root != null) {
                Object controller = root.getProperties().get("controller");
                if (controller instanceof RootController rootController) {
                    rootController.setSelectedMonth(monthYear);
                }
            }
        }
    }

    private void handleExport(String type) {
        try {
            String selectedMonthYear = monthYearComboBox.getValue();
            if (selectedMonthYear == null) {
                return;
            }

            String[] parts = selectedMonthYear.split(" ");
            String monthName = parts[0];
            int year = Integer.parseInt(parts[1]);
            Month month = Month.valueOf(monthName.toUpperCase());
            YearMonth selectedMonth = YearMonth.of(year, month.getValue());

            String title = "Attendance Report - " + selectedMonthYear;
            String fileName = String.format("attendance_%s.%s",
                    selectedMonthYear.replace(" ", "_").toLowerCase(),
                    type.equals("excel") ? "xlsx" : type.toLowerCase());
            String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

            AttendanceTableExporter exporter = new AttendanceTableExporter(selectedMonth);
            switch (type) {
                case "excel" ->
                    exporter.exportToExcel(attendanceTable, title, outputPath);
                case "pdf" ->
                    exporter.exportToPdf(attendanceTable, title, outputPath);
                case "csv" ->
                    exporter.exportToCsv(attendanceTable, title, outputPath);
            }
            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TableColumn<Student, String> createWeekColumn(String weekLabel) {
        TableColumn<Student, String> weekColumn = new TableColumn<>(weekLabel);
        weekColumn.setStyle("-fx-alignment: CENTER;");
        // Let week column width be determined by its children
        return weekColumn;
    }

    private TableColumn<Student, String> createDayColumn(LocalDate date) {
        TableColumn<Student, String> dayColumn = new TableColumn<>(String.valueOf(date.getDayOfMonth()));
        
        // Calculate optimal width based on possible content
        double maxContentWidth = Math.max(
            AttendanceUtil.PRESENT_MARK.length(),
            Math.max(
                AttendanceUtil.ABSENT_MARK.length(),
                Math.max(
                    AttendanceUtil.HALF_DAY_MARK.length(),
                    AttendanceUtil.EXCUSED_MARK.length()
                )
            )
        ) * 12; // Approximate width per character

        // Add padding for better appearance
        double columnWidth = Math.max(maxContentWidth + 20, 52);
        
        dayColumn.setMinWidth(columnWidth);
        dayColumn.setPrefWidth(columnWidth);
        dayColumn.setMaxWidth(columnWidth * 1.5);
        dayColumn.setResizable(false);
        dayColumn.setStyle("-fx-alignment: CENTER;");

        dayColumn.setCellValueFactory(data -> {
            Student student = data.getValue();
            String status = AttendanceUtil.getAttendanceStatus(student, date, attendanceLog);
            return new SimpleStringProperty(status);
        });

        dayColumn.setCellFactory(column -> createDayCell(date));
        return dayColumn;
    }

    private TableCell<Student, String> createDayCell(LocalDate date) {
        TableCell<Student, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || date.isAfter(LocalDate.now())) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    // Adjust font size based on column width
                    double columnWidth = getTableColumn().getWidth();
                    if (columnWidth > 0) {
                        // Calculate font size based on column width
                        // Base size is 12px, adjust between 8px and 14px
                        double fontSize = Math.min(14, Math.max(8, columnWidth / 4));
                        setStyle(String.format("-fx-font-size: %.1fpx; -fx-alignment: CENTER;", fontSize));
                    }
                }
            }
        };

        // Add listener to adjust font when column width changes
        cell.tableColumnProperty().addListener((obs, oldCol, newCol) -> {
            if (newCol != null) {
                newCol.widthProperty().addListener((obs2, oldWidth, newWidth) -> {
                    if (!cell.isEmpty() && newWidth.doubleValue() > 0) {
                        double fontSize = Math.min(14, Math.max(8, newWidth.doubleValue() / 4));
                        cell.setStyle(String.format("-fx-font-size: %.1fpx; -fx-alignment: CENTER;", fontSize));
                    }
                });
            }
        });

        if (!date.isAfter(LocalDate.now())) {
            ContextMenu contextMenu = new ContextMenu();

            MenuItem viewAttendanceItem = new MenuItem("View Attendance Log");
            viewAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    showAttendanceLogDialog(student, date);
                }
            });

            MenuItem editAttendanceItem = new MenuItem("Edit Attendance");
            editAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    String currentStatus = cell.getText();
                    var comboBox = new javafx.scene.control.ComboBox<String>();
                    comboBox.getItems().addAll(
                            AttendanceUtil.PRESENT_MARK,
                            AttendanceUtil.ABSENT_MARK,
                            AttendanceUtil.HALF_DAY_MARK,
                            AttendanceUtil.EXCUSED_MARK);
                    comboBox.setValue(currentStatus.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentStatus);
                    cell.setGraphic(comboBox);
                    cell.setText(null);
                    comboBox.setOnAction(event -> updateCellValue(cell, student, date, comboBox.getValue()));
                    comboBox.show();
                }
            });

            contextMenu.getItems().addAll(viewAttendanceItem, editAttendanceItem);
            cell.setContextMenu(contextMenu);
        }
        return cell;
    }

    private String generateLogKey(Student student, AttendanceRecord record) {
        return student.getStudentID() + "-" + record.getRecordID();
    }

    private AttendanceLog findExistingLog(Student student, AttendanceRecord record) {
        String key = generateLogKey(student, record);
        
        // First check cache
        AttendanceLog cachedLog = logCache.get(key);
        if (cachedLog != null) {
            return cachedLog;
        }

        // Then check database
        return AttendanceLogDAO.getAttendanceLogList().stream()
            .filter(log -> log != null 
                && log.getRecordID() != null 
                && log.getStudentID() != null
                && log.getRecordID().getRecordID() == record.getRecordID()
                && log.getStudentID().getStudentID() == student.getStudentID())
            .findFirst()
            .orElse(null);
    }

    private int getNextLogId() {
        return AttendanceLogDAO.getAttendanceLogList().stream()
                .mapToInt(AttendanceLog::getLogID)
                .max()
                .orElse(0) + 1;
    }

    private AttendanceLog createNewLog(Student student, AttendanceRecord record) {
        int nextId = getNextLogId();
        return new AttendanceLog(nextId, record, student, 0, 0, 0, 0);
    }

    private AttendanceRecord findOrCreateRecord(LocalDate date) {
        if (recordCache.containsKey(date)) {
            return recordCache.get(date);
        }

        AttendanceRecord record = AttendanceRecordDAO.getRecordList().stream()
                .filter(r -> r.getYear() == date.getYear()
                && r.getMonth() == date.getMonthValue()
                && r.getDay() == date.getDayOfMonth())
                .findFirst()
                .orElseGet(() -> {
                    AttendanceRecord newRecord = createAttendanceRecord(date);
                    AttendanceRecordDAO.insert(newRecord);
                    return newRecord;
                });

        recordCache.put(date, record);
        return record;
    }

    private void updateCellValue(TableCell<Student, String> cell, Student student, LocalDate date, String newValue) {
        if (newValue == null) return;

        try {
            AttendanceRecord record = findOrCreateRecord(date);
            AttendanceLog existingLog = findExistingLog(student, record);
            AttendanceLog updatedLog;

            if (existingLog != null) {
                updatedLog = existingLog;
                updateLogTimes(updatedLog, newValue);
                AttendanceLogDAO.update(updatedLog);
            } else {
                updatedLog = createNewLog(student, record);
                updateLogTimes(updatedLog, newValue);  // Fixed: Added missing newValue parameter
                AttendanceLogDAO.insert(updatedLog);
            }

            // Update cache
            logCache.put(generateLogKey(student, record), updatedLog);
            recordCache.put(date, record);

            // Update UI
            Platform.runLater(() -> {
                cell.setGraphic(null);
                cell.setText(newValue);
                // Force a refresh of all related cells
                attendanceTable.refresh();
            });

            // Reload logs to ensure consistency
            refreshAttendanceLogs();
            
        } catch (Exception e) {
            System.err.println("Error updating attendance: " + e.getMessage());
            e.printStackTrace();
            // Revert UI on error
            Platform.runLater(() -> {
                cell.setGraphic(null);
                cell.setText(AttendanceUtil.ABSENT_MARK);
            });
        }
    }

    private int getNextRecordId() {
        return AttendanceRecordDAO.getRecordList().stream()
                .mapToInt(AttendanceRecord::getRecordID)
                .max()
                .orElse(0) + 1;
    }

    private AttendanceRecord createAttendanceRecord(LocalDate date) {
        try {
            int nextId = getNextRecordId();
            int month = date.getMonthValue();
            int day = date.getDayOfMonth();
            int year = date.getYear();
            
            // Validate date components before creating record
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                throw new IllegalArgumentException(
                    String.format("Invalid date components: year=%d, month=%d, day=%d", 
                    year, month, day));
            }
            
            return new AttendanceRecord(nextId, month, day, year);
        } catch (Exception e) {
            System.err.println("Error creating attendance record: " + e.getMessage());
            throw e;
        }
    }

    private void showAttendanceLogDialog(Student student, LocalDate date) {
        try {
            List<AttendanceLog> dialogLogs = new ArrayList<>(AttendanceLogDAO.getAttendanceLogList());
            AttendanceLogDialog dialog = new AttendanceLogDialog(student, date, dialogLogs);
            dialog.load();
        } catch (Exception e) {
            System.err.println("Error showing attendance log dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isFutureDate(AttendanceLog log) {
        try {
            if (log == null || log.getRecordID() == null) {
                return true;
            }
            AttendanceRecord record = log.getRecordID();
            int year = record.getYear();
            int month = record.getMonth();
            int day = record.getDay();
            
            // Validate date components
            if (month < 1 || month > 12) {
                System.err.println("Invalid month in record: year=" + year + 
                    ", month=" + month + ", day=" + day);
                return true;
            }
            
            try {
                LocalDate logDate = LocalDate.of(year, month, day);
                return logDate.isAfter(LocalDate.now());
            } catch (DateTimeException e) {
                System.err.println("Invalid date components: year=" + year + 
                    ", month=" + month + ", day=" + day);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error checking future date: " + e.getMessage());
            return true;
        }
    }

    // Update the attendance log's time values based on the attendance status.
    private void updateLogTimes(AttendanceLog log, String attendanceValue) {
        int timeValue;
        switch (attendanceValue) {
            case AttendanceUtil.PRESENT_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_IN_PM);
                log.setTimeOutPM(AttendanceUtil.TIME_OUT_PM);
                break;
            case AttendanceUtil.ABSENT_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutAM(AttendanceUtil.TIME_ABSENT);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
                break;
            case AttendanceUtil.HALF_DAY_MARK:
                log.setTimeInAM(AttendanceUtil.TIME_IN_AM);
                log.setTimeOutAM(AttendanceUtil.TIME_OUT_AM);
                log.setTimeInPM(AttendanceUtil.TIME_ABSENT);
                log.setTimeOutPM(AttendanceUtil.TIME_ABSENT);
                break;
            case AttendanceUtil.EXCUSED_MARK:
                timeValue = AttendanceUtil.TIME_EXCUSED;
                log.setTimeInAM(timeValue);
                log.setTimeOutAM(timeValue);
                log.setTimeInPM(timeValue);
                log.setTimeOutPM(timeValue);
                break;
        }
    }

    // Refresh the attendance logs from the database and update the UI.
    private void refreshAttendanceLogs() {
        try {
            clearCaches();
            // Use a local variable to avoid race conditions
            List<AttendanceLog> dbLogs = new ArrayList<>(AttendanceLogDAO.getAttendanceLogList());
            
            // Filter and validate logs
            List<AttendanceLog> validLogs = dbLogs.stream()
                .filter(log -> {
                    if (log == null || log.getRecordID() == null || log.getStudentID() == null) {
                        return false;
                    }
                    int month = log.getRecordID().getMonth();
                    return month >= 1 && month <= 12;
                })
                .collect(Collectors.toList());

            // Update observable list
            Platform.runLater(() -> {
                attendanceLog = FXCollections.observableArrayList(validLogs)
                    .filtered(log -> !isFutureDate(log));
                
                // Pre-populate caches with valid logs
                validLogs.forEach(log -> {
                    String key = generateLogKey(log.getStudentID(), log.getRecordID());
                    logCache.put(key, log);
                    LocalDate logDate = LocalDate.of(
                        log.getRecordID().getYear(),
                        log.getRecordID().getMonth(),
                        log.getRecordID().getDay()
                    );
                    recordCache.put(logDate, log.getRecordID());
                });
                
                setupMonthColumns();
                attendanceTable.refresh();
            });

        } catch (Exception e) {
            System.err.println("Error refreshing attendance logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshDisplay() {
        Platform.runLater(() -> {
            loadAttendanceLogs();
            setupMonthColumns();
            attendanceTable.refresh();
        });
    }

    private void clearCaches() {
        logCache.clear();
        recordCache.clear();
    }

    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

    public void initializeWithYear(String year) {
        if (year == null) return;
    
        String[] yearRange = year.split("-");
        int startYear = Integer.parseInt(yearRange[0]);
        int endYear = Integer.parseInt(yearRange[1]);
    
        List<Student> students = StudentDAO.getStudentList().stream()
            .filter(student -> student.getYearID() != null &&
                               student.getYearID().getYearStart() == startYear &&
                               student.getYearID().getYearEnd() == endYear)
            .collect(Collectors.toList());
    
        studentList = FXCollections.observableArrayList(students);
        attendanceTable.setItems(studentList);
    
        if (monthYearComboBox != null) {
            String prevValue = monthYearComboBox.getValue();
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);
            
            String selectedMonth = (String) getParameter("selectedMonth");
            System.out.println("initializeWithYear: selectedMonth parameter = " + selectedMonth);
            
            // First try to keep the previous selection if it exists in the new items
            if (prevValue != null && monthYearComboBox.getItems().contains(prevValue)) {
                monthYearComboBox.setValue(prevValue);
            }
            // Then try to use the selectedMonth parameter
            else if (selectedMonth != null && monthYearComboBox.getItems().contains(selectedMonth)) {
                monthYearComboBox.setValue(selectedMonth);
            }
            // Finally default to current month only if no other value was set
            else if (monthYearComboBox.getValue() == null) {
                YearMonth current = YearMonth.now();
                String currentFormatted = current.format(DateTimeUtils.MONTH_YEAR_FORMATTER);
                if (monthYearComboBox.getItems().contains(currentFormatted)) {
                    monthYearComboBox.setValue(currentFormatted);
                } else if (!monthYearComboBox.getItems().isEmpty()) {
                    monthYearComboBox.setValue(monthYearComboBox.getItems().get(0));
                }
            }
            
            // Only setup columns once
            if (isInitialSetup) {
                setupMonthColumns();
            }
        }
        updateStudentCountLabels();
    }

    @FXML
    private void handleViewStudentButton() {
        initializeViewStudent();
    }

    private void initializeViewStudent() {
        StudentProfileLoader loader = (StudentProfileLoader) FXLoaderFactory
                .createInstance(StudentProfileLoader.class, getClass().getResource(STUDENT_PROFILE_FXML))
                .initialize();
        loader.load();
    }

    private String getSelectedYearOrDefault() {
        String selectedYear = (String) getParameter("selectedYear");
        if (selectedYear == null) {
            LocalDate now = LocalDate.now();
            int year = now.getMonthValue() >= 6 ? now.getYear() : now.getYear() - 1;
            selectedYear = year + "-" + (year + 1);
        }
        return selectedYear;
    }

    private void updateStudentCountLabels() {
        if (studentList == null || attendanceTable == null
                || selectedStudentsLabel == null || totalStudentsLabel == null) {
            return;
        }

        int totalStudents = studentList.size();
        int selectedStudents = attendanceTable.getSelectionModel().getSelectedItems().size();
        selectedStudentsLabel.setText(String.format("Selected: %d", selectedStudents));
        totalStudentsLabel.setText(String.format("Total: %d", totalStudents));
    }

    public void setSelectedMonth(String monthYear) {
        if (monthYearComboBox != null && monthYearComboBox.getItems().contains(monthYear)) {
            monthYearComboBox.setValue(monthYear);
            setupMonthColumns();
        }
    }

    public String getSelectedMonth() {
        return monthYearComboBox != null ? monthYearComboBox.getValue() : null;
    }

    private int getTotalDays() {
        return monthAttendanceColumn.getColumns().stream()
                .mapToInt(weekCol -> weekCol.getColumns().stream()
                        .mapToInt(dayNameCol -> dayNameCol.getColumns().size())
                        .sum())
                .sum();
    }

    private void handleInitialLayout() {
        if (!isInitialSetup) return;
        
        attendanceTable.applyCss();
        attendanceTable.layout();
        
        double tableWidth = attendanceTable.getWidth();
        if (tableWidth > 0) {
            adjustColumnWidths();
            isInitialSetup = false;
        } else {
            // Add one-time listener for initial width
            attendanceTable.widthProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Number> obs, 
                        Number oldVal, Number newVal) {
                    if (newVal.doubleValue() > 0 && isInitialSetup) {
                        adjustColumnWidths();
                        isInitialSetup = false;
                        attendanceTable.widthProperty().removeListener(this);
                    }
                }
            });
        }
    }
}
