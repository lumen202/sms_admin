/**
 * Manager class for handling profile-related data updates in the SMS administrative module.
 * <p>
 * Provides static methods to update and persist {@link dev.finalproject.models.Guardian},
 * {@link dev.finalproject.models.Address}, {@link dev.finalproject.models.Cluster}, and
 * {@link dev.finalproject.models.Student} objects based on JavaFX UI input fields.
 * </p>
 */
package sms.admin.util.profile;

import dev.finalproject.data.GuardianDAO;
import dev.finalproject.models.*;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;
import java.util.Optional;

public class ProfileDataManager {

    /**
     * Updates a Guardian object with values from provided text fields.
     * Does not persist changes to the database.
     *
     * @param guardian          the Guardian to update
     * @param studentGuardian   the StudentGuardian relationship (unused but
     *                          retained for extension)
     * @param firstNameField    TextField containing the guardian's first name
     * @param middleNameField   TextField containing the guardian's middle name
     * @param lastNameField     TextField containing the guardian's last name
     * @param relationshipField TextField containing the guardian's relationship
     * @param contactField      TextField containing the guardian's contact number
     */
    public static void updateGuardianInfo(Guardian guardian,
            StudentGuardian studentGuardian,
            TextField firstNameField,
            TextField middleNameField,
            TextField lastNameField,
            TextField relationshipField,
            TextField contactField) {
        guardian.setFirstName(firstNameField.getText());
        guardian.setMiddleName(middleNameField.getText());
        guardian.setLastName(lastNameField.getText());
        guardian.setContact(contactField.getText());
        guardian.setRelationship(relationshipField.getText());
    }

