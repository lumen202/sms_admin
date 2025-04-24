package sms.admin.util.enrollment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for importing student data from a CSV file.
 * 
 * <p>
 * This class reads a CSV file, parses its contents, and returns a list of
 * {@link CsvStudent}
 * objects, one for each student row (excluding the header). It handles quoted
 * values properly
 * using a regex-based split.
 * </p>
 */
public class CsvImporter {

    /**
     * Imports student data from the specified CSV file.
     *
     * @param file the CSV file to read
     * @return a list of {@link CsvStudent} objects parsed from the file
     * @throws IOException if an I/O error occurs reading from the file
     */
    public static List<CsvStudent> importCsv(File file) throws IOException {
        List<CsvStudent> students = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        // Skip the header and parse each subsequent line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            // Use regex to split on commas not inside quotes
            String[] data = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

            // Ensure the line contains at least 8 fields
            if (data.length >= 8) {
                students.add(new CsvStudent(data));
            }
        }

        return students;
    }
}
