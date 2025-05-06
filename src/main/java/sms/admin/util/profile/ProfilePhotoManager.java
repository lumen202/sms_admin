/**
 * Utility class for managing student profile photos, including saving uploaded images
 * and loading them into JavaFX ImageView components. Falls back to a default image or
 * a styled placeholder if no photo is available.
 */
package sms.admin.util.profile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProfilePhotoManager {

    /**
     * Directory where student profile photos are stored.
     */
    private static final String STUDENT_PHOTOS_DIR = "src/main/resources/sms/admin/assets/img/profile";

    /**
     * Possible classpath locations for the default profile image.
     */
    private static final String[] DEFAULT_PHOTO_PATHS = {
            "/assets/img/default-profile.png",
            "/sms/admin/assets/img/default-profile.png",
            "/img/default-profile.png",
            "/default-profile.png"
    };

    /**
     * Saves a photo file for a given student, naming it by student ID and
     * preserving extension.
     * Creates the target directory if it does not exist.
     *
     * @param sourceFile the uploaded image file
     * @param studentId  the unique ID of the student
     * @return the absolute path of the saved file, or null on failure
     */
    public static String savePhoto(File sourceFile, int studentId) {
        try {
            String extension = sourceFile.getName()
                    .substring(sourceFile.getName().lastIndexOf('.'));
            String fileName = "student_" + studentId + extension;
            File profileDir = new File(STUDENT_PHOTOS_DIR);
            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }
            File destinationFile = new File(profileDir, fileName);
            Files.copy(sourceFile.toPath(), destinationFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads a student's photo into the provided ImageView. If no student-specific
     * photo
     * is found or loading fails, a default image or styled placeholder is used.
     *
     * @param imageView the ImageView to set the photo on
     * @param studentId the unique ID of the student
     */
    public static void loadPhoto(ImageView imageView, int studentId) {
        try {
            String photoPath = findStudentPhoto(studentId);
            if (photoPath != null) {
                Image image = new Image(new File(photoPath).toURI().toString(), true);
                if (!image.isError()) {
                    imageView.setImage(image);
                    return;
                }
            }
            loadDefaultPhoto(imageView);
        } catch (Exception e) {
            loadDefaultPhoto(imageView);
        }
    }

    /**
     * Searches for an existing photo file for the student in supported extensions.
     *
     * @param studentId the unique ID of the student
     * @return the absolute path if found, or null otherwise
     */
    private static String findStudentPhoto(int studentId) {
        String[] extensions = { ".jpg", ".jpeg", ".png" };
        String baseFileName = "student_" + studentId;
        for (String ext : extensions) {
            File photoFile = new File(STUDENT_PHOTOS_DIR, baseFileName + ext);
            if (photoFile.exists()) {
                return photoFile.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Attempts to load a default profile image from classpath locations.
     * Falls back to a styled placeholder if no resource is found.
     *
     * @param imageView the ImageView to set the default image on
     */
    private static void loadDefaultPhoto(ImageView imageView) {
        for (String path : DEFAULT_PHOTO_PATHS) {
            try {
                var resourceUrl = ProfilePhotoManager.class.getResource(path);
                if (resourceUrl != null) {
                    Image defaultImage = new Image(resourceUrl.toExternalForm());
                    if (!defaultImage.isError()) {
                        imageView.setImage(defaultImage);
                        return;
                    }
                }
            } catch (Exception e) {
                // try next path
            }
        }
        createEmptyProfileImage(imageView);
    }

    /**
     * Sets the ImageView to a styled empty placeholder (no image) using CSS styles.
     *
     * @param imageView the ImageView to style as a placeholder
     */
    private static void createEmptyProfileImage(ImageView imageView) {
        imageView.setImage(null);
        imageView.setStyle(
                "-fx-background-color: #f0f0f0;" +
                        "-fx-background-radius: 75;" +
                        "-fx-border-color: #cccccc;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 75;");
    }
}
