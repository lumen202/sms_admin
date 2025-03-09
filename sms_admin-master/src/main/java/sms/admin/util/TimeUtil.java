package sms.admin.util;

public class TimeUtil {
    public static String formatTime(int time) {
        if (time == 0) return "";
        int hours = time / 100;
        int minutes = time % 100;
        return String.format("%02d:%02d", hours, minutes);
    }

    public static int parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 0;
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
    }
}
