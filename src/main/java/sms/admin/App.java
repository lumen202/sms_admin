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

    /**
     * Helper method that builds the JDBC URL using connection details defined locally.
     */
    private static String buildJdbcUrl(String host) {
        final String DB_USER = "root";
        final String DB_PASSWORD = "admin";
        final String DB_NAME = "student_management_system_db";
        final int DB_PORT = 3306;
        final String CONNECTION_OPTIONS = "?allowPublicKeyRetrieval=true&useSSL=false";
        return String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s%s",
                host, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, CONNECTION_OPTIONS);
    }

    public static final String REMOTE_HOST = buildJdbcUrl("192.168.254.108");
    public static final String LOCAL_HOST = buildJdbcUrl("localhost");

    public static final FXControllerRegister CONTROLLER_REGISTRY = FXControllerRegister.INSTANCE;
    public static final FXCollectionsRegister COLLECTIONS_REGISTRY = FXCollectionsRegister.INSTANCE;
    public static final FXNodeRegister NODE_REGISTER = FXNodeRegister.INSTANCE;
    public static final DBService DB_SMS = DBService.INSTANCE.initialize(LOCAL_HOST);

    @Override
    public void initialize() throws Exception {
        try {
            configureApplication();
            initializeDataset();
            initializeApplication();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize application", e);
            throw e;
        }
    }

    private void configureApplication() {
        setTitle("Student Management System - Admin");
        setSkin(FXSkin.PRIMER_LIGHT);
        // Set application icon
        getApplicationStage().getIcons().add(
                new Image(getClass().getResource("/sms/admin/assets/img/logo.png").toExternalForm())
        );
        applicationStage.setWidth(900);
        applicationStage.setHeight(700);
        applicationStage.setOnCloseRequest(this::handleApplicationClose);
    }

    public void initializeDataset() {
        try {
            initializeBaseCollections();
            initializeDependentCollections();
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
        StudentDAO.initialize(
                COLLECTIONS_REGISTRY.getList("CLUSTER"),
                COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR"));
        COLLECTIONS_REGISTRY.register("STUDENT",
                FXCollections.observableArrayList(StudentDAO.getStudentList()));
        COLLECTIONS_REGISTRY.register("GUARDIAN",
                FXCollections.observableArrayList(GuardianDAO.getGuardianList()));
        COLLECTIONS_REGISTRY.register("STUDENT_GUARDIAN",
                FXCollections.observableArrayList(StudentGuardianDAO.getStudentGuardianList()));
    }

    private void initializeRelatedCollections() {
        AddressDAO.initialize(COLLECTIONS_REGISTRY.getList("STUDENT"));
        COLLECTIONS_REGISTRY.register("ADDRESS",
                FXCollections.observableArrayList(AddressDAO.getAddressesList()));

        COLLECTIONS_REGISTRY.register("ATTENDANCE_RECORD",
                FXCollections.observableArrayList(AttendanceRecordDAO.getRecordList()));

        AttendanceLogDAO.initialize(
                COLLECTIONS_REGISTRY.getList("STUDENT"),
                COLLECTIONS_REGISTRY.getList("ATTENDANCE_RECORD"));
        COLLECTIONS_REGISTRY.register("ATTENDANCE_LOG",
                FXCollections.observableArrayList(AttendanceLogDAO.getAttendanceLogList()));
    }

    private void initializeApplication() {
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

    private void handleApplicationClose(WindowEvent event) {
        try {
            clearCollections();
            Platform.exit();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during application cleanup", e);
        }
    }

    private void clearCollections() {
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
