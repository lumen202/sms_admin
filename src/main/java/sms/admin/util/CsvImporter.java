package sms.admin.util;

import java.io.*;
import java.util.*;

public class CsvImporter {
    public static List<CsvStudent> importCsv(File file) throws IOException {
        List<CsvStudent> students = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (data.length >= 8) {
                    students.add(new CsvStudent(data));
                }
            }
        }
        return students;
    }
}
