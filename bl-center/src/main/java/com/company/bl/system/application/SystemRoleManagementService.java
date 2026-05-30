package com.company.bl.system.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.system.infrastructure.SystemRoleJdbcRepository;
import com.company.common.security.authorization.MenuEntryPermissionResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class SystemRoleManagementService {

    private final SystemRoleJdbcRepository systemRoleJdbcRepository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public SystemRoleManagementService(SystemRoleJdbcRepository systemRoleJdbcRepository,
                                       NumberingService numberingService,
                                       OperationAuditService operationAuditService) {
        this.systemRoleJdbcRepository = systemRoleJdbcRepository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
    }

    @Transactional(readOnly = true)
    public List<SystemManagementService.RoleView> listRoles() {
        return systemRoleJdbcRepository.findRoles().stream().map(this::toRoleView).toList();
    }

    @Transactional
    public SystemManagementService.RoleView createRole(SystemManagementService.CreateRoleCommand command) {
        String roleCode = resolveCreateCode(command.roleCode(), numberingService::generateRoleCode);
        return operationAuditService.audit("SYSTEM", "ROLE", "create_role", () -> {
            try {
                return toRoleView(systemRoleJdbcRepository.insertRole(new SystemRoleJdbcRepository.CreateRoleRow(
                    "ROLE-" + UUID.randomUUID(),
                    roleCode,
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
        }, SystemManagementService.RoleView::id, () -> roleCode);
    }

    @Transactional
    public SystemManagementService.RoleView updateRole(String roleId, SystemManagementService.UpdateRoleCommand command) {
        return operationAuditService.audit("SYSTEM", "ROLE", "update_role", () -> {
            SystemRoleJdbcRepository.RoleRow current = ensureRoleExists(roleId);
            String roleCode = resolveExistingCode(command.roleCode(), current.roleCode(), "Role code");
            try {
                systemRoleJdbcRepository.updateRole(roleId, new SystemRoleJdbcRepository.UpdateRoleRow(
                    roleCode,
                    command.roleName(),
                    command.roleType(),
                    command.dataScope(),
                    command.remarks(),
                    command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Role code already exists");
            }
            return toRoleView(systemRoleJdbcRepository.findRoleById(roleId));
        }, SystemManagementService.RoleView::id, () -> roleId);
    }

    @Transactional
    public void deleteRole(String roleId) {
        operationAuditService.audit("SYSTEM", "ROLE", "delete_role", () -> {
            ensureRoleExists(roleId);
            if (systemRoleJdbcRepository.countRoleAssignments(roleId) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Role is still assigned to users");
            }
            systemRoleJdbcRepository.deleteRole(roleId);
            return roleId;
        }, value -> roleId, () -> roleId);
    }

    @Transactional(readOnly = true)
    public SystemManagementService.RoleAuthorizationView getRoleAuthorization(String roleId) {
        ensureRoleExists(roleId);
        SystemRoleJdbcRepository.RoleAuthorizationRow authorization = systemRoleJdbcRepository.findRoleAuthorization(roleId);
        List<String> normalizedMenuIds = normalizeIds(authorization.menuIds());
        return new SystemManagementService.RoleAuthorizationView(
            roleId,
            normalizedMenuIds,
            normalizeManualPermissionIds(normalizedMenuIds, authorization.permissionIds()),
            authorization.topicIds(),
            authorization.statScopes());
    }

    @Transactional
    public SystemManagementService.RoleAuthorizationView updateRoleAuthorization(
        String roleId,
        SystemManagementService.UpdateRoleAuthorizationCommand command
    ) {
        return operationAuditService.audit("SYSTEM", "ROLE_AUTH", "update_role_authorization", () -> {
            ensureRoleExists(roleId);
            List<String> menuIds = normalizeIds(command.menuIds());
            List<String> permissionIds = normalizeManualPermissionIds(menuIds, command.permissionIds());
            systemRoleJdbcRepository.replaceRoleAuthorizations(roleId, new SystemRoleJdbcRepository.AuthorizationCommand(
                menuIds,
                permissionIds,
                safeList(command.topicIds()),
                command.statScopes() == null ? Map.of() : command.statScopes()));
            return getRoleAuthorization(roleId);
        }, SystemManagementService.RoleAuthorizationView::roleId, () -> roleId);
    }

    @Transactional(readOnly = true)
    public List<SystemManagementService.MenuView> listMenus() {
        return systemRoleJdbcRepository.findMenus().stream().map(menu -> new SystemManagementService.MenuView(
            menu.id(),
            menu.parentId(),
            menu.menuCode(),
            menu.menuName(),
            menu.menuType(),
            menu.path(),
            menu.componentName(),
            menu.icon(),
            menu.permissionPrefix(),
            menu.sortOrder(),
            menu.visible(),
            menu.enabled())).toList();
    }

    @Transactional(readOnly = true)
    public List<SystemManagementService.PermissionView> listPermissions() {
        List<SystemRoleJdbcRepository.MenuRow> menuRows = systemRoleJdbcRepository.findMenus();
        List<SystemRoleJdbcRepository.PermissionRow> permissionRows = systemRoleJdbcRepository.findPermissions();
        Set<String> entryPermissionIds = resolveEntryPermissionIds(menuRows, permissionRows);
        return permissionRows.stream().map(permission -> new SystemManagementService.PermissionView(
            permission.id(),
            permission.permissionCode(),
            permission.permissionName(),
            permission.menuId(),
            permission.actionKey(),
            permission.httpMethod(),
            permission.resourcePath(),
            permission.permissionGroup(),
            permission.sortOrder(),
            permission.enabled(),
            entryPermissionIds.contains(permission.id()))).toList();
    }

    @Transactional(readOnly = true)
    public List<SystemManagementService.MessageTopicView> listMessageTopics() {
        return systemRoleJdbcRepository.findMessageTopics().stream().map(topic -> new SystemManagementService.MessageTopicView(
            topic.id(),
            topic.topicCode(),
            topic.topicName(),
            topic.topicCategory(),
            topic.description(),
            topic.enabled())).toList();
    }

    @Transactional(readOnly = true)
    public List<SystemManagementService.StatCategoryView> listStatCategories() {
        return systemRoleJdbcRepository.findStatCategories().stream().map(stat -> new SystemManagementService.StatCategoryView(
            stat.id(),
            stat.statCode(),
            stat.statName(),
            stat.statScope(),
            stat.description(),
            stat.enabled())).toList();
    }

    private SystemRoleJdbcRepository.RoleRow ensureRoleExists(String roleId) {
        SystemRoleJdbcRepository.RoleRow role = systemRoleJdbcRepository.findRoleById(roleId);
        if (role == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Role not found");
        }
        return role;
    }

    private SystemManagementService.RoleView toRoleView(SystemRoleJdbcRepository.RoleRow role) {
        return new SystemManagementService.RoleView(
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

    private List<String> normalizeIds(List<String> values) {
        return safeList(values).stream()
            .filter(this::hasText)
            .distinct()
            .toList();
    }

    private List<String> normalizeManualPermissionIds(List<String> selectedMenuIds, List<String> permissionIds) {
        List<String> normalizedMenuIds = normalizeIds(selectedMenuIds);
        List<String> normalizedPermissionIds = normalizeIds(permissionIds);
        if (normalizedMenuIds.isEmpty() || normalizedPermissionIds.isEmpty()) {
            return List.of();
        }

        Set<String> selectedMenuIdSet = Set.copyOf(normalizedMenuIds);
        List<SystemRoleJdbcRepository.MenuRow> menuRows = systemRoleJdbcRepository.findMenus();
        List<SystemRoleJdbcRepository.PermissionRow> permissionRows = systemRoleJdbcRepository.findPermissions();
        Set<String> entryPermissionIds = resolveEntryPermissionIds(menuRows, permissionRows);

        return permissionRows.stream()
            .filter(permission -> normalizedPermissionIds.contains(permission.id()))
            .filter(permission -> selectedMenuIdSet.contains(permission.menuId()))
            .filter(permission -> !entryPermissionIds.contains(permission.id()))
            .map(SystemRoleJdbcRepository.PermissionRow::id)
            .distinct()
            .toList();
    }

    private Set<String> resolveEntryPermissionIds(List<SystemRoleJdbcRepository.MenuRow> menuRows,
                                                  List<SystemRoleJdbcRepository.PermissionRow> permissionRows) {
        Map<String, SystemRoleJdbcRepository.MenuRow> menusById = menuRows.stream()
            .collect(Collectors.toMap(SystemRoleJdbcRepository.MenuRow::id, menu -> menu));
        List<MenuEntryPermissionResolver.MenuPermissionBinding> bindings = permissionRows.stream()
            .map(permission -> {
                SystemRoleJdbcRepository.MenuRow menu = menusById.get(permission.menuId());
                return menu == null ? null : new MenuEntryPermissionResolver.MenuPermissionBinding(
                    menu.id(),
                    menu.menuType(),
                    permission.id(),
                    permission.permissionCode(),
                    permission.actionKey(),
                    permission.sortOrder(),
                    menu.enabled(),
                    permission.enabled());
            })
            .filter(binding -> binding != null)
            .toList();
        return MenuEntryPermissionResolver.resolveEntryPermissionIds(bindings);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
