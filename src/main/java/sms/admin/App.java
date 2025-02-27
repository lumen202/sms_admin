package sms.admin;

import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.data.ClusterDAO;
import dev.finalproject.data.GuardianDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.data.StudentGuardianDAO;
import dev.sol.core.application.FXApplication;
import dev.sol.core.application.loader.FXLoaderFactory;
import dev.sol.core.registry.FXCollectionsRegister;
import dev.sol.core.registry.FXControllerRegister;
import dev.sol.core.registry.FXNodeRegister;
import dev.sol.core.scene.FXSkin;
import dev.sol.db.DBService;
import javafx.collections.FXCollections;
import sms.admin.app.RootLoader;

public class App extends FXApplication {

        public static final FXControllerRegister CONTROLLER_REGISTRY = FXControllerRegister.INSTANCE;
        public static final FXCollectionsRegister COLLECTIONS_REGISTRY = FXCollectionsRegister.INSTANCE;
        public static final FXNodeRegister NODE_REGISTER = FXNodeRegister.INSTANCE;
        public static final DBService DB_SMS = DBService.INSTANCE
                        .initialize("jdbc:mysql://localhost/student_management_system_db?user=root&password=");

        @Override
        public void initialize() throws Exception {
                setTitle("Student Management System - Admin");
                setSkin(FXSkin.PRIMER_LIGHT);

                applicationStage.getOnCloseRequest();
                // initialize_dataset();
                initialize_application();
        }

        public void initialize_dataset() {

                COLLECTIONS_REGISTRY.register("CLUSTER",
                                FXCollections.observableArrayList(ClusterDAO.getClusterList()));

                StudentDAO.initialize(COLLECTIONS_REGISTRY.getList("CLUSTER"));
                COLLECTIONS_REGISTRY.register("STUDENT",
                                FXCollections.observableArrayList(StudentDAO.getStudentList()));

                COLLECTIONS_REGISTRY.register("GUARDIAN",
                                FXCollections.observableArrayList(GuardianDAO.getGuardianList()));

                COLLECTIONS_REGISTRY.register("STUDENT_GUARDIAN",
                                FXCollections.observableArrayList(StudentGuardianDAO.getStudentGuardianList()));

                COLLECTIONS_REGISTRY.register("ATTENDANCE_LOG",
                                FXCollections.observableArrayList(AttendanceLogDAO.getLogList()));

                AddressDAO.initialize(COLLECTIONS_REGISTRY.getList("STUDENT"));
                COLLECTIONS_REGISTRY.register("ADDRESS",
                                FXCollections.observableArrayList(AddressDAO.getAddressesList()));

                AttendanceRecordDAO.initialize(
                                COLLECTIONS_REGISTRY.getList("STUDENT"),
                                COLLECTIONS_REGISTRY.getList("ATTENDANCE_LOG"));
                COLLECTIONS_REGISTRY.register("ATTENDANCE_RECORD",
                                FXCollections.observableArrayList(AttendanceRecordDAO.getAttendanceRecordList()));
        }

        private void initialize_application() {
                RootLoader rootLoader = (RootLoader) FXLoaderFactory
                                .createInstance(RootLoader.class,
                                                App.class.getResource("/sms/admin/app/ROOTv2.fxml"))
                                .addParameter("scene", applicationScene)
                                .addParameter("OWNER", applicationStage)
                                .initialize();
                applicationStage.requestFocus();
                rootLoader.load();
        }
}
