module sms.admin {

    requires transitive javafx.controls;
    requires transitive core.fx;
    requires transitive core.db;
    requires transitive dev.finalproject;
    requires transitive atlantafx.base;

    requires javafx.fxml;
    requires javafx.graphics;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.materialdesign;
    requires core.util;
    requires javafx.base;
    requires java.sql.rowset;
    requires java.desktop; // Add this line for AWT/Swing classes

    // Add iText 7 module dependencies
    requires kernel;
    requires layout;
    requires io;

    // Add Apache POI module dependencies
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens sms.admin to javafx.fxml;
    opens sms.admin.app to javafx.fxml, core.fx;
    opens sms.admin.app.attendance to core.fx, javafx.fxml;
    opens sms.admin.app.student.viewstudent to core.fx, javafx.fxml;
    opens sms.admin.app.payroll to core.fx, javafx.fxml;
    opens sms.admin.app.payroll.dialog to core.fx, javafx.fxml; // Add this line
    opens sms.admin.app.student to core.fx, javafx.fxml;
    opens sms.admin.app.student.enrollment to core.fx, javafx.fxml;
    opens sms.admin.app.schoolyear to core.fx, javafx.fxml;
    opens sms.admin.app.attendance.dialog to core.fx, javafx.fxml;
    opens sms.admin.util.exporter to core.fx, javafx.fxml;
    opens sms.admin.util to javafx.base;
    opens sms.admin.app.deleted_student to core.fx, javafx.fxml;

    exports sms.admin;
    exports sms.admin.app;
    exports sms.admin.app.student;
    exports sms.admin.app.student.viewstudent;
    exports sms.admin.app.schoolyear;
    exports sms.admin.app.attendance.dialog;
    exports sms.admin.app.payroll.dialog;
}
