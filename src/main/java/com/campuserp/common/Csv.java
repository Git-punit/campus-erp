package com.campuserp.seed;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Just enough CSV reading for the sample data: header row, commas, double-quoted fields. */
final class Csv {

    private Csv() {}

    static List<Map<String, String>> read(String file) {
        String text;
        try (var in = new ClassPathResource("sample-data/" + file).getInputStream()) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read sample-data/" + file, e);
        }
        List<List<String>> rows = parse(text);
        List<Map<String, String>> out = new ArrayList<>();
        if (rows.isEmpty()) {
            return out;
        }
        List<String> header = rows.getFirst();
        for (List<String> r : rows.subList(1, rows.size())) {
            if (r.size() == 1 && r.getFirst().isEmpty()) {
                continue;
            }
            Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
                m.put(header.get(i), i < r.size() ? r.get(i) : "");
            }
            out.add(m);
        }
        return out;
    }

    static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
