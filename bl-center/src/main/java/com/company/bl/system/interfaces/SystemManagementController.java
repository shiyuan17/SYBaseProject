package com.company.bl.system.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.system.application.SystemManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "系统管理", description = "系统用户、角色、菜单、权限与授权维护接口")
public class SystemManagementController {

    private final SystemManagementService systemManagementService;

    public SystemManagementController(SystemManagementService systemManagementService) {
        this.systemManagementService = systemManagementService;
    }

    @Operation(summary = "分页查询系统用户", description = "查询系统用户分页列表。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_QUERY)
    @GetMapping("/api/v1/system-users")
    public SystemManagementService.PagedResult<SystemManagementService.UserView> listUsers(
        @Parameter(description = "页码，从 1 开始") @RequestParam(name = "page", defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(name = "size", defaultValue = "20") int size) {
        return systemManagementService.listUsers(page, size);
    }

    @Operation(summary = "新增系统用户", description = "新增系统用户基础资料。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_CREATE)
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

    @Operation(summary = "更新用户启用状态", description = "更新指定用户的启停状态。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_UPDATE)
    @PatchMapping("/api/v1/system-users/{id}/enabled")
    public SystemManagementService.UserView updateUserEnabled(@Parameter(description = "用户 ID") @PathVariable("id") String id,
                                                              @Valid @RequestBody UpdateEnabledRequest request) {
        return systemManagementService.updateUserEnabled(id, request.enabled());
    }

    @Operation(summary = "分页查询用户登录日志", description = "查询指定用户的登录日志分页列表。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_QUERY)
    @GetMapping("/api/v1/system-users/{id}/login-logs")
    public SystemManagementService.PagedResult<SystemManagementService.UserLoginLogView> listUserLoginLogs(
        @Parameter(description = "用户 ID") @PathVariable("id") String id,
        @Parameter(description = "页码，从 1 开始") @RequestParam(name = "page", defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(name = "size", defaultValue = "20") int size) {
        return systemManagementService.listUserLoginLogs(id, page, size);
    }

    @Operation(summary = "分配用户角色", description = "全量覆盖指定用户的角色分配关系。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_UPDATE)
    @PutMapping("/api/v1/system-users/{id}/roles")
    public SystemManagementService.UserView assignUserRoles(@Parameter(description = "用户 ID") @PathVariable("id") String id,
                                                            @Valid @RequestBody AssignUserRolesRequest request) {
        return systemManagementService.assignUserRoles(id, new SystemManagementService.AssignUserRolesCommand(
            request.assignments().stream()
                .map(item -> new SystemManagementService.RoleAssignmentInput(item.roleId(), item.primary()))
                .toList()));
    }

    @Operation(summary = "查询角色列表", description = "查询系统内全部角色。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_QUERY)
    @GetMapping("/api/v1/roles")
    public List<SystemManagementService.RoleView> listRoles() {
        return systemManagementService.listRoles();
    }

    @Operation(summary = "新增角色", description = "新增系统角色定义。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_CREATE)
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

    @Operation(summary = "查询角色授权", description = "查询指定角色的菜单、权限、主题与统计范围。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_QUERY)
    @GetMapping("/api/v1/roles/{id}/authorizations")
    public SystemManagementService.RoleAuthorizationView getRoleAuthorization(@Parameter(description = "角色 ID") @PathVariable("id") String id) {
        return systemManagementService.getRoleAuthorization(id);
    }

    @Operation(summary = "更新角色授权", description = "全量覆盖指定角色的菜单、权限、消息主题与统计范围。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_ASSIGN)
    @PutMapping("/api/v1/roles/{id}/authorizations")
    public SystemManagementService.RoleAuthorizationView updateRoleAuthorization(@Parameter(description = "角色 ID") @PathVariable("id") String id,
                                                                                 @Valid @RequestBody UpdateRoleAuthorizationRequest request) {
        return systemManagementService.updateRoleAuthorization(id,
            new SystemManagementService.UpdateRoleAuthorizationCommand(
                request.menuIds(),
                request.permissionIds(),
                request.topicIds(),
                request.statScopes() == null ? Map.of() : request.statScopes()));
    }

    @Operation(summary = "查询菜单列表", description = "查询系统菜单定义列表。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_QUERY)
    @GetMapping("/api/v1/menus")
    public List<SystemManagementService.MenuView> listMenus() {
        return systemManagementService.listMenus();
    }

    @Operation(summary = "查询权限列表", description = "查询系统权限定义列表。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_QUERY)
    @GetMapping("/api/v1/permissions")
    public List<SystemManagementService.PermissionView> listPermissions() {
        return systemManagementService.listPermissions();
    }

    @Operation(summary = "查询消息主题列表", description = "查询系统消息主题定义列表。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_QUERY)
    @GetMapping("/api/v1/message-topics")
    public List<SystemManagementService.MessageTopicView> listMessageTopics() {
        return systemManagementService.listMessageTopics();
    }

    @Operation(summary = "查询统计分类列表", description = "查询系统统计分类定义列表。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_QUERY)
    @GetMapping("/api/v1/stat-categories")
    public List<SystemManagementService.StatCategoryView> listStatCategories() {
        return systemManagementService.listStatCategories();
    }

    @Schema(name = "CreateUserRequest", description = "新增系统用户请求")
    public record CreateUserRequest(
        @Schema(description = "用户编码")
        @Size(max = 64, message = "User code must not exceed 64 characters")
        String userCode,
        @Schema(description = "登录名", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Login name must not be blank")
        @Size(max = 64, message = "Login name must not exceed 64 characters")
        String loginName,
        @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "User name must not be blank")
        @Size(max = 100, message = "User name must not exceed 100 characters")
        String name,
        @Schema(description = "登录密码")
        @Size(max = 255, message = "Password must not exceed 255 characters")
        String password,
        @Schema(description = "工号")
        @Size(max = 64, message = "Job number must not exceed 64 characters")
        String jobNo,
        @Schema(description = "职称")
        @Size(max = 100, message = "Title name must not exceed 100 characters")
        String titleName,
        @Schema(description = "科室 ID")
        @Size(max = 64, message = "Department id must not exceed 64 characters")
        String departmentId,
        @Schema(description = "科室名称")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String departmentName,
        @Schema(description = "手机号")
        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone,
        @Schema(description = "邮箱")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,
        @Schema(description = "头像地址")
        @Size(max = 500, message = "Avatar must not exceed 500 characters")
        String avatar,
        @Schema(description = "登录标签编码")
        @Size(max = 64, message = "Login tag code must not exceed 64 characters")
        String loginTagCode,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "SystemUserUpdateEnabledRequest", description = "更新用户启用状态请求")
    public record UpdateEnabledRequest(@Schema(description = "是否启用") boolean enabled) {
    }

    @Schema(name = "AssignUserRolesRequest", description = "分配用户角色请求")
    public record AssignUserRolesRequest(@Schema(description = "角色分配列表") List<RoleAssignmentRequest> assignments) {
    }

    @Schema(name = "RoleAssignmentRequest", description = "角色分配条目")
    public record RoleAssignmentRequest(
        @Schema(description = "角色 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Role id must not be blank")
        String roleId,
        @Schema(description = "是否为主角色")
        boolean primary
    ) {
    }

    @Schema(name = "CreateRoleRequest", description = "新增角色请求")
    public record CreateRoleRequest(
        @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Role code must not be blank")
        @Size(max = 64, message = "Role code must not exceed 64 characters")
        String roleCode,
        @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Role name must not be blank")
        @Size(max = 100, message = "Role name must not exceed 100 characters")
        String roleName,
        @Schema(description = "角色类型")
        @Size(max = 50, message = "Role type must not exceed 50 characters")
        String roleType,
        @Schema(description = "数据范围")
        @Size(max = 50, message = "Data scope must not exceed 50 characters")
        String dataScope,
        @Schema(description = "备注")
        @Size(max = 500, message = "Remarks must not exceed 500 characters")
        String remarks,
        @Schema(description = "是否启用")
        boolean enabled
    ) {
    }

    @Schema(name = "UpdateRoleAuthorizationRequest", description = "更新角色授权请求")
    public record UpdateRoleAuthorizationRequest(
        @Schema(description = "菜单 ID 列表")
        List<String> menuIds,
        @Schema(description = "权限 ID 列表")
        List<String> permissionIds,
        @Schema(description = "消息主题 ID 列表")
        List<String> topicIds,
        @Schema(description = "统计范围映射，key 为统计分类 ID，value 为范围值")
        Map<String, String> statScopes
    ) {
    }
}
