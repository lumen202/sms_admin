package sms.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.AttendanceRecordDAO;
import dev.finalproject.data.ClusterDAO;
import dev.finalproject.data.GuardianDAO;
import dev.finalproject.data.SchoolYearDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.data.StudentGuardianDAO;
import dev.sol.core.application.FXApplication;
import dev.sol.core.application.loader.FXLoaderFactory;
import dev.sol.core.registry.FXCollectionsRegister;
import dev.sol.core.registry.FXControllerRegister;
import dev.sol.core.registry.FXNodeRegister;
import dev.sol.core.scene.FXSkin;
import dev.sol.db.DBService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.stage.WindowEvent;
import sms.admin.app.RootLoader;

public class App extends FXApplication {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static final FXControllerRegister CONTROLLER_REGISTRY = FXControllerRegister.INSTANCE;
    public static final FXCollectionsRegister COLLECTIONS_REGISTRY = FXCollectionsRegister.INSTANCE;
    public static final FXNodeRegister NODE_REGISTER = FXNodeRegister.INSTANCE;
    public static final DBService DB_SMS = DBService.INSTANCE;

    @Override
    public void initialize() throws Exception {
        try {
            configureApplication();
            initializeDatabase();
            initialize_dataset();
            initialize_application();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize application", e);
            throw e;
        }
    }

    private void configureApplication() {
        setTitle("Student Management System - Admin");
        setSkin(FXSkin.PRIMER_LIGHT);
        getApplicationStage().getIcons().add(
                new Image(getClass().getResource("/sms/admin/assets/img/logo.png").toExternalForm()));

        // Set initial window size
        applicationStage.setWidth(900);
        applicationStage.setHeight(700);

        // Add proper cleanup on window close
        applicationStage.setOnCloseRequest(this::handleApplicationClose);
    }

    private void initializeDatabase() {
        try {
            // Ensure the connection string is correct
            DB_SMS.initialize("jdbc:mysql://127.0.0.1:3306/student_management_system_db?user=root&password=&useSSL=false&serverTimezone=UTC&connectTimeout=10000"

);
            LOGGER.info("Database connection established successfully");
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public void initialize_dataset() {
        try {
            // Initialize base data first
            initializeBaseCollections();

            // Initialize dependent data
            initializeDependentCollections();

            // Initialize related data last
            initializeRelatedCollections();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize dataset", e);
            throw new RuntimeException("Dataset initialization failed", e);
        }
    }

    private void initializeBaseCollections() {
        COLLECTIONS_REGISTRY.register("CLUSTER",
                FXCollections.observableArrayList(ClusterDAO.getClusterList()));
        COLLECTIONS_REGISTRY.register("SCHOOL_YEAR",
                FXCollections.observableArrayList(SchoolYearDAO.getSchoolYearList()));
    }

    private void initializeDependentCollections() {
        // Initialize Student related collections
        StudentDAO.initialize(
                COLLECTIONS_REGISTRY.getList("CLUSTER"),
                COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR"));
        COLLECTIONS_REGISTRY.register("STUDENT",
                FXCollections.observableArrayList(StudentDAO.getStudentList()));

        // Initialize Guardian related collections
        COLLECTIONS_REGISTRY.register("GUARDIAN",
                FXCollections.observableArrayList(GuardianDAO.getGuardianList()));
        COLLECTIONS_REGISTRY.register("STUDENT_GUARDIAN",
                FXCollections.observableArrayList(StudentGuardianDAO.getStudentGuardianList()));
    }

    private void initializeRelatedCollections() {
        // Initialize Address
        AddressDAO.initialize(COLLECTIONS_REGISTRY.getList("STUDENT"));
        COLLECTIONS_REGISTRY.register("ADDRESS",
                FXCollections.observableArrayList(AddressDAO.getAddressesList()));

        // Initialize Attendance Records first
        COLLECTIONS_REGISTRY.register("ATTENDANCE_RECORD",
                FXCollections.observableArrayList(AttendanceRecordDAO.getRecordList()));

        // Initialize AttendanceLogDAO before getting logs
        AttendanceLogDAO.initialize(
                COLLECTIONS_REGISTRY.getList("STUDENT"),
                COLLECTIONS_REGISTRY.getList("ATTENDANCE_RECORD"));

        // Now get the logs after initialization
        COLLECTIONS_REGISTRY.register("ATTENDANCE_LOG",
                FXCollections.observableArrayList(AttendanceLogDAO.getAttendanceLogList()));
    }

    private void initialize_application() {
        try {
            RootLoader rootLoader = (RootLoader) FXLoaderFactory
                    .createInstance(RootLoader.class,
                            App.class.getResource("/sms/admin/app/ROOTv2.fxml"))
                    .addParameter("scene", applicationScene)
                    .addParameter("OWNER", applicationStage)
                    .initialize();

            applicationStage.requestFocus();
            rootLoader.load();

            LOGGER.info("Application UI initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize application UI", e);
            throw new RuntimeException("Application UI initialization failed", e);
        }
    }

    private void handleApplicationClose(@SuppressWarnings("unused") WindowEvent event) {
        try {
            // Clear specific known collections
            clearCollections();
            // Exit the application
            Platform.exit();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during application cleanup", e);
        }
    }

    private void clearCollections() {
        // Clear known collections by registering empty lists
        String[] knownCollections = {
            "CLUSTER", "SCHOOL_YEAR", "STUDENT", "GUARDIAN",
            "STUDENT_GUARDIAN", "ADDRESS", "ATTENDANCE_RECORD",
            "ATTENDANCE_LOG"
        };

        for (String key : knownCollections) {
            try {
                COLLECTIONS_REGISTRY.register(key, FXCollections.observableArrayList());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to clear collection: " + key, e);
            }
        }
    }
}
