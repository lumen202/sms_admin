package sms.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
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
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.scene.image.Image;
import javafx.stage.WindowEvent;
import sms.admin.app.RootLoader;
import sms.admin.util.db.DatabaseChangeListener;
import sms.admin.util.db.DatabaseConnection;

public class App extends FXApplication {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    // public static final String REMOTE_HOST = buildJdbcUrl("192.168.254.108");
    // public static final String LOCAL_HOST = buildJdbcUrl("localhost");
    // public static final String LAB_HOST = "jdbc:mysql://192.168.254.108:3306/student_management_system_db?user=remote_user&allowPublicKeyRetrieval=true&useSSL=false";

    // public static final FXControllerRegister CONTROLLER_REGISTRY = FXControllerRegister.INSTANCE;
    // public static final FXCollectionsRegister COLLECTIONS_REGISTRY = FXCollectionsRegister.INSTANCE;
    // public static final FXNodeRegister NODE_REGISTER = FXNodeRegister.INSTANCE;
    // public static final DBService DB_SMS = DBService.INSTANCE.initialize(LOCAL_HOST);

    
    public static final String REMOTE_HOST = "jdbc:mysql://192.168.254.108:3306/student_management_system_db?user=root&password=admin&allowPublicKeyRetrieval=true&useSSL=false";
    public static final String LOCAL_HOST = "jdbc:mysql://localhost:3306/student_management_system_db?user=root&password=admin&allowPublicKeyRetrieval=true&useSSL=false";
    public static final String LAB_HOST = "jdbc:mysql://192.168.254.108:3306/student_management_system_db?user=remote_user&allowPublicKeyRetrieval=true&useSSL=false";

    public static final FXControllerRegister CONTROLLER_REGISTRY = FXControllerRegister.INSTANCE;
    public static final FXCollectionsRegister COLLECTIONS_REGISTRY = FXCollectionsRegister.INSTANCE;
    public static final FXNodeRegister NODE_REGISTER = FXNodeRegister.INSTANCE;

    public static final DBService DB_SMS = DBService.INSTANCE
.initialize(LOCAL_HOST);

    private DatabaseChangeListener dbChangeListener;

    @Override
    public void initialize() throws Exception {
        try {
            configureApplication();
            initializeDatabaseListener();
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

    private void initializeDatabaseListener() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            dbChangeListener = new DatabaseChangeListener(conn);
            dbChangeListener.addChangeHandler((tableName, changeType) -> {
                Platform.runLater(() -> {
                    LOGGER.info("Database change detected: " + tableName + " - " + changeType);
                    refreshCollectionForTable(tableName);
                });
            });
            dbChangeListener.startListening(5); // Check every 5 seconds
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database listener", e);
        }
    }

