package com.example.culture_archive.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeriodParser {

    private static final Pattern RANGE_WITH_SEPARATOR = Pattern.compile("(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\s*[~–-]\\s*(?:(\\d{4})[./-])?(\\d{1,2})[./-](\\d{1,2})");
    private static final Pattern RANGE_WITHOUT_SEPARATOR = Pattern.compile("(\\d{8})\\s*[~–-]\\s*(\\d{8})");
    private static final DateTimeFormatter NO_SEPARATOR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static Optional<DateRange> parse(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        String cleaned = text.replaceAll("\\s+", "").replace("–", "~");

        Matcher mWithoutSep = RANGE_WITHOUT_SEPARATOR.matcher(cleaned);
        if (mWithoutSep.find()) {
            LocalDate start = toDateWithoutSeparator(mWithoutSep.group(1));
            LocalDate end = toDateWithoutSeparator(mWithoutSep.group(2));
            if (start != null && end != null) return Optional.of(new DateRange(start, end));
        }

        Matcher mWithSep = RANGE_WITH_SEPARATOR.matcher(cleaned);
        if (mWithSep.find()) {
            LocalDate start = toDateWithSeparator(mWithSep.group(1));
            String endYear = (mWithSep.group(2) != null) ? mWithSep.group(2) : (start != null ? String.valueOf(start.getYear()) : null);
            LocalDate end = (endYear == null) ? null : toDateWithSeparator(endYear + "-" + mWithSep.group(3) + "-" + mWithSep.group(4));
            if (start != null && end != null) return Optional.of(new DateRange(start, end));
        }

        return firstDate(cleaned).map(d -> new DateRange(d, d));
    }

    private static Optional<LocalDate> firstDate(String s) {
        Matcher oneWithoutSep = Pattern.compile("(\\d{8})").matcher(s);
        if (oneWithoutSep.find()) {
            return Optional.ofNullable(toDateWithoutSeparator(oneWithoutSep.group(1)));
        }
        Matcher oneWithSep = Pattern.compile("(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})").matcher(s);
        if (oneWithSep.find()) {
            return Optional.ofNullable(toDateWithSeparator(oneWithSep.group(1)));
        }
        return Optional.empty();
    }

    private static LocalDate toDateWithSeparator(String s) {
        if (s == null) return null;
        String t = s.replace('.', '-').replace('/', '-').trim();
        try {
            return LocalDate.parse(t, DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (Exception ex) { return null; }
    }

    private static LocalDate toDateWithoutSeparator(String s) {
        if (s == null || s.length() != 8) return null;
        try {
            return LocalDate.parse(s, NO_SEPARATOR_FORMATTER);
        } catch (Exception ex) { return null; }
    }
}