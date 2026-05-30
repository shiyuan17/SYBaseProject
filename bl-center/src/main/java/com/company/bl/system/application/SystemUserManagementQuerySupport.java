package com.company.bl.system.application;

import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemRoleJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
class SystemUserManagementQuerySupport extends AbstractSystemUserManagementSupport {

    SystemUserManagementQuerySupport(SystemUserJdbcRepository systemUserJdbcRepository) {
        super(systemUserJdbcRepository);
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

    @Transactional(readOnly = true)
    public SystemManagementService.PagedResult<SystemManagementService.UserLoginLogView> listUserLoginLogs(String userId,
                                                                                                            int page,
                                                                                                            int size) {
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

    @Transactional(readOnly = true)
    public SystemManagementService.PrintLoginTagView printLoginTag(String userId) {
        SystemJdbcRepository.UserRow user = ensureUserExists(userId);
        String loginTagCode = blankToNull(user.loginTagCode());
        return new SystemManagementService.PrintLoginTagView(
            loginTagCode,
            user.name() + " 鐧诲綍鏍囩",
            "濮撳悕: " + user.name() + "\n鐧诲綍鍚? " + user.loginName() + "\n鏍囩缂栫爜: " + (loginTagCode == null ? "-" : loginTagCode));
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
