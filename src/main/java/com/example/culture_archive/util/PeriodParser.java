package com.example.culture_archive.util;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeriodParser {

    // yyyy.MM.dd ~ (yyyy.)MM.dd  / 구분자(~,–,-,/ .) / 공백 허용
    private static final Pattern RANGE = Pattern.compile(
            "(\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\s*[~–-]\\s*(?:(\\d{4})[./-])?(\\d{1,2})[./-](\\d{1,2})"
    );

    public static Optional<DateRange> parse(String text) {
        if (text == null) return Optional.empty();

        // 흔한 표현 정리
        String cleaned = text
                .replaceAll("\\s+", " ")
                .replace("–", "~")
                .replace("~ ~", "~")
                .trim();

        // "상시" → 아주 먼 미래로 간주
        if (cleaned.contains("상시")) {
            // 날짜가 하나라도 있으면 시작=그 날짜, 끝=2099-12-31 로 해석
            LocalDate start = firstDate(cleaned).orElse(LocalDate.now());
            return Optional.of(new DateRange(start, LocalDate.of(2099, 12, 31)));
        }

        Matcher m = RANGE.matcher(cleaned);
        if (!m.find()) {
            // yyyy.MM.dd 같은 단일 날짜만 있을 경우: 시작=그 날, 끝=같은 날
            return firstDate(cleaned).map(d -> new DateRange(d, d));
        }

        LocalDate start = toDate(m.group(1));
        String endYear = (m.group(2) != null) ? m.group(2)
                : (start != null ? String.valueOf(start.getYear()) : null);
        LocalDate end = (endYear == null) ? null
                : toDate(endYear + "-" + m.group(3) + "-" + m.group(4));

        if (start == null || end == null) return Optional.empty();
        return Optional.of(new DateRange(start, end));
    }

    private static Optional<LocalDate> firstDate(String s) {
        Matcher one = Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})").matcher(s);
        if (one.find()) {
            return Optional.of(LocalDate.of(
                    Integer.parseInt(one.group(1)),
                    Integer.parseInt(one.group(2)),
                    Integer.parseInt(one.group(3))
            ));
        }
        return Optional.empty();
    }

    private static LocalDate toDate(String s) {
        if (s == null) return null;
        String t = s.replace('.', '-').replace('/', '-').trim();
        String[] a = t.split("-");
        if (a.length < 3) return null;
        try {
            return LocalDate.of(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
        } catch (Exception ex) {
            return null;
        }
    }
}
