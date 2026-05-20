package com.company.bl.system.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SystemManagementService {

    private final SystemJdbcRepository systemJdbcRepository;
    private final OperationAuditService operationAuditService;
    private final Sm3PasswordEncoder sm3PasswordEncoder;

    public SystemManagementService(SystemJdbcRepository systemJdbcRepository,
                                   OperationAuditService operationAuditService,
                                   Sm3PasswordEncoder sm3PasswordEncoder) {
        this.systemJdbcRepository = systemJdbcRepository;
        this.operationAuditService = operationAuditService;
        this.sm3PasswordEncoder = sm3PasswordEncoder;
    }

    @Transactional(readOnly = true)
    public PagedResult<UserView> listUsers(int page, int size) {
        SystemJdbcRepository.PagedUsers pagedUsers = systemJdbcRepository.findUsers(page, size);
        return new PagedResult<>(
            pagedUsers.users().stream()
                .map(user -> toUserView(user, pagedUsers.assignments().getOrDefault(user.id(), List.of())))
                .toList(),
            page,
            size,
            pagedUsers.total());
    }

    @Transactional
    public UserView createUser(CreateUserCommand command) {
        return operationAuditService.audit("SYSTEM", "USER", "create_user", () -> {
            try {
                EncodedPassword encodedPassword = encodePassword(command.password());
                SystemJdbcRepository.UserRow user = systemJdbcRepository.insertUser(new SystemJdbcRepository.CreateUserRow(
                    "USER-" + UUID.randomUUID(),
                    command.userCode(),
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
                    command.loginTagCode(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return toUserView(user, List.of());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "User login name or code already exists");
            }
        }, UserView::id, () -> command.loginName());
    }

    @Transactional
    public UserView updateUserEnabled(String userId, boolean enabled) {
        return operationAuditService.audit("SYSTEM", "USER", "update_user_enabled", () -> {
            ensureUserExists(userId);
            systemJdbcRepository.updateUserEnabled(userId, enabled);
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            List<SystemJdbcRepository.RoleAssignmentRow> assignments =
                systemJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(user, assignments);
        }, UserView::id, () -> userId + ":" + enabled);
    }

    @Transactional
    public UserView assignUserRoles(String userId, AssignUserRolesCommand command) {
        return operationAuditService.audit("SYSTEM", "USER_ROLE", "assign_user_roles", () -> {
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            long primaryCount = command.assignments().stream().filter(RoleAssignmentInput::primary).count();
            if (primaryCount > 1) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Only one primary role is allowed");
            }
            systemJdbcRepository.replaceUserRoles(userId, command.assignments().stream()
                .map(input -> new SystemJdbcRepository.UserRoleAssignmentCommand(input.roleId(), input.primary()))
                .toList());
            List<SystemJdbcRepository.RoleAssignmentRow> assignments =
                systemJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(systemJdbcRepository.findUserById(userId), assignments);
        }, UserView::id, () -> userId);
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles() {
        return systemJdbcRepository.findRoles().stream().map(this::toRoleView).toList();
    }

    @Transactional
    public RoleView createRole(CreateRoleCommand command) {
        return operationAuditService.audit("SYSTEM", "ROLE", "create_role", () -> {
            try {
                return toRoleView(systemJdbcRepository.insertRole(new SystemJdbcRepository.CreateRoleRow(
                    "ROLE-" + UUID.randomUUID(),
                    command.roleCode(),
                    command.roleName(),
                    command.roleType(),
                    command.dataScope(),
                    command.remarks(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Role code already exists");
            }
        }, RoleView::id, command::roleCode);
    }

    @Transactional(readOnly = true)
    public RoleAuthorizationView getRoleAuthorization(String roleId) {
        ensureRoleExists(roleId);
        SystemJdbcRepository.RoleAuthorizationRow authorization = systemJdbcRepository.findRoleAuthorization(roleId);
        return new RoleAuthorizationView(roleId, authorization.menuIds(), authorization.permissionIds(),
            authorization.topicIds(), authorization.statScopes());
    }

    @Transactional
    public RoleAuthorizationView updateRoleAuthorization(String roleId, UpdateRoleAuthorizationCommand command) {
        return operationAuditService.audit("SYSTEM", "ROLE_AUTH", "update_role_authorization", () -> {
            ensureRoleExists(roleId);
            systemJdbcRepository.replaceRoleAuthorizations(roleId, new SystemJdbcRepository.AuthorizationCommand(
                safeList(command.menuIds()),
                safeList(command.permissionIds()),
                safeList(command.topicIds()),
                command.statScopes() == null ? Map.of() : command.statScopes()));
            return getRoleAuthorization(roleId);
        }, RoleAuthorizationView::roleId, () -> roleId);
    }

    @Transactional
    public void recordUserLogin(RecordUserLoginCommand command) {
        LocalDateTime loginAt = command.loginAt() == null ? LocalDateTime.now() : command.loginAt();
        String loginResult = normalizeLoginResult(command.loginResult());
        String userId = blankToNull(command.userId());
        if ("SUCCESS".equals(loginResult)) {
            if (userId == null) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Successful login requires user id");
            }
            ensureUserExists(userId);
            systemJdbcRepository.updateUserLastLogin(userId, loginAt, command.clientIp(), command.clientDevice());
        } else if (userId != null) {
            ensureUserExists(userId);
        }

        systemJdbcRepository.insertUserLoginLog(new SystemJdbcRepository.CreateUserLoginLogRow(
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
    public PagedResult<UserLoginLogView> listUserLoginLogs(String userId, int page, int size) {
        ensureUserExists(userId);
        SystemJdbcRepository.PagedUserLoginLogs pagedLogs = systemJdbcRepository.findUserLoginLogs(userId, page, size);
        return new PagedResult<>(
            pagedLogs.logs().stream().map(this::toUserLoginLogView).toList(),
            page,
            size,
            pagedLogs.total());
    }

    @Transactional(readOnly = true)
    public List<MenuView> listMenus() {
        return systemJdbcRepository.findMenus().stream().map(menu -> new MenuView(
            menu.id(), menu.parentId(), menu.menuCode(), menu.menuName(), menu.menuType(), menu.path(),
            menu.componentName(), menu.icon(), menu.permissionPrefix(), menu.sortOrder(), menu.visible(), menu.enabled()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionView> listPermissions() {
        return systemJdbcRepository.findPermissions().stream().map(permission -> new PermissionView(
            permission.id(), permission.permissionCode(), permission.permissionName(), permission.menuId(),
            permission.actionKey(), permission.httpMethod(), permission.resourcePath(), permission.permissionGroup(),
            permission.sortOrder(), permission.enabled())).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageTopicView> listMessageTopics() {
        return systemJdbcRepository.findMessageTopics().stream().map(topic -> new MessageTopicView(
            topic.id(), topic.topicCode(), topic.topicName(), topic.topicCategory(), topic.description(), topic.enabled()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StatCategoryView> listStatCategories() {
        return systemJdbcRepository.findStatCategories().stream().map(stat -> new StatCategoryView(
            stat.id(), stat.statCode(), stat.statName(), stat.statScope(), stat.description(), stat.enabled()))
            .toList();
    }

    private SystemJdbcRepository.UserRow ensureUserExists(String userId) {
        SystemJdbcRepository.UserRow user = systemJdbcRepository.findUserById(userId);
        if (user == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "User not found");
        }
        return user;
    }

    private void ensureRoleExists(String roleId) {
        if (systemJdbcRepository.findRoleById(roleId) == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Role not found");
        }
    }

    private UserView toUserView(SystemJdbcRepository.UserRow user,
                                List<SystemJdbcRepository.RoleAssignmentRow> assignments) {
        return new UserView(
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
            assignments.stream().map(assignment -> new AssignedRoleView(
                assignment.roleId(), assignment.roleCode(), assignment.roleName(), assignment.primary())).toList(),
            stringify(user.lastLoginAt()),
            user.lastLoginIp(),
            user.lastLoginDevice(),
            stringify(user.createdAt()),
            stringify(user.updatedAt()));
    }

    private RoleView toRoleView(SystemJdbcRepository.RoleRow role) {
        return new RoleView(
            role.id(),
            role.roleCode(),
            role.roleName(),
            role.roleType(),
            role.dataScope(),
            role.remarks(),
            role.enabled(),
            stringify(role.createdAt()),
            stringify(role.updatedAt()));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
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

    private UserLoginLogView toUserLoginLogView(SystemJdbcRepository.UserLoginLogRow row) {
        return new UserLoginLogView(
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

    public record PagedResult<T>(List<T> items, int page, int size, long total) {
    }

    public record UserView(
        String id,
        String userCode,
        String loginName,
        String name,
        String jobNo,
        String titleName,
        String departmentId,
        String departmentName,
        String phone,
        String email,
        String avatar,
        String loginTagCode,
        boolean enabled,
        List<AssignedRoleView> roles,
        String lastLoginAt,
        String lastLoginIp,
        String lastLoginDevice,
        String createdAt,
        String updatedAt
    ) {
    }

    public record UserLoginLogView(
        String id,
        String userId,
        String loginName,
        String loginResult,
        String clientIp,
        String clientDevice,
        String loginAt,
        String logoutAt,
        String failureReason,
        String remarks
    ) {
    }

    public record AssignedRoleView(String roleId, String roleCode, String roleName, boolean primary) {
    }

    public record CreateUserCommand(
        String userCode,
        String loginName,
        String name,
        String password,
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

    public record AssignUserRolesCommand(List<RoleAssignmentInput> assignments) {
    }

    public record RoleAssignmentInput(String roleId, boolean primary) {
    }

    public record RoleView(
        String id,
        String roleCode,
        String roleName,
        String roleType,
        String dataScope,
        String remarks,
        boolean enabled,
        String createdAt,
        String updatedAt
    ) {
    }

    public record CreateRoleCommand(
        String roleCode,
        String roleName,
        String roleType,
        String dataScope,
        String remarks,
        boolean enabled
    ) {
    }

    public record RoleAuthorizationView(
        String roleId,
        List<String> menuIds,
        List<String> permissionIds,
        List<String> topicIds,
        Map<String, String> statScopes
    ) {
    }

    public record UpdateRoleAuthorizationCommand(
        List<String> menuIds,
        List<String> permissionIds,
        List<String> topicIds,
        Map<String, String> statScopes
    ) {
    }

    public record RecordUserLoginCommand(
        String userId,
        String loginName,
        String loginResult,
        String clientIp,
        String clientDevice,
        String failureReason,
        String remarks,
        LocalDateTime loginAt
    ) {
    }

    public record MenuView(
        String id,
        String parentId,
        String menuCode,
        String menuName,
        String menuType,
        String path,
        String componentName,
        String icon,
        String permissionPrefix,
        int sortOrder,
        boolean visible,
        boolean enabled
    ) {
    }

    public record PermissionView(
        String id,
        String permissionCode,
        String permissionName,
        String menuId,
        String actionKey,
        String httpMethod,
        String resourcePath,
        String permissionGroup,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record MessageTopicView(
        String id,
        String topicCode,
        String topicName,
        String topicCategory,
        String description,
        boolean enabled
    ) {
    }

    public record StatCategoryView(
        String id,
        String statCode,
        String statName,
        String statScope,
        String description,
        boolean enabled
    ) {
    }

    private record EncodedPassword(String password, String passwordAlgo, String passwordSalt) {
    }
}
