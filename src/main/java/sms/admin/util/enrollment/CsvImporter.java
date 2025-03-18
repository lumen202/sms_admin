package sms.admin.util.enrollment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CsvImporter {
    public static List<CsvStudent> importCsv(File file) throws IOException {
        List<CsvStudent> students = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());
        
        // Skip header line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] data = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // Split by comma, respect quotes
            if (data.length >= 8) { // Ensure we have all required fields
                students.add(new CsvStudent(data));
            }
        }
        return students;
    }
}
