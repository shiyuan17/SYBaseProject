package com.company.bl.system.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(name = "SystemManagementPagedResult", description = "系统管理分页结果")
    public record PagedResult<T>(
        @Schema(description = "当前页数据") List<T> items,
        @Schema(description = "页码，从 1 开始") int page,
        @Schema(description = "每页条数") int size,
        @Schema(description = "总记录数") long total) {
    }

    @Schema(name = "UserView", description = "系统用户")
    public record UserView(
        @Schema(description = "用户 ID") String id,
        @Schema(description = "用户编码") String userCode,
        @Schema(description = "登录名") String loginName,
        @Schema(description = "姓名") String name,
        @Schema(description = "工号") String jobNo,
        @Schema(description = "职称") String titleName,
        @Schema(description = "科室 ID") String departmentId,
        @Schema(description = "科室名称") String departmentName,
        @Schema(description = "手机号") String phone,
        @Schema(description = "邮箱") String email,
        @Schema(description = "头像地址") String avatar,
        @Schema(description = "登录标签编码") String loginTagCode,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "已分配角色") List<AssignedRoleView> roles,
        @Schema(description = "最近登录时间") String lastLoginAt,
        @Schema(description = "最近登录 IP") String lastLoginIp,
        @Schema(description = "最近登录设备") String lastLoginDevice,
        @Schema(description = "创建时间") String createdAt,
        @Schema(description = "更新时间") String updatedAt
    ) {
    }

    @Schema(name = "UserLoginLogView", description = "用户登录日志")
    public record UserLoginLogView(
        @Schema(description = "日志 ID") String id,
        @Schema(description = "用户 ID") String userId,
        @Schema(description = "登录名") String loginName,
        @Schema(description = "登录结果") String loginResult,
        @Schema(description = "客户端 IP") String clientIp,
        @Schema(description = "客户端设备") String clientDevice,
        @Schema(description = "登录时间") String loginAt,
        @Schema(description = "登出时间") String logoutAt,
        @Schema(description = "失败原因") String failureReason,
        @Schema(description = "备注") String remarks
    ) {
    }

    @Schema(name = "AssignedRoleView", description = "已分配角色")
    public record AssignedRoleView(
        @Schema(description = "角色 ID") String roleId,
        @Schema(description = "角色编码") String roleCode,
        @Schema(description = "角色名称") String roleName,
        @Schema(description = "是否主角色") boolean primary) {
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

    @Schema(name = "RoleView", description = "系统角色")
    public record RoleView(
        @Schema(description = "角色 ID") String id,
        @Schema(description = "角色编码") String roleCode,
        @Schema(description = "角色名称") String roleName,
        @Schema(description = "角色类型") String roleType,
        @Schema(description = "数据范围") String dataScope,
        @Schema(description = "备注") String remarks,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "创建时间") String createdAt,
        @Schema(description = "更新时间") String updatedAt
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

    @Schema(name = "RoleAuthorizationView", description = "角色授权视图")
    public record RoleAuthorizationView(
        @Schema(description = "角色 ID") String roleId,
        @Schema(description = "菜单 ID 列表") List<String> menuIds,
        @Schema(description = "权限 ID 列表") List<String> permissionIds,
        @Schema(description = "消息主题 ID 列表") List<String> topicIds,
        @Schema(description = "统计范围映射，key 为统计分类 ID，value 为范围值") Map<String, String> statScopes
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

    @Schema(name = "MenuView", description = "菜单定义")
    public record MenuView(
        @Schema(description = "菜单 ID") String id,
        @Schema(description = "父级菜单 ID") String parentId,
        @Schema(description = "菜单编码") String menuCode,
        @Schema(description = "菜单名称") String menuName,
        @Schema(description = "菜单类型") String menuType,
        @Schema(description = "路由路径") String path,
        @Schema(description = "前端组件名") String componentName,
        @Schema(description = "图标") String icon,
        @Schema(description = "权限前缀") String permissionPrefix,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否可见") boolean visible,
        @Schema(description = "是否启用") boolean enabled
    ) {
    }

    @Schema(name = "PermissionView", description = "权限定义")
    public record PermissionView(
        @Schema(description = "权限 ID") String id,
        @Schema(description = "权限码") String permissionCode,
        @Schema(description = "权限名称") String permissionName,
        @Schema(description = "关联菜单 ID") String menuId,
        @Schema(description = "动作标识") String actionKey,
        @Schema(description = "HTTP 方法") String httpMethod,
        @Schema(description = "资源路径") String resourcePath,
        @Schema(description = "权限分组") String permissionGroup,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled
    ) {
    }

    @Schema(name = "MessageTopicView", description = "消息主题定义")
    public record MessageTopicView(
        @Schema(description = "主题 ID") String id,
        @Schema(description = "主题编码") String topicCode,
        @Schema(description = "主题名称") String topicName,
        @Schema(description = "主题分类") String topicCategory,
        @Schema(description = "说明") String description,
        @Schema(description = "是否启用") boolean enabled
    ) {
    }

    @Schema(name = "StatCategoryView", description = "统计分类定义")
    public record StatCategoryView(
        @Schema(description = "统计分类 ID") String id,
        @Schema(description = "统计分类编码") String statCode,
        @Schema(description = "统计分类名称") String statName,
        @Schema(description = "统计范围类型") String statScope,
        @Schema(description = "说明") String description,
        @Schema(description = "是否启用") boolean enabled
    ) {
    }

    private record EncodedPassword(String password, String passwordAlgo, String passwordSalt) {
    }
}
