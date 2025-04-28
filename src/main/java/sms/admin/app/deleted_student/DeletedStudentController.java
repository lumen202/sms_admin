package sms.admin.app.deleted_student;

import java.util.stream.Collectors;

import dev.finalproject.data.StudentDAO;
import dev.finalproject.database.DataManager;
import dev.finalproject.models.SchoolYear;
import dev.finalproject.models.Student;
import dev.sol.core.application.FXController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import sms.admin.util.datetime.SchoolYearUtil;

public class DeletedStudentController extends FXController {

    @FXML
    private TableView<Student> studentTableView;
    @FXML
    private TableColumn<Student, Integer> studentIDColumn;
    @FXML
    private TableColumn<Student, String> colFullName;
    @FXML
    private TableColumn<Student, String> dateDeletedColumn;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> yearComboBox;

    private Stage stage;
    private ObservableList<SchoolYear> schoolYearList;
    private ObservableList<Student> masterStudentList = FXCollections.observableArrayList();
    private FilteredList<Student> filteredStudents;

    @Override
    protected void load_bindings() {
        studentIDColumn.setCellValueFactory(cell -> cell.getValue().studentIDProperty().asObject());
        studentIDColumn.setStyle("-fx-alignment: CENTER;");
        colFullName.setCellValueFactory(cellData -> {
            Student student = cellData.getValue();
            String fullName = String.format("%s, %s %s %s",
                    student.getLastName(),
                    student.getFirstName(),
                    student.getMiddleName(),
                    student.getNameExtension() != null ? student.getNameExtension() : "");
            return new SimpleStringProperty(fullName.trim());
        });
        dateDeletedColumn.setStyle("-fx-alignment: CENTER;");
        dateDeletedColumn.setCellValueFactory(cell -> cell.getValue().deletedAtProperty());
    }

    @Override
    protected void load_fields() {
        try {
            schoolYearList = FXCollections.observableArrayList(
                    DataManager.getInstance().getCollectionsRegistry().getList("SCHOOL_YEAR"));
            yearComboBox.setItems(SchoolYearUtil.convertToStringList(schoolYearList));

            String selectedYear = (String) getParameter("selectedYear");
            if (selectedYear != null) {
                yearComboBox.setValue(selectedYear);
            } else {
                SchoolYear currentYear = SchoolYearUtil.findCurrentYear(schoolYearList);
                if (currentYear != null) {
                    yearComboBox.setValue(SchoolYearUtil.formatSchoolYear(currentYear));
                } else if (!yearComboBox.getItems().isEmpty()) {
                    yearComboBox.setValue(yearComboBox.getItems().get(0));
                }
            }

            filteredStudents = new FilteredList<>(masterStudentList);
            studentTableView.setItems(filteredStudents);
            updateStudentList(yearComboBox.getValue());

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Database Error", "Failed to load data",
                    "An error occurred while loading data from the database.");
        }
    }

    @Override
    protected void load_listeners() {
        yearComboBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (newYear != null && !newYear.equals(oldYear)) {
                updateStudentList(newYear);
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String searchText = newValue.toLowerCase();
            filteredStudents.setPredicate(student -> {
                if (searchText == null || searchText.isEmpty()) {
                    return true;
                }
                return student.getFirstName().toLowerCase().contains(searchText)
                        || student.getLastName().toLowerCase().contains(searchText)
                        || student.getMiddleName().toLowerCase().contains(searchText)
                        || String.valueOf(student.getStudentID()).contains(searchText);
            });
        });
    }

    private void updateStudentList(String schoolYear) {
        try {
            ObservableList<Student> students = DataManager.getInstance()
                    .getCollectionsRegistry()
                    .getList("STUDENT");

            int startYear = Integer.parseInt(schoolYear.split("-")[0]);
            masterStudentList.setAll(
                    students.stream()
                            .filter(student -> student != null
                            && student.getYearID() != null
                            && student.getYearID().getYearStart() == startYear
                            && student.isDeleted() == 1)
                            .collect(Collectors.toList()));

            studentTableView.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to update student list", e.getMessage());
        }
    }

    @FXML
    public void handleRestoreMenuItem() {
        Student selectedStudent = studentTableView.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            selectedStudent.setDeleted(0);
            selectedStudent.setDeletedAt(null);
            StudentDAO.update(selectedStudent);
            masterStudentList.remove(selectedStudent);
            studentTableView.refresh();
            showInfoAlert("Restore Successful",
                    "Student " + selectedStudent.getFullName() + " has been restored.");
        } else {
            showWarningAlert("No Selection", "Please select a student to restore.");
        }
    }

    @FXML
    public void handleDeleteMenuItem() {
        Student selectedStudent = studentTableView.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Delete Confirmation");
            confirmAlert.setHeaderText("Are you sure you want to permanently delete this student?");
            confirmAlert.setContentText(String.format("""
                    Student ID: %d
                    Name: %s, %s %s
                    This action cannot be undone.""",
                    selectedStudent.getStudentID(),
                    selectedStudent.getLastName(),
                    selectedStudent.getFirstName(),
                    selectedStudent.getMiddleName()));

            if (confirmAlert.showAndWait().get().getButtonData().isDefaultButton()) {
                masterStudentList.remove(selectedStudent);
                StudentDAO.delete(selectedStudent);

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Delete Successful");
                successAlert.setHeaderText(null);
                successAlert.setContentText(
                        "Student " + selectedStudent.getFirstName() + " " + selectedStudent.getLastName()
                        + " has been deleted permanently.");
                successAlert.showAndWait();
            }
        } else {
            showWarningAlert("No Selection", "Please select a student to delete.");
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void initialize() {
        load_bindings();
        load_fields();
        load_listeners();
    }
}
