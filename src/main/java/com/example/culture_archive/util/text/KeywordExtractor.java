// src/main/java/com/example/culture_archive/service/KeywordExtractor.java
package com.example.culture_archive.util.text;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class KeywordExtractor {
    private static final Pattern SPLIT = Pattern.compile("[^\\p{IsHangul}A-Za-z0-9]+");

    public List<String> extract(List<String> allowed, String... texts) {
        if (allowed == null || allowed.isEmpty()) return List.of();
        Set<String> allow = allowed.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        Set<String> hits = new LinkedHashSet<>();
        for (String t : texts) {
            if (t == null || t.isBlank()) continue;
            for (String tok : SPLIT.split(t)) {
                if (!tok.isBlank() && allow.contains(tok)) hits.add(tok);
            }
        }
        return hits.stream().limit(12).toList();
    }
}
