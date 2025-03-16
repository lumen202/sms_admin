package sms.admin.util.profile;

import java.io.File;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ProfilePhotoManager {
    private static final String STUDENT_PHOTOS_DIR = "src/main/resources/sms/admin/assets/img/profile";
    private static final String[] DEFAULT_PHOTO_PATHS = {
        "/assets/img/default-profile.png",
        "/sms/admin/assets/img/default-profile.png",
        "/img/default-profile.png",
        "/default-profile.png"
    };

    public static String savePhoto(File sourceFile, int studentId) {
        try {
            String extension = sourceFile.getName().substring(sourceFile.getName().lastIndexOf('.'));
            String fileName = "student_" + studentId + extension;
            File profileDir = new File(STUDENT_PHOTOS_DIR);
            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }
            File destinationFile = new File(profileDir, fileName);
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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
                continue;
            }
        }
        createEmptyProfileImage(imageView);
    }

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
