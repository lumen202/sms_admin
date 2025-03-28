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
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.attendance.CommonAttendanceUtil;
import sms.admin.util.dialog.DialogManager;

public class AttendanceLogDialogController extends FXController {
    private Stage stage;

    @FXML
    private Label studentNameLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private TableView<AttendanceLog> logTable;
    @FXML
    private TableColumn<AttendanceLog, String> dateColumn;
    @FXML
    private TableColumn<AttendanceLog, String> timeInAMColumn;
    @FXML
    private TableColumn<AttendanceLog, String> timeOutAMColumn;
    @FXML
    private TableColumn<AttendanceLog, String> timeInPMColumn;
    @FXML
    private TableColumn<AttendanceLog, String> timeOutPMColumn;
    @FXML
    private Button closeButton;
    @FXML
    private VBox contentBox;

    private Student currentStudent;
    private LocalDate currentDate;
    private ObservableList<AttendanceLog> currentLogs;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public AttendanceLogDialogController() {
        System.out.println("AttendanceLogDialogController constructor called");
    }

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

        // Now refresh logs after fields are set
        refreshLogs(allLogs);
    }

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
        // Initialize all days with null (will be shown as absent), excluding weekends
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
                AttendanceRecord record = new AttendanceRecord(-1, currentDate.getMonthValue(), day,
                        currentDate.getYear());
                AttendanceLog absentLog = new AttendanceLog(-1, record, currentStudent,
                        AttendanceUtil.TIME_ABSENT,
                        AttendanceUtil.TIME_ABSENT,
                        AttendanceUtil.TIME_ABSENT,
                        AttendanceUtil.TIME_ABSENT);
                logList.add(absentLog);
            } else {
                logList.add(log);
            }
        });

        logTable.setItems(logList);
        logTable.refresh();
    }

    public void restoreState(AttendanceLogDialogController previous) {
        if (previous != null && previous.currentLogs != null) {
            this.currentLogs = previous.currentLogs;
        }
    }

    @Override
    protected void load_fields() {
        if (contentBox != null) {
            contentBox.setSpacing(15);
            contentBox.setPadding(new Insets(10));
        }
    }

    @Override
    protected void load_bindings() {
        // No bindings needed
    }

    @Override
    protected void load_listeners() {
        if (closeButton != null) {
            closeButton.setOnAction(e -> handleClose());
        }
    }

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

        // Apply common styles
        List.of(dateColumn, timeInAMColumn, timeOutAMColumn, timeInPMColumn, timeOutPMColumn)
                .forEach(col -> {
                    col.setStyle("-fx-alignment: CENTER;");
                    col.setSortable(false);
                    col.setResizable(true);
                });
    }

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

    private String getCellValue(AttendanceLog log, java.util.function.Function<AttendanceLog, Integer> timeGetter) {
        if (log == null)
            return "";
        if (log.getLogID() == -1)
            return "--:--"; // Dummy log for absent
        return AttendanceUtil.formatTime(timeGetter.apply(log));
    }

    @FXML
    private void handleClose() {
        DialogManager.closeWithFade(stage, null);
    }
}
