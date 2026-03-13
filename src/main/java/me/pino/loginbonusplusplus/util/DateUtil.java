package me.pino.loginbonusplusplus.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDate getToday() {
        return LocalDate.now();
    }

    public static int getCurrentDayOfMonth() {
        return LocalDate.now().getDayOfMonth();
    }

    public static int getCurrentMonthLength() {
        return LocalDate.now().lengthOfMonth();
    }

    public static String format(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static boolean isYesterday(String lastLoginDate) {
        if (lastLoginDate == null || lastLoginDate.trim().isEmpty()) {
            return false;
        }

        try {
            LocalDate lastLogin = LocalDate.parse(lastLoginDate, DATE_FORMATTER);
            LocalDate yesterday = LocalDate.now().minusDays(1);
            return lastLogin.equals(yesterday);
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
