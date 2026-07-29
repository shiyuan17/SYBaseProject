package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

class SpecimenWorkflowInputNormalizer {

    String trim(String value) {
        return value == null ? null : value.trim();
    }

    boolean blank(String value) {
        return value == null || value.isBlank();
    }

    String defaultIfBlank(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    String normalizeQualityCheckResult(String value) {
        String normalized = normalizeStatus(value);
        if (normalized == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Quality check result is required");
        }
        if (!"PASSED".equals(normalized) && !"FAILED".equals(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported quality check result");
        }
        return normalized;
    }

    List<String> normalizeQualityIssueCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String code = trim(value);
            if (code != null) {
                normalized.add(code.toUpperCase());
            }
        }
        return List.copyOf(normalized);
    }

    String joinQualityIssueCodes(List<String> values) {
        List<String> normalized = normalizeQualityIssueCodes(values);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    int normalizePage(int page) {
        return Math.max(page, 1);
    }

    int normalizeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }

    int normalizeExportSize(int size) {
        return size <= 0 ? 10000 : Math.min(size, 10000);
    }

    LocalDateTime parseDateFrom(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    LocalDateTime parseDateTo(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay();
    }

    LocalDate parseLocalDateFrom(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    LocalDate parseLocalDate(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    LocalDate parseLocalDateTo(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).plusDays(1);
    }

    String normalizeStatus(String value) {
        return blank(value) ? null : value.trim().toUpperCase();
    }
}
