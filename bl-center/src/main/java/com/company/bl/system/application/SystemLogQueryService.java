package com.company.bl.system.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class SystemLogQueryService {

    private static final int CONTENT_MAX_LENGTH = 2000;
    private static final int FAILURE_MAX_LENGTH = 500;
    private static final Pattern SENSITIVE_PAIR = Pattern.compile(
        "(?i)(password|token|authorization|accessToken|refreshToken|secret|salt)([\"'=:\\s]+)([^,&\\s\"'}]+)");

    private final SupportJdbcRepository supportJdbcRepository;
    private final SystemUserJdbcRepository systemUserJdbcRepository;

    SystemLogQueryService(SupportJdbcRepository supportJdbcRepository,
                          SystemUserJdbcRepository systemUserJdbcRepository) {
        this.supportJdbcRepository = supportJdbcRepository;
        this.systemUserJdbcRepository = systemUserJdbcRepository;
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PagedResult<SystemManagementService.UserLoginLogView> listLoginLogs(
        SystemManagementService.LoginLogQuery query) {
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        SystemJdbcRepository.PagedUserLoginLogs pagedLogs = systemUserJdbcRepository.findLoginLogs(
            new SystemJdbcRepository.LoginLogSearchCriteria(
                page,
                size,
                query.startAt(),
                query.endAt(),
                trimToNull(query.result()),
                trimToNull(query.ip()),
                trimToNull(query.keyword()),
                trimToNull(query.loginName()),
                trimToNull(query.userId()),
                trimToNull(query.clientDevice())));
        return new SystemManagementService.PagedResult<>(
            pagedLogs.logs().stream().map(this::toLoginLogView).toList(),
            page,
            size,
            pagedLogs.total());
    }

    @Transactional(readOnly = true)
    public SystemManagementService.UserLoginLogView getLoginLog(String id) {
        SystemJdbcRepository.UserLoginLogRow row = systemUserJdbcRepository.findLoginLogById(id);
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Login log not found");
        }
        return toLoginLogView(row);
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PagedResult<SystemManagementService.OperationLogView> listOperationLogs(
        SystemManagementService.OperationLogQuery query) {
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        SupportJdbcRepository.PagedOperationLogs pagedLogs = supportJdbcRepository.findOperationLogs(
            new SupportJdbcRepository.OperationLogSearchCriteria(
                page,
                size,
                query.startAt(),
                query.endAt(),
                trimToNull(query.result()),
                trimToNull(query.ip()),
                trimToNull(query.keyword()),
                trimToNull(query.operatorKeyword()),
                trimToNull(query.moduleCode()),
                trimToNull(query.businessType()),
                trimToNull(query.businessId()),
                trimToNull(query.operationName()),
                trimToNull(query.contentKeyword())));
        return new SystemManagementService.PagedResult<>(
            pagedLogs.logs().stream().map(row -> toOperationLogView(row, false)).toList(),
            page,
            size,
            pagedLogs.total());
    }

    @Transactional(readOnly = true)
    public SystemManagementService.OperationLogView getOperationLog(String id) {
        SupportJdbcRepository.OperationLogViewRow row = supportJdbcRepository.findOperationLogById(id);
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Operation log not found");
        }
        return toOperationLogView(row, true);
    }

    private SystemManagementService.UserLoginLogView toLoginLogView(SystemJdbcRepository.UserLoginLogRow row) {
        return new SystemManagementService.UserLoginLogView(
            row.id(),
            row.userId(),
            row.loginName(),
            row.loginResult(),
            row.clientIp(),
            row.clientDevice(),
            stringify(row.loginAt()),
            stringify(row.logoutAt()),
            sanitize(row.failureReason(), FAILURE_MAX_LENGTH),
            sanitize(row.remarks(), FAILURE_MAX_LENGTH));
    }

    private SystemManagementService.OperationLogView toOperationLogView(
        SupportJdbcRepository.OperationLogViewRow row,
        boolean includeContent) {
        return new SystemManagementService.OperationLogView(
            row.id(),
            row.moduleCode(),
            row.businessType(),
            row.businessId(),
            row.operationName(),
            row.operationResult(),
            row.operatorUserId(),
            row.operatorName(),
            row.operatorIp(),
            stringify(row.operationAt()),
            includeContent ? sanitize(row.operationContent(), CONTENT_MAX_LENGTH) : null,
            sanitize(row.failureReason(), FAILURE_MAX_LENGTH));
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = SENSITIVE_PAIR.matcher(value).replaceAll("$1$2***");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
