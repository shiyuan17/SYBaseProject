package com.company.bl.system.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemRoleJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;

import java.time.LocalDateTime;
import java.util.List;

abstract class AbstractSystemUserManagementSupport {

    protected final SystemUserJdbcRepository systemUserJdbcRepository;

    protected AbstractSystemUserManagementSupport(SystemUserJdbcRepository systemUserJdbcRepository) {
        this.systemUserJdbcRepository = systemUserJdbcRepository;
    }

    protected SystemJdbcRepository.UserRow ensureUserExists(String userId) {
        SystemJdbcRepository.UserRow user = systemUserJdbcRepository.findUserById(userId);
        if (user == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "User not found");
        }
        return user;
    }

    protected SystemManagementService.UserView toUserView(SystemJdbcRepository.UserRow user,
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

    protected SystemManagementService.UserLoginLogView toUserLoginLogView(SystemJdbcRepository.UserLoginLogRow row) {
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

    protected String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    protected String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
