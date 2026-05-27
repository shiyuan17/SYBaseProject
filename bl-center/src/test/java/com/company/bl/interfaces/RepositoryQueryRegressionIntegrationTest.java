package com.company.bl.interfaces;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.notification.infrastructure.NotificationCenterJdbcRepository;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = com.company.bl.BlCenterApplication.class)
class RepositoryQueryRegressionIntegrationTest extends AbstractDiagnosticWorkflowIntegrationTest {

    @Autowired
    private SystemJdbcRepository systemJdbcRepository;

    @Autowired
    private NotificationCenterJdbcRepository notificationCenterJdbcRepository;

    @Autowired
    private SpecimenWorkflowRepository specimenWorkflowRepository;

    @Autowired
    private TechnicalWorkflowRepository technicalWorkflowRepository;

    @Autowired
    private DiagnosticReportRepository diagnosticReportRepository;

    @Test
    void shouldQueryUserLoginLogsAndRoleAuthorizationsThroughRepositories() {
        LocalDateTime now = LocalDateTime.now();
        systemJdbcRepository.insertUserLoginLog(new SystemJdbcRepository.CreateUserLoginLogRow(
            "ULL-REPO-001-" + System.nanoTime(),
            USER_M1_ADMIN,
            userLoginName(USER_M1_ADMIN),
            "SUCCESS",
            "127.0.0.1",
            "repo-test",
            now.minusMinutes(2),
            null,
            null,
            "older"));
        systemJdbcRepository.insertUserLoginLog(new SystemJdbcRepository.CreateUserLoginLogRow(
            "ULL-REPO-002-" + System.nanoTime(),
            USER_M1_ADMIN,
            userLoginName(USER_M1_ADMIN),
            "SUCCESS",
            "127.0.0.1",
            "repo-test",
            now.minusMinutes(1),
            null,
            null,
            "newer"));

        SystemJdbcRepository.PagedUserLoginLogs pagedLogs = systemJdbcRepository.findUserLoginLogs(USER_M1_ADMIN, 1, 2);
        assertThat(pagedLogs.total()).isGreaterThanOrEqualTo(2);
        assertThat(pagedLogs.logs()).hasSize(2);
        assertThat(pagedLogs.logs().get(0).loginAt()).isAfterOrEqualTo(pagedLogs.logs().get(1).loginAt());

        long assignmentCount = systemJdbcRepository.countRoleAssignments("ROLE_PATHOLOGY_ADMIN");
        assertThat(assignmentCount).isGreaterThan(0);

        List<SystemJdbcRepository.RoleAssignmentRow> roleAssignments =
            systemJdbcRepository.findRoleAssignments("ROLE_PATHOLOGY_ADMIN");
        assertThat(roleAssignments).isNotEmpty();
        assertThat(roleAssignments).anyMatch(item -> USER_M1_ADMIN.equals(item.userId()));

        SystemJdbcRepository.RoleAuthorizationRow authorization =
            systemJdbcRepository.findRoleAuthorization("ROLE_PATHOLOGY_ADMIN");
        assertThat(authorization.menuIds()).isNotEmpty();
        assertThat(authorization.permissionIds()).isNotEmpty();
        assertThat(authorization.topicIds()).isNotEmpty();
        assertThat(authorization.statScopes()).isNotEmpty();

        assertThat(systemJdbcRepository.findUserRoleAssignments(List.of(USER_M1_ADMIN)))
            .containsKey(USER_M1_ADMIN);
    }

