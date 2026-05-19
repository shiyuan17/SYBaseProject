package com.company.bl.system.interfaces;

import com.company.bl.system.application.SystemManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SystemManagementController {

    private final SystemManagementService systemManagementService;

    public SystemManagementController(SystemManagementService systemManagementService) {
        this.systemManagementService = systemManagementService;
    }

    @GetMapping("/api/v1/system-users")
    public SystemManagementService.PagedResult<SystemManagementService.UserView> listUsers(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "20") int size) {
        return systemManagementService.listUsers(page, size);
    }

    @PostMapping("/api/v1/system-users")
    public SystemManagementService.UserView createUser(@Valid @RequestBody CreateUserRequest request) {
        return systemManagementService.createUser(new SystemManagementService.CreateUserCommand(
            request.userCode(),
            request.loginName(),
            request.name(),
            request.password(),
            request.jobNo(),
            request.titleName(),
            request.departmentId(),
            request.departmentName(),
            request.phone(),
            request.email(),
            request.avatar(),
            request.loginTagCode(),
            request.enabled()));
    }

    @PatchMapping("/api/v1/system-users/{id}/enabled")
    public SystemManagementService.UserView updateUserEnabled(@PathVariable("id") String id,
                                                              @Valid @RequestBody UpdateEnabledRequest request) {
        return systemManagementService.updateUserEnabled(id, request.enabled());
    }

    @GetMapping("/api/v1/system-users/{id}/login-logs")
    public SystemManagementService.PagedResult<SystemManagementService.UserLoginLogView> listUserLoginLogs(
        @PathVariable("id") String id,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "20") int size) {
        return systemManagementService.listUserLoginLogs(id, page, size);
    }

    @PutMapping("/api/v1/system-users/{id}/roles")
    public SystemManagementService.UserView assignUserRoles(@PathVariable("id") String id,
                                                            @Valid @RequestBody AssignUserRolesRequest request) {
        return systemManagementService.assignUserRoles(id, new SystemManagementService.AssignUserRolesCommand(
            request.assignments().stream()
                .map(item -> new SystemManagementService.RoleAssignmentInput(item.roleId(), item.primary()))
                .toList()));
    }

    @GetMapping("/api/v1/roles")
    public List<SystemManagementService.RoleView> listRoles() {
        return systemManagementService.listRoles();
    }

    @PostMapping("/api/v1/roles")
    public SystemManagementService.RoleView createRole(@Valid @RequestBody CreateRoleRequest request) {
        return systemManagementService.createRole(new SystemManagementService.CreateRoleCommand(
            request.roleCode(),
            request.roleName(),
            request.roleType(),
            request.dataScope(),
            request.remarks(),
            request.enabled()));
    }

    @GetMapping("/api/v1/roles/{id}/authorizations")
    public SystemManagementService.RoleAuthorizationView getRoleAuthorization(@PathVariable("id") String id) {
        return systemManagementService.getRoleAuthorization(id);
    }

    @PutMapping("/api/v1/roles/{id}/authorizations")
    public SystemManagementService.RoleAuthorizationView updateRoleAuthorization(@PathVariable("id") String id,
                                                                                 @Valid @RequestBody UpdateRoleAuthorizationRequest request) {
        return systemManagementService.updateRoleAuthorization(id,
            new SystemManagementService.UpdateRoleAuthorizationCommand(
                request.menuIds(),
                request.permissionIds(),
                request.topicIds(),
                request.statScopes() == null ? Map.of() : request.statScopes()));
    }

    @GetMapping("/api/v1/menus")
    public List<SystemManagementService.MenuView> listMenus() {
        return systemManagementService.listMenus();
    }

    @GetMapping("/api/v1/permissions")
    public List<SystemManagementService.PermissionView> listPermissions() {
        return systemManagementService.listPermissions();
    }

    @GetMapping("/api/v1/message-topics")
    public List<SystemManagementService.MessageTopicView> listMessageTopics() {
        return systemManagementService.listMessageTopics();
    }

    @GetMapping("/api/v1/stat-categories")
    public List<SystemManagementService.StatCategoryView> listStatCategories() {
        return systemManagementService.listStatCategories();
    }

    public record CreateUserRequest(
        @Size(max = 64, message = "User code must not exceed 64 characters")
        String userCode,
        @NotBlank(message = "Login name must not be blank")
        @Size(max = 64, message = "Login name must not exceed 64 characters")
        String loginName,
        @NotBlank(message = "User name must not be blank")
        @Size(max = 100, message = "User name must not exceed 100 characters")
        String name,
        @Size(max = 255, message = "Password must not exceed 255 characters")
        String password,
        @Size(max = 64, message = "Job number must not exceed 64 characters")
        String jobNo,
        @Size(max = 100, message = "Title name must not exceed 100 characters")
        String titleName,
        @Size(max = 64, message = "Department id must not exceed 64 characters")
        String departmentId,
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String departmentName,
        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone,
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,
        @Size(max = 500, message = "Avatar must not exceed 500 characters")
        String avatar,
        @Size(max = 64, message = "Login tag code must not exceed 64 characters")
        String loginTagCode,
        boolean enabled
    ) {
    }

    public record UpdateEnabledRequest(boolean enabled) {
    }

    public record AssignUserRolesRequest(List<RoleAssignmentRequest> assignments) {
    }

    public record RoleAssignmentRequest(
        @NotBlank(message = "Role id must not be blank")
        String roleId,
        boolean primary
    ) {
    }

    public record CreateRoleRequest(
        @NotBlank(message = "Role code must not be blank")
        @Size(max = 64, message = "Role code must not exceed 64 characters")
        String roleCode,
        @NotBlank(message = "Role name must not be blank")
        @Size(max = 100, message = "Role name must not exceed 100 characters")
        String roleName,
        @Size(max = 50, message = "Role type must not exceed 50 characters")
        String roleType,
        @Size(max = 50, message = "Data scope must not exceed 50 characters")
        String dataScope,
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks,
        boolean enabled
    ) {
    }

    public record UpdateRoleAuthorizationRequest(
        List<String> menuIds,
        List<String> permissionIds,
        List<String> topicIds,
        Map<String, String> statScopes
    ) {
    }
}
