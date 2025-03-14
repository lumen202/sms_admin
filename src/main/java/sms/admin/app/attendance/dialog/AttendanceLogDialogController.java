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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.dialog.DialogManager;

public class AttendanceLogDialogController extends FXController {
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
        // Set header information
        studentNameLabel.setText(String.format("%s, %s", student.getLastName(), student.getFirstName()));
        dateLabel.setText(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));

        // Filter logs more accurately
        List<AttendanceLog> matchingLogs = allLogs.stream()
                .filter(l -> l != null && l.getStudentID() != null && l.getRecordID() != null)
                .filter(l -> l.getStudentID().getStudentID() == student.getStudentID())
                .filter(l -> {
                    AttendanceRecord record = l.getRecordID();
                    return record.getYear() == date.getYear() 
                        && record.getMonth() == date.getMonthValue() 
                        && record.getDay() == date.getDayOfMonth();
                })
                .toList();

        setupTableColumns();

        // Update tables with logs
        ObservableList<AttendanceLog> logList = FXCollections.observableArrayList(matchingLogs);
        amLogTable.setItems(logList);
        pmLogTable.setItems(logList);

        // Move styling to CSS
        if (contentBox != null) {
            contentBox.getStyleClass().add("dialog-content");
        }
        
        if (amLogTable != null) {
            amLogTable.getStyleClass().add("attendance-table");
        }
        
        if (pmLogTable != null) {
            pmLogTable.getStyleClass().add("attendance-table");
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

    private void setupTableColumns() {
        // AM columns
        timeInAMColumn.setCellValueFactory(
                data -> new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeInAM())));
        timeOutAMColumn.setCellValueFactory(
                data -> new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeOutAM())));

        // PM columns
        timeInPMColumn.setCellValueFactory(
                data -> new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeInPM())));
        timeOutPMColumn.setCellValueFactory(
                data -> new SimpleStringProperty(AttendanceUtil.formatTime(data.getValue().getTimeOutPM())));

        // Set equal column widths
        timeInAMColumn.setPrefWidth(150);
        timeOutAMColumn.setPrefWidth(150);
        timeInPMColumn.setPrefWidth(150);
        timeOutPMColumn.setPrefWidth(150);
    }

    @FXML
    private void handleClose() {
        // Get the window and find the parent Dialog
        Window window = closeButton.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    @Override
    protected void load_bindings() {
        // Remove console logging
    }

    @Override
    protected void load_listeners() {
    }
}
