package com.company.bl.notification.application;

import com.company.bl.support.application.OperationAuditService;
import com.company.bl.notification.infrastructure.NotificationCenterJdbcRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationCenterService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCenterService.class);
    private static final String USER_NOTIFICATION_PREFERENCES = "user_notification_preferences";
    private static final String USER_NOTIFICATIONS = "user_notifications";

    private final NotificationCenterJdbcRepository repository;
    private final ObjectMapper objectMapper;
    private final OperationAuditService operationAuditService;

    public NotificationCenterService(
        NotificationCenterJdbcRepository repository,
        ObjectMapper objectMapper,
        OperationAuditService operationAuditService
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.operationAuditService = operationAuditService;
    }

    @Transactional
    public NotificationPageView listNotifications(NotificationListCommand command) {
        return operationAuditService.audit(
            "SYSTEM",
            "MY_NOTIFICATION",
            "query_my_notifications",
            () -> {
                try {
                    NotificationCenterJdbcRepository.PreferenceRow preference = resolvePreferenceRow(command.userId());
                    Set<String> authorizedTopicCodes = repository.findAuthorizedTopicCodes(command.userId());
                    NotificationCenterJdbcRepository.PagedNotifications paged =
                        repository.findNotifications(
                            command.userId(),
                            command.page(),
                            command.size(),
                            command.status(),
                            command.category(),
                            command.keyword(),
                            authorizedTopicCodes,
                            preference
                        );
                    return new NotificationPageView(
                        paged.items().stream().map(this::toView).toList(),
                        Math.max(1, command.page()),
                        Math.max(1, command.size()),
                        paged.total()
                    );
                } catch (DataAccessException exception) {
                    if (isNotificationSchemaMissing(exception)) {
                        logNotificationFallback("listNotifications", command.userId(), exception);
                        return emptyPage(command);
                    }
                    throw exception;
                }
            },
            ignored -> command.userId(),
            () -> command.userId(),
            () -> buildListAuditContent(command));
    }

    @Transactional(readOnly = true)
    public UnreadCountView getUnreadCount(String userId) {
        try {
            NotificationCenterJdbcRepository.PreferenceRow preference = resolvePreferenceRow(userId);
            long unreadCount = repository.countUnreadNotifications(
                userId,
                repository.findAuthorizedTopicCodes(userId),
                preference
            );
            return new UnreadCountView(unreadCount);
        } catch (DataAccessException exception) {
            if (isNotificationSchemaMissing(exception)) {
                logNotificationFallback("getUnreadCount", userId, exception);
                return new UnreadCountView(0L);
            }
            throw exception;
        }
    }

    @Transactional
    public void markRead(String userId, String notificationId) {
        operationAuditService.audit(
            "SYSTEM",
            "MY_NOTIFICATION",
            "mark_my_notification_read",
            () -> {
                try {
                    repository.markRead(userId, notificationId, LocalDateTime.now());
                    return null;
                } catch (DataAccessException exception) {
                    if (isNotificationSchemaMissing(exception)) {
                        logNotificationFallback("markRead", userId, exception);
                        return null;
                    }
                    throw exception;
                }
            },
            ignored -> userId,
            () -> userId,
            () -> "notificationId=" + notificationId);
    }

    @Transactional
    public void markAllRead(String userId) {
        operationAuditService.audit(
            "SYSTEM",
            "MY_NOTIFICATION",
            "mark_all_my_notifications_read",
            () -> {
                try {
                    NotificationCenterJdbcRepository.PreferenceRow preference = resolvePreferenceRow(userId);
                    repository.markAllRead(
                        userId,
                        repository.findAuthorizedTopicCodes(userId),
                        preference,
                        LocalDateTime.now()
                    );
                    return null;
                } catch (DataAccessException exception) {
                    if (isNotificationSchemaMissing(exception)) {
                        logNotificationFallback("markAllRead", userId, exception);
                        return null;
                    }
                    throw exception;
                }
            },
            ignored -> userId,
            () -> userId,
            () -> "scope=visible-unread");
    }

    @Transactional
    public void archiveOne(String userId, String notificationId) {
        operationAuditService.audit(
            "SYSTEM",
            "MY_NOTIFICATION",
            "archive_my_notification",
            () -> {
                try {
                    repository.archiveOne(userId, notificationId, LocalDateTime.now());
                    return null;
                } catch (DataAccessException exception) {
                    if (isNotificationSchemaMissing(exception)) {
                        logNotificationFallback("archiveOne", userId, exception);
                        return null;
                    }
                    throw exception;
                }
            },
            ignored -> userId,
            () -> userId,
            () -> "notificationId=" + notificationId);
    }

    @Transactional
    public void archiveMany(String userId, List<String> notificationIds) {
        operationAuditService.audit(
            "SYSTEM",
            "MY_NOTIFICATION",
            "archive_my_notifications",
            () -> {
                try {
                    repository.archiveMany(userId, notificationIds, LocalDateTime.now());
                    return null;
                } catch (DataAccessException exception) {
                    if (isNotificationSchemaMissing(exception)) {
                        logNotificationFallback("archiveMany", userId, exception);
                        return null;
                    }
                    throw exception;
                }
            },
            ignored -> userId,
            () -> userId,
            () -> "notificationCount=" + countNotificationIds(notificationIds));
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceView getPreferences(String userId) {
        try {
            return toPreferenceView(resolvePreferenceRow(userId));
        } catch (DataAccessException exception) {
            if (isNotificationSchemaMissing(exception)) {
                logNotificationFallback("getPreferences", userId, exception);
                return toPreferenceView(defaultPreferenceRow(userId));
            }
            throw exception;
        }
    }

    @Transactional
    public NotificationPreferenceView updatePreferences(
        String userId,
        UpdateNotificationPreferenceCommand command
    ) {
        NotificationCenterJdbcRepository.PreferenceRow nextPreference =
            new NotificationCenterJdbcRepository.PreferenceRow(
                userId,
                command.accountPassword(),
                command.systemMessage(),
                command.todoTask(),
                LocalDateTime.now()
            );
        return operationAuditService.audit(
            "SYSTEM",
            "MY_NOTIFICATION",
            "update_my_notification_preferences",
            () -> {
                try {
                    repository.upsertPreference(userId, nextPreference, LocalDateTime.now());
                    return toPreferenceView(resolvePreferenceRow(userId));
                } catch (DataAccessException exception) {
                    if (isNotificationSchemaMissing(exception)) {
                        logNotificationFallback("updatePreferences", userId, exception);
                        return toPreferenceView(nextPreference);
                    }
                    throw exception;
                }
            },
            ignored -> userId,
            () -> userId,
            () -> buildPreferenceAuditContent(command));
    }

    private NotificationCenterJdbcRepository.PreferenceRow resolvePreferenceRow(String userId) {
        NotificationCenterJdbcRepository.PreferenceRow preference = repository.findPreference(userId);
        if (preference != null) {
            return preference;
        }
        return defaultPreferenceRow(userId);
    }

    private NotificationCenterJdbcRepository.PreferenceRow defaultPreferenceRow(String userId) {
        return new NotificationCenterJdbcRepository.PreferenceRow(userId, true, true, true, null);
    }

    private NotificationPageView emptyPage(NotificationListCommand command) {
        return new NotificationPageView(
            List.of(),
            Math.max(1, command.page()),
            Math.max(1, command.size()),
            0L
        );
    }

    private void logNotificationFallback(String operation, String userId, DataAccessException exception) {
        log.warn(
            "Notification center schema unavailable, fallback applied. operation={}, userId={}",
            operation,
            userId,
            exception
        );
    }

    private boolean isNotificationSchemaMissing(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if ((normalized.contains(USER_NOTIFICATION_PREFERENCES) || normalized.contains(USER_NOTIFICATIONS))
                    && (normalized.contains("invalid table")
                    || normalized.contains("invalid object")
                    || normalized.contains("not found")
                    || normalized.contains("table or view")
                    || normalized.contains("无效的表或视图名"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String buildListAuditContent(NotificationListCommand command) {
        return "page=" + Math.max(1, command.page())
            + ",size=" + Math.max(1, command.size())
            + ",status=" + normalizeAuditValue(command.status(), "ALL")
            + ",category=" + normalizeAuditValue(command.category(), "ALL")
            + ",keywordPresent=" + (command.keyword() != null && !command.keyword().isBlank());
    }

    private String buildPreferenceAuditContent(UpdateNotificationPreferenceCommand command) {
        return "accountPassword=" + command.accountPassword()
            + ",systemMessage=" + command.systemMessage()
            + ",todoTask=" + command.todoTask();
    }

    private int countNotificationIds(List<String> notificationIds) {
        if (notificationIds == null) {
            return 0;
        }
        return (int) notificationIds.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .distinct()
            .count();
    }

    private String normalizeAuditValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private NotificationRecordView toView(NotificationCenterJdbcRepository.NotificationRow row) {
        return new NotificationRecordView(
            row.id(),
            row.title(),
            row.content(),
            row.summary(),
            row.avatar(),
            row.topicCode(),
            row.category(),
            row.level(),
            row.status(),
            stringify(row.createdAt()),
            stringify(row.readAt()),
            row.actionText(),
            row.actionTarget(),
            row.actionType(),
            parseActionPayload(row.actionPayloadJson())
        );
    }

    private NotificationPreferenceView toPreferenceView(NotificationCenterJdbcRepository.PreferenceRow row) {
        return new NotificationPreferenceView(
            row.accountPasswordEnabled(),
            row.systemMessageEnabled(),
            row.todoTaskEnabled()
        );
    }

    private NotificationActionPayloadView parseActionPayload(String actionPayloadJson) {
        if (actionPayloadJson == null || actionPayloadJson.isBlank()) {
            return new NotificationActionPayloadView(Map.of());
        }
        try {
            JsonNode root = objectMapper.readTree(actionPayloadJson);
            JsonNode queryNode = root.path("query");
            if (!queryNode.isObject()) {
                return new NotificationActionPayloadView(Map.of());
            }
            Map<String, String> query = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = queryNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                query.put(field.getKey(), field.getValue().isNull() ? "" : field.getValue().asText());
            }
            return new NotificationActionPayloadView(query);
        } catch (Exception ignored) {
            return new NotificationActionPayloadView(Map.of());
        }
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    public record NotificationListCommand(
        String userId,
        int page,
        int size,
        String status,
        String category,
        String keyword
    ) {
    }

    public record UpdateNotificationPreferenceCommand(
        boolean accountPassword,
        boolean systemMessage,
        boolean todoTask
    ) {
    }

    @Schema(name = "MyNotificationPageView", description = "个人通知分页结果")
    public record NotificationPageView(
        @Schema(description = "当前页数据") List<NotificationRecordView> items,
        @Schema(description = "当前页码") int page,
        @Schema(description = "每页条数") int size,
        @Schema(description = "总记录数") long total
    ) {
    }

    @Schema(name = "MyNotificationRecordView", description = "个人通知记录")
    public record NotificationRecordView(
        @Schema(description = "通知 ID") String id,
        @Schema(description = "标题") String title,
        @Schema(description = "正文") String content,
        @Schema(description = "摘要") String summary,
        @Schema(description = "头像") String avatar,
        @Schema(description = "主题编码") String topicCode,
        @Schema(description = "类别") String category,
        @Schema(description = "级别") String level,
        @Schema(description = "状态") String status,
        @Schema(description = "创建时间") String createdAt,
        @Schema(description = "已读时间") String readAt,
        @Schema(description = "动作文案") String actionText,
        @Schema(description = "动作目标") String actionTarget,
        @Schema(description = "动作类型") String actionType,
        @Schema(description = "动作载荷") NotificationActionPayloadView actionPayload
    ) {
    }

    @Schema(name = "MyNotificationActionPayloadView", description = "通知动作载荷")
    public record NotificationActionPayloadView(
        @Schema(description = "路由查询参数") Map<String, String> query
    ) {
    }

    @Schema(name = "MyNotificationUnreadCountView", description = "个人未读通知数量")
    public record UnreadCountView(
        @Schema(description = "未读数量") long unreadCount
    ) {
    }

    @Schema(name = "MyNotificationPreferenceView", description = "个人通知提醒偏好")
    public record NotificationPreferenceView(
        @Schema(description = "账户与密码提醒开关") boolean accountPassword,
        @Schema(description = "系统消息提醒开关") boolean systemMessage,
        @Schema(description = "待办任务提醒开关") boolean todoTask
    ) {
    }
}
