package sms.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import dev.finalproject.data.AddressDAO;
import dev.finalproject.data.AttendanceLogDAO;
import dev.finalproject.data.GuardianDAO;
import dev.finalproject.data.SchoolYearDAO;
import dev.finalproject.data.StudentDAO;
import dev.finalproject.datbase.DataManager;
import dev.sol.core.application.FXApplication;
import dev.sol.core.application.loader.FXLoaderFactory;
import dev.sol.core.registry.FXCollectionsRegister;
import dev.sol.core.scene.FXSkin;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.stage.WindowEvent;
import sms.admin.app.RootLoader;
import sms.admin.util.db.DatabaseChangeListener;
import sms.admin.util.db.DatabaseConnection;

public class App extends FXApplication {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private DatabaseChangeListener dbChangeListener;

    @Override
    public void initialize() throws Exception {
        try {
            configureApplication();
            initializeDatabaseListener();
            // Instead of initializing dataset locally, reuse DataManager's initialization.
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
                new Image(getClass().getResource("/sms/admin/assets/img/logo.png").toExternalForm()));
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
            FXCollectionsRegister collectionsRegistry = DataManager.getInstance().getCollectionsRegistry();
            switch (tableName.toLowerCase()) {
                case "student" -> {
                    var newList = FXCollections.observableArrayList(StudentDAO.getStudentList());
                    collectionsRegistry.register("STUDENT", newList);
                }
                case "guardian" -> {
                    var newList = FXCollections.observableArrayList(GuardianDAO.getGuardianList());
                    collectionsRegistry.register("GUARDIAN", newList);
                }
                case "address" -> {
                    var newList = FXCollections.observableArrayList(AddressDAO.getAddressesList());
                    collectionsRegistry.register("ADDRESS", newList);
                }
                case "attendance_log" -> {
                    var newList = FXCollections.observableArrayList(AttendanceLogDAO.getAttendanceLogList());
                    collectionsRegistry.register("ATTENDANCE_LOG", newList);
                }
                case "school_year" -> {
                    var newList = FXCollections.observableArrayList(SchoolYearDAO.getSchoolYearList());
                    collectionsRegistry.register("SCHOOL_YEAR", newList);
                }
                default -> LOGGER.warning("Unknown table for refresh: " + tableName);
            }
            LOGGER.info("Successfully refreshed collection for table: " + tableName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refreshing collection for table: " + tableName, e);
        }
    }

    /**
     * Instead of initializing the dataset locally with separate DAO calls,
     * delegate initialization to the shared DataManager from finalproject.
     */
    public void initializeDataset() {
        try {
            DataManager.getInstance().initializeData();
            LOGGER.info("Dataset initialized via DataManager");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize dataset", e);
            throw new RuntimeException("Dataset initialization failed", e);
        }
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
        FXCollectionsRegister collectionsRegistry = DataManager.getInstance().getCollectionsRegistry();
        String[] knownCollections = {
                "CLUSTER", "SCHOOL_YEAR", "STUDENT", "GUARDIAN",
                "STUDENT_GUARDIAN", "ADDRESS", "ATTENDANCE_RECORD", "ATTENDANCE_LOG"
        };

        for (String key : knownCollections) {
            try {
                var collection = collectionsRegistry.getList(key);
                if (collection != null) {
                    collection.clear(); // Clears all elements from the collection
                } else {
                    LOGGER.warning("Collection not found for key: " + key);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to clear collection: " + key, e);
            }
        }
    }

}
