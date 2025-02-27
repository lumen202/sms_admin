package sms.admin.app.enrollment;

import dev.sol.core.application.FXController;

public class EnrollmentController extends FXController {

    @Override
    protected void load_bindings() {
        System.out.println();
    }

    @Override
    protected void load_fields() {
        System.out.println("Enrollment is called");
    }

    @Override
    protected void load_listeners() {
        System.out.println();
    }

}
