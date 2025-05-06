package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sms.admin.util.attendance.CommonAttendanceUtil;
import sms.admin.util.dialog.DialogManager;

/**
 * Controller for the attendance log dialog, displaying detailed attendance
 * records for a student in a specific month.
 * This class manages the UI elements and logic for showing daily attendance
 * logs, including time in and out for morning and afternoon sessions.
 */
public class AttendanceLogDialogController extends FXController {
    private Stage stage;

    @FXML
    private Label studentNameLabel; // Label to display the student's full name
    @FXML
    private Label dateLabel; // Label to display the selected month and year
    @FXML
    private TableView<AttendanceLog> logTable; // Table to display attendance logs
    @FXML
    private TableColumn<AttendanceLog, String> dateColumn; // Column for the day of the month
    @FXML
    private TableColumn<AttendanceLog, String> timeInAMColumn; // Column for morning time in
    @FXML
    private TableColumn<AttendanceLog, String> timeOutAMColumn; // Column for morning time out
    @FXML
    private TableColumn<AttendanceLog, String> timeInPMColumn; // Column for afternoon time in
    @FXML
    private TableColumn<AttendanceLog, String> timeOutPMColumn; // Column for afternoon time out
    @FXML
    private Button closeButton; // Button to close the dialog
    @FXML
    private VBox contentBox; // Container for the dialog content

    private Student currentStudent; // The student whose attendance is being viewed
    private LocalDate currentDate; // The date (month and year) for the attendance logs
    private ObservableList<AttendanceLog> currentLogs; // List of attendance logs for the table

    /**
     * Sets the stage for this dialog.
     *
     * @param stage The stage to set for this dialog.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Default constructor for the controller. Prints a message to indicate
     * instantiation.
     */
    public AttendanceLogDialogController() {
        System.out.println("AttendanceLogDialogController constructor called");
    }

    /**
     * Initializes the dialog with the student, date, and attendance logs.
     *
     * @param student The student whose attendance is being viewed.
     * @param date    The date (month and year) for which attendance is being
     *                viewed.
     * @param allLogs The list of all attendance logs to process.
     */
    public void initData(Student student, LocalDate date, List<AttendanceLog> allLogs) {
        // Set fields first
        this.currentStudent = student;
        this.currentDate = date;
        this.currentLogs = FXCollections.observableArrayList();

        // Set up labels before loading logs
        if (currentStudent != null) {
            String studentName = String.format("%s, %s %s",
                    currentStudent.getLastName(),
                    currentStudent.getFirstName(),
                    currentStudent.getMiddleName() != null ? currentStudent.getMiddleName() : "");
            studentNameLabel.setText(studentName.trim());
        }

        if (currentDate != null) {
            dateLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }

        // Refresh logs after fields are set
        refreshLogs(allLogs);
    }

