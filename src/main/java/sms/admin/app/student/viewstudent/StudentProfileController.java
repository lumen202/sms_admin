package sms.admin.app.student.viewstudent;

import atlantafx.base.controls.ModalPane;
import dev.sol.core.application.FXController;
import dev.sol.core.application.loader.FXLoaderFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import sms.admin.app.student.viewstudent.studentform.StudentFormLoader;

public class StudentProfileController extends FXController {

    @FXML
    private Button editButton;
    @FXML
    private ModalPane forModal;

    @FXML
    private void handleEditButton() {
        System.out.println("edit button is clicked");
        initializeEdit();
    }

    private void initializeEdit() {
        if (forModal == null) {
            forModal = new ModalPane();
            forModal.setId("studentFormModal");
            forModal.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        }

        // Get the current scene root
        if (editButton.getScene() != null) {
            // Create a new loader instance
            StudentFormLoader loader = (StudentFormLoader) FXLoaderFactory
                    .createInstance(StudentFormLoader.class,
                            getClass().getResource(
                                    "/sms.admin/app/viewstudent/studentform/STUDENT_FORM.fxml"));

            loader.addParameter("MODAL", forModal);
            loader.addParameter("SCENE_ROOT", editButton.getScene().getRoot());
            loader.initialize();
            loader.load();
        }
    }

    @Override
    protected void load_bindings() {
        System.out.println("Student profile is clicked");
    }

    @Override
    protected void load_fields() {
        System.out.println("load fields");
    }

    @Override
    protected void load_listeners() {
        editButton.setOnAction(event -> handleEditButton());
    }

}