    private void refreshCollectionForTable(String tableName) {
        try {
            switch (tableName.toLowerCase()) {
                case "student" -> {
                    var newList = FXCollections.observableArrayList(StudentDAO.getStudentList());
                    COLLECTIONS_REGISTRY.register("STUDENT", newList);
                }
                case "guardian" -> {
                    var newList = FXCollections.observableArrayList(GuardianDAO.getGuardianList());
                    COLLECTIONS_REGISTRY.register("GUARDIAN", newList);
                }
                case "address" -> {
                    var newList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
                    COLLECTIONS_REGISTRY.register("ADDRESS", newList);
                }
                case "attendance_log" -> {
                    var newList = FXCollections.observableArrayList(AttendanceLogDAO.getAttendanceLogList());
                    COLLECTIONS_REGISTRY.register("ATTENDANCE_LOG", newList);
                }
                case "school_year" -> {
                    var newList = FXCollections.observableArrayList(SchoolYearDAO.getSchoolYearList());
                    COLLECTIONS_REGISTRY.register("SCHOOL_YEAR", newList);
                }
                default ->
                    LOGGER.warning("Unknown table for refresh: " + tableName);
            }
            LOGGER.info("Successfully refreshed collection for table: " + tableName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refreshing collection for table: " + tableName, e);
        }
    }

    public void initializeDataset() {
        try {
            initializeBaseCollections();
            // Lazy-load dependent and related collections by registering a lazy wrapper.
            initializeDependentCollections();
            initializeRelatedCollections();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize dataset", e);
            throw new RuntimeException("Dataset initialization failed", e);
        }
    }

    private void initializeBaseCollections() {
        // Base collections are essential; load them synchronously.
        COLLECTIONS_REGISTRY.register("CLUSTER",
                FXCollections.observableArrayList(ClusterDAO.getClusterList()));
        COLLECTIONS_REGISTRY.register("SCHOOL_YEAR",
                FXCollections.observableArrayList(SchoolYearDAO.getSchoolYearList()));
    }

    private void initializeDependentCollections() {
        StudentDAO.initialize(
                COLLECTIONS_REGISTRY.getList("CLUSTER"),
                COLLECTIONS_REGISTRY.getList("SCHOOL_YEAR"));
        // Use lazy wrappers for potentially large datasets.
        COLLECTIONS_REGISTRY.register("STUDENT",
                new LazyObservableList<>(StudentDAO::getStudentList));
        COLLECTIONS_REGISTRY.register("GUARDIAN",
                new LazyObservableList<>(GuardianDAO::getGuardianList));
        COLLECTIONS_REGISTRY.register("STUDENT_GUARDIAN",
                new LazyObservableList<>(StudentGuardianDAO::getStudentGuardianList));
    }

    private void initializeRelatedCollections() {
        AddressDAO.initialize(COLLECTIONS_REGISTRY.getList("STUDENT"));
        COLLECTIONS_REGISTRY.register("ADDRESS",
                new LazyObservableList<>(AddressDAO::getAddressesList));
        COLLECTIONS_REGISTRY.register("ATTENDANCE_RECORD",
                new LazyObservableList<>(AttendanceRecordDAO::getRecordList));
        AttendanceLogDAO.initialize(
                COLLECTIONS_REGISTRY.getList("STUDENT"),
                COLLECTIONS_REGISTRY.getList("ATTENDANCE_RECORD"));
        COLLECTIONS_REGISTRY.register("ATTENDANCE_LOG",
                new LazyObservableList<>(AttendanceLogDAO::getAttendanceLogList));
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
            if (dbChangeListener != null) {
                dbChangeListener.stop();
            }
            DatabaseConnection.closeConnection();
            clearCollections();
            Platform.exit();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during application cleanup", e);
        }
    }

    private void clearCollections() {
        String[] knownCollections = {
            "CLUSTER", "SCHOOL_YEAR", "STUDENT", "GUARDIAN",
            "STUDENT_GUARDIAN", "ADDRESS", "ATTENDANCE_RECORD", "ATTENDANCE_LOG"
        };

        for (String key : knownCollections) {
            try {
                COLLECTIONS_REGISTRY.register(key, FXCollections.observableArrayList());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to clear collection: " + key, e);
            }
        }
    }

    private static String buildJdbcUrl(String host) {
        final String DB_USER = "root";
        final String DB_PASSWORD = "admin";
        final String DB_NAME = "student_management_system_db";
        final int DB_PORT = 3306;
        final String CONNECTION_OPTIONS = "?allowPublicKeyRetrieval=true&useSSL=false";
        return String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s%s",
                host, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, CONNECTION_OPTIONS);
    }

    /**
     * A simple lazy-loading wrapper for an ObservableList. The data is loaded
     * only on first access.
     */
    private static class LazyObservableList<T> extends ObservableListBase<T> {

        private ObservableList<T> backing;
        private final Supplier<java.util.List<T>> supplier;

        public LazyObservableList(Supplier<java.util.List<T>> supplier) {
            this.supplier = supplier;
        }

        private void load() {
            if (backing == null) {
                backing = FXCollections.observableArrayList(supplier.get());
            }
        }

        @Override
        public T get(int index) {
            load();
            return backing.get(index);
        }

        @Override
        public int size() {
            load();
            return backing.size();
        }

    }
}
