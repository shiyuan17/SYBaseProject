package com.company.bl.system.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemRoleJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class SystemUserManagementService {

    private final SystemUserJdbcRepository systemUserJdbcRepository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;
    private final Sm3PasswordEncoder sm3PasswordEncoder;

    public SystemUserManagementService(SystemUserJdbcRepository systemUserJdbcRepository,
                                       NumberingService numberingService,
                                       OperationAuditService operationAuditService,
                                       Sm3PasswordEncoder sm3PasswordEncoder) {
        this.systemUserJdbcRepository = systemUserJdbcRepository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
        this.sm3PasswordEncoder = sm3PasswordEncoder;
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PagedResult<SystemManagementService.UserView> listUsers(int page,
                                                                                           int size,
                                                                                           Boolean enabled,
                                                                                           String keyword) {
        SystemJdbcRepository.PagedUsers pagedUsers = systemUserJdbcRepository.findUsers(page, size, enabled, keyword);
        return new SystemManagementService.PagedResult<>(
            pagedUsers.users().stream()
                .map(user -> toUserView(user, pagedUsers.assignments().getOrDefault(user.id(), List.of())))
                .toList(),
            page,
            size,
            pagedUsers.total());
    }

    @Transactional
    public SystemManagementService.UserView createUser(SystemManagementService.CreateUserCommand command) {
        String userCode = resolveCreateCode(command.userCode(), numberingService::generateUserCode);
        String loginTagCode = resolveCreateCode(command.loginTagCode(), numberingService::generateLoginTagCode);
        return operationAuditService.audit("SYSTEM", "USER", "create_user", () -> {
            try {
                EncodedPassword encodedPassword = encodePassword(command.password());
                SystemJdbcRepository.UserRow user = systemUserJdbcRepository.insertUser(new SystemJdbcRepository.CreateUserRow(
                    "USER-" + UUID.randomUUID(),
                    userCode,
                    command.loginName(),
                    command.name(),
                    encodedPassword.password(),
                    encodedPassword.passwordAlgo(),
                    encodedPassword.passwordSalt(),
                    null,
                    command.jobNo(),
                    command.titleName(),
                    command.departmentId(),
                    command.departmentName(),
                    command.phone(),
                    command.email(),
                    command.avatar(),
                    loginTagCode,
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return toUserView(user, List.of());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "User login name or code already exists");
            }
        }, SystemManagementService.UserView::id, () -> command.loginName());
    }

    @Transactional
    public SystemManagementService.UserView updateUser(String userId, SystemManagementService.UpdateUserCommand command) {
        return operationAuditService.audit("SYSTEM", "USER", "update_user", () -> {
            SystemJdbcRepository.UserRow current = ensureUserExists(userId);
            String userCode = resolveExistingCode(command.userCode(), current.userCode(), "User code");
            String loginTagCode = resolveExistingCode(command.loginTagCode(), current.loginTagCode(), "Login tag code");
            try {
                systemUserJdbcRepository.updateUser(userId, new SystemJdbcRepository.UpdateUserRow(
                    userCode,
                    command.name(),
                    command.jobNo(),
                    command.titleName(),
                    command.departmentId(),
                    command.departmentName(),
                    command.phone(),
                    command.email(),
                    command.avatar(),
                    loginTagCode,
                    command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "User code, job number or login tag code already exists");
            }
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            List<SystemRoleJdbcRepository.RoleAssignmentRow> assignments =
                systemUserJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(user, assignments);
        }, SystemManagementService.UserView::id, () -> userId);
    }

    @Transactional
    public SystemManagementService.UserView updateUserEnabled(String userId, boolean enabled) {
        return operationAuditService.audit("SYSTEM", "USER", "update_user_enabled", () -> {
            ensureUserExists(userId);
            systemUserJdbcRepository.updateUserEnabled(userId, enabled);
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            List<SystemRoleJdbcRepository.RoleAssignmentRow> assignments =
                systemUserJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(user, assignments);
        }, SystemManagementService.UserView::id, () -> userId + ":" + enabled);
    }

    @Transactional
    public SystemManagementService.UserView assignUserRoles(String userId, SystemManagementService.AssignUserRolesCommand command) {
        return operationAuditService.audit("SYSTEM", "USER_ROLE", "assign_user_roles", () -> {
            ensureUserExists(userId);
            long primaryCount = command.assignments().stream().filter(SystemManagementService.RoleAssignmentInput::primary).count();
            if (primaryCount > 1) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Only one primary role is allowed");
            }
            systemUserJdbcRepository.replaceUserRoles(userId, command.assignments().stream()
                .map(input -> new SystemJdbcRepository.UserRoleAssignmentCommand(input.roleId(), input.primary()))
                .toList());
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            List<SystemRoleJdbcRepository.RoleAssignmentRow> assignments =
                systemUserJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(user, assignments);
        }, SystemManagementService.UserView::id, () -> userId);
    }

    @Transactional
    public void recordUserLogin(SystemManagementService.RecordUserLoginCommand command) {
        LocalDateTime loginAt = command.loginAt() == null ? LocalDateTime.now() : command.loginAt();
        String loginResult = normalizeLoginResult(command.loginResult());
        String userId = blankToNull(command.userId());
        if ("SUCCESS".equals(loginResult)) {
            if (userId == null) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Successful login requires user id");
            }
            ensureUserExists(userId);
            systemUserJdbcRepository.updateUserLastLogin(userId, loginAt, command.clientIp(), command.clientDevice());
        } else if (userId != null) {
            ensureUserExists(userId);
        }

        systemUserJdbcRepository.insertUserLoginLog(new SystemJdbcRepository.CreateUserLoginLogRow(
            "ULL-" + UUID.randomUUID(),
            userId,
            command.loginName(),
            loginResult,
            command.clientIp(),
            command.clientDevice(),
            loginAt,
            null,
            command.failureReason(),
            command.remarks()));
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PagedResult<SystemManagementService.UserLoginLogView> listUserLoginLogs(String userId, int page, int size) {
        ensureUserExists(userId);
        SystemJdbcRepository.PagedUserLoginLogs pagedLogs = systemUserJdbcRepository.findUserLoginLogs(userId, page, size);
        return new SystemManagementService.PagedResult<>(
            pagedLogs.logs().stream().map(this::toUserLoginLogView).toList(),
            page,
            size,
            pagedLogs.total());
    }

    @Transactional(readOnly = true)
    public byte[] exportUsers(Boolean enabled, String keyword) {
        List<SystemJdbcRepository.UserRow> users = systemUserJdbcRepository.findUsers(enabled, keyword);
        Map<String, List<SystemRoleJdbcRepository.RoleAssignmentRow>> assignments =
            systemUserJdbcRepository.findUserRoleAssignments(users.stream().map(SystemJdbcRepository.UserRow::id).toList());
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("userCode,loginName,name,jobNo,titleName,departmentName,phone,email,loginTagCode,enabled,roles,lastLoginAt\r\n");
        for (SystemJdbcRepository.UserRow user : users) {
            String roles = assignments.getOrDefault(user.id(), List.of()).stream()
                .map(SystemRoleJdbcRepository.RoleAssignmentRow::roleName)
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
            appendCsvRow(builder, List.of(
                safe(user.userCode()),
                safe(user.loginName()),
                safe(user.name()),
                safe(user.jobNo()),
                safe(user.titleName()),
                safe(user.departmentName()),
                safe(user.phone()),
                safe(user.email()),
                safe(user.loginTagCode()),
                user.enabled() ? "true" : "false",
                roles,
                safe(stringify(user.lastLoginAt()))));
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public SystemManagementService.ImportResult importUsers(byte[] content) {
        List<Map<String, String>> rows = parseCsv(content);
        int successCount = 0;
        int failureCount = 0;
        for (Map<String, String> row : rows) {
            String loginName = trimToNull(row.get("loginName"));
            String name = trimToNull(row.get("name"));
            if (loginName == null || name == null) {
                failureCount++;
                continue;
            }
            try {
                SystemJdbcRepository.UserRow existing = systemUserJdbcRepository.findUserByLoginName(loginName);
                if (existing == null) {
                    createUser(new SystemManagementService.CreateUserCommand(
                        trimToNull(row.get("userCode")),
                        loginName,
                        name,
                        trimToNull(row.get("password")),
                        trimToNull(row.get("jobNo")),
                        trimToNull(row.get("titleName")),
                        trimToNull(row.get("departmentId")),
                        trimToNull(row.get("departmentName")),
                        trimToNull(row.get("phone")),
                        trimToNull(row.get("email")),
                        trimToNull(row.get("avatar")),
                        trimToNull(row.get("loginTagCode")),
                        parseBoolean(row.get("enabled"), true)));
                } else {
                    updateUser(existing.id(), new SystemManagementService.UpdateUserCommand(
                        trimToNull(row.get("userCode")),
                        name,
                        trimToNull(row.get("jobNo")),
                        trimToNull(row.get("titleName")),
                        trimToNull(row.get("departmentId")),
                        trimToNull(row.get("departmentName")),
                        trimToNull(row.get("phone")),
                        trimToNull(row.get("email")),
                        trimToNull(row.get("avatar")),
                        trimToNull(row.get("loginTagCode")),
                        parseBoolean(row.get("enabled"), existing.enabled())));
                }
                successCount++;
            } catch (RuntimeException exception) {
                failureCount++;
            }
        }
        return new SystemManagementService.ImportResult(successCount, failureCount);
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PrintLoginTagView printLoginTag(String userId) {
        SystemJdbcRepository.UserRow user = ensureUserExists(userId);
        String loginTagCode = blankToNull(user.loginTagCode());
        return new SystemManagementService.PrintLoginTagView(
            loginTagCode,
            user.name() + " 登录标签",
            "姓名: " + user.name() + "\n登录名: " + user.loginName() + "\n标签编码: " + (loginTagCode == null ? "-" : loginTagCode));
    }

    private SystemJdbcRepository.UserRow ensureUserExists(String userId) {
        SystemJdbcRepository.UserRow user = systemUserJdbcRepository.findUserById(userId);
        if (user == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "User not found");
        }
        return user;
    }

    private SystemManagementService.UserView toUserView(SystemJdbcRepository.UserRow user,
                                                        List<SystemRoleJdbcRepository.RoleAssignmentRow> assignments) {
        return new SystemManagementService.UserView(
            user.id(),
            user.userCode(),
            user.loginName(),
            user.name(),
            user.jobNo(),
            user.titleName(),
            user.departmentId(),
            user.departmentName(),
            user.phone(),
            user.email(),
            user.avatar(),
            user.loginTagCode(),
            user.enabled(),
            assignments.stream().map(assignment -> new SystemManagementService.AssignedRoleView(
                assignment.roleId(), assignment.roleCode(), assignment.roleName(), assignment.primary())).toList(),
            stringify(user.lastLoginAt()),
            user.lastLoginIp(),
            user.lastLoginDevice(),
            stringify(user.createdAt()),
            stringify(user.updatedAt()));
    }

    private void appendCsvRow(StringBuilder builder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(escapeCsv(values.get(index)));
        }
        builder.append("\r\n");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean quoted = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return quoted ? "\"" + escaped + "\"" : escaped;
    }

    private List<Map<String, String>> parseCsv(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        List<String> lines = text.lines().filter(line -> !line.isBlank()).toList();
        if (lines.isEmpty()) {
            return List.of();
        }
        List<String> headers = parseCsvLine(lines.get(0));
        List<Map<String, String>> rows = new java.util.ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            List<String> values = parseCsvLine(lines.get(index));
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                row.put(headers.get(column), column < values.size() ? values.get(column) : null);
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        return "1".equals(normalized) || "true".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeLoginResult(String loginResult) {
        if (loginResult == null || loginResult.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Login result must not be blank");
        }
        String normalized = loginResult.trim().toUpperCase();
        if (!"SUCCESS".equals(normalized) && !"FAILED".equals(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Login result must be SUCCESS or FAILED");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = blankToNull(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = blankToNull(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(
            BlErrorCode.INVALID_ARGUMENT,
            400,
            fieldLabel + " cannot be changed once created");
    }

    private EncodedPassword encodePassword(String rawPassword) {
        String normalizedPassword = blankToNull(rawPassword);
        if (normalizedPassword == null) {
            return new EncodedPassword(null, null, null);
        }
        String salt = sm3PasswordEncoder.generateSalt();
        return new EncodedPassword(
            sm3PasswordEncoder.encode(normalizedPassword, salt),
            Sm3PasswordEncoder.PASSWORD_ALGO_SM3,
            salt);
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private SystemManagementService.UserLoginLogView toUserLoginLogView(SystemJdbcRepository.UserLoginLogRow row) {
        return new SystemManagementService.UserLoginLogView(
            row.id(),
            row.userId(),
            row.loginName(),
            row.loginResult(),
            row.clientIp(),
            row.clientDevice(),
            stringify(row.loginAt()),
            stringify(row.logoutAt()),
            row.failureReason(),
            row.remarks());
    }

    private record EncodedPassword(String password, String passwordAlgo, String passwordSalt) {
    }
}