    @Test
    void shouldQueryAuthorizedNotificationsThroughRepository() {
        resetNotificationPreferences(USER_M1_ADMIN, true, true, true);
        insertNotification(
            "NOTIFY_REPO_UNREAD_" + System.nanoTime(),
            USER_M1_ADMIN,
            "REPORT_REVISION",
            "SYSTEM_MESSAGE",
            "UNREAD",
            "Repository Query Alpha");
        insertNotification(
            "NOTIFY_REPO_READ_" + System.nanoTime(),
            USER_M1_ADMIN,
            "CRITICAL_VALUE",
            "TODO_TASK",
            "READ",
            "Repository Query Beta");

        NotificationCenterJdbcRepository.PreferenceRow preference =
            notificationCenterJdbcRepository.findPreference(USER_M1_ADMIN);
        assertThat(preference).isNotNull();

        var topicCodes = notificationCenterJdbcRepository.findAuthorizedTopicCodes(USER_M1_ADMIN);
        assertThat(topicCodes).contains("REPORT_REVISION", "CRITICAL_VALUE");

        NotificationCenterJdbcRepository.PagedNotifications unreadPage =
            notificationCenterJdbcRepository.findNotifications(
                USER_M1_ADMIN,
                1,
                10,
                "UNREAD",
                null,
                "alpha",
                topicCodes,
                preference
            );
        assertThat(unreadPage.total()).isGreaterThanOrEqualTo(1);
        assertThat(unreadPage.items()).isNotEmpty();
        assertThat(unreadPage.items()).allMatch(item -> USER_M1_ADMIN.equals(item.userId()));
        assertThat(unreadPage.items()).allMatch(item -> "UNREAD".equals(item.status()));
        assertThat(unreadPage.items()).anyMatch(item -> item.title().contains("Alpha"));

        long unreadCount = notificationCenterJdbcRepository.countUnreadNotifications(USER_M1_ADMIN, topicCodes, preference);
        assertThat(unreadCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldQueryPendingSpecimenAndTrackingDataThroughRepository() throws Exception {
        String applicationId = createApplication("APP-REPO-PENDING-" + System.nanoTime());
        String barcode = "BC-REPO-PENDING-" + System.nanoTime();
        var registration = registerSpecimens(applicationId, USER_REGISTER, "P-REPO-01", "/api/v1/specimens/register", barcode);
        String specimenId = registration.path("specimens").get(0).path("id").asText();

        SpecimenWorkflowRepository.PagedPendingSpecimens pendingFixations =
            specimenWorkflowRepository.findPendingFixations(
                new SpecimenWorkflowRepository.PendingSpecimenQuery(1, 10, applicationId, null, null, null, null, null)
            );
        assertThat(pendingFixations.total()).isGreaterThanOrEqualTo(1);
        assertThat(pendingFixations.items()).anyMatch(item -> specimenId.equals(item.specimenId()));

        completeFixation(barcode);
        String transportOrderId = createTransportOrder(applicationId, barcode).path("id").asText();

        postJson("/api/v1/transport-orders/%s/handover".formatted(transportOrderId), USER_TRANSPORT, """
            {
              "receiverUserName": "receiver-repo",
              "terminalCode": "T-REPO"
            }
            """);

        SpecimenWorkflowRepository.PagedPendingSpecimens pendingReceipts =
            specimenWorkflowRepository.findPendingReceipts(
                new SpecimenWorkflowRepository.PendingSpecimenQuery(1, 10, applicationId, null, null, null, null, null)
            );
        assertThat(pendingReceipts.total()).isGreaterThanOrEqualTo(1);
        assertThat(pendingReceipts.items()).anyMatch(item -> specimenId.equals(item.specimenId()));

        assertThat(specimenWorkflowRepository.findTransportOrderSpecimenBarcodes(transportOrderId))
            .contains(barcode);

        List<TrackingEvent> applicationEvents = specimenWorkflowRepository.findTrackingEventsByApplicationId(applicationId);
        assertThat(applicationEvents).isNotEmpty();
        assertThat(applicationEvents).isSortedAccordingTo((left, right) -> left.eventTime().compareTo(right.eventTime()));
    }

    @Test
    void shouldQueryDiagnosticAndTechnicalWorkflowAggregatesThroughRepositories() throws Exception {
        PublishedReportContext context = preparePublishedReportContext(
            "APP-REPO-DIAG-" + System.nanoTime(),
            "BC-REPO-DIAG-" + System.nanoTime()
        );

        List<TrackingEvent> caseEvents = technicalWorkflowRepository.findTrackingEventsByCaseId(context.caseId());
        assertThat(caseEvents).isNotEmpty();
        assertThat(caseEvents).isSortedAccordingTo((left, right) -> left.eventTime().compareTo(right.eventTime()));

        List<TrackingEvent> recentEvents = technicalWorkflowRepository.findRecentTrackingEventsByCaseId(context.caseId(), 2);
        assertThat(recentEvents).hasSizeLessThanOrEqualTo(2);
        assertThat(recentEvents).isSortedAccordingTo((left, right) -> right.eventTime().compareTo(left.eventTime()));

        DiagnosticReportRepository.PathologyReport currentReport =
            diagnosticReportRepository.findCurrentReportByCaseIdAndScope(context.caseId(), "ROUTINE").orElseThrow();
        assertThat(currentReport.id()).isEqualTo(context.reportId());
        assertThat(currentReport.reportNo()).isEqualTo(context.reportNo());
        assertThat(currentReport.reportStatus()).isEqualTo("PUBLISHED");

        assertThat(diagnosticReportRepository.findReportVersionsByCaseId(context.caseId()))
            .hasSizeGreaterThanOrEqualTo(2);

        DiagnosticReportRepository.PagedDiagnosticTasks diagnosticTasks =
            diagnosticReportRepository.findDiagnosticTasks(
                new DiagnosticReportRepository.PendingDiagnosticTaskQuery(
                    1,
                    20,
                    null,
                    null,
                    context.pathologyNo(),
                    USER_M4_DIAGNOSIS,
                    "M4_DIAGNOSIS"
                )
            );
        assertThat(diagnosticTasks.total()).isGreaterThanOrEqualTo(1);
        assertThat(diagnosticTasks.items()).anyMatch(item -> context.pathologyNo().equals(item.pathologyNo()));
    }

    private void resetNotificationPreferences(
        String userId,
        boolean accountPasswordEnabled,
        boolean systemMessageEnabled,
        boolean todoTaskEnabled
    ) {
        jdbcTemplate.update("""
            update user_notification_preferences
            set account_password_enabled = :accountPasswordEnabled,
                system_message_enabled = :systemMessageEnabled,
                todo_task_enabled = :todoTaskEnabled,
                updated_at = :updatedAt
            where user_id = :userId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("accountPasswordEnabled", accountPasswordEnabled ? 1 : 0)
            .addValue("systemMessageEnabled", systemMessageEnabled ? 1 : 0)
            .addValue("todoTaskEnabled", todoTaskEnabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    private void insertNotification(
        String id,
        String userId,
        String topicCode,
        String category,
        String status,
        String titleSuffix
    ) {
        jdbcTemplate.update("""
            insert into user_notifications
                (id, user_id, topic_code, category, level, title, content, summary,
                 avatar, action_type, action_target, action_payload_json, action_text,
                 status, read_at, archived_at, created_at)
            values
                (:id, :userId, :topicCode, :category, 'MEDIUM', :title, :content, :summary,
                 :avatar, 'ROUTE', '/notifications', '{"query":{"id":"repo"}}', '查看',
                 :status, null, null, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId)
            .addValue("topicCode", topicCode)
            .addValue("category", category)
            .addValue("title", titleSuffix)
            .addValue("content", "repository regression notification")
            .addValue("summary", "repository regression summary")
            .addValue("avatar", "https://avatar.vercel.sh/repo.svg?text=RQ")
            .addValue("status", status)
            .addValue("createdAt", LocalDateTime.now()));
    }
}
