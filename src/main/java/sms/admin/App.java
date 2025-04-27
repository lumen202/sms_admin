package sms.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

import dev.finalproject.database.DataManager;
import dev.sol.core.application.FXApplication;
import dev.sol.core.application.loader.FXLoaderFactory;
import dev.sol.core.registry.FXCollectionsRegister;
import dev.sol.core.scene.FXSkin;
import javafx.scene.image.Image;
import javafx.stage.WindowEvent;
import sms.admin.app.RootLoader;
import sms.admin.util.db.DatabaseConnection;

/**
 * Main application class for the Student Management System Admin interface.
 * This class extends {@link FXApplication} and is responsible for initializing
 * the application, configuring the UI, setting up the dataset, and handling
 * application shutdown.
 */
public class App extends FXApplication {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName()); // Logger for application events

    /**
     * Initializes the application by configuring settings, dataset, and UI.
     *
     * @throws Exception If initialization fails.
     */
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

    /**
     * Configures the application settings, including title, skin, icon, and
     * window properties.
     */
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

    /**
     * Initializes the dataset for the application using the DataManager.
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

    /**
     * Initializes the application UI by loading the root view.
     */
    private void initializeApplication() {
        try {
            RootLoader rootLoader = (RootLoader) FXLoaderFactory
                    .createInstance(RootLoader.class,
                            App.class.getResource("/sms/admin/app/ROOT.fxml"))
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

    /**
     * Handles the application close event, cleaning up resources and closing
     * the database connection.
     *
     * @param event The window close event.
     */
    private void handleApplicationClose(WindowEvent event) {
        try {
            // Close database connection and clear collections before exiting
            DatabaseConnection.closeConnection();
            clearCollections();
            applicationStage.hide();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during application cleanup", e);
        }
    }

    /**
     * Clears all collections in the DataManager to free up resources.
     */
    private void clearCollections() {
        FXCollectionsRegister collectionsRegistry = DataManager.getInstance().getCollectionsRegistry();
        String[] knownCollections = {
            "CLUSTER", "SCHOOL_YEAR", "STUDENT", "GUARDIAN",
            "STUDENT_GUARDIAN", "ADDRESS", "ATTENDANCE_RECORD", "ATTENDANCE_LOG", "SETTINGS"
        };

        for (String key : knownCollections) {
            try {
                var collection = collectionsRegistry.getList(key);
                if (collection != null) {
                    collection.clear();
                } else {
                    LOGGER.warning("Collection not found for key: " + key);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to clear collection: " + key, e);
            }
        }
    }
}
