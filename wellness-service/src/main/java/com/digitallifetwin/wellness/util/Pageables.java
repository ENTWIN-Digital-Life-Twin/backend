package com.digitallifetwin.wellness.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pageables {

    private Pageables() {
    }

    public static Pageable of(int page, int size, String sort, String defaultProperty) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        String[] parts = sort == null ? new String[0] : sort.split(",");
        String property = parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : defaultProperty;
        property = property.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (property.isBlank()) {
            property = defaultProperty;
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim().replace("\"", "").replace("]", ""))) {
            direction = Sort.Direction.ASC;
        }
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
