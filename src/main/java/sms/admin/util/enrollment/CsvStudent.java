package sms.admin.util.enrollment;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a student record parsed from a CSV file.
 * <p>
 * This class provides properties and utility methods to extract structured data
 * from a raw CSV input. It supports detection of name suffixes (e.g., Jr.,
 * Sr.),
 * and parsing loosely structured address fields into components like street,
 * barangay, city, and municipality.
 */
public class CsvStudent {

    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty middleName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final StringProperty nameExtension = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty cluster = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();
    private final StringProperty timestamp = new SimpleStringProperty();

    private static final Set<String> KNOWN_EXTENSIONS = new HashSet<>(Arrays.asList("Jr", "Sr", "II", "III", "IV"));

    /**
     * Constructs a CsvStudent instance by parsing fields from a CSV data row.
     * 
     * @param data the raw CSV row data as an array of strings
     */
    public CsvStudent(String[] data) {
        setTimestamp(trimToNull(data[0]));
        setEmail(trimToNull(data[1]));
        setFirstName(trimToNull(data[2]));
        setMiddleName(trimToNull(data[3]));

        // Parse last name and optional name extension
        String rawLastName = trimToNull(data[4]);
        if (rawLastName != null) {
            String[] tokens = rawLastName.split("\\s+");
            if (tokens.length > 1 && KNOWN_EXTENSIONS.contains(tokens[tokens.length - 1])) {
                setLastName(String.join(" ", Arrays.copyOf(tokens, tokens.length - 1)).trim());
                setNameExtension(tokens[tokens.length - 1].trim());
            } else {
                setLastName(rawLastName);
            }
        }

        // Clean up address string
        String rawAddress = trimToNull(data[5]);
        if (rawAddress != null) {
            rawAddress = rawAddress.replace("\"", "")
                    .replace(" ,", ",")
                    .replace(", ", ",");
            while (rawAddress.endsWith(",")) {
                rawAddress = rawAddress.substring(0, rawAddress.length() - 1).trim();
            }
        }
        setAddress(rawAddress);

        setCluster(trimToNull(data[6]));
        setContact(trimToNull(data[7]));
    }

    // Add helper method
    private String trimToNull(String str) {
        if (str == null) return null;
        str = str.trim();
        return str.isEmpty() ? null : str;
    }

    /**
     * Attempts to parse the address into four components:
     * street, barangay, city, and municipality.
     * 
     * @return a string array of length 4 containing parsed address parts
     */
    public String[] getAddressParts() {
        String raw = getAddress();
        if (raw.contains(",")) {
            String[] parts = raw.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            if (parts.length == 3) {
                String first = parts[0].toLowerCase();
                if (first.contains("st") || first.contains("street") || first.contains("ave")) {
                    String street = parts[0];
                    String middle = parts[1];
                    String municipality = parts[2];
                    String barangay, city;
                    int idx = middle.indexOf("Brgy.");
                    if (idx != -1) {
                        barangay = middle.substring(0, idx + "Brgy.".length()).trim();
                        city = middle.substring(idx + "Brgy.".length()).trim();
                    } else {
                        String[] tokens = middle.split("\\s+", 2);
                        barangay = tokens.length == 2 ? tokens[0].trim() : middle;
                        city = tokens.length == 2 ? tokens[1].trim() : "";
                    }
                    return new String[] { street, barangay, city, municipality };
                } else {
                    return new String[] { "", parts[0], parts[1], parts[2] };
                }
            } else if (parts.length >= 4) {
                return new String[] { parts[0], parts[1], parts[2], parts[3] };
            } else {
                return new String[] { raw.trim(), "", "", "" };
            }
        } else {
            String[] tokens = raw.split("\\s+");
            if (tokens.length == 4 && tokens[0].equalsIgnoreCase("Barangay")) {
                return new String[] { "", tokens[0] + " " + tokens[1], tokens[2], tokens[3] };
            } else if (tokens.length == 2) {
                return new String[] { getMiddleName(), getLastName(), tokens[0], tokens[1] };
            } else if (tokens.length == 3) {
                return new String[] { "", tokens[0], tokens[1], tokens[2] };
            } else {
                return new String[] { raw.trim(), "", "", "" };
            }
        }
    }

    // Convenience methods to get parsed address fields
    public String getStreet() {
        return getAddressParts()[0];
    }

    public String getBarangay() {
        return getAddressParts()[1];
    }

    public String getCity() {
        return getAddressParts()[2];
    }

    public String getMunicipality() {
        return getAddressParts()[3];
    }

    // Property getters and setters
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String value) {
        email.set(value);
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getFirstName() {
        return firstName.get();
    }

    public void setFirstName(String value) {
        firstName.set(value);
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName.get();
    }

    public void setMiddleName(String value) {
        middleName.set(value);
    }

    public StringProperty middleNameProperty() {
        return middleName;
    }

    public String getLastName() {
        return lastName.get();
    }

    public void setLastName(String value) {
        lastName.set(value);
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public String getNameExtension() {
        return nameExtension.get();
    }

    public void setNameExtension(String value) {
        nameExtension.set(value);
    }

    public StringProperty nameExtensionProperty() {
        return nameExtension;
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String value) {
        address.set(value);
    }

    public StringProperty addressProperty() {
        return address;
    }

    public String getCluster() {
        return cluster.get();
    }

    public void setCluster(String value) {
        cluster.set(value);
    }

    public StringProperty clusterProperty() {
        return cluster;
    }

    public String getContact() {
        return contact.get();
    }

    public void setContact(String value) {
        contact.set(value);
    }

    public StringProperty contactProperty() {
        return contact;
    }

    public String getTimestamp() {
        return timestamp.get();
    }

    public void setTimestamp(String value) {
        timestamp.set(value);
    }

    public StringProperty timestampProperty() {
        return timestamp;
    }
}
