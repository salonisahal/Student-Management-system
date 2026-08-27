package com.example.app.util;

import com.example.app.exception.BadRequestException;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts a list endpoint's sort parameter into a safe Spring Data {@link Sort}.
 * Supports both the common {@code field,asc}/{@code field,desc} form and the
 * human-readable {@code field,ascending}/{@code field,descending} form.
 */
public final class SortUtil {

    private SortUtil() {
    }

    public static Sort parse(String sortParameter, Class<?> entityType) {
        String[] parts = sortParameter == null ? new String[0] : sortParameter.split(",", -1);
        if (parts.length == 0 || parts[0].isBlank() || parts.length > 2) {
            throw new BadRequestException("Sort must be in the format 'field,asc' or 'field,desc'");
        }

        String requestedField = parts[0].trim();
        Sort.Direction direction = parts.length == 2 ? parseDirection(parts[1].trim()) : Sort.Direction.ASC;
        List<String> properties = resolveProperties(requestedField, entityType);

        return Sort.by(properties.stream()
                .map(property -> new Sort.Order(direction, property))
                .toList());
    }

    private static Sort.Direction parseDirection(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "asc", "ascending" -> Sort.Direction.ASC;
            case "desc", "descending" -> Sort.Direction.DESC;
            default -> throw new BadRequestException(
                    "Invalid sort direction '" + value + "'. Use asc, desc, ascending, or descending");
        };
    }

    private static List<String> resolveProperties(String requestedField, Class<?> entityType) {
        Set<String> sortableFields = Arrays.stream(entityType.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !Collection.class.isAssignableFrom(field.getType()))
                .map(Field::getName)
                .collect(Collectors.toSet());

        // The list UIs expose a single "name" column while person entities store names separately.
        if ("name".equalsIgnoreCase(requestedField)) {
            if (sortableFields.contains("courseName")) {
                return List.of("courseName");
            }
            if (sortableFields.contains("lastName") && sortableFields.contains("firstName")) {
                return List.of("lastName", "firstName");
            }
        }

        String property = sortableFields.stream()
                .filter(field -> field.equalsIgnoreCase(requestedField))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Invalid sort field '" + requestedField + "' for " + entityType.getSimpleName()));
        return List.of(property);
    }
}
