package sms.admin.util.exporter;

import dev.finalproject.models.Student;

public class AttendanceTableExporter extends BaseTableExporter<Student> {
    @Override
    protected String getSheetName() {
        return "Attendance";
    }
}
