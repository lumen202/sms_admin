package sms.admin.app.attendance;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import atlantafx.base.controls.ModalPane;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import sms.admin.app.attendance.dialog.AttendanceLogDialog;
import sms.admin.app.attendance.dialog.AttendanceLogDialogController;
import sms.admin.app.student.viewstudent.StudentProfileLoader;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.WeeklyAttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.exporter.AttendanceTableExporter;
import sms.admin.util.mock.DataUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TablePosition;

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

    @Override
    protected void load_bindings() {
        // Comment out binding to test if manual visibility works
        // dialogContainer.visibleProperty().bind(modalContainer.displayProperty());
    }

    @Override
    protected void load_fields() {
        // Initialize lists with fresh data
        studentList = FXCollections.observableArrayList(DataUtil.createStudentList());
        attendanceLog = FXCollections.observableArrayList(DataUtil.createAttendanceLogList());

        // Setup table
        setupTable();
        setupColumnWidths();

        // Initialize with selected year
        String selectedYear = getSelectedYearOrDefault();
        initializeWithYear(selectedYear);

        setupMonthColumns();
        updateStudentCountLabels();
    }

    private void setupTable() {
        // Configure student list columns
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String fullName = String.format("%s, %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName());
            return new SimpleStringProperty(fullName);
        });

        // Set items
        attendanceTable.setItems(studentList);

        // Set selection mode
        attendanceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private TableCell<?, ?> findCell(int row, TableColumn<?, ?> column) {
        for (Node node : attendanceTable.lookupAll(".table-cell")) {
            if (node instanceof TableCell) {
                @SuppressWarnings("unchecked")
                TableCell<?, ?> cell = (TableCell<?, ?>) node;
                if (cell.getTableRow() != null
                        && cell.getTableRow().getIndex() == row
                        && cell.getTableColumn() == column) {
                    return cell;
                }
            }
        }
        return null;
    }

    private void setupColumnWidths() {
        // Fixed columns - base values reduced by 5%
        colNo.setPrefWidth(44);  // 38 * 1.15 (base reduced from 40)
        colNo.setMinWidth(44);
        colNo.setMaxWidth(44);
        colNo.setResizable(false);

        colFullName.setPrefWidth(148);  // Reduced by 10% from 164
        colFullName.setMinWidth(148);   // Reduced by 10% from 164
        colFullName.setMaxWidth(246);   // Reduced by 10% from 273

        // Set monthAttendanceColumn to fill remaining space
        monthAttendanceColumn.setPrefWidth(655);  // 570 * 1.15 (base reduced from 600)
    }

    private void setupMonthColumns() {
        monthAttendanceColumn.getColumns().clear();

        String selectedMonthYear = monthYearComboBox.getValue();
        if (selectedMonthYear == null) {
            return;
        }

        // Parse month and year
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
            if (currentDate.getDayOfWeek() == DayOfWeek.MONDAY
                    && !currentWeekColumn.getColumns().isEmpty()) {
                weekNumber++;
                currentWeekColumn = createWeekColumn(weekNumber);
                monthAttendanceColumn.getColumns().add(currentWeekColumn);
            }

            if (!AttendanceUtil.isWeekend(currentDate)) {
                final LocalDate cellDate = currentDate;
                TableColumn<Student, String> dayColumn = createDayColumn(cellDate);
                currentWeekColumn.getColumns().add(dayColumn);
            }

            currentDate = currentDate.plusDays(1);
        }

        // Calculate and set widths after adding all columns
        Platform.runLater(this::adjustColumnWidths);
    }

    private void adjustColumnWidths() {
        double availableWidth = attendanceTable.getWidth() - colNo.getWidth() - colFullName.getWidth() - 20;
        int weekCount = monthAttendanceColumn.getColumns().size();

        if (weekCount > 0) {
            // Ensure equal width for all weeks
            double weekWidth = availableWidth / weekCount;

            monthAttendanceColumn.getColumns().forEach(weekCol -> {
                TableColumn<?, ?> column = (TableColumn<?, ?>) weekCol;
                int daysInWeek = column.getColumns().size();

                // Set fixed width for the week column with 15% increase
                column.setMinWidth(weekWidth * 1.15);
                column.setPrefWidth(weekWidth * 1.15);
                column.setMaxWidth(weekWidth * 1.15);

                if (daysInWeek > 0) {
                    // For weeks with few days, center them by adding padding
                    double dayBaseWidth = (weekWidth * 1.15) / Math.max(5, daysInWeek);
                    double totalDayWidth = dayBaseWidth * daysInWeek;
                    double padding = ((weekWidth * 1.15) - totalDayWidth) / 2;

                    // Apply width to day columns
                    column.getColumns().forEach(dayCol -> {
                        TableColumn<?, ?> dc = (TableColumn<?, ?>) dayCol;
                        dc.setMinWidth(dayBaseWidth);
                        dc.setPrefWidth(dayBaseWidth);
                        dc.setMaxWidth(dayBaseWidth);
                    });
                }
            });
        }
    }

    @Override
    protected void load_listeners() {
        monthYearComboBox.setOnAction(event -> setupMonthColumns());

        // Add export menu item handlers
        exportExcel.setOnAction(event -> handleExport("excel"));
        exportCsv.setOnAction(event -> handleExport("csv"));
        exportPdf.setOnAction(event -> handleExport("pdf"));

        // Add selection listener
        attendanceTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateStudentCountLabels());

        // Add resize listener for dynamic column adjustment
        attendanceTable.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                Platform.runLater(this::adjustColumnWidths);
            }
        });

        // Remove the monthAttendanceColumn width listener since we're not using bindings
    }

    private void handleExport(String type) {
        try {
            String selectedMonthYear = monthYearComboBox.getValue();
            if (selectedMonthYear == null) {
                return;
            }

            String title = "Attendance Report - " + selectedMonthYear;
            String fileName = String.format("attendance_%s.%s",
                    selectedMonthYear.replace(" ", "_").toLowerCase(),
                    type.equals("excel") ? "xlsx" : type.toLowerCase());
            String outputPath = System.getProperty("user.home") + "/Downloads/" + fileName;

            AttendanceTableExporter exporter = new AttendanceTableExporter();
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

    private TableColumn<Student, String> createWeekColumn(int weekNumber) {
        TableColumn<Student, String> weekColumn = new TableColumn<>("Week " + weekNumber);
        weekColumn.setStyle("-fx-alignment: CENTER;");
        weekColumn.setMinWidth(150);
        return weekColumn;
    }

    private TableColumn<Student, String> createDayColumn(LocalDate date) {
        TableColumn<Student, String> dayColumn = WeeklyAttendanceUtil.createDayColumn(date, attendanceLog);
        dayColumn.setMinWidth(52);  // 45 * 1.15
        dayColumn.setPrefWidth(52); // 45 * 1.15
        dayColumn.setMaxWidth(69);  // 60 * 1.15
        dayColumn.setResizable(false);

        // Add cell factory for handling context menu
        dayColumn.setCellFactory(column -> {
            TableCell<Student, String> cell = new TableCell<Student, String>() {
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
            MenuItem viewAttendanceItem = new MenuItem("View Attendance Log");
            MenuItem editAttendanceItem = new MenuItem("Edit Attendance");

            viewAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    showAttendanceLogDialog(student, date);
                }
            });

            editAttendanceItem.setOnAction(e -> {
                Student student = cell.getTableRow().getItem();
                if (student != null) {
                    String currentStatus = cell.getText(); // Get current cell value
                    ComboBox<String> comboBox = new ComboBox<>();
                    comboBox.getItems().addAll(
                            AttendanceUtil.PRESENT_MARK,
                            AttendanceUtil.ABSENT_MARK,
                            AttendanceUtil.HALF_DAY_MARK,
                            AttendanceUtil.EXCUSED_MARK
                    );

                    // Set default value from cell's current text
                    comboBox.setValue(currentStatus.isEmpty() ? AttendanceUtil.PRESENT_MARK : currentStatus);

                    cell.setGraphic(comboBox);
                    cell.setText(null);

                    // Request focus after showing
                    Platform.runLater(() -> {
                        comboBox.requestFocus();
                        comboBox.show();
                    });

                    // Handle focus lost
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
                        }
                    });

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
                        }
                    });
                }
            });

            contextMenu.getItems().addAll(viewAttendanceItem, editAttendanceItem);
            cell.setContextMenu(contextMenu);

            return cell;
        });

        return dayColumn;
    }

    private void showAttendanceLogDialog(Student student, LocalDate date) {
        new AttendanceLogDialog(student, date, attendanceLog);
        System.out.println("Attendance Log dialog closed.");
    }

    public void updateYear(String newYear) {
        initializeWithYear(newYear);
    }

    public void initializeWithYear(String year) {
        if (year == null) {
            return;
        }

        // Parse the year range (e.g., "2023-2024")
        String[] yearRange = year.split("-");
        int startYear = Integer.parseInt(yearRange[0]);
        int endYear = Integer.parseInt(yearRange[1]);

        // Filter students based on school year
        studentList = DataUtil.createStudentList().filtered(student -> {
            if (student.getYearID() == null) {
                return false;
            }
            SchoolYear schoolYear = student.getYearID();
            return schoolYear.getYearStart() == startYear
                    && schoolYear.getYearEnd() == endYear;
        });

        // Update table
        attendanceTable.setItems(studentList);

        // Update month combo box and select appropriate month
        if (monthYearComboBox != null) {
            DateTimeUtils.updateMonthYearComboBox(monthYearComboBox, year);

            // Get current month-year
            YearMonth current = YearMonth.now();
            String currentFormatted = current.format(DateTimeUtils.MONTH_YEAR_FORMATTER);

            // Try to select current month if it's in the list
            if (monthYearComboBox.getItems().contains(currentFormatted)) {
                monthYearComboBox.setValue(currentFormatted);
            } // If current month is not in the academic year, select first month
            else if (!monthYearComboBox.getItems().isEmpty()) {
                monthYearComboBox.setValue(monthYearComboBox.getItems().get(0));
            }

            setupMonthColumns();
        }

        updateStudentCountLabels();
    }

    @FXML
    private void handleViewStudentButton() {
        initializeViewStudent();
    }

    private void initializeViewStudent() {
        StudentProfileLoader loader = (StudentProfileLoader) FXLoaderFactory
                .createInstance(StudentProfileLoader.class,
                        getClass().getResource(STUDENT_PROFILE_FXML))
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

    private LocalDate getDayColumnDate(String columnText) {
        // Parse day number from column header (e.g., "1M" -> 1)
        int day = Integer.parseInt(columnText.replaceAll("[MTWF]", ""));
        String monthYear = monthYearComboBox.getValue();
        String[] parts = monthYear.split(" ");
        Month month = Month.valueOf(parts[0].toUpperCase());
        int year = Integer.parseInt(parts[1]);
        return LocalDate.of(year, month, day);
    }

    private void updateAttendanceRecord(Student student, LocalDate date, String attendanceValue) {
        ObservableList<AttendanceRecord> records = DataUtil.createAttendanceRecordList();
        ObservableList<AttendanceLog> logs = DataUtil.createAttendanceLogList();

        AttendanceLog log = AttendanceUtil.findOrCreateAttendanceLog(
                student, date, logs, records);

        // Update the log based on attendance value
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

        // Update the main attendance log list
        attendanceLog = logs;
    }
}
