package com.company.bl.system.interfaces;

import com.company.bl.interfaces.auth.AuditOperation;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

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
        @Parameter(description = "每页条数，默认 20") @RequestParam(name = "size", defaultValue = "20") int size,
        @Parameter(description = "是否启用") @RequestParam(name = "enabled", required = false) Boolean enabled,
        @Parameter(description = "关键字") @RequestParam(name = "keyword", required = false) String keyword) {
        return systemManagementService.listUsers(page, size, enabled, keyword);
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

    @Operation(summary = "更新系统用户", description = "更新系统用户基础资料。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_UPDATE)
    @PatchMapping("/api/v1/system-users/{id}")
    public SystemManagementService.UserView updateUser(@Parameter(description = "用户 ID") @PathVariable("id") String id,
                                                       @Valid @RequestBody UpdateUserRequest request) {
        return systemManagementService.updateUser(id, new SystemManagementService.UpdateUserCommand(
            request.userCode(),
            request.name(),
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

    @Operation(summary = "分页查询全局登录日志", description = "按筛选条件查询全局登录日志分页列表。")
    @RequirePermission(M1PermissionCodes.LOG_QUERY)
    @AuditOperation(moduleCode = "SYSTEM", businessType = "AUDIT_LOG", operationName = "query_login_logs", sensitiveQuery = true)
    @GetMapping("/api/v1/system/logs/login")
    public SystemManagementService.PagedResult<SystemManagementService.UserLoginLogView> listLoginLogs(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "startAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @RequestParam(name = "endAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        @RequestParam(name = "result", required = false) String result,
        @RequestParam(name = "ip", required = false) String ip,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "loginName", required = false) String loginName,
        @RequestParam(name = "userId", required = false) String userId,
        @RequestParam(name = "clientDevice", required = false) String clientDevice) {
        return systemManagementService.listLoginLogs(new SystemManagementService.LoginLogQuery(
            page, size, startAt, endAt, result, ip, keyword, loginName, userId, clientDevice));
    }

    @Operation(summary = "查询登录日志详情", description = "查询指定登录日志的脱敏详情。")
    @RequirePermission(M1PermissionCodes.LOG_DETAIL)
    @AuditOperation(moduleCode = "SYSTEM", businessType = "AUDIT_LOG", operationName = "get_login_log_detail", sensitiveQuery = true)
    @GetMapping("/api/v1/system/logs/login/{id}")
    public SystemManagementService.UserLoginLogView getLoginLog(@PathVariable("id") String id) {
        return systemManagementService.getLoginLog(id);
    }

    @Operation(summary = "分页查询操作日志", description = "按筛选条件查询系统操作日志分页列表。")
    @RequirePermission(M1PermissionCodes.LOG_QUERY)
    @AuditOperation(moduleCode = "SYSTEM", businessType = "AUDIT_LOG", operationName = "query_operation_logs", sensitiveQuery = true)
    @GetMapping("/api/v1/system/logs/operations")
    public SystemManagementService.PagedResult<SystemManagementService.OperationLogView> listOperationLogs(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        @RequestParam(name = "startAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @RequestParam(name = "endAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        @RequestParam(name = "result", required = false) String result,
        @RequestParam(name = "ip", required = false) String ip,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "operatorKeyword", required = false) String operatorKeyword,
        @RequestParam(name = "moduleCode", required = false) String moduleCode,
        @RequestParam(name = "businessType", required = false) String businessType,
        @RequestParam(name = "businessId", required = false) String businessId,
        @RequestParam(name = "operationName", required = false) String operationName,
        @RequestParam(name = "contentKeyword", required = false) String contentKeyword) {
        return systemManagementService.listOperationLogs(new SystemManagementService.OperationLogQuery(
            page, size, startAt, endAt, result, ip, keyword, operatorKeyword, moduleCode, businessType, businessId,
            operationName, contentKeyword));
    }

    @Operation(summary = "查询操作日志详情", description = "查询指定操作日志的脱敏详情。")
    @RequirePermission(M1PermissionCodes.LOG_DETAIL)
    @AuditOperation(moduleCode = "SYSTEM", businessType = "AUDIT_LOG", operationName = "get_operation_log_detail", sensitiveQuery = true)
    @GetMapping("/api/v1/system/logs/operations/{id}")
    public SystemManagementService.OperationLogView getOperationLog(@PathVariable("id") String id) {
        return systemManagementService.getOperationLog(id);
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

    @Operation(summary = "导入系统用户", description = "按 CSV 文件导入系统用户。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_CREATE)
    @PostMapping(value = "/api/v1/system-users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SystemManagementService.ImportResult importUsers(@RequestParam("file") MultipartFile file) throws Exception {
        return systemManagementService.importUsers(file.getBytes());
    }

    @Operation(summary = "导出系统用户", description = "按筛选条件导出系统用户。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_QUERY)
    @GetMapping("/api/v1/system-users/export")
    public ResponseEntity<byte[]> exportUsers(@RequestParam(name = "enabled", required = false) Boolean enabled,
                                              @RequestParam(name = "keyword", required = false) String keyword) {
        byte[] content = systemManagementService.exportUsers(enabled, keyword);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=system-users.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(content);
    }

    @Operation(summary = "打印用户登录标签", description = "返回登录标签打印预览内容。")
    @RequirePermission(M1PermissionCodes.SYSTEM_USER_QUERY)
    @PostMapping("/api/v1/system-users/{id}/print-login-tag")
    public SystemManagementService.PrintLoginTagView printLoginTag(@Parameter(description = "用户 ID") @PathVariable("id") String id) {
        return systemManagementService.printLoginTag(id);
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

    @Operation(summary = "更新角色", description = "更新角色基础信息。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_CREATE)
    @PatchMapping("/api/v1/roles/{id}")
    public SystemManagementService.RoleView updateRole(@Parameter(description = "角色 ID") @PathVariable("id") String id,
                                                       @Valid @RequestBody UpdateRoleRequest request) {
        return systemManagementService.updateRole(id, new SystemManagementService.UpdateRoleCommand(
            request.roleCode(),
            request.roleName(),
            request.roleType(),
            request.dataScope(),
            request.remarks(),
            request.enabled()));
    }

    @Operation(summary = "删除角色", description = "删除未分配给用户的角色。")
    @RequirePermission(M1PermissionCodes.SYSTEM_ROLE_CREATE)
    @DeleteMapping("/api/v1/roles/{id}")
    public void deleteRole(@Parameter(description = "角色 ID") @PathVariable("id") String id) {
        systemManagementService.deleteRole(id);
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

    @Schema(name = "UpdateUserRequest", description = "更新系统用户请求")
    public record UpdateUserRequest(
        @Schema(description = "用户编码")
        @Size(max = 64, message = "User code must not exceed 64 characters")
        String userCode,
        @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "User name must not be blank")
        @Size(max = 100, message = "User name must not exceed 100 characters")
        String name,
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
        @Schema(description = "角色编码，可为空，由系统自动生成")
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

    @Schema(name = "UpdateRoleRequest", description = "更新角色请求")
    public record UpdateRoleRequest(
        @Schema(description = "角色编码，创建后不可修改")
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