    /**
     * Creates a new guardian or updates an existing one, persists via DAO,
     * and maintains the observable list of guardians.
     *
     * @param viewName          the display name for the guardian
     * @param viewContact       the display contact info for the guardian
     * @param firstNameField    TextField with first name input
     * @param middleNameField   TextField with middle name input
     * @param lastNameField     TextField with last name input
     * @param relationshipField TextField with relationship input
     * @param contactField      TextField with contact input
     * @param existingGuardian  the Guardian to update, or null to create new
     * @param guardianList      the ObservableList of all Guardians
     * @return the created or updated Guardian instance
     * @throws RuntimeException if the DAO insert/update operation fails
     */
    public static Guardian createOrUpdateGuardian(
            String viewName,
            String viewContact,
            TextField firstNameField,
            TextField middleNameField,
            TextField lastNameField,
            TextField relationshipField,
            TextField contactField,
            Guardian existingGuardian,
            ObservableList<Guardian> guardianList) {
        Guardian guardian = existingGuardian != null
                ? existingGuardian
                : createNewGuardian(guardianList);
        updateGuardianFields(guardian,
                firstNameField,
                middleNameField,
                lastNameField,
                relationshipField,
                contactField);

        try {
            if (existingGuardian == null) {
                GuardianDAO.insert(guardian);
                guardianList.add(guardian);
            } else {
                GuardianDAO.update(guardian);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save guardian information");
        }

        return guardian;
    }

    /**
     * Creates a new Guardian instance with a unique ID based on the max ID in the
     * list.
     *
     * @param guardianList the ObservableList of existing Guardians
     * @return a new Guardian with generated ID and empty fields
     */
    private static Guardian createNewGuardian(ObservableList<Guardian> guardianList) {
        int newId = guardianList.stream()
                .mapToInt(Guardian::getGuardianID)
                .max()
                .orElse(0) + 1;
        return new Guardian(newId, "", "", "", "", "");
    }

    /**
     * Updates the fields of a Guardian object from text fields (trimming
     * whitespace).
     *
     * @param guardian          the Guardian to update
     * @param firstNameField    TextField containing the first name
     * @param middleNameField   TextField containing the middle name
     * @param lastNameField     TextField containing the last name
     * @param relationshipField TextField containing the relationship
     * @param contactField      TextField containing the contact number
     */
    private static void updateGuardianFields(
            Guardian guardian,
            TextField firstNameField,
            TextField middleNameField,
            TextField lastNameField,
            TextField relationshipField,
            TextField contactField) {
        guardian.setFirstName(firstNameField.getText().trim());
        guardian.setMiddleName(middleNameField.getText().trim());
        guardian.setLastName(lastNameField.getText().trim());
        guardian.setContact(contactField.getText().trim());
        guardian.setRelationship(relationshipField.getText().trim());
    }

    /**
     * Updates an Address object with values from text fields, associating it with a
     * student.
     * Parses the zip code text to integer, defaulting to 0 on parse error.
     *
     * @param address           the Address to update
     * @param student           the Student associated with this address
     * @param streetField       TextField with street input
     * @param barangayField     TextField with barangay input
     * @param cityField         TextField with city input
     * @param municipalityField TextField with municipality input
     * @param zipCodeField      TextField with zip code input
     */
    public static void updateAddressInfo(
            Address address,
            Student student,
            TextField streetField,
            TextField barangayField,
            TextField cityField,
            TextField municipalityField,
            TextField zipCodeField) {
        address.setStudentID(student);
        address.setStreet(streetField.getText());
        address.setBarangay(barangayField.getText());
        address.setCity(cityField.getText());
        address.setMunipality(municipalityField.getText());
        try {
            address.setZipCode(Integer.parseInt(zipCodeField.getText().trim()));
        } catch (NumberFormatException e) {
            address.setZipCode(0);
        }
    }

    /**
     * Handles creation or update of a Cluster based on provided name and optional
     * ID.
     *
     * @param clusterName the desired cluster name
     * @param clusterId   the existing cluster ID as string, or empty/null to create
     *                    new
     * @param clusterList the ObservableList of existing Clusters
     * @return Optional containing the updated or newly created Cluster, or empty if
     *         name is blank
     */
    public static Optional<Cluster> handleClusterUpdate(
            String clusterName,
            String clusterId,
            ObservableList<Cluster> clusterList) {
        if (!clusterName.isEmpty()) {
            if (clusterId != null && !clusterId.isEmpty()) {
                int id = Integer.parseInt(clusterId);
                return Optional.of(updateExistingCluster(id, clusterName, clusterList));
            } else {
                return Optional.of(createNewCluster(clusterName, clusterList));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds and updates an existing Cluster's name.
     *
     * @param id          the ID of the cluster to update
     * @param name        the new name for the cluster
     * @param clusterList the ObservableList of clusters
     * @return the updated Cluster, or a new Cluster if not found
     */
    private static Cluster updateExistingCluster(
            int id,
            String name,
            ObservableList<Cluster> clusterList) {
        return clusterList.stream()
                .filter(c -> c.getClusterID() == id)
                .findFirst()
                .map(c -> {
                    c.setClusterName(name);
                    return c;
                })
                .orElseGet(() -> new Cluster(id, name));
    }

    /**
     * Creates a new Cluster with a unique ID and the given name.
     *
     * @param name        the cluster name
     * @param clusterList the ObservableList of existing clusters
     * @return the newly created Cluster
     */
    private static Cluster createNewCluster(
            String name,
            ObservableList<Cluster> clusterList) {
        int newId = clusterList.stream()
                .mapToInt(Cluster::getClusterID)
                .max()
                .orElse(0) + 1;
        return new Cluster(newId, name);
    }

    /**
     * Updates basic Student fields from text inputs: name, contact, email, and
     * fare.
     * Parses fare text to double, defaulting to 0.0 if empty or invalid.
     *
     * @param student         the Student to update
     * @param firstNameField  TextField with first name input
     * @param middleNameField TextField with middle name input
     * @param lastNameField   TextField with last name input
     * @param nameExtField    TextField with name extension input
     * @param contactField    TextField with contact input
     * @param emailField      TextField with email input
     * @param fareField       TextField with fare input
     */
    public static void updateBasicStudentInfo(
            Student student,
            TextField firstNameField,
            TextField middleNameField,
            TextField lastNameField,
            TextField nameExtField,
            TextField contactField,
            TextField emailField,
            TextField fareField) {
        student.setFirstName(firstNameField.getText());
        student.setMiddleName(middleNameField.getText());
        student.setLastName(lastNameField.getText());
        student.setNameExtension(nameExtField.getText());
        student.setContact(contactField.getText());
        student.setEmail(emailField.getText());
        String fareText = fareField.getText().trim();
        student.setFare(!fareText.isEmpty() ? Double.valueOf(fareText) : 0.0);
    }
}
