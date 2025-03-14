package sms.admin.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CsvStudent {
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty firstName = new SimpleStringProperty();
    private final StringProperty middleName = new SimpleStringProperty();
    private final StringProperty lastName = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty cluster = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();
    
    public CsvStudent(String[] data) {
        setEmail(data[1].trim());
        setFirstName(data[2].trim());
        setMiddleName(data[3].trim());
        setLastName(data[4].trim());
        setAddress(data[5].trim());
        setCluster(data[6].trim());
        setContact(data[7].trim());
    }

    // Getters and setters
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
    
    public String getAddress() { return address.get(); }
    public void setAddress(String value) { address.set(value); }
    public StringProperty addressProperty() { return address; }
    
    public String getCluster() { return cluster.get(); }
    public void setCluster(String value) { cluster.set(value); }
    public StringProperty clusterProperty() { return cluster; }
    
    public String getContact() { return contact.get(); }
    public void setContact(String value) { contact.set(value); }
    public StringProperty contactProperty() { return contact; }
}
