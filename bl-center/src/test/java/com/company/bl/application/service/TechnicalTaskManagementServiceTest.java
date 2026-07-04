package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.notification.application.WorkflowNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalTaskManagementServiceTest {

    @Mock
    private TechnicalWorkflowRepository technicalWorkflowRepository;

    @Mock
    private TechnicalWorkflowSupport technicalWorkflowSupport;

    @Mock
    private TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy;

    @Mock
    private WorkflowNotificationService workflowNotificationService;

    private TechnicalTaskManagementService service;

    @BeforeEach
    void setUp() {
        service = new TechnicalTaskManagementService(
            technicalWorkflowRepository,
            technicalWorkflowSupport,
            technicalTaskTimeoutPolicy,
            workflowNotificationService);
        lenient().when(technicalTaskTimeoutPolicy.snapshot(any())).thenReturn(new TechnicalTaskTimeoutPolicy.TimeoutSnapshot(
            LocalDateTime.of(2026, 7, 4, 12, 0),
            Map.of()));
        lenient().when(technicalTaskTimeoutPolicy.evaluate(any(), any()))
            .thenReturn(new TechnicalTaskTimeoutPolicy.TimeoutEvaluation(null, null, false));
    }

    @Test
    void assignShouldRejectCrossNodeWorkstationRole() {
        stubTask(task("TASK-1", TechnicalWorkflowConstants.NODE_GROSSING, "USER-M3-GROSSING", "Grossing User"));

        assertThatThrownBy(() -> service.assignTechnicalTask(new TechnicalWorkflowModels.TechnicalTaskAssignCommand(
            "TASK-1",
            "PRIORITY",
            "G-01",
            "Grossing Station",
            "USER-M3-GROSSING",
            "Grossing User",
            LocalDateTime.of(2026, 7, 5, 12, 0),
            "dispatch",
            "USER-M3-DEHYDRATION",
            "Dehydration User",
            "M3_DEHYDRATION",
            "TERM-1")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("role")
            .extracting("errorCode")
            .isEqualTo(BlErrorCode.PERMISSION_DENIED);
    }

    @Test
    void claimShouldRejectClaimingForAnotherUser() {
        stubTask(task("TASK-1", TechnicalWorkflowConstants.NODE_GROSSING, null, null));

        assertThatThrownBy(() -> service.claimTechnicalTask(new TechnicalWorkflowModels.TechnicalTaskClaimCommand(
            "TASK-1",
            "USER-OTHER",
            "Other User",
            "G-01",
            "Grossing Station",
            "USER-M3-GROSSING",
            "Grossing User",
            "M3_GROSSING",
            "TERM-2",
            "claim")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("same operator")
            .extracting("errorCode")
            .isEqualTo(BlErrorCode.PERMISSION_DENIED);
    }

    @Test
    void claimShouldRejectTaskOwnedByAnotherUser() {
        stubTask(task("TASK-1", TechnicalWorkflowConstants.NODE_GROSSING, "USER-OTHER", "Other User"));

        assertThatThrownBy(() -> service.claimTechnicalTask(new TechnicalWorkflowModels.TechnicalTaskClaimCommand(
            "TASK-1",
            "USER-M3-GROSSING",
            "Grossing User",
            "G-01",
            "Grossing Station",
            "USER-M3-GROSSING",
            "Grossing User",
            "M3_GROSSING",
            "TERM-3",
            "claim")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already assigned")
            .extracting("errorCode")
            .isEqualTo(BlErrorCode.OPERATION_NOT_ALLOWED);
    }

    @Test
    void releaseShouldRejectNonAssigneeWorkstationUser() {
        stubTask(task("TASK-1", TechnicalWorkflowConstants.NODE_GROSSING, "USER-OTHER", "Other User"));

        assertThatThrownBy(() -> service.releaseTechnicalTask(new TechnicalWorkflowModels.TechnicalTaskReleaseCommand(
            "TASK-1",
            "USER-M3-GROSSING",
            "Grossing User",
            "M3_GROSSING",
            "TERM-4",
            "release")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("own assigned task")
            .extracting("errorCode")
            .isEqualTo(BlErrorCode.PERMISSION_DENIED);
    }

    @Test
    void releaseShouldRejectAlreadyUnassignedTask() {
        stubTask(task("TASK-1", TechnicalWorkflowConstants.NODE_GROSSING, null, null));

        assertThatThrownBy(() -> service.releaseTechnicalTask(new TechnicalWorkflowModels.TechnicalTaskReleaseCommand(
            "TASK-1",
            "USER-M3-GROSSING",
            "Grossing User",
            "M3_GROSSING",
            "TERM-4A",
            "release")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already unassigned")
            .extracting("errorCode")
            .isEqualTo(BlErrorCode.OPERATION_NOT_ALLOWED);
    }

    @Test
    void priorityShouldRejectWorkstationRoleWithoutAdminOverride() {
        stubTask(task("TASK-1", TechnicalWorkflowConstants.NODE_GROSSING, "USER-M3-GROSSING", "Grossing User"));

        assertThatThrownBy(() -> service.updateTechnicalTaskPriority(new TechnicalWorkflowModels.TechnicalTaskPriorityCommand(
            "TASK-1",
            "STAT",
            "rush",
            "USER-M3-GROSSING",
            "Grossing User",
            "M3_GROSSING",
            "TERM-5")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("priority")
            .extracting("errorCode")
            .isEqualTo(BlErrorCode.PERMISSION_DENIED);
    }

    @Test
    void remarksShouldRejectUserWhoDoesNotOwnTask() {
        stubTask(task("TASK-1", TechnicalWorkflowConstants.NODE_GROSSING, "USER-OTHER", "Other User"));

        assertThatThrownBy(() -> service.updateTechnicalTaskRemarks(new TechnicalWorkflowModels.TechnicalTaskRemarksCommand(
            "TASK-1",
            "executor remark",
            null,
            "USER-M3-GROSSING",
            "Grossing User",
            "M3_GROSSING",
            "TERM-6")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("own assigned task")
            .extracting("errorCode")
            .isEqualTo(BlErrorCode.PERMISSION_DENIED);
    }

    private void stubTask(TechnicalWorkflowRecords.TechnicalTask task) {
        when(technicalWorkflowRepository.findTechnicalTaskById(task.id())).thenReturn(Optional.of(task));
    }

    private TechnicalWorkflowRecords.TechnicalTask task(
        String taskId,
        String currentNode,
        String assignedToUserId,
        String assignedToName
    ) {
        return new TechnicalWorkflowRecords.TechnicalTask(
            taskId,
            "APP-1",
            "APP-NO-1",
            "患者甲",
            "PATIENT-1",
            "08305",
            "CASE-1",
            "PATH-1",
            "SPECIMEN-1",
            currentNode,
            TechnicalWorkflowConstants.TASK_PENDING,
            TechnicalWorkflowConstants.OBJECT_CASE,
            "OBJ-1",
            "OBJ-1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "NORMAL",
            currentNode,
            "ST-1",
            "Station",
            assignedToUserId,
            assignedToName,
            null,
            null,
            LocalDateTime.of(2026, 7, 4, 8, 0),
            null,
            null,
            LocalDateTime.of(2026, 7, 4, 7, 30),
            null,
            null);
    }
}
