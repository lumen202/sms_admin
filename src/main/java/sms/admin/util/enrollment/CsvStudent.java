package sms.admin.util.enrollment;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CsvStudent {
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty middleName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final StringProperty nameExtension = new SimpleStringProperty(); // New property for name extension
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty cluster = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();
    private final StringProperty timestamp = new SimpleStringProperty();
    
    /**
     * Constructs a CsvStudent by parsing an array of String data.
     * Expected CSV columns:
     * 0: Timestamp
     * 1: Email Address
     * 2: Given Name (First name)
     * 3: Middle name
     * 4: Family Name (Last name and optionally name extension; e.g., "Dela Torre")
     * 5: Home Address (in the format "Street, Barangay, City, Municipality")
     * 6: Cluster Graduated From Phase 1
     * 7: Contact Number
     *
     * For the Family Name (data[4]), if it contains two tokens separated by whitespace,
     * the first token is set as the last name and the second as the name extension.
     *
     * @param data the CSV data array.
     */
    public CsvStudent(String[] data) {
        setTimestamp(data[0].trim());
        setEmail(data[1].trim());
        setFirstName(data[2].trim());
        setMiddleName(data[3].trim());
        
        // Process family name (data[4]) for last name and name extension.
        String rawLastName = data[4].trim();
        // Split by whitespace. For "Dela Torre", tokens will be ["Dela", "Torre"]
        String[] tokens = rawLastName.split("\\s+");
        if (tokens.length == 2) {
            // Set the first token as last name and the second as name extension.
            setLastName(tokens[0].trim());
            setNameExtension(tokens[1].trim());
        } else {
            // If the format is unexpected, use the entire string as the last name.
            setLastName(rawLastName);
        }
        
        // Clean up address format.
        // Expected format: "Soob, Poblacion, Albuera, Leyte"
        String rawAddress = data[5].trim()
            .replace("\"", "")
            .replace(" ,", ",")
            .replace(", ", ",");
        setAddress(rawAddress);
        
        setCluster(data[6].trim());
        setContact(data[7].trim());
    }

    // Helper methods to retrieve parsed address components
    public String[] getAddressParts() {
        return getAddress().split(",");
    }

    /**
     * Returns the street (first component) of the address.
     */
    public String getStreet() {
        String[] parts = getAddressParts();
        return parts.length > 0 ? parts[0] : "";
    }

    /**
     * Returns the barangay (second component) of the address.
     */
    public String getBarangay() {
        String[] parts = getAddressParts();
        return parts.length > 1 ? parts[1] : "";
    }

    /**
     * Returns the city (third component) of the address.
     */
    public String getCity() {
        String[] parts = getAddressParts();
        return parts.length > 2 ? parts[2] : "";
    }

    /**
     * Returns the municipality (fourth component) of the address.
     */
    public String getMunicipality() {
        String[] parts = getAddressParts();
        return parts.length > 3 ? parts[3] : "";
    }

    // Getters and setters for all properties

    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }
    
    public String getFirstName() { return firstName.get(); }
    public void setFirstName(String value) { firstName.set(value); }
    public StringProperty firstNameProperty() { return firstName; }
    
    public String getMiddleName() { return middleName.get(); }
    public void setMiddleName(String value) { middleName.set(value); }
    public StringProperty middleNameProperty() { return middleName; }
    
    public String getLastName() { return lastName.get(); }
    public void setLastName(String value) { lastName.set(value); }
    public StringProperty lastNameProperty() { return lastName; }
    
    public String getNameExtension() { return nameExtension.get(); }
    public void setNameExtension(String value) { nameExtension.set(value); }
    public StringProperty nameExtensionProperty() { return nameExtension; }
    
    public String getAddress() { return address.get(); }
    public void setAddress(String value) { address.set(value); }
    public StringProperty addressProperty() { return address; }
    
    public String getCluster() { return cluster.get(); }
    public void setCluster(String value) { cluster.set(value); }
    public StringProperty clusterProperty() { return cluster; }
    
    public String getContact() { return contact.get(); }
    public void setContact(String value) { contact.set(value); }
    public StringProperty contactProperty() { return contact; }

    public String getTimestamp() { return timestamp.get(); }
    public void setTimestamp(String value) { timestamp.set(value); }
    public StringProperty timestampProperty() { return timestamp; }
}