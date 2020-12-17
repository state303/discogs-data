package io.dsub.discogsdata.batch.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class MalformedDateParser {

    private static final Instant UNKNOWN =
            getUTCInstant(LocalDate.of(1500, 1, 1));

    private MalformedDateParser() {
    }

    /**
     * @param dateString as xml dump recorded date in either yyyy-mm-dd, yyyy-mm, yyyy.
     *                   Some may contain 19XX, which indicates unknown year, or month.
     * @return formatted release date of LocalDate type.
     * <p>
     * NOTE: IF any malformed date is detected AND it does NOT have year info
     * >> the date will be parsed as Jan 01 1500.
     */
    public static Instant parse(String dateString) {
        try {
            if (dateString == null || dateString.isBlank()) return UNKNOWN;

            // 19XX, 199X, 1999
            if (dateString.length() <= 4 && dateString.length() >= 2) {
                return getUTCInstantFromYear(toYear(dateString));
            }

            // 19XX-1X, 1999-10
            if (dateString.matches("^[0-9]{2,4}-[0-9]{1,2}$")) {
                String[] parts = dateString.split("-");
                int year = toYear(parts[0]);
                int month = toMonth(parts[1]);
                return getUTCInstant(LocalDate.of(year, month, 1));
            }

            if (dateString.matches("^[0-9]{2,4}-[0-9]{1,2}-[0-9]{1,2}$")) {
                String[] parts = dateString.split("-");
                int year = toYear(parts[0]);
                int month = toMonth(parts[1]);
                int day = toMonth(parts[2]);
                return getUTCInstant(LocalDate.of(year, month, day));
            }
        } catch (Throwable ignored) {
        }
        return UNKNOWN;
    }

    private static Instant getUTCInstantFromYear(Integer year) {
        return getUTCInstant(LocalDate.of(year, 1, 1));
    }

    private static Instant getUTCInstant(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
    }

    private static Integer replaceNonDigits(String dateString, int target) {
        return Integer.parseInt(dateString.replaceAll("[^0-9]", String.valueOf(target)));
    }

    private static int toYear(String year) {
        return replaceNonDigits(year, 0);
    }

    private static int toMonth(String month) {
        return replaceNonDigits(month, 1);
    }
}
