package com.campuserp.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads an uploaded CSV file into records, all or nothing: every row is
 * checked first, and if any row is wrong nothing is saved and the error lists
 * each bad row. Column names are matched without caring about case or spaces.
 */
@Component
public class CsvImporter {

    public record Result(int imported) {}

    private static final int MAX_ROWS = 5000;
    private static final int MAX_ERRORS_SHOWN = 15;

    private final Validator validator;

    public CsvImporter(Validator validator) {
        this.validator = validator;
    }

    /**
     * @param build turns one row into a record ready to save; throw BusinessRuleException,
     *              NotFoundException or IllegalArgumentException for a bad row
     */
    public <T> List<T> read(String csv, List<String> requiredColumns, Function<Map<String, String>, T> build) {
        List<Map<String, String>> rows = Csv.rows(csv == null ? "" : csv).stream().map(CsvImporter::normalise).toList();
        if (rows.isEmpty()) {
            throw new BusinessRuleException("The file has no rows. The first line must be the column names: "
                    + String.join(", ", requiredColumns));
        }
        List<String> missing = requiredColumns.stream().filter(c -> !rows.getFirst().containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("Missing columns: " + String.join(", ", missing)
                    + ". The first line must name the columns.");
        }
        if (rows.size() > MAX_ROWS) {
            throw new BusinessRuleException("At most " + MAX_ROWS + " rows per file");
        }
        List<T> out = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            try {
                out.add(build.apply(rows.get(i)));
            } catch (BusinessRuleException | NotFoundException | IllegalArgumentException e) {
                // the header is line 1 in Excel, so the first data row is line 2
                errors.add("Line " + (i + 2) + ": " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            String shown = String.join("\n", errors.subList(0, Math.min(errors.size(), MAX_ERRORS_SHOWN)));
            String more = errors.size() > MAX_ERRORS_SHOWN ? "\n...and " + (errors.size() - MAX_ERRORS_SHOWN) + " more" : "";
            throw new BusinessRuleException("Nothing was imported. Fix these lines and upload again:\n" + shown + more);
        }
        return out;
    }

    /** The same checks as the normal add form (@NotBlank, @Email...), as a BusinessRuleException. */
    public void check(Object request) {
        var problems = validator.validate(request);
        if (!problems.isEmpty()) {
            throw new BusinessRuleException(problems.stream()
                    .map((ConstraintViolation<Object> v) -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .reduce((a, b) -> a + ", " + b).orElse(""));
        }
    }

    /** "12" -> 12; anything else (including empty) -> a clear error */
    public static int number(Map<String, String> row, String column) {
        String v = row.getOrDefault(column, "").trim();
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " must be a number, got '" + v + "'");
        }
    }

    public static String text(Map<String, String> row, String column) {
        String v = row.getOrDefault(column, "").trim();
        return v.isEmpty() ? null : v;
    }

    /** Parses an enum like ACTIVE / on leave / On_Leave, or returns the default when empty. */
    public static <E extends Enum<E>> E choice(Map<String, String> row, String column, Class<E> type, E fallback) {
        String v = text(row, column);
        if (v == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, v.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(column + " must be one of "
                    + java.util.Arrays.toString(type.getEnumConstants()) + ", got '" + v + "'");
        }
    }

    private static Map<String, String> normalise(Map<String, String> row) {
        Map<String, String> out = new LinkedHashMap<>();
        row.forEach((k, v) -> out.put(k.trim().toLowerCase(Locale.ROOT).replace(' ', '_'), v));
        return out;
    }
}
