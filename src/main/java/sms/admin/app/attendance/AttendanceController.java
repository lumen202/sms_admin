package sms.admin.app.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
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
import sms.admin.util.TableViewTransitions;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;
import sms.admin.util.mock.DataUtil;

public class AttendanceController extends FXController {

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
    private Label selectedStudentsLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private MenuButton exportButton;
    @FXML
    private MenuItem exportExcel;
    @FXML
    private MenuItem exportCsv;
    @FXML
    private MenuItem exportPdf;

    private ObservableList<Student> studentList;
    private FilteredList<Student> filteredStudentList;
    private ObservableList<AttendanceLog> attendanceLog;

    @Override
    protected void load_fields() {
        // Initialize data lists
        studentList = FXCollections.observableArrayList(DataUtil.createStudentList());
        filteredStudentList = new FilteredList<>(studentList, p -> true);
        attendanceLog = FXCollections.observableArrayList(DataUtil.createAttendanceLogList());

        // Setup the table and fixed column widths
        setupTable();
        setupColumnWidths();

        // Initialize view based on selected academic year
        initializeWithYear(getSelectedYearOrDefault());

        // Setup the dynamic month columns and update labels
        setupMonthColumns();
        disableColumnReordering(attendanceTable);
        updateStudentCountLabels();
    }

    @Override
    protected void load_bindings() {
        // Place for property bindings if needed.
    }

