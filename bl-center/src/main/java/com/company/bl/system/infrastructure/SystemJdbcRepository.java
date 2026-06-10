package com.company.bl.system.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class SystemJdbcRepository {

    private SystemJdbcRepository() {
    }

    public record PagedUsers(
        List<UserRow> users,
        Map<String, List<SystemRoleJdbcRepository.RoleAssignmentRow>> assignments,
        long total
    ) {
    }

    public record UserRow(
        String id,
        String userCode,
        String loginName,
        String name,
        String role,
        String jobNo,
        String titleName,
        String departmentId,
        String departmentName,
        String phone,
        String email,
        String avatar,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        String lastLoginDevice,
        String loginTagCode,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateUserRow(
        String id,
        String userCode,
        String loginName,
        String name,
        String password,
        String passwordAlgo,
        String passwordSalt,
        String role,
        String jobNo,
        String titleName,
        String departmentId,
        String departmentName,
        String phone,
        String email,
        String avatar,
        String loginTagCode,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record UpdateUserRow(
        String userCode,
        String name,
        String jobNo,
        String titleName,
        String departmentId,
        String departmentName,
        String phone,
        String email,
        String avatar,
        String loginTagCode,
        boolean enabled
    ) {
    }

    public record CreateUserLoginLogRow(
        String id,
        String userId,
        String loginName,
        String loginResult,
        String clientIp,
        String clientDevice,
        LocalDateTime loginAt,
        LocalDateTime logoutAt,
        String failureReason,
        String remarks
    ) {
    }

    public record UserRoleAssignmentCommand(String roleId, boolean primary) {
    }

    public record PagedUserLoginLogs(List<UserLoginLogRow> logs, long total) {
    }

    public record LoginLogSearchCriteria(
        int page,
        int size,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String result,
        String ip,
        String keyword,
        String loginName,
        String userId,
        String clientDevice
    ) {
    }

    public record UserLoginLogRow(
        String id,
        String userId,
        String loginName,
        String loginResult,
        String clientIp,
        String clientDevice,
        LocalDateTime loginAt,
        LocalDateTime logoutAt,
        String failureReason,
        String remarks
    ) {
    }
}
