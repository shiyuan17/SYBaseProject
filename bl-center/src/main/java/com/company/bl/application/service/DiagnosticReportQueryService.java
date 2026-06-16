package com.company.bl.application.service;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ArchiveRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.DiagnosticTrackingQueryRepository;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.ReportRevisionRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class DiagnosticReportQueryService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final DiagnosticTrackingQueryRepository diagnosticTrackingQueryRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final ArchiveRepository archiveRepository;

    DiagnosticReportQueryService(DiagnosticReportRepository diagnosticReportRepository,
                                 DiagnosticTrackingQueryRepository diagnosticTrackingQueryRepository,
                                 DiagnosticReportSupport diagnosticReportSupport,
                                 ArchiveRepository archiveRepository) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.diagnosticTrackingQueryRepository = diagnosticTrackingQueryRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.archiveRepository = archiveRepository;
    }

    @Transactional(readOnly = true)
    DiagnosticReportModels.PendingDiagnosticTaskPage listPendingTasks(DiagnosticReportModels.PendingDiagnosticTaskQuery query) {
        DiagnosticReportRepository.PagedDiagnosticTasks paged = diagnosticReportRepository.findDiagnosticTasks(
            new DiagnosticReportRepository.PendingDiagnosticTaskQuery(
                query.page(),
                query.size(),
                query.taskType(),
                query.taskStatus(),
                query.pathologyNo(),
                query.currentUserId(),
                query.currentRoleCode()));
        return new DiagnosticReportModels.PendingDiagnosticTaskPage(
            paged.items().stream().map(this::toTaskView).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    DiagnosticReportViews.DiagnosticWorkbenchView getDiagnosticWorkbench(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate =
            diagnosticTrackingQueryRepository.getDiagnosticWorkbench(caseId);
        Map<String, com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo =
            aggregate.embeddingBoxes().stream()
                .collect(Collectors.toMap(
                    com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox::embeddingBoxNo,
                    Function.identity(),
                    (left, right) -> left));
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId =
            indexObjectArchives(aggregate.embeddingBoxArchives());
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId =
            indexObjectArchives(aggregate.slideArchives());
        return new DiagnosticReportViews.DiagnosticWorkbenchView(
            aggregate.caseId(),
            aggregate.applicationNo(),
            aggregate.pathologyNo(),
            aggregate.caseStatus(),
            aggregate.patientName(),
            aggregate.patientId(),
            aggregate.patientGender(),
            aggregate.patientAge(),
            aggregate.applicationType(),
            aggregate.inpatientNo(),
            aggregate.outpatientNo(),
            aggregate.bedNo(),
            aggregate.phone(),
            aggregate.submittingDepartmentName(),
            aggregate.submittingDoctorName(),
            aggregate.clinicalDiagnosis(),
            aggregate.applicationRemarks(),
            archiveStatus(aggregate.applicationFormArchive()),
            archiveLocation(aggregate.applicationFormArchive()),
            archiveImageUrl(aggregate.applicationFormArchive()),
            aggregate.specimens().stream().map(item -> new DiagnosticReportViews.WorkbenchSpecimenSummary(
                item.id(), item.specimenNo(), item.barcode(), item.specimenNameStandardized(), item.specimenStatus().name())).toList(),
            aggregate.blocks().stream().map(item -> toBlockSummary(item, embeddingBoxesByNo, embeddingBoxArchiveByObjectId)).toList(),
            aggregate.slides().stream().map(item -> toSlideSummary(item, slideArchiveByObjectId)).toList(),
            aggregate.diagnosticTasks().stream().map(this::toTaskView).toList(),
            aggregate.currentReport() == null ? null : toReportView(aggregate.currentReport()),
            aggregate.recentEvents().stream().map(this::toTrackingEvent).toList(),
            aggregate.revisions().stream().map(this::toRevisionView).toList(),
            aggregate.medicalOrders().stream().map(this::toMedicalOrderView).toList(),
            aggregate.consultations().stream().map(this::toConsultationView).toList(),
            aggregate.historicalPathologies().stream().map(this::toHistoricalPathologyView).toList(),
            List.of(),
            buildReportTraces(aggregate),
            buildRemarkSections(aggregate),
            buildChargeItemViews(aggregate),
            aggregate.hasPendingRevision());
    }

    @Transactional(readOnly = true)
    DiagnosticReportViews.ReportTrackingView getReportTracking(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        DiagnosticTrackingQueryRepository.ReportTrackingAggregate aggregate =
            diagnosticTrackingQueryRepository.getReportTracking(caseId);
        return new DiagnosticReportViews.ReportTrackingView(
            aggregate.caseId(),
            aggregate.applicationNo(),
            aggregate.pathologyNo(),
            aggregate.caseStatus(),
            aggregate.patientName(),
            archiveStatus(aggregate.applicationFormArchive()),
            archiveLocation(aggregate.applicationFormArchive()),
            archiveImageUrl(aggregate.applicationFormArchive()),
            aggregate.diagnosticTasks().stream().map(this::toTaskView).toList(),
            aggregate.currentReport() == null ? null : toReportView(aggregate.currentReport()),
            aggregate.versions().stream().map(item -> new DiagnosticReportViews.ReportVersionView(
                item.id(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(),
                stringify(item.signedAt()), stringify(item.createdAt()), item.deliveryStatus(),
                stringify(item.issuedAt()), stringify(item.plannedIssueAt()))).toList(),
            aggregate.events().stream().map(this::toTrackingEvent).toList(),
            aggregate.revisions().stream().map(this::toRevisionView).toList(),
            aggregate.medicalOrders().stream().map(this::toMedicalOrderView).toList(),
            aggregate.consultations().stream().map(this::toConsultationView).toList(),
            aggregate.latestEffectiveVersionNo(),
            aggregate.currentDraftVersionNo(),
            aggregate.hasPendingRevision());
    }

    @Transactional(readOnly = true)
    DiagnosticReportViews.CaseLifecycleTrackingView getCaseLifecycleTracking(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate workbenchAggregate =
            diagnosticTrackingQueryRepository.getDiagnosticWorkbench(caseId);
        DiagnosticTrackingQueryRepository.ReportTrackingAggregate reportTrackingAggregate =
            diagnosticTrackingQueryRepository.getReportTracking(caseId);

        Map<String, ArchiveRepository.ObjectArchiveSummary> specimenArchiveByObjectId =
            indexObjectArchives(archiveRepository.findSpecimenArchiveSummaries(caseId));
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId =
            indexObjectArchives(workbenchAggregate.embeddingBoxArchives());
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId =
            indexObjectArchives(workbenchAggregate.slideArchives());
        Map<String, TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo =
            workbenchAggregate.embeddingBoxes().stream().collect(Collectors.toMap(
                TechnicalWorkflowRecords.EmbeddingBox::embeddingBoxNo,
                Function.identity(),
                (left, right) -> left));
        Map<String, List<TechnicalWorkflowRecords.SamplingBlock>> blocksBySpecimenId =
            workbenchAggregate.blocks().stream().collect(Collectors.groupingBy(
                TechnicalWorkflowRecords.SamplingBlock::specimenId));
        Map<String, List<TechnicalWorkflowProcessingRecords.Slide>> slidesByEmbeddingBoxId =
            workbenchAggregate.slides().stream()
                .filter(item -> item.embeddingBoxId() != null)
                .collect(Collectors.groupingBy(TechnicalWorkflowProcessingRecords.Slide::embeddingBoxId));

        List<DiagnosticReportViews.LifecycleSpecimenView> specimenViews = workbenchAggregate.specimens().stream()
            .map(specimen -> toLifecycleSpecimenView(
                specimen,
                specimenArchiveByObjectId.get(specimen.id()),
                blocksBySpecimenId.getOrDefault(specimen.id(), List.of()),
                embeddingBoxesByNo,
                embeddingBoxArchiveByObjectId,
                slidesByEmbeddingBoxId,
                slideArchiveByObjectId))
            .toList();

        return new DiagnosticReportViews.CaseLifecycleTrackingView(
            new DiagnosticReportViews.CaseSummaryView(
                workbenchAggregate.caseId(),
                workbenchAggregate.applicationNo(),
                workbenchAggregate.pathologyNo(),
                workbenchAggregate.caseStatus(),
                workbenchAggregate.patientName(),
                workbenchAggregate.patientGender(),
                workbenchAggregate.patientAge(),
                workbenchAggregate.applicationType(),
                workbenchAggregate.submittingDepartmentName(),
                workbenchAggregate.submittingDoctorName(),
                null,
                firstPresent(
                    reportTrackingAggregate.currentReport() == null ? null : reportTrackingAggregate.currentReport().reportStatus(),
                    workbenchAggregate.caseStatus()),
                workbenchAggregate.hasPendingRevision()),
            new DiagnosticReportViews.ApplicationFormView(
                archiveStatus(workbenchAggregate.applicationFormArchive()),
                archiveLocation(workbenchAggregate.applicationFormArchive()),
                archiveImageUrl(workbenchAggregate.applicationFormArchive()),
                workbenchAggregate.submittingDoctorName(),
                null,
                workbenchAggregate.applicationRemarks()),
            buildLifecycleStageGroups(workbenchAggregate, reportTrackingAggregate, specimenViews),
            specimenViews,
            new DiagnosticReportViews.ReportLifecycleView(
                reportTrackingAggregate.currentReport() == null ? null : toReportView(reportTrackingAggregate.currentReport()),
                reportTrackingAggregate.diagnosticTasks().stream().map(this::toTaskView).toList(),
                reportTrackingAggregate.versions().stream().map(item -> new DiagnosticReportViews.ReportVersionView(
                    item.id(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(),
                    stringify(item.signedAt()), stringify(item.createdAt()), item.deliveryStatus(),
                    stringify(item.issuedAt()), stringify(item.plannedIssueAt()))).toList(),
                reportTrackingAggregate.revisions().stream().map(this::toRevisionView).toList(),
                reportTrackingAggregate.consultations().stream().map(this::toConsultationView).toList(),
                reportTrackingAggregate.medicalOrders().stream().map(this::toMedicalOrderView).toList()));
    }

    @Transactional(readOnly = true)
    List<DiagnosticReportModels.FormalReportVersionView> listFormalReportVersions(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        return diagnosticReportRepository.findFormalReportVersionsByCaseId(caseId).stream()
            .map(item -> {
                DiagnosticReportRepository.PathologyReport report =
                    diagnosticReportRepository.findPathologyReportById(item.reportId()).orElse(null);
                return new DiagnosticReportModels.FormalReportVersionView(
                    item.id(),
                    item.reportId(),
                    report == null ? null : report.reportNo(),
                    item.versionNo(),
                    item.versionStatus(),
                    item.signedByName(),
                    stringify(item.signedAt()),
                    report == null ? null : stringify(report.publishedAt()),
                    item.printStatus(),
                    stringify(item.printedAt()),
                    item.deliveryStatus(),
                    stringify(item.plannedIssueAt()),
                    stringify(item.issuedAt()),
                    stringify(item.recalledAt()));
            })
            .toList();
    }

    @Transactional(readOnly = true)
    List<DiagnosticReportModels.CaseReportVersionView> listCaseReportVersions(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        Map<String, DiagnosticReportRepository.ReportVersion> formalVersionByReportAndStatus = diagnosticReportRepository
            .findFormalReportVersionsByCaseId(caseId)
            .stream()
            .collect(Collectors.toMap(
                item -> reportVersionKey(item.reportId(), item.versionStatus()),
                Function.identity(),
                (left, right) -> right));
        return diagnosticReportRepository.findPathologyReportsByCaseId(caseId).stream()
            .map(report -> {
                DiagnosticReportRepository.ReportVersion matchedVersion =
                    formalVersionByReportAndStatus.get(reportVersionKey(report.id(), report.reportStatus()));
                return new DiagnosticReportModels.CaseReportVersionView(
                    matchedVersion == null ? report.id() : matchedVersion.id(),
                    report.id(),
                    report.reportNo(),
                    report.versionNo(),
                    report.reportStatus(),
                    report.signedByName(),
                    stringify(report.submittedAt()),
                    stringify(report.reviewedAt()),
                    stringify(report.signedAt()),
                    stringify(report.publishedAt()),
                    matchedVersion == null ? null : matchedVersion.printStatus(),
                    matchedVersion == null ? null : stringify(matchedVersion.printedAt()),
                    matchedVersion == null ? null : matchedVersion.deliveryStatus(),
                    matchedVersion == null ? null : stringify(matchedVersion.plannedIssueAt()),
                    matchedVersion == null ? null : stringify(matchedVersion.issuedAt()),
                    matchedVersion == null ? null : stringify(matchedVersion.recalledAt()));
            })
            .toList();
    }

    private DiagnosticReportModels.TaskView toTaskView(DiagnosticReportRepository.DiagnosticTask task) {
        return new DiagnosticReportModels.TaskView(
            task.id(),
            task.applicationId(),
            task.applicationNo(),
            task.patientName(),
            task.caseId(),
            task.pathologyNo(),
            task.taskType(),
            task.status(),
            task.diagnosisDoctorUserId(),
            task.diagnosisDoctorName(),
            task.primaryDoctorUserId(),
            task.primaryDoctorName(),
            task.reviewerUserId(),
            task.reviewerName(),
            stringify(task.assignedAt()),
            stringify(task.acceptedAt()),
            stringify(task.completedAt()),
            task.remarks());
    }

    private DiagnosticReportViews.PathologyReportView toReportView(DiagnosticReportRepository.PathologyReport report) {
        return new DiagnosticReportViews.PathologyReportView(
            report.id(),
            report.reportNo(),
            report.reportStatus(),
            report.clinicalDiagnosis(),
            report.grossExam(),
            report.microscopicExam(),
            report.finalDiagnosis(),
            report.richTextContent(),
            stringify(report.submittedAt()),
            stringify(report.reviewedAt()),
            stringify(report.signedAt()),
            stringify(report.publishedAt()),
            report.reviewerName(),
            report.signedByName(),
            null,
            null,
            null,
            report.versionNo());
    }

    private DiagnosticReportViews.TrackingEventView toTrackingEvent(TrackingEvent event) {
        return new DiagnosticReportViews.TrackingEventView(
            event.nodeCode(),
            event.eventType(),
            event.eventStatus(),
            stringify(event.eventTime()),
            event.operatorName(),
            event.eventContent());
    }

    private DiagnosticReportViews.RevisionRequestView toRevisionView(ReportRevisionRepository.ReportRevisionRequest request) {
        return new DiagnosticReportViews.RevisionRequestView(
            request.id(),
            request.reportId(),
            request.currentVersionNo(),
            request.requestStatus(),
            request.requestReason(),
            request.requestedByName(),
            stringify(request.requestedAt()),
            request.reviewedByName(),
            stringify(request.reviewedAt()),
            request.rejectReason(),
            request.approvedVersionNo());
    }

    private DiagnosticReportViews.MedicalOrderView toMedicalOrderView(MedicalOrderRepository.MedicalOrder order) {
        return new DiagnosticReportViews.MedicalOrderView(
            order.id(),
            order.caseId(),
            order.pathologyNo(),
            order.applicationNo(),
            order.patientName(),
            order.orderNumber(),
            order.orderType(),
            order.orderContent(),
            order.orderItemId(),
            order.orderItemCode(),
            order.orderItemName(),
            order.orderCategoryId(),
            order.orderCategoryCode(),
            order.orderCategoryName(),
            order.executionScope(),
            order.billingStatus(),
            order.status(),
            order.doctorName(),
            order.executorName(),
            stringify(order.orderDate()),
            stringify(order.acceptedAt()),
            stringify(order.completedAt()),
            stringify(order.cancelledAt()),
            order.remarks());
    }

    private DiagnosticReportViews.ConsultationView toConsultationView(DiagnosticTrackingQueryRepository.ConsultationView consultation) {
        return new DiagnosticReportViews.ConsultationView(
            consultation.consultationCase().id(),
            consultation.consultationCase().consultationType(),
            consultation.consultationCase().status(),
            consultation.consultationCase().requestedByName(),
            stringify(consultation.consultationCase().requestedAt()),
            consultation.consultationCase().hostName(),
            stringify(consultation.consultationCase().completedAt()),
            consultation.consultationCase().opinion(),
            consultation.participants().size(),
            consultation.participants().stream()
                .map(item -> new DiagnosticReportViews.ConsultationParticipantView(
                    item.id(),
                    item.participantUserId(),
                    item.participantName(),
                    item.participantRole(),
                    item.opinion(),
                    item.draftedByName(),
                    stringify(item.commentedAt())))
                .toList());
    }

    private DiagnosticReportViews.HistoricalPathologyView toHistoricalPathologyView(
        DiagnosticTrackingQueryRepository.HistoricalPathology item
    ) {
        return new DiagnosticReportViews.HistoricalPathologyView(
            item.age(),
            item.inpatientNo(),
            item.examinationNo(),
            item.submissionType(),
            stringify(item.reportTime()),
            item.diagnosis());
    }

    private DiagnosticReportViews.ChargeItemView toChargeItemView(
        DiagnosticTrackingQueryRepository.ChargeItem item
    ) {
        return new DiagnosticReportViews.ChargeItemView(
            item.itemName(),
            stringify(item.chargedAt()),
            item.chargedByName());
    }

    private List<DiagnosticReportViews.ChargeItemView> buildChargeItemViews(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate
    ) {
        if (!aggregate.chargeItems().isEmpty()) {
            return aggregate.chargeItems().stream().map(this::toChargeItemView).toList();
        }
        return aggregate.medicalOrders().stream()
            .filter(order -> order.billingStatus() != null && !order.billingStatus().isBlank())
            .map(order -> new DiagnosticReportViews.ChargeItemView(
                firstPresent(order.orderContent(), order.orderNumber()),
                stringify(firstPresent(order.completedAt(), order.acceptedAt(), order.orderDate())),
                firstPresent(order.executorName(), order.doctorName())))
            .toList();
    }

    private List<DiagnosticReportViews.ReportTraceView> buildReportTraces(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate
    ) {
        List<DiagnosticReportViews.ReportTraceView> traces = new ArrayList<>();
        DiagnosticReportRepository.PathologyReport report = aggregate.currentReport();
        if (report != null) {
            traces.add(new DiagnosticReportViews.ReportTraceView(
                traces.size() + 1,
                firstPresent(report.signedByName(), report.reviewerName()),
                stringify(firstPresent(report.publishedAt(), report.signedAt(), report.reviewedAt(), report.submittedAt(), report.reportDate())),
                report.reportStatus(),
                report.finalDiagnosis()));
        }
        for (DiagnosticReportRepository.DiagnosticTask task : aggregate.diagnosticTasks()) {
            traces.add(new DiagnosticReportViews.ReportTraceView(
                traces.size() + 1,
                firstPresent(task.primaryDoctorName(), task.diagnosisDoctorName(), task.reviewerName()),
                stringify(firstPresent(task.completedAt(), task.reviewedAt(), task.primaryDiagnosedAt(), task.acceptedAt(), task.assignedAt())),
                task.status(),
                task.remarks()));
        }
        return traces;
    }

    private List<DiagnosticReportViews.RemarkSectionView> buildRemarkSections(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate
    ) {
        List<DiagnosticReportViews.RemarkSectionView> sections = new ArrayList<>();
        sections.add(new DiagnosticReportViews.RemarkSectionView(
            "APPLICATION",
            "申请备注",
            aggregate.applicationNo(),
            aggregate.applicationRemarks()));
        sections.add(new DiagnosticReportViews.RemarkSectionView(
            "GROSSING",
            "取材备注",
            aggregate.pathologyNo(),
            aggregate.blocks().stream()
                .map(DiagnosticReportQueryService::firstNonBlankBlockRemark)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null)));
        sections.add(new DiagnosticReportViews.RemarkSectionView(
            "DIAGNOSIS",
            "诊断备注",
            aggregate.pathologyNo(),
            aggregate.currentReport() == null ? null : aggregate.currentReport().remarks()));
        aggregate.medicalOrders().stream()
            .filter(order -> order.remarks() != null && !order.remarks().isBlank())
            .forEach(order -> sections.add(new DiagnosticReportViews.RemarkSectionView(
                "MEDICAL_ORDER",
                "医嘱备注",
                order.orderNumber(),
                order.remarks())));
        return sections;
    }

    private static String firstNonBlankBlockRemark(TechnicalWorkflowRecords.SamplingBlock block) {
        if (block.specialRequirement() != null && !block.specialRequirement().isBlank()) {
            return block.specialRequirement();
        }
        return block.grossDescription();
    }

    @SafeVarargs
    private static <T> T firstPresent(T... values) {
        for (T value : values) {
            if (value instanceof String text && text.isBlank()) {
                continue;
            }
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private DiagnosticReportViews.WorkbenchBlockSummary toBlockSummary(
        com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock block,
        Map<String, com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo,
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId
    ) {
        com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox embeddingBox = embeddingBoxesByNo.get(block.embeddingBoxNo());
        ArchiveRepository.ObjectArchiveSummary archiveSummary = embeddingBox == null ? null : embeddingBoxArchiveByObjectId.get(embeddingBox.id());
        return new DiagnosticReportViews.WorkbenchBlockSummary(
            block.id(),
            block.specimenId(),
            block.blockCode(),
            block.embeddingBoxNo(),
            block.blockDescription(),
            archiveStatus(archiveSummary),
            archiveLocation(archiveSummary),
            loanStatus(archiveSummary));
    }

    private DiagnosticReportViews.WorkbenchSlideSummary toSlideSummary(
        com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slide slide,
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId
    ) {
        ArchiveRepository.ObjectArchiveSummary archiveSummary = slideArchiveByObjectId.get(slide.id());
        return new DiagnosticReportViews.WorkbenchSlideSummary(
            slide.id(),
            slide.specimenId(),
            slide.embeddingBoxId(),
            slide.slideNo(),
            slide.slideStatus(),
            slide.qualityStatus(),
            archiveStatus(archiveSummary),
            archiveLocation(archiveSummary),
            loanStatus(archiveSummary));
    }

    private List<DiagnosticReportViews.LifecycleStageGroupView> buildLifecycleStageGroups(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate workbenchAggregate,
        DiagnosticTrackingQueryRepository.ReportTrackingAggregate reportTrackingAggregate,
        List<DiagnosticReportViews.LifecycleSpecimenView> specimenViews
    ) {
        List<DiagnosticReportViews.LifecycleNodeView> applicationNodes = List.of(
            buildLifecycleNode(
                "APPLICATION",
                "APPLICATION_CREATED",
                "申请创建",
                workbenchAggregate.applicationNo() == null ? "PENDING" : "COMPLETED",
                null,
                workbenchAggregate.submittingDoctorName(),
                List.of(
                    buildKeyFact("申请单号", workbenchAggregate.applicationNo()),
                    buildKeyFact("申请类型", workbenchAggregate.applicationType())),
                workbenchAggregate.applicationRemarks()));
        List<DiagnosticReportViews.LifecycleNodeView> specimenNodes = List.of(
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN",
                "标本",
                specimenViews.isEmpty() ? "PENDING" : "COMPLETED",
                null,
                null,
                List.of(
                    buildKeyFact("标本数", String.valueOf(specimenViews.size())),
                    buildKeyFact("当前状态", specimenViews.isEmpty() ? null : specimenViews.get(0).specimenStatus())),
                null));
        List<DiagnosticReportViews.LifecycleNodeView> technicalNodes = List.of(
            buildLifecycleNode(
                "TECHNICAL",
                "TECHNICAL_PROCESSING",
                "技术处理",
                workbenchAggregate.slides().isEmpty() ? "PENDING" : "COMPLETED",
                null,
                null,
                List.of(
                    buildKeyFact("蜡块数", String.valueOf(workbenchAggregate.blocks().size())),
                    buildKeyFact("玻片数", String.valueOf(workbenchAggregate.slides().size()))),
                null));
        List<DiagnosticReportViews.LifecycleNodeView> reportNodes = List.of(
            buildLifecycleNode(
                "REPORT",
                "DIAGNOSTIC_REPORT",
                "诊断报告",
                reportTrackingAggregate.currentReport() == null ? "PENDING" : reportTrackingAggregate.currentReport().reportStatus(),
                reportTrackingAggregate.currentReport() == null ? null : stringify(firstPresent(
                    reportTrackingAggregate.currentReport().publishedAt(),
                    reportTrackingAggregate.currentReport().signedAt(),
                    reportTrackingAggregate.currentReport().reviewedAt(),
                    reportTrackingAggregate.currentReport().submittedAt())),
                reportTrackingAggregate.currentReport() == null ? null
                    : firstPresent(reportTrackingAggregate.currentReport().signedByName(), reportTrackingAggregate.currentReport().reviewerName()),
                List.of(
                    buildKeyFact("报告号", reportTrackingAggregate.currentReport() == null ? null : reportTrackingAggregate.currentReport().reportNo()),
                    buildKeyFact("版本", reportTrackingAggregate.currentReport() == null
                        ? null
                        : String.valueOf(reportTrackingAggregate.currentReport().versionNo()))),
                reportTrackingAggregate.currentReport() == null ? null : reportTrackingAggregate.currentReport().finalDiagnosis()));
        List<DiagnosticReportViews.LifecycleNodeView> archiveNodes = List.of(
            buildLifecycleNode(
                "ARCHIVE",
                "ARCHIVE_AND_LOAN",
                "归档借阅",
                summarizeArchiveStageStatus(workbenchAggregate, specimenViews),
                null,
                null,
                List.of(
                    buildKeyFact("申请单归档", archiveStatus(workbenchAggregate.applicationFormArchive())),
                    buildKeyFact("玻片归档数", String.valueOf(workbenchAggregate.slideArchives().size()))),
                null));
        return List.of(
            new DiagnosticReportViews.LifecycleStageGroupView("APPLICATION", "申请创建", applicationNodes),
            new DiagnosticReportViews.LifecycleStageGroupView("SPECIMEN", "标本", specimenNodes),
            new DiagnosticReportViews.LifecycleStageGroupView("TECHNICAL", "技术处理", technicalNodes),
            new DiagnosticReportViews.LifecycleStageGroupView("REPORT", "诊断报告", reportNodes),
            new DiagnosticReportViews.LifecycleStageGroupView("ARCHIVE", "归档借阅", archiveNodes));
    }

    private DiagnosticReportViews.LifecycleSpecimenView toLifecycleSpecimenView(
        com.company.bl.domain.model.Specimen specimen,
        ArchiveRepository.ObjectArchiveSummary specimenArchive,
        List<TechnicalWorkflowRecords.SamplingBlock> blocks,
        Map<String, TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo,
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId,
        Map<String, List<TechnicalWorkflowProcessingRecords.Slide>> slidesByEmbeddingBoxId,
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId
    ) {
        List<DiagnosticReportViews.LifecycleNodeView> specimenEvents = List.of(
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_CREATED",
                "标本创建",
                specimen.registeredAt() == null ? "PENDING" : "COMPLETED",
                stringify(specimen.registeredAt()),
                specimen.registeredByName(),
                List.of(
                    buildKeyFact("标本编号", specimen.specimenNo()),
                    buildKeyFact("条码", specimen.barcode())),
                specimen.remarks()),
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_REMOVAL",
                "离体",
                specimen.specimenRemovalAt() == null ? "PENDING" : "COMPLETED",
                stringify(specimen.specimenRemovalAt()),
                specimen.specimenRemovalOperatorName(),
                List.of(
                    buildKeyFact("送检科室", specimen.applicantDepartmentName()),
                    buildKeyFact("送检医生", specimen.applicantDoctorName())),
                null),
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_RECEIPT",
                "确认/入库/签收",
                firstPresent(specimen.receiptStatus(), specimen.checkInStatus(), specimen.verificationStatus()),
                stringify(firstPresent(specimen.checkedInAt(), specimen.specimenConfirmedAt(), specimen.verificationCompletedAt())),
                firstPresent(specimen.checkedInByName(), specimen.verifiedByName()),
                List.of(
                    buildKeyFact("确认状态", specimen.verificationStatus()),
                    buildKeyFact("入库状态", specimen.checkInStatus()),
                    buildKeyFact("签收状态", specimen.receiptStatus())),
                null));
        List<DiagnosticReportViews.LifecycleBlockView> blockViews = blocks.stream()
            .map(block -> toLifecycleBlockView(
                block,
                embeddingBoxesByNo.get(block.embeddingBoxNo()),
                embeddingBoxArchiveByObjectId,
                slidesByEmbeddingBoxId,
                slideArchiveByObjectId))
            .toList();
        return new DiagnosticReportViews.LifecycleSpecimenView(
            specimen.id(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenNameStandardized(),
            specimen.specimenStatus() == null ? null : specimen.specimenStatus().name(),
            archiveStatus(specimenArchive),
            archiveLocation(specimenArchive),
            loanStatus(specimenArchive),
            stringify(specimen.registeredAt()),
            stringify(specimen.specimenRemovalAt()),
            null,
            stringify(specimen.specimenConfirmedAt()),
            stringify(specimen.checkedInAt()),
            specimen.receiptStatus(),
            null,
            null,
            specimenEvents,
            blockViews);
    }

    private DiagnosticReportViews.LifecycleBlockView toLifecycleBlockView(
        TechnicalWorkflowRecords.SamplingBlock block,
        TechnicalWorkflowRecords.EmbeddingBox embeddingBox,
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId,
        Map<String, List<TechnicalWorkflowProcessingRecords.Slide>> slidesByEmbeddingBoxId,
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId
    ) {
        ArchiveRepository.ObjectArchiveSummary blockArchive =
            embeddingBox == null ? null : embeddingBoxArchiveByObjectId.get(embeddingBox.id());
        List<DiagnosticReportViews.LifecycleNodeView> blockEvents = List.of(
            buildLifecycleNode(
                "TECHNICAL",
                "GROSSING",
                "取材",
                block.blockCode() == null ? "PENDING" : "COMPLETED",
                null,
                null,
                List.of(
                    buildKeyFact("蜡块号", block.blockCode()),
                    buildKeyFact("描述", firstPresent(block.blockDescription(), block.grossDescription()))),
                block.grossDescription()),
            buildLifecycleNode(
                "ARCHIVE",
                "BLOCK_ARCHIVE",
                "蜡块归档/借阅",
                archiveStatus(blockArchive),
                null,
                null,
                List.of(
                    buildKeyFact("归档状态", archiveStatus(blockArchive)),
                    buildKeyFact("归档位置", archiveLocation(blockArchive)),
                    buildKeyFact("借阅状态", loanStatus(blockArchive))),
                null));
        List<DiagnosticReportViews.LifecycleSlideView> slideViews = (embeddingBox == null
            ? List.<TechnicalWorkflowProcessingRecords.Slide>of()
            : slidesByEmbeddingBoxId.getOrDefault(embeddingBox.id(), List.of())).stream()
            .map(slide -> toLifecycleSlideView(slide, slideArchiveByObjectId.get(slide.id())))
            .toList();
        return new DiagnosticReportViews.LifecycleBlockView(
            block.id(),
            block.specimenId(),
            block.blockCode(),
            block.embeddingBoxNo(),
            block.blockDescription(),
            block.specimenName(),
            block.grossDescription(),
            archiveStatus(blockArchive),
            archiveLocation(blockArchive),
            loanStatus(blockArchive),
            null,
            null,
            null,
            null,
            null,
            embeddingBox == null ? null : embeddingBox.sliceNotice(),
            null,
            null,
            null,
            blockEvents,
            slideViews);
    }

    private DiagnosticReportViews.LifecycleSlideView toLifecycleSlideView(
        TechnicalWorkflowProcessingRecords.Slide slide,
        ArchiveRepository.ObjectArchiveSummary slideArchive
    ) {
        List<DiagnosticReportViews.LifecycleNodeView> slideEvents = List.of(
            buildLifecycleNode(
                "TECHNICAL",
                "SLICING",
                "切片",
                slide.slideStatus(),
                null,
                null,
                List.of(
                    buildKeyFact("玻片号", slide.slideNo()),
                    buildKeyFact("质控状态", slide.qualityStatus())),
                null),
            buildLifecycleNode(
                "ARCHIVE",
                "SLIDE_ARCHIVE",
                "玻片归档/借阅",
                archiveStatus(slideArchive),
                null,
                null,
                List.of(
                    buildKeyFact("归档状态", archiveStatus(slideArchive)),
                    buildKeyFact("归档位置", archiveLocation(slideArchive)),
                    buildKeyFact("借阅状态", loanStatus(slideArchive))),
                null));
        return new DiagnosticReportViews.LifecycleSlideView(
            slide.id(),
            slide.specimenId(),
            slide.embeddingBoxId(),
            slide.slideNo(),
            slide.slideStatus(),
            slide.qualityStatus(),
            archiveStatus(slideArchive),
            archiveLocation(slideArchive),
            loanStatus(slideArchive),
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
            slideEvents);
    }

    private DiagnosticReportViews.LifecycleNodeView buildLifecycleNode(
        String stageCode,
        String nodeCode,
        String title,
        String status,
        String occurredAt,
        String operatorName,
        List<DiagnosticReportViews.KeyFactView> keyFacts,
        String eventContent
    ) {
        return new DiagnosticReportViews.LifecycleNodeView(
            stageCode,
            nodeCode,
            title,
            normalizeLifecycleStatus(status),
            occurredAt,
            operatorName,
            keyFacts,
            eventContent);
    }

    private DiagnosticReportViews.KeyFactView buildKeyFact(String label, String value) {
        return new DiagnosticReportViews.KeyFactView(label, value);
    }

    private String reportVersionKey(String reportId, String versionStatus) {
        return reportId + "::" + (versionStatus == null ? "" : versionStatus);
    }

    private String normalizeLifecycleStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        return status;
    }

    private String summarizeArchiveStageStatus(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate workbenchAggregate,
        List<DiagnosticReportViews.LifecycleSpecimenView> specimenViews
    ) {
        if (workbenchAggregate.applicationFormArchive() != null
            || !workbenchAggregate.embeddingBoxArchives().isEmpty()
            || !workbenchAggregate.slideArchives().isEmpty()
            || specimenViews.stream().anyMatch(item -> item.archiveStatus() != null)) {
            return "IN_STORAGE";
        }
        return "PENDING";
    }

    private Map<String, ArchiveRepository.ObjectArchiveSummary> indexObjectArchives(List<ArchiveRepository.ObjectArchiveSummary> archives) {
        return archives.stream().collect(Collectors.toMap(
            ArchiveRepository.ObjectArchiveSummary::objectId,
            Function.identity(),
            (left, right) -> left));
    }

    private String archiveStatus(ArchiveRepository.ApplicationArchiveSummary summary) {
        return summary == null ? null : summary.archiveStatus();
    }

    private String archiveLocation(ArchiveRepository.ApplicationArchiveSummary summary) {
        return summary == null ? null : summary.archiveLocation();
    }

    private String archiveImageUrl(ArchiveRepository.ApplicationArchiveSummary summary) {
        return summary == null ? null : summary.imageUrl();
    }

    private String archiveStatus(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.archiveStatus();
    }

    private String archiveLocation(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.archiveLocation();
    }

    private String loanStatus(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.loanStatus();
    }

    private String stringify(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
