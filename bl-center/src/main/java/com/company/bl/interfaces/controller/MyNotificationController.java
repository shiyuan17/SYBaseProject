package com.company.bl.interfaces.controller;

import com.company.bl.notification.application.NotificationCenterService;
import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.context.AuthenticatedPrincipalContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/my")
@Tag(name = "个人通知中心", description = "当前登录用户的站内通知与提醒偏好接口")
public class MyNotificationController {

    private final NotificationCenterService notificationCenterService;

    public MyNotificationController(NotificationCenterService notificationCenterService) {
        this.notificationCenterService = notificationCenterService;
    }

    @GetMapping("/notifications")
    @Operation(summary = "分页查询个人通知", description = "按状态、类别和关键词筛选当前用户的站内通知。")
    public NotificationCenterService.NotificationPageView listNotifications(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "ALL") @Size(max = 16) String status,
        @RequestParam(required = false) @Size(max = 32) String category,
        @RequestParam(required = false) @Size(max = 100) String keyword,
        HttpServletRequest request
    ) {
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.requirePrincipal(request);
        return notificationCenterService.listNotifications(new NotificationCenterService.NotificationListCommand(
            principal.userId(),
            page,
            size,
            status,
            category,
            keyword
        ));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "查询个人未读通知数量", description = "返回当前用户站内通知的未读数量。")
    public NotificationCenterService.UnreadCountView unreadCount(HttpServletRequest request) {
        return notificationCenterService.getUnreadCount(currentUserId(request));
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "标记单条通知已读", description = "将当前用户的一条通知标记为已读，重复操作幂等。")
    public void markRead(@PathVariable("id") String id, HttpServletRequest request) {
        notificationCenterService.markRead(currentUserId(request), id);
    }

    @PatchMapping("/notifications/read-all")
    @Operation(summary = "全部标记已读", description = "将当前用户可见的未读通知全部标记为已读。")
    public void markAllRead(HttpServletRequest request) {
        notificationCenterService.markAllRead(currentUserId(request));
    }

    @PatchMapping("/notifications/{id}/archive")
    @Operation(summary = "归档单条通知", description = "将当前用户的一条通知归档，重复操作幂等。")
    public void archiveOne(@PathVariable("id") String id, HttpServletRequest request) {
        notificationCenterService.archiveOne(currentUserId(request), id);
    }

    @PatchMapping("/notifications/archive")
    @Operation(summary = "批量归档通知", description = "按通知 ID 批量归档当前用户的通知。")
    public void archiveMany(
        @Valid @RequestBody ArchiveNotificationsRequest requestBody,
        HttpServletRequest request
    ) {
        notificationCenterService.archiveMany(currentUserId(request), requestBody.notificationIds());
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "查询个人提醒偏好", description = "读取当前用户的提醒展示偏好。")
    public NotificationCenterService.NotificationPreferenceView getPreferences(HttpServletRequest request) {
        return notificationCenterService.getPreferences(currentUserId(request));
    }

    @PutMapping("/notification-preferences")
    @Operation(summary = "保存个人提醒偏好", description = "保存当前用户的提醒展示偏好设置。")
    public NotificationCenterService.NotificationPreferenceView updatePreferences(
        @Valid @RequestBody UpdateNotificationPreferencesRequest requestBody,
        HttpServletRequest request
    ) {
        return notificationCenterService.updatePreferences(
            currentUserId(request),
            new NotificationCenterService.UpdateNotificationPreferenceCommand(
                requestBody.accountPassword(),
                requestBody.systemMessage(),
                requestBody.todoTask()
            )
        );
    }

    private String currentUserId(HttpServletRequest request) {
        return AuthenticatedPrincipalContext.requirePrincipal(request).userId();
    }

    @Schema(name = "ArchiveNotificationsRequest", description = "批量归档通知请求")
    public record ArchiveNotificationsRequest(
        @Schema(description = "通知 ID 列表")
        @NotEmpty List<@NotNull @Size(max = 64) String> notificationIds
    ) {
    }

    @Schema(name = "UpdateNotificationPreferencesRequest", description = "更新个人提醒偏好请求")
    public record UpdateNotificationPreferencesRequest(
        @Schema(description = "账户与密码提醒开关") @NotNull Boolean accountPassword,
        @Schema(description = "系统消息提醒开关") @NotNull Boolean systemMessage,
        @Schema(description = "待办任务提醒开关") @NotNull Boolean todoTask
    ) {
    }
}
