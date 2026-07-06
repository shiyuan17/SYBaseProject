package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.integration.application.BillingManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalOrderWorkflowServiceTest {

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private DiagnosticReportSupport diagnosticReportSupport;

    @Mock
    private BillingManagementService billingManagementService;

    @Mock
    private TechnicalWorkflowRepository technicalWorkflowRepository;

    @Test
    void changeMedicalOrderBlockShouldRejectConcurrentSnapshotUpdate() {
        MedicalOrderWorkflowService service = service();
        MedicalOrderRepository.MedicalOrder order = pendingBlockOrder();
        DiagnosticReportModels.ChangeMedicalOrderBlockCommand command =
            new DiagnosticReportModels.ChangeMedicalOrderBlockCommand(
                order.id(),
                "A3",
                "doctor-1",
                "Doctor One",
                "M4-TEST-01",
                "change block");

        when(medicalOrderRepository.findMedicalOrderById(order.id()))
            .thenReturn(Optional.of(order));
        when(diagnosticReportSupport.getLatestDiagnosticTask(order.caseId()))
            .thenReturn(diagnosticTask(order.caseId(), "doctor-1"));
        when(diagnosticReportSupport.getCase(order.caseId()))
            .thenReturn(new PathologyCase(order.caseId(), "APP-1", "BC-1", "IN_PROGRESS", null, null, null, null, null, null, null));
        when(technicalWorkflowRepository.findSamplingBlocksByCaseId(order.caseId()))
            .thenReturn(List.of(new TechnicalWorkflowRecords.SamplingBlock(
                "BLOCK-FORMAL-2",
                order.caseId(),
                order.targetSpecimenId(),
                "SAMPLING-1",
                2,
                "A3",
                "site",
                "desc",
                "BOX-2",
                "Box 2",
                null,
                null,
                "Specimen",
                null)));
        when(technicalWorkflowRepository.findSpecimenById(order.targetSpecimenId()))
            .thenReturn(Optional.empty());
        when(medicalOrderRepository.updateMedicalOrderTargetSnapshot(any()))
            .thenReturn(0);

        assertThatThrownBy(() -> service.changeMedicalOrderBlock(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("concurrently")
            .satisfies(ex -> {
                BlBusinessException businessException = (BlBusinessException) ex;
                assert businessException.getErrorCode() == BlErrorCode.RESOURCE_CONFLICT;
            });
    }

    @Test
    void changeMedicalOrderBlockShouldReuseExistingMedicalOrderOnlyBlockWhenInsertRaces() {
        MedicalOrderWorkflowService service = service();
        MedicalOrderRepository.MedicalOrder order = pendingBlockOrder();
        DiagnosticReportModels.ChangeMedicalOrderBlockCommand command =
            new DiagnosticReportModels.ChangeMedicalOrderBlockCommand(
                order.id(),
                "A3",
                "doctor-1",
                "Doctor One",
                "M4-TEST-02",
                "change block");
        MedicalOrderRepository.MedicalOrderBlock existingBlock =
            new MedicalOrderRepository.MedicalOrderBlock("MOB-EXISTING", order.caseId(), "A3", "doctor-2", "Doctor Two", LocalDateTime.now());
        MedicalOrderRepository.MedicalOrder updatedOrder = updatedOrder(order, existingBlock);

        when(medicalOrderRepository.findMedicalOrderById(order.id()))
            .thenReturn(Optional.of(order), Optional.of(updatedOrder));
        when(diagnosticReportSupport.getLatestDiagnosticTask(order.caseId()))
            .thenReturn(diagnosticTask(order.caseId(), "doctor-1"));
        when(diagnosticReportSupport.getCase(order.caseId()))
            .thenReturn(new PathologyCase(order.caseId(), "APP-1", "BC-1", "IN_PROGRESS", null, null, null, null, null, null, null));
        when(technicalWorkflowRepository.findSamplingBlocksByCaseId(order.caseId()))
            .thenReturn(List.of());
        when(medicalOrderRepository.findMedicalOrderBlockByCaseIdAndBlockNo(order.caseId(), "A3"))
            .thenReturn(Optional.empty(), Optional.of(existingBlock));
        when(technicalWorkflowRepository.findSpecimenById(order.targetSpecimenId()))
            .thenReturn(Optional.empty());
        when(medicalOrderRepository.updateMedicalOrderTargetSnapshot(any()))
            .thenReturn(1);
        doThrow(new DuplicateKeyException("duplicate"))
            .when(medicalOrderRepository)
            .insertMedicalOrderBlock(any());

        service.changeMedicalOrderBlock(command);

        verify(medicalOrderRepository).insertMedicalOrderBlock(any());
        verify(medicalOrderRepository).updateMedicalOrderTargetSnapshot(any());
        verify(diagnosticReportSupport).insertWorkflowEvent(
            eq(order.caseId()),
            eq("MEDICAL_ORDER_CHANGE_BLOCK"),
            eq("CHANGE_BLOCK"),
            eq("SUCCESS"),
            eq(command.operatorUserId()),
            eq(command.operatorName()),
            eq(command.terminalCode()),
            eq("A3"));
    }

    private MedicalOrderWorkflowService service() {
        return new MedicalOrderWorkflowService(
            medicalOrderRepository,
            diagnosticReportSupport,
            billingManagementService,
            technicalWorkflowRepository);
    }

    private static DiagnosticReportRepository.DiagnosticTask diagnosticTask(String caseId, String doctorUserId) {
        return new DiagnosticReportRepository.DiagnosticTask(
            "TASK-1",
            "APP-1",
            "APPNO-1",
            "Patient",
            "PAT-1",
            "08305",
            caseId,
            "BC-1",
            "SPEC-1",
            "PRIMARY",
            "HE",
            1,
            "Dept",
            "Specimen",
            "PRIMARY",
            "IN_PROGRESS",
            null,
            null,
            null,
            null,
            doctorUserId,
            "Doctor One",
            doctorUserId,
            "Doctor One",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "remarks",
            LocalDateTime.now());
    }

    private static MedicalOrderRepository.MedicalOrder pendingBlockOrder() {
        LocalDateTime now = LocalDateTime.now();
        return new MedicalOrderRepository.MedicalOrder(
            "MO-1",
            "CASE-1",
            "BC-1",
            "APPNO-1",
            "IP-1",
            null,
            null,
            false,
            List.<String>of(),
            "Patient",
            "PAT-1",
            "08305",
            "Dept",
            "MO-NO-1",
            "change block target",
            "SPECIAL",
            null,
            null,
            null,
            null,
            null,
            null,
            DiagnosticReportConstants.ORDER_SCOPE_TECHNICAL,
            DiagnosticReportConstants.ORDER_BILLING_PENDING,
            DiagnosticReportConstants.ORDER_PENDING,
            "doctor-1",
            "Doctor One",
            null,
            null,
            now,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DiagnosticReportConstants.ORDER_TARGET_BLOCK,
            "SPEC-1",
            "SP-1",
            "BLOCK-1",
            "A1",
            null,
            null,
            "remarks",
            now,
            now);
    }

    private static MedicalOrderRepository.MedicalOrder updatedOrder(MedicalOrderRepository.MedicalOrder order,
                                                                    MedicalOrderRepository.MedicalOrderBlock existingBlock) {
        return new MedicalOrderRepository.MedicalOrder(
            order.id(),
            order.caseId(),
            order.pathologyNo(),
            order.applicationNo(),
            order.inpatientNo(),
            order.slicingTaskId(),
            order.slicingPrintGroupId(),
            order.slicingMergedPrintGroup(),
            order.slicingTaskIds(),
            order.patientName(),
            order.patientId(),
            order.patientIdDisplay(),
            order.submittingDepartmentName(),
            order.orderNumber(),
            order.orderContent(),
            order.orderType(),
            order.orderItemId(),
            order.orderItemCode(),
            order.orderItemName(),
            order.orderCategoryId(),
            order.orderCategoryCode(),
            order.orderCategoryName(),
            order.executionScope(),
            order.billingStatus(),
            order.status(),
            order.doctorUserId(),
            order.doctorName(),
            order.executorUserId(),
            order.executorName(),
            order.orderDate(),
            order.acceptedAt(),
            order.printedByUserId(),
            order.printedByName(),
            order.printedAt(),
            order.releasedByUserId(),
            order.releasedByName(),
            order.releasedAt(),
            order.completedAt(),
            order.cancelledAt(),
            order.terminatedByUserId(),
            order.terminatedByName(),
            order.terminatedAt(),
            order.terminationReasonCode(),
            order.terminationReasonLabel(),
            order.targetType(),
            order.targetSpecimenId(),
            order.targetSpecimenNo(),
            existingBlock.id(),
            existingBlock.blockNo(),
            order.targetSlideId(),
            order.targetSlideNo(),
            "change block",
            order.createdAt(),
            order.updatedAt());
    }
}