    /**
     * Refreshes the attendance logs in the table based on the provided list of all
     * logs.
     * Excludes weekends and future dates, marking absent days appropriately.
     *
     * @param allLogs The list of all attendance logs to filter and display.
     */
    public void refreshLogs(List<AttendanceLog> allLogs) {
        if (currentStudent == null || currentDate == null) {
            System.err.println("Cannot refresh logs - student or date is null");
            return;
        }

        setupTableColumns();
        logTable.setFixedCellSize(35);
        logTable.setEditable(false);
        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Create a map for all days in the month
        Map<Integer, AttendanceLog> monthLogs = new TreeMap<>();
        int daysInMonth = currentDate.lengthOfMonth();
        // Initialize all weekdays with null (absent) up to the current date
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDayDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), day);
            if (!currentDayDate.isAfter(LocalDate.now()) && !CommonAttendanceUtil.isWeekend(currentDayDate)) {
                monthLogs.put(day, null);
            }
        }

        // Fill in actual logs, excluding weekends
        if (allLogs != null) {
            allLogs.stream()
                    .filter(l -> l != null &&
                            l.getStudentID() != null &&
                            l.getStudentID().getStudentID() == currentStudent.getStudentID() &&
                            l.getRecordID() != null &&
                            l.getRecordID().getYear() == currentDate.getYear() &&
                            l.getRecordID().getMonth() == currentDate.getMonthValue() &&
                            !CommonAttendanceUtil.isWeekend(LocalDate.of(
                                    l.getRecordID().getYear(),
                                    l.getRecordID().getMonth(),
                                    l.getRecordID().getDay())))
                    .forEach(log -> monthLogs.put(log.getRecordID().getDay(), log));
        }

        // Create observable list with all weekdays
        ObservableList<AttendanceLog> logList = FXCollections.observableArrayList();
        monthLogs.forEach((day, log) -> {
            if (log == null) {
                // Create a dummy log for absent days
                AttendanceRecord record = new AttendanceRecord(-1, currentDate.getMonthValue(), day,
                        currentDate.getYear());
                AttendanceLog absentLog = new AttendanceLog(-1, record, currentStudent,
                        CommonAttendanceUtil.TIME_ABSENT,
                        CommonAttendanceUtil.TIME_ABSENT,
                        CommonAttendanceUtil.TIME_ABSENT,
                        CommonAttendanceUtil.TIME_ABSENT);
                logList.add(absentLog);
            } else {
                logList.add(log);
            }
        });

        logTable.setItems(logList);
        logTable.refresh();
    }

    /**
     * Restores the state from a previous controller instance.
     *
     * @param previous The previous controller instance to restore state from.
     */
    public void restoreState(AttendanceLogDialogController previous) {
        if (previous != null && previous.currentLogs != null) {
            this.currentLogs = previous.currentLogs;
        }
    }

    /**
     * Loads the initial fields and configurations for the UI, such as spacing and
     * padding.
     */
    @Override
    protected void load_fields() {
        if (contentBox != null) {
            contentBox.setSpacing(15);
            contentBox.setPadding(new Insets(10));
        }
    }

    /**
     * Loads bindings for UI components. No bindings are needed in this
     * implementation.
     */
    @Override
    protected void load_bindings() {
        // No bindings needed
    }

    /**
     * Loads event listeners for UI interactions, such as the close button action.
     */
    @Override
    protected void load_listeners() {
        if (closeButton != null) {
            closeButton.setOnAction(e -> handleClose());
        }
    }

    /**
     * Sets up the table columns for displaying attendance logs, organizing them
     * into morning and afternoon groups.
     */
    @SuppressWarnings("unchecked")
    private void setupTableColumns() {
        dateColumn.setText("Day");

        // Create AM/PM group columns
        TableColumn<AttendanceLog, String> amGroup = new TableColumn<>("Morning");
        amGroup.getColumns().addAll(timeInAMColumn, timeOutAMColumn);
        timeInAMColumn.setText("Time In");
        timeOutAMColumn.setText("Time Out");

        TableColumn<AttendanceLog, String> pmGroup = new TableColumn<>("Afternoon");
        pmGroup.getColumns().addAll(timeInPMColumn, timeOutPMColumn);
        timeInPMColumn.setText("Time In");
        timeOutPMColumn.setText("Time Out");

        // Clear existing columns and add new structure
        logTable.getColumns().clear();
        logTable.getColumns().addAll(dateColumn, amGroup, pmGroup);

        // Setup cell factories with tooltips
        dateColumn.setCellFactory(col -> createTooltipCell("Day of Month"));
        timeInAMColumn.setCellFactory(col -> createTooltipCell("Morning Time In"));
        timeOutAMColumn.setCellFactory(col -> createTooltipCell("Morning Time Out"));
        timeInPMColumn.setCellFactory(col -> createTooltipCell("Afternoon Time In"));
        timeOutPMColumn.setCellFactory(col -> createTooltipCell("Afternoon Time Out"));

        // Configure cell value factories
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatDay(cellData.getValue())));

        timeInAMColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(getCellValue(cellData.getValue(), AttendanceLog::getTimeInAM)));

        timeOutAMColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(getCellValue(cellData.getValue(), AttendanceLog::getTimeOutAM)));

        timeInPMColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(getCellValue(cellData.getValue(), AttendanceLog::getTimeInPM)));

        timeOutPMColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(getCellValue(cellData.getValue(), AttendanceLog::getTimeOutPM)));

        // Apply common styles to columns
        List.of(dateColumn, timeInAMColumn, timeOutAMColumn, timeInPMColumn, timeOutPMColumn)
                .forEach(col -> {
                    col.setStyle("-fx-alignment: CENTER;");
                    col.setSortable(false);
                    col.setResizable(true);
                });
    }

    /**
     * Formats the day for display in the table, showing "-" for future dates.
     *
     * @param log The attendance log to format.
     * @return The formatted day string (e.g., "01" or "-").
     */
    private String formatDay(AttendanceLog log) {
        if (log == null || log.getRecordID() == null)
            return "";
        AttendanceRecord record = log.getRecordID();
        LocalDate logDate = LocalDate.of(record.getYear(), record.getMonth(), record.getDay());
        if (logDate.isAfter(LocalDate.now())) {
            return "-";
        }
        return String.format("%02d", record.getDay());
    }

    /**
     * Creates a table cell with a tooltip for better user interaction.
     *
     * @param tooltipText The text to display in the tooltip.
     * @return A configured TableCell instance.
     */
    private TableCell<AttendanceLog, String> createTooltipCell(String tooltipText) {
        return new TableCell<>() {
            final Tooltip tooltip = new Tooltip(tooltipText);

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(tooltip);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    /**
     * Retrieves and formats the cell value for a specific time field (e.g., time
     * in/out).
     *
     * @param log        The attendance log to process.
     * @param timeGetter The function to extract the time value from the log.
     * @return The formatted time string (e.g., "08:00" or "--:--").
     */
    private String getCellValue(AttendanceLog log, java.util.function.Function<AttendanceLog, Integer> timeGetter) {
        if (log == null)
            return "";
        if (log.getLogID() == -1)
            return "--:--"; // Dummy log for absent
        return CommonAttendanceUtil.formatTime(timeGetter.apply(log));
    }

    /**
     * Handles the close action for the dialog, fading it out.
     */
    @FXML
    private void handleClose() {
        DialogManager.closeWithFade(stage, null);
    }
}