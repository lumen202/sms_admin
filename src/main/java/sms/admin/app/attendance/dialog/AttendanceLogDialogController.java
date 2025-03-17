package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.dialog.DialogManager;

public class AttendanceLogDialogController extends FXController {
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private Label studentNameLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private TableView<AttendanceLog> amLogTable;
    @FXML
    private TableView<AttendanceLog> pmLogTable;
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
    private VBox contentBox; // Add this field to your FXML

    public AttendanceLogDialogController() {
        System.out.println("AttendanceLogDialogController constructor called");
    }

    public void initData(Student student, LocalDate date, List<AttendanceLog> allLogs) {
        if (student != null) {
            String studentName = String.format("%s, %s %s", 
                student.getLastName(),
                student.getFirstName(),
                student.getMiddleName() != null ? student.getMiddleName() : "");
            studentNameLabel.setText(studentName.trim());
        }

        if (date != null) {
            dateLabel.setText(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        }

        // Find matching log
        AttendanceLog matchingLog = allLogs.stream()
            .filter(l -> l != null && l.getRecordID() != null && l.getStudentID() != null)
            .filter(l -> l.getStudentID().getStudentID() == student.getStudentID())
            .filter(l -> {
                AttendanceRecord record = l.getRecordID();
                return record.getYear() == date.getYear() 
                    && record.getMonth() == date.getMonthValue() 
                    && record.getDay() == date.getDayOfMonth();
            })
            .findFirst()
            .orElse(null);

        setupTableColumns();

        // Update tables with single log if found
        if (matchingLog != null) {
            ObservableList<AttendanceLog> logList = FXCollections.observableArrayList(matchingLog);
            amLogTable.setItems(logList);
            pmLogTable.setItems(logList);
        } else {
            amLogTable.setItems(FXCollections.emptyObservableList());
            pmLogTable.setItems(FXCollections.emptyObservableList());
        }
    }

    @Override
    protected void load_fields() {
        setupTableColumns();
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
        // Add button listener
        if (closeButton != null) {
            closeButton.setOnAction(e -> handleClose());
        }
    }

    private void setupTableColumns() {
        // AM columns
        timeInAMColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeInAM())));
        timeOutAMColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeOutAM())));

        // PM columns
        timeInPMColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeInPM())));
        timeOutPMColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeOutPM())));

        // Set column widths
        timeInAMColumn.setPrefWidth(160);
        timeOutAMColumn.setPrefWidth(160);
        timeInPMColumn.setPrefWidth(160);
        timeOutPMColumn.setPrefWidth(160);
    }

    @FXML
    private void handleClose() {
        DialogManager.closeWithFade(stage, null);
    }
}