    @Override
    protected void load_listeners() {
        // Listener for month/year ComboBox changes
        monthYearComboBox.setOnAction(event -> setupMonthColumns());

        // Handlers for export menu items
        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));

        // Listener to update student count labels on selection changes
        attendanceTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateStudentCountLabels());

        // Listener to adjust dynamic column widths when the TableView is resized
        attendanceTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                Platform.runLater(this::adjustColumnWidths);
            }
        });
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
        attendanceTable.setItems(filteredStudentList);
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Disable column reordering for the main columns
        colNo.setReorderable(false);
        colFullName.setReorderable(false);
        monthAttendanceColumn.setReorderable(false);
    }

    private void setupColumnWidths() {
        // Fixed columns for "No" and "Full Name"
        colNo.setPrefWidth(44);
        colNo.setMinWidth(44);
        colNo.setMaxWidth(44);
        colNo.setResizable(false);

        colFullName.setPrefWidth(148);
        colFullName.setMinWidth(148);
        colFullName.setMaxWidth(246);

        // Base width for the container of dynamic attendance columns
        monthAttendanceColumn.setPrefWidth(655);
    }

    private void setupMonthColumns() {
        monthAttendanceColumn.getColumns().clear();

        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null) {
            return;
        }

        // Parse month and year from the ComboBox value
        String[] parts = selectedMonthYear.split(" ");
        String monthName = parts[0];
        int yearNumber = Integer.parseInt(parts[1]);
        Month month = Month.valueOf(monthName.toUpperCase());

        LocalDate currentDate = LocalDate.of(yearNumber, month.getValue(), 1);
        TableColumn<Student, String> currentWeekColumn = new TableColumn<>("Week 1");
        currentWeekColumn.setStyle("-fx-alignment: CENTER;");
        monthAttendanceColumn.getColumns().add(currentWeekColumn);

        int weekNumber = 1;
        while (currentDate.getMonth() == month) {
            if (currentDate.getDayOfWeek() == DayOfWeek.MONDAY && !currentWeekColumn.getColumns().isEmpty()) {
                weekNumber++;
                currentWeekColumn = createWeekColumn(weekNumber);
                monthAttendanceColumn.getColumns().add(currentWeekColumn);
            }
            if (!AttendanceUtil.isWeekend(currentDate)) {
                TableColumn<Student, String> dayColumn = createDayColumn(currentDate);
                currentWeekColumn.getColumns().add(dayColumn);
            }
            currentDate = currentDate.plusDays(1);
        }

        Platform.runLater(this::adjustColumnWidths);
    }

    private void adjustColumnWidths() {
        double availableWidth = attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth() - 20;
        int weekCount = monthAttendanceColumn.getColumns().size();
        if (weekCount > 0) {
            double baseWeekWidth = availableWidth / weekCount;
            double extraHeaderSpace = 40; // Extra space to preserve header title
            for (int i = 0; i < weekCount; i++) {
                TableColumn<?, ?> weekCol = monthAttendanceColumn.getColumns().get(i);
                int daysInWeek = weekCol.getColumns().size();
                double finalWeekWidth = baseWeekWidth;
                if (daysInWeek < 5) {
                    // allocate extra space for header
                    finalWeekWidth += extraHeaderSpace;
                }
                weekCol.setMinWidth(finalWeekWidth);
                weekCol.setPrefWidth(finalWeekWidth);
                weekCol.setMaxWidth(finalWeekWidth);
                if (daysInWeek > 0) {
                    double availableForDays = (daysInWeek < 5) ? (finalWeekWidth - extraHeaderSpace) : finalWeekWidth;
                    double dayWidth = availableForDays / daysInWeek;
                    weekCol.getColumns().forEach(dayCol -> {
                        dayCol.setMinWidth(dayWidth);
                        dayCol.setPrefWidth(dayWidth);
                        dayCol.setMaxWidth(dayWidth);
                    });
                }
            }
        }
    }

    private void disableColumnReordering(TableView<?> table) {
        for (TableColumn<?, ?> column : table.getColumns()) {
            column.setReorderable(false);
            disableColumnReorderingRecursive(column);
        }
    }

    private void disableColumnReorderingRecursive(TableColumn<?, ?> parent) {
        if (!parent.getColumns().isEmpty()) {
            for (TableColumn<?, ?> child : parent.getColumns()) {
                child.setReorderable(false);
                disableColumnReorderingRecursive(child);
            }
        }
    }

    private TableColumn<Student, String> createWeekColumn(int weekNumber) {
        TableColumn<Student, String> weekColumn = new TableColumn<>("Week " + weekNumber);
        weekColumn.setStyle("-fx-alignment: CENTER;");
        weekColumn.setMinWidth(150);
        weekColumn.setReorderable(false);  // Disable reordering on week columns
        return weekColumn;
    }

    private TableColumn<Student, String> createDayColumn(LocalDate date) {
        TableColumn<Student, String> dayColumn = WeeklyAttendanceUtil.createDayColumn(date, attendanceLog);
        dayColumn.setMinWidth(52);
        dayColumn.setPrefWidth(52);
        dayColumn.setMaxWidth(69);
        dayColumn.setResizable(false);
        dayColumn.setReorderable(false);

        // Add a cell factory to support context menu editing of attendance
        dayColumn.setCellFactory(column -> {
            TableCell<Student, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                    }
                }
            };

            ContextMenu contextMenu = new ContextMenu();
            MenuItem editAttendanceItem = new MenuItem("Edit Attendance");
            editAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    String currentStatus = cell.getText();
                    ComboBox<String> comboBox = new ComboBox<>();
                    comboBox.getItems().addAll(
                            AttendanceUtil.PRESENT_MARK,
                            AttendanceUtil.ABSENT_MARK,
                            AttendanceUtil.HALF_DAY_MARK,
                            AttendanceUtil.EXCUSED_MARK
                    );
                    comboBox.setValue(currentStatus.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentStatus);
                    cell.setGraphic(comboBox);
                    cell.setText(null);

                    Platform.runLater(() -> {
                        comboBox.requestFocus();
                        comboBox.show();
                    });

                    // When focus is lost, update attendance, refresh, and apply transition.
                    comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                        if (!isFocused) {
                            String newValue = comboBox.getValue();
                            if (newValue != null) {
                                if (newValue.equals(AttendanceUtil.EXCUSED_MARK)) {
                                    DataUtil.createExcusedAttendance(student, date);
                                } else {
                                    updateAttendanceRecord(student, date, newValue);
                                }
                            }
                            cell.setGraphic(null);
                            cell.setText(newValue);
                            attendanceTable.refresh();
                            TableViewTransitions.applyUpdateTransition(attendanceTable);
                        }
                    });

                    // Also update attendance on action and then apply transition.
                    comboBox.setOnAction(event -> {
                        String newValue = comboBox.getValue();
                        if (newValue != null) {
                            if (newValue.equals(AttendanceUtil.EXCUSED_MARK)) {
                                DataUtil.createExcusedAttendance(student, date);
                            } else {
                                updateAttendanceRecord(student, date, newValue);
                            }
                            cell.setGraphic(null);
                            cell.setText(newValue);
                            setupMonthColumns();
                            attendanceTable.refresh();
                            TableViewTransitions.applyUpdateTransition(attendanceTable);
                        }
                    });
                }
            });
            contextMenu.getItems().add(editAttendanceItem);
            cell.setContextMenu(contextMenu);

            return cell;
        });

        return dayColumn;
    }

    private String getSelectedYearOrDefault() {
        Object paramYear = getParameter("selectedYear");
        if (paramYear != null) {
            return (String) paramYear;
        }
        LocalDate now = LocalDate.now();
        int year = now.getMonthValue() >= 6 ? now.getYear() : now.getYear() - 1;
        return year + "-" + (year + 1);
    }

    public void initializeWithYear(String year) {
        if (year == null) {
            return;
        }
        // Parse academic year (e.g., "2023-2024")
        String[] yearRange = year.split("-");
        int startYear = Integer.parseInt(yearRange[0]);
        int endYear = Integer.parseInt(yearRange[1]);

        // Filter the student list based on the academic year
        filteredStudentList.setPredicate(student -> {
            if (student.getYearID() == null) {
                return false;
            }
            return student.getYearID().getYearStart() == startYear
                    && student.getYearID().getYearEnd() == endYear;
        });

        // Update the month/year ComboBox based on the academic year
        if (monthYearComboBox != null) {
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);
            YearMonth current = YearMonth.now();
            String currentFormatted = current.format(DateTimeUtils.MONTH_YEAR_FORMATTER);
            if (monthYearComboBox.getItems().contains(currentFormatted)) {
                monthYearComboBox.setValue(currentFormatted);
            } else if (!monthYearComboBox.getItems().isEmpty()) {
                monthYearComboBox.setValue(monthYearComboBox.getItems().get(0));
            }
            setupMonthColumns();
        }
        updateStudentCountLabels();
    }

    private void updateStudentCountLabels() {
        int selectedStudents = attendanceTable.getSelectionModel().getSelectedItems().size();
        int totalStudents = filteredStudentList.size();
        selectedStudentsLabel.setText("Selected: " + selectedStudents);
        totalStudentsLabel.setText("Total: " + totalStudents);
    }

    private void handleExport(String type) {
        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null) {
            return;
        }

        String title = "Attendance Report - " + selectedMonthYear;
        // Map "excel" to "xlsx" as the file extension; for pdf and csv, use the type directly
        String ext = type.equalsIgnoreCase("excel") ? "xlsx" : type.toLowerCase();
        String fileName = String.format("attendance_%s.%s",
                selectedMonthYear.replace(" ", "_").toLowerCase(),
                ext);
        String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

        try {
            AttendanceTableExporter exporter = new sms.admin.util.exporter.AttendanceTableExporter();

            switch (type.toLowerCase()) {
                case "excel" ->
                    exporter.exportToExcel(attendanceTable, title, outputPath);
                case "pdf" ->
                    exporter.exportToPdf(attendanceTable, title, outputPath);
                case "csv" ->
                    exporter.exportToCsv(attendanceTable, title, outputPath);
                default -> {
                    System.out.println("Invalid export type: " + type);
                    return;
                }
            }

            System.out.println("Export completed: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAttendanceRecord(Student student, LocalDate date, String attendanceValue) {
        // Retrieve attendance records and logs (implementation assumed in DataUtil)
        var records = DataUtil.createAttendanceRecordList();
        var logs = DataUtil.createAttendanceLogList();

        AttendanceLog log = AttendanceUtil.findOrCreateAttendanceLog(student, date, logs, records);

        // Update attendance log based on the provided value
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
                log.setTimeInAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutAM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeInPM(AttendanceUtil.TIME_EXCUSED);
                log.setTimeOutPM(AttendanceUtil.TIME_EXCUSED);
                break;
        }

        if (!logs.contains(log)) {
            logs.add(log);
        }
        attendanceLog = logs;
    }
}
