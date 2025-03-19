package sms.admin.util.enrollment;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CsvStudent {
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty middleName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final StringProperty nameExtension = new SimpleStringProperty(); // Property for name extension
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty cluster = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();
    private final StringProperty timestamp = new SimpleStringProperty();

    // Define a set of known name extensions/suffixes.
    private static final Set<String> KNOWN_EXTENSIONS = new HashSet<>(Arrays.asList("Jr", "Sr", "II", "III", "IV"));

    /**
     * Constructs a CsvStudent by parsing an array of String data.
     * Expected CSV columns:
     * 0: Timestamp
     * 1: Email Address
     * 2: Given Name (First name)
     * 3: Middle name
     * 4: Family Name (Last name and optionally name extension; e.g., "Dela Torre" or "Tahil Jr")
     * 5: Home Address (in the format "Street, Barangay, City, Municipality" or other variations)
     * 6: Cluster Graduated From Phase 1
     * 7: Contact Number
     *
     * For the Family Name (data[4]), if the last token matches a known name extension,
     * the tokens except for the last one are considered as the last name and the last token as the extension.
     * Otherwise, the entire string is used as the last name.
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
        String[] tokens = rawLastName.split("\\s+");
        if (tokens.length > 1 && KNOWN_EXTENSIONS.contains(tokens[tokens.length - 1])) {
            setLastName(String.join(" ", Arrays.copyOf(tokens, tokens.length - 1)).trim());
            setNameExtension(tokens[tokens.length - 1].trim());
        } else {
            setLastName(rawLastName);
        }

        // Clean up address format.
        // Remove quotes, extra spaces, and any trailing commas.
        String rawAddress = data[5].trim()
            .replace("\"", "")
            .replace(" ,", ",")
            .replace(", ", ",");
        while (rawAddress.endsWith(",")) {
            rawAddress = rawAddress.substring(0, rawAddress.length() - 1).trim();
        }
        setAddress(rawAddress);

        setCluster(data[6].trim());
        setContact(data[7].trim());
    }

    /**
     * Returns an array of four address components: {street, barangay, city, municipality}.
     * 
     * The parser handles multiple cases:
     * 
     * 1. If the raw address contains commas:
     *    a) For 4 parts, it assumes "Street, Barangay, City, Municipality".
     *    b) For 3 parts, it uses a heuristic:
     *         - If the first part looks like a street (contains "st", "street", or "ave"),
     *           assume the format is "Street, Barangay+City, Municipality" (splitting the middle part via "Brgy." or whitespace).
     *         - Otherwise, assume no street was provided and map the parts as: barangay, city, municipality.
     * 
     * 2. If no comma is present:
     *    a) If there are exactly 4 tokens and the first token equals "Barangay", use that format.
     *    b) If there are exactly 2 tokens, assume the address field provides only city and municipality;
     *       in that case, use the middle name as street and the last name as barangay.
     *    c) If there are exactly 3 tokens, assume no street was provided.
     *    d) Otherwise, treat the entire string as the street.
     * 
     * @return a String array of length 4: {street, barangay, city, municipality}
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
                    // Format: "Street, Barangay+City, Municipality"
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
                        if (tokens.length == 2) {
                            barangay = tokens[0].trim();
                            city = tokens[1].trim();
                        } else {
                            barangay = middle;
                            city = "";
                        }
                    }
                    return new String[] { street, barangay, city, municipality };
                } else {
                    // No street provided.
                    return new String[] { "", parts[0], parts[1], parts[2] };
                }
            } else if (parts.length >= 4) {
                return new String[] { parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim() };
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
                // Assume no street provided.
                return new String[] { "", tokens[0], tokens[1], tokens[2] };
            } else {
                return new String[] { raw.trim(), "", "", "" };
            }
        }
    }

    public String getStreet() {
        String[] parts = getAddressParts();
        return parts.length > 0 ? parts[0] : "";
    }

    public String getBarangay() {
        String[] parts = getAddressParts();
        return parts.length > 1 ? parts[1] : "";
    }

    public String getCity() {
        String[] parts = getAddressParts();
        return parts.length > 2 ? parts[2] : "";
    }

    public String getMunicipality() {
        String[] parts = getAddressParts();
        return parts.length > 3 ? parts[3] : "";
    }

    // Getters and setters for all properties.

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
