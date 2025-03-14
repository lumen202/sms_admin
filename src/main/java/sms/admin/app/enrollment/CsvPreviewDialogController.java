package sms.admin.app.enrollment;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import sms.admin.util.CsvStudent;
import sms.admin.util.dialog.DialogManager;

import java.util.List;

public class CsvPreviewDialogController {
    @FXML private TableView<CsvStudent> previewTable;
    @FXML private TableColumn<CsvStudent, String> firstNameColumn;
    @FXML private TableColumn<CsvStudent, String> middleNameColumn;
    @FXML private TableColumn<CsvStudent, String> lastNameColumn;
    @FXML private TableColumn<CsvStudent, String> emailColumn;
    @FXML private TableColumn<CsvStudent, String> addressColumn;
    @FXML private TableColumn<CsvStudent, String> clusterColumn;
    @FXML private TableColumn<CsvStudent, String> contactColumn;
    
    private Stage dialogStage;
    private CsvStudent selectedStudent;
    private boolean importClicked = false;
    private List<CsvStudent> allStudents;
    
    @FXML
    private void initialize() {
        firstNameColumn.setCellValueFactory(cellData -> cellData.getValue().firstNameProperty());
        middleNameColumn.setCellValueFactory(cellData -> cellData.getValue().middleNameProperty());
        lastNameColumn.setCellValueFactory(cellData -> cellData.getValue().lastNameProperty());
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        addressColumn.setCellValueFactory(cellData -> cellData.getValue().addressProperty());
        clusterColumn.setCellValueFactory(cellData -> cellData.getValue().clusterProperty());
        contactColumn.setCellValueFactory(cellData -> cellData.getValue().contactProperty());
        
        previewTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    
    public void setStudents(List<CsvStudent> students) {
        this.allStudents = students;
        previewTable.getItems().addAll(students);
    }

    public List<CsvStudent> getAllStudents() {
        return allStudents;
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public CsvStudent getSelectedStudent() {
        return selectedStudent;
    }
    
    public boolean isImportClicked() {
        return importClicked;
    }
    
    @FXML
    private void handleImport() {
        selectedStudent = previewTable.getSelectionModel().getSelectedItem();
        if (selectedStudent != null) {
            importClicked = true;
            DialogManager.closeWithFade(dialogStage, () -> {
                DialogManager.setOverlayEffect(dialogStage.getOwner(), false);
                dialogStage.close();
            });
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a student from the table.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleImportAll() {
        if (!allStudents.isEmpty()) {
            importClicked = true;
            selectedStudent = null; // Indicates we want all students
            DialogManager.closeWithFade(dialogStage, () -> {
                DialogManager.setOverlayEffect(dialogStage.getOwner(), false);
                dialogStage.close();
            });
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Students");
            alert.setHeaderText(null);
            alert.setContentText("No students to import.");
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleCancel() {
        DialogManager.closeWithFade(dialogStage, () -> {
            DialogManager.setOverlayEffect(dialogStage.getOwner(), false);
            dialogStage.close();
        });
    }
}
