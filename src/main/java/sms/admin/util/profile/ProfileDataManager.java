package sms.admin.util.profile;

import dev.finalproject.models.*;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;
import java.util.Optional;

public class ProfileDataManager {
    public static void updateGuardianInfo(Guardian guardian, StudentGuardian studentGuardian,
            TextField firstNameField, TextField middleNameField, TextField lastNameField,
            TextField relationshipField, TextField contactField) {
        guardian.setFirstName(firstNameField.getText());
        guardian.setMiddleName(middleNameField.getText());
        guardian.setLastName(lastNameField.getText());
        guardian.setContact(contactField.getText());
        guardian.setRelationship(relationshipField.getText());
    }

    public static void updateAddressInfo(Address address, Student student,
            TextField streetField, TextField barangayField, TextField cityField,
            TextField municipalityField, TextField zipCodeField) {
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

    public static Optional<Cluster> handleClusterUpdate(String clusterName, String clusterId, 
            ObservableList<Cluster> clusterList) {
        if (!clusterName.isEmpty()) {
            if (clusterId != null && !clusterId.isEmpty()) {
                // Update existing cluster
                int id = Integer.parseInt(clusterId);
                return Optional.of(updateExistingCluster(id, clusterName, clusterList));
            } else {
                // Create new cluster
                return Optional.of(createNewCluster(clusterName, clusterList));
            }
        }
        return Optional.empty();
    }

    private static Cluster updateExistingCluster(int id, String name, ObservableList<Cluster> clusterList) {
        return clusterList.stream()
                .filter(c -> c.getClusterID() == id)
                .findFirst()
                .map(c -> {
                    c.setClusterName(name);
                    return c;
                })
                .orElseGet(() -> new Cluster(id, name));
    }

    private static Cluster createNewCluster(String name, ObservableList<Cluster> clusterList) {
        int newId = clusterList.stream()
                .mapToInt(Cluster::getClusterID)
                .max()
                .orElse(0) + 1;
        return new Cluster(newId, name);
    }

    public static void updateBasicStudentInfo(Student student,
            TextField firstNameField, TextField middleNameField, TextField lastNameField,
            TextField nameExtField, TextField contactField, TextField emailField,
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
