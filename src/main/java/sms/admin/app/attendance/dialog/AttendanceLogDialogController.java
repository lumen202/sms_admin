package sms.admin.app.attendance.dialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import sms.admin.util.attendance.AttendanceUtil;

public class AttendanceLogDialogController extends FXController {
    @FXML private Label studentNameLabel;
    @FXML private Label dateLabel;
    @FXML private TableView<AttendanceLog> amLogTable;
    @FXML private TableView<AttendanceLog> pmLogTable;
    @FXML private TableColumn<AttendanceLog, String> timeInAMColumn;
    @FXML private TableColumn<AttendanceLog, String> timeOutAMColumn;
    @FXML private TableColumn<AttendanceLog, String> timeInPMColumn;
    @FXML private TableColumn<AttendanceLog, String> timeOutPMColumn;
    @FXML private Button closeButton;

    public AttendanceLogDialogController(){
        System.out.println("AttendanceLogDialogController constructor called");
    }

    public void initData(Student student, LocalDate date, List<AttendanceLog> allLogs) {
        studentNameLabel.setText(String.format("%s, %s", student.getLastName(), student.getFirstName()));
        dateLabel.setText(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));

        List<AttendanceLog> filteredLogs = allLogs.stream()
            .filter(log -> log.getStudentID().equals(student)
                && log.getRecordID().getYear() == date.getYear()
                && log.getRecordID().getMonth() == date.getMonthValue()
                && log.getRecordID().getDay() == date.getDayOfMonth())
            .collect(Collectors.toList());

        setupTableColumns();
        amLogTable.setItems(FXCollections.observableArrayList(filteredLogs));
        pmLogTable.setItems(FXCollections.observableArrayList(filteredLogs));
    }

    @Override
    protected void load_fields() {
        setupTableColumns();
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
            
        // Set equal column widths
        timeInAMColumn.setPrefWidth(150);
        timeOutAMColumn.setPrefWidth(150);
        timeInPMColumn.setPrefWidth(150);
        timeOutPMColumn.setPrefWidth(150);
    }

    @FXML
    private void handleClose() {
        // Instead of casting to ModalPane, close the window directly
        closeButton.getScene().getWindow().hide();
    }

    @Override
    protected void load_bindings() {
        System.out.println("Attendance Log Dialog is Called");
    }

    @Override
    protected void load_listeners() {}
}
