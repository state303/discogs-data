package io.dsub.discogsdata.batch.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MalformedDateParser {

    public static final LocalDate UNKNOWN = LocalDate.of(1500, 1, 1);

    public static final String YEAR_PATTERN = "\\d{2}[a-zA-Z0-9]{2}";
    public static final String MONTH_PATTERN = "[a-zA-Z0-9]{0,2}";
    public static final String DATE_PATTERN = "[a-zA-Z0-9]{0,2}";

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
    public static LocalDate parse(String dateString) {
        try {
            if (dateString == null || dateString.isBlank()) return UNKNOWN;

            // trim last '-' char if it happens to have no value appended to it.
            if (dateString.matches(".*-$")) {
                dateString = dateString.substring(0, dateString.length() - 1);
            }

            // 19XX, 199X, 1999
            if (dateString.matches("^" + YEAR_PATTERN + "$")) {
                return LocalDate.of(replaceNonDigitFromYear(dateString), 1, 1);
            }

            // 19XX-XX, 1999-XX, 1999-10
            if (dateString.matches("^" + YEAR_PATTERN + "-" + MONTH_PATTERN + "$")) {
                String[] parts = dateString.split("-");
                int year = replaceNonDigitFromYear(parts[0]);
                int month = replaceNonDigitFromMonth(parts[1]);
                return LocalDate.of(year, month, 1);
            }

            if (dateString.matches("^" + YEAR_PATTERN + "-" + MONTH_PATTERN + "-" + DATE_PATTERN + "$")) {
                String[] parts = dateString.split("-");
                int year = replaceNonDigitFromYear(parts[0]);
                int month = replaceNonDigitFromMonth(parts[1]);
                int day = replaceNonDigitFromMonth(parts[2]);
                int maxDay = LocalDate.of(year, month + 1,  1).minusDays(1).getDayOfMonth();
                if (day > maxDay) {
                    day = 1;
                }
                return LocalDate.of(year, month, day);
            }
        } catch (Throwable ignored) {
            log.info("failed to parse date {}. proceeding with unknown date", dateString);
        }
        return UNKNOWN;
    }

    private static Instant getUTCInstantFromYear(Integer year) {
        return getUTCInstant(LocalDate.of(year, 1, 1));
    }

    private static Instant getUTCInstant(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
    }

    private static int replaceNonDigitFromYear(String year) {
        return replaceNonDigits(year, 0);
    }

    private static int replaceNonDigitFromMonth(String month) {
        if (month.equalsIgnoreCase("xx")) {
            return 1;
        }
        return replaceNonDigits(month, 1);
    }

    private static Integer replaceNonDigits(String dateString, int target) {
        if (dateString.matches("[a-zA-Z]+[0-9]")) {
            return Integer.parseInt(dateString.replaceAll("[^0-9]", "0"));
        }
        return Integer.parseInt(dateString.replaceAll("[^0-9]", String.valueOf(target)));
    }
}
