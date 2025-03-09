package sms.admin.app.payroll;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.text.DecimalFormat;

import dev.finalproject.models.AttendanceLog;
import dev.finalproject.models.AttendanceRecord;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Label;
import sms.admin.util.YearData;
import sms.admin.util.attendance.AttendanceUtil;
import sms.admin.util.datetime.DateTimeUtils;
import sms.admin.util.mock.DataUtil;

public class PayrollController extends FXController {

    @FXML private ComboBox<String> yearMonthComboBox;
    @FXML private AnchorPane rootPane;
    @FXML private TableView<Student> payrollTable;
    @FXML private TableColumn<Student, Integer> colNo;
    @FXML private TableColumn<Student, String> colFullName;
    @FXML private TableColumn<Student, Integer> colTotalDays;
    @FXML private TableColumn<Student, Double> colFare;
    @FXML private TableColumn<Student, Double> colTotalAmount;
    @FXML private Label totalAmountLabel;

    private ObservableList<Student> studentList;
    private ObservableList<AttendanceLog> attendanceLog;

    private final DecimalFormat daysFormat = new DecimalFormat("#.# day(s)");
    private final DecimalFormat currencyFormat = new DecimalFormat("₱#,##0.00");

    @Override
    protected void load_fields() {
        // Initialize lists
        studentList = DataUtil.createStudentList();
        attendanceLog = DataUtil.createAttendanceLogList();

        if (studentList == null) {
            studentList = FXCollections.observableArrayList();
        }

        if (attendanceLog == null) {
            attendanceLog = FXCollections.observableArrayList();
        }

        setupTable();

        if (yearMonthComboBox != null) {
            String selectedYear = (String) getParameter("selectedYear");
            if (selectedYear == null) {
                selectedYear = YearData.getCurrentAcademicYear();
            }
            DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, selectedYear);
            System.out.println("PayrollController.load_fields: selectedYear = " + selectedYear);
        }
        updateTotalAmount();
    }

    private void setupTable() {
        // Configure columns
        colNo.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colFullName.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String fullName = String.format("%s, %s %s", 
                student.getLastName(), 
                student.getFirstName(),
                student.getMiddleName());
            return new SimpleStringProperty(fullName);
        });

        // Update total days formatting
        colTotalDays.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            double totalDays = calculateTotalDays(student);
            return new SimpleIntegerProperty((int)(totalDays * 10)).asObject();  // Multiply by 10 to preserve decimal
        });
        colTotalDays.setCellFactory(column -> new TableCell<Student, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    double days = item / 10.0;  // Convert back to actual value
                    setText(daysFormat.format(days));
                }
            }
        });

        // Update fare formatting
        colFare.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            return new SimpleDoubleProperty(student.getFare()).asObject();  // Add asObject()
        });
        colFare.setCellFactory(column -> new TableCell<Student, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(item));
                }
            }
        });

        // Update total amount formatting
        colTotalAmount.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            double totalDays = calculateTotalDays(student);
            double fare = student.getFare();
            return new SimpleDoubleProperty(totalDays * fare).asObject();  // Add asObject()
        });
        colTotalAmount.setCellFactory(column -> new TableCell<Student, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(item));
                }
            }
        });

        // Set items
        payrollTable.setItems(studentList);
        updateTotalAmount();
    }

    private void updateTotalAmount() {
        double totalAmount = studentList.stream()
            .mapToDouble(student -> {
                double totalDays = calculateTotalDays(student);
                return totalDays * student.getFare();
            })
            .sum();
        
        totalAmountLabel.setText(currencyFormat.format(totalAmount));
    }

    private double calculateTotalDays(Student student) {
        if (yearMonthComboBox.getValue() == null) return 0;

        String[] parts = yearMonthComboBox.getValue().split(" ");
        YearMonth selectedMonth = YearMonth.of(
            Integer.parseInt(parts[1]),
            Month.valueOf(parts[0].toUpperCase())
        );

        double totalDays = 0;
        
        for (AttendanceLog log : attendanceLog) {
            AttendanceRecord record = log.getRecordID();
            if (log.getStudentID().getStudentID() == student.getStudentID() && 
                YearMonth.of(record.getYear(), record.getMonth()).equals(selectedMonth)) {
                
                LocalDate date = LocalDate.of(record.getYear(), record.getMonth(), record.getDay());
                String status = AttendanceUtil.getAttendanceStatus(student, date, attendanceLog);
                switch (status) {
                    case AttendanceUtil.PRESENT_MARK:
                        totalDays += 1.0;
                        break;
                    case AttendanceUtil.HALF_DAY_MARK:
                        totalDays += 0.5;
                        break;
                    case AttendanceUtil.EXCUSED_MARK:
                        totalDays += 1.0;
                        break;
                }
            }
        }
        
        return totalDays;
    }

    @Override
    protected void load_bindings() {
        System.out.println("PayrollController.load_bindings called");
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    rootPane.prefWidthProperty().bind(newScene.widthProperty());
                    rootPane.prefHeightProperty().bind(newScene.heightProperty());
                }
            });
        }
    }

    @Override
    protected void load_listeners() {
        yearMonthComboBox.setOnAction(event -> {
            payrollTable.refresh(); // Refresh calculations when month changes
            updateTotalAmount();
        });
    }

    @FXML
    public void initialize() {
    }

    public void updateYear(String newYear) {
        if (yearMonthComboBox != null) {
            System.out.println("PayrollController.updateYear called with newYear = " + newYear);
            DateTimeUtils.updateMonthYearComboBox(yearMonthComboBox, newYear);
        }
    }
}
