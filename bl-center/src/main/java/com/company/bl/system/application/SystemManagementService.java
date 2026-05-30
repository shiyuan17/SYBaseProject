package com.company.bl.system.application;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SystemManagementService {

    private final SystemUserManagementService systemUserManagementService;
    private final SystemRoleManagementService systemRoleManagementService;

    public SystemManagementService(SystemUserManagementService systemUserManagementService,
                                   SystemRoleManagementService systemRoleManagementService) {
        this.systemUserManagementService = systemUserManagementService;
        this.systemRoleManagementService = systemRoleManagementService;
    }

    @Transactional(readOnly = true)
    public PagedResult<UserView> listUsers(int page, int size) {
        return systemUserManagementService.listUsers(page, size, null, null);
    }

    @Transactional(readOnly = true)
    public PagedResult<UserView> listUsers(int page, int size, Boolean enabled, String keyword) {
        return systemUserManagementService.listUsers(page, size, enabled, keyword);
    }

    @Transactional
    public UserView createUser(CreateUserCommand command) {
        return systemUserManagementService.createUser(command);
    }

    @Transactional
    public UserView updateUser(String userId, UpdateUserCommand command) {
        return systemUserManagementService.updateUser(userId, command);
    }

    @Transactional
    public UserView updateUserEnabled(String userId, boolean enabled) {
        return systemUserManagementService.updateUserEnabled(userId, enabled);
    }

    @Transactional
    public UserView assignUserRoles(String userId, AssignUserRolesCommand command) {
        return systemUserManagementService.assignUserRoles(userId, command);
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles() {
        return systemRoleManagementService.listRoles();
    }

    @Transactional
    public RoleView createRole(CreateRoleCommand command) {
        return systemRoleManagementService.createRole(command);
    }

    @Transactional
    public RoleView updateRole(String roleId, UpdateRoleCommand command) {
        return systemRoleManagementService.updateRole(roleId, command);
    }

    @Transactional
    public void deleteRole(String roleId) {
        systemRoleManagementService.deleteRole(roleId);
    }

    @Transactional(readOnly = true)
    public RoleAuthorizationView getRoleAuthorization(String roleId) {
        return systemRoleManagementService.getRoleAuthorization(roleId);
    }

    @Transactional
    public RoleAuthorizationView updateRoleAuthorization(String roleId, UpdateRoleAuthorizationCommand command) {
        return systemRoleManagementService.updateRoleAuthorization(roleId, command);
    }

    @Transactional
    public void recordUserLogin(RecordUserLoginCommand command) {
        systemUserManagementService.recordUserLogin(command);
    }

    @Transactional(readOnly = true)
    public PagedResult<UserLoginLogView> listUserLoginLogs(String userId, int page, int size) {
        return systemUserManagementService.listUserLoginLogs(userId, page, size);
    }

    @Transactional(readOnly = true)
    public List<MenuView> listMenus() {
        return systemRoleManagementService.listMenus();
    }

    @Transactional(readOnly = true)
    public List<PermissionView> listPermissions() {
        return systemRoleManagementService.listPermissions();
    }

    @Transactional(readOnly = true)
    public List<MessageTopicView> listMessageTopics() {
        return systemRoleManagementService.listMessageTopics();
    }

    @Transactional(readOnly = true)
    public List<StatCategoryView> listStatCategories() {
        return systemRoleManagementService.listStatCategories();
    }

    @Transactional(readOnly = true)
    public byte[] exportUsers(Boolean enabled, String keyword) {
        return systemUserManagementService.exportUsers(enabled, keyword);
    }

    @Transactional
    public ImportResult importUsers(byte[] content) {
        return systemUserManagementService.importUsers(content);
    }

    @Transactional(readOnly = true)
    public PrintLoginTagView printLoginTag(String userId) {
        return systemUserManagementService.printLoginTag(userId);
    }

    @Schema(name = "SystemManagementPagedResult")
    public record PagedResult<T>(List<T> items, int page, int size, long total) {
    }

    @Schema(name = "UserView")
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

    @Schema(name = "UserLoginLogView")
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

    @Schema(name = "AssignedRoleView")
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

    public record UpdateUserCommand(
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

    public record AssignUserRolesCommand(List<RoleAssignmentInput> assignments) {
    }

    public record RoleAssignmentInput(String roleId, boolean primary) {
    }

    @Schema(name = "RoleView")
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

    public record UpdateRoleCommand(
        String roleCode,
        String roleName,
        String roleType,
        String dataScope,
        String remarks,
        boolean enabled
    ) {
    }

    @Schema(name = "RoleAuthorizationView")
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

    @Schema(name = "MenuView")
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

    @Schema(name = "PermissionView")
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
        boolean enabled,
        boolean entryPermission
    ) {
    }

    @Schema(name = "MessageTopicView")
    public record MessageTopicView(
        String id,
        String topicCode,
        String topicName,
        String topicCategory,
        String description,
        boolean enabled
    ) {
    }

    @Schema(name = "StatCategoryView")
    public record StatCategoryView(
        String id,
        String statCode,
        String statName,
        String statScope,
        String description,
        boolean enabled
    ) {
    }

    @Schema(name = "SystemImportResult")
    public record ImportResult(int successCount, int failureCount, List<ImportError> errors) {
    }

    @Schema(name = "SystemImportError")
    public record ImportError(int rowNumber, String field, String rejectedValue, String message) {
    }

    @Schema(name = "PrintLoginTagView")
    public record PrintLoginTagView(String loginTagCode, String title, String content) {
    }
}
