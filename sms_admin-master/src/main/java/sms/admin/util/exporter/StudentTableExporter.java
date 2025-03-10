package sms.admin.util.exporter;

import dev.finalproject.models.Student;

public class StudentTableExporter extends BaseTableExporter<Student> {
    @Override
    protected String getSheetName() {
        return "Students";
    }
}
