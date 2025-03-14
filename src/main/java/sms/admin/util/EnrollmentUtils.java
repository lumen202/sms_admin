package sms.admin.util;

import dev.finalproject.data.*;
import dev.finalproject.models.*;
import java.util.Date;

public class EnrollmentUtils {

    public static Student enrollStudent(
            String firstName, String middleName, String lastName, String nameExt,
            String email, String status, String contact, Date dateOfBirth,
            double fare, String street, String city, String municipality,
            String postalCode, String guardianName, String guardianContact, // These params can be removed later
            SchoolYear schoolYear) throws Exception {

        try {
            DatabaseManager.beginTransaction();

            // Create cluster first
            Cluster defaultCluster = new Cluster(0, "Default");
            ClusterDAO.insert(defaultCluster);

            // Create student first so we have the ID
            Student student = new Student(
                    0, firstName, middleName, lastName, nameExt,
                    email, status, contact, dateOfBirth, fare,
                    defaultCluster, schoolYear);
            StudentDAO.insert(student);

            // Now create address with student reference
            Address address = new Address(
                    student,
                    0,
                    city,
                    municipality,
                    street,
                    "",
                    Integer.parseInt(postalCode));
            AddressDAO.insert(address);

            DatabaseManager.commitTransaction();
            return student;

        } catch (Exception e) {
            DatabaseManager.rollbackTransaction();
            throw e;
        }
    }

    public static Student enrollStudentFromCsv(
            CsvStudent csvStudent, SchoolYear schoolYear) throws Exception {

        try {
            DatabaseManager.beginTransaction();

            // Create cluster
            Cluster defaultCluster = new Cluster(0, "Default");
            ClusterDAO.insert(defaultCluster);

            // Create student first
            Student student = new Student(
                    0,
                    csvStudent.getFirstName(),
                    csvStudent.getMiddleName(),
                    csvStudent.getLastName(),
                    "",
                    csvStudent.getEmail(),
                    "Active",
                    csvStudent.getContact(),
                    new Date(),
                    0.0,
                    defaultCluster,
                    schoolYear);
            StudentDAO.insert(student);

            // Create address if exists
            if (csvStudent.getAddress() != null && !csvStudent.getAddress().trim().isEmpty()) {
                String[] addressParts = csvStudent.getAddress().split(",");
                Address address = new Address(
                        student,
                        0,
                        addressParts.length > 1 ? addressParts[1].trim() : "",
                        addressParts.length > 2 ? addressParts[2].trim() : "",
                        addressParts.length > 0 ? addressParts[0].trim() : "",
                        "",
                        0);
                AddressDAO.insert(address);
            }

            DatabaseManager.commitTransaction();
            return student;

        } catch (Exception e) {
            DatabaseManager.rollbackTransaction();
            throw e;
        }
    }
}
