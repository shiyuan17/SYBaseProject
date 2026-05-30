package com.company.bl.system.application;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class SystemUserManagementImportSupport extends AbstractSystemUserManagementSupport {

    private final SystemUserManagementMutationSupport mutationSupport;

    SystemUserManagementImportSupport(SystemUserJdbcRepository systemUserJdbcRepository,
                                     SystemUserManagementMutationSupport mutationSupport) {
        super(systemUserJdbcRepository);
        this.mutationSupport = mutationSupport;
    }

    @Transactional
    public SystemManagementService.ImportResult importUsers(byte[] content) {
        List<Map<String, String>> rows = parseCsv(content);
        int successCount = 0;
        int failureCount = 0;
        List<SystemManagementService.ImportError> errors = new java.util.ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, String> row = rows.get(index);
            int rowNumber = index + 2;
            String loginName = trimToNull(row.get("loginName"));
            String name = trimToNull(row.get("name"));
            if (loginName == null) {
                failureCount++;
                errors.add(new SystemManagementService.ImportError(
                    rowNumber,
                    "loginName",
                    row.get("loginName"),
                    "Login name must not be blank"));
                continue;
            }
            if (name == null) {
                failureCount++;
                errors.add(new SystemManagementService.ImportError(
                    rowNumber,
                    "name",
                    row.get("name"),
                    "User name must not be blank"));
                continue;
            }
            try {
                String requestedUserCode = trimToNull(row.get("userCode"));
                String requestedLoginTagCode = trimToNull(row.get("loginTagCode"));
                SystemJdbcRepository.UserRow existing = systemUserJdbcRepository.findUserByLoginName(loginName);
                if (existing != null) {
                    if (requestedUserCode != null && !requestedUserCode.equals(existing.userCode())) {
                        failureCount++;
                        errors.add(new SystemManagementService.ImportError(
                            rowNumber,
                            "userCode",
                            row.get("userCode"),
                            "User code cannot be changed once created"));
                        continue;
                    }
                    if (requestedLoginTagCode != null && !requestedLoginTagCode.equals(existing.loginTagCode())) {
                        failureCount++;
                        errors.add(new SystemManagementService.ImportError(
                            rowNumber,
                            "loginTagCode",
                            row.get("loginTagCode"),
                            "Login tag code cannot be changed once created"));
                        continue;
                    }
                }
                if (existing == null) {
                    mutationSupport.createUser(new SystemManagementService.CreateUserCommand(
                        requestedUserCode,
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
                        requestedLoginTagCode,
                        parseBoolean(row.get("enabled"), true)));
                } else {
                    mutationSupport.updateUser(existing.id(), new SystemManagementService.UpdateUserCommand(
                        requestedUserCode,
                        name,
                        trimToNull(row.get("jobNo")),
                        trimToNull(row.get("titleName")),
                        trimToNull(row.get("departmentId")),
                        trimToNull(row.get("departmentName")),
                        trimToNull(row.get("phone")),
                        trimToNull(row.get("email")),
                        trimToNull(row.get("avatar")),
                        requestedLoginTagCode,
                        parseBoolean(row.get("enabled"), existing.enabled())));
                }
                successCount++;
            } catch (BlBusinessException exception) {
                failureCount++;
                errors.add(new SystemManagementService.ImportError(
                    rowNumber,
                    "loginName",
                    row.get("loginName"),
                    exception.getMessage()));
            } catch (RuntimeException exception) {
                failureCount++;
                errors.add(new SystemManagementService.ImportError(
                    rowNumber,
                    "loginName",
                    row.get("loginName"),
                    exception.getMessage() == null ? "User import failed" : exception.getMessage()));
            }
        }
        return new SystemManagementService.ImportResult(successCount, failureCount, errors);
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
}
