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
            aggregate.patientIdDisplay(),
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
                slideArchiveByObjectId,
                workbenchAggregate.recentEvents()))
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
            task.patientId(),
            task.patientIdDisplay(),
            task.caseId(),
            task.pathologyNo(),
            task.applicationType(),
            task.checkItem(),
            task.blockCount(),
            task.submittingDepartmentName(),
            task.specimenName(),
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
        List<DiagnosticReportViews.LifecycleNodeView> specimenNodes = specimenViews.stream()
            .flatMap(item -> item.specimenEvents().stream())
            .filter(item -> "SPECIMEN".equals(item.stageCode()))
            .toList();
        List<DiagnosticReportViews.LifecycleNodeView> technicalNodes = specimenViews.stream()
            .flatMap(specimen -> specimen.blocks().stream())
            .flatMap(block -> java.util.stream.Stream.concat(
                block.blockEvents().stream(),
                block.slides().stream().flatMap(slide -> slide.slideEvents().stream())))
            .filter(item -> "TECHNICAL".equals(item.stageCode()))
            .toList();
        DiagnosticReportRepository.PathologyReport currentReport = reportTrackingAggregate.currentReport();
        List<DiagnosticReportViews.LifecycleNodeView> reportNodes = new ArrayList<>();
        reportTrackingAggregate.diagnosticTasks().stream().findFirst().ifPresent(task -> reportNodes.add(
            buildLifecycleNode(
                "REPORT",
                "DIAGNOSIS_ASSIGNMENT",
                "诊断分配",
                task.status(),
                stringify(task.assignedAt()),
                firstPresent(task.primaryDoctorName(), task.reviewerName(), task.diagnosisDoctorName()),
                List.of(
                    buildKeyFact("初诊阅片人", task.primaryDoctorName()),
                    buildKeyFact("签发阅片人", task.reviewerName())),
                task.remarks())));
        if (currentReport != null) {
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "PRIMARY_READING",
                "初步阅片",
                currentReport.reportStatus(),
                stringify(currentReport.submittedAt()),
                null,
                List.of(
                    buildKeyFact("初步阅片人", null),
                    buildKeyFact("初步阅片时间", stringify(currentReport.submittedAt()))),
                currentReport.finalDiagnosis()));
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_REVIEW",
                "复核",
                currentReport.reportStatus(),
                stringify(currentReport.reviewedAt()),
                currentReport.reviewerName(),
                List.of(
                    buildKeyFact("复核人", currentReport.reviewerName()),
                    buildKeyFact("复核时间", stringify(currentReport.reviewedAt()))),
                null));
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_SIGN",
                "签发",
                currentReport.reportStatus(),
                stringify(currentReport.signedAt()),
                currentReport.signedByName(),
                List.of(
                    buildKeyFact("签发人", currentReport.signedByName()),
                    buildKeyFact("签发时间", stringify(currentReport.signedAt()))),
                null));
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_DETAIL",
                "详情报告",
                currentReport.reportStatus(),
                stringify(firstPresent(currentReport.publishedAt(), currentReport.signedAt())),
                firstPresent(currentReport.signedByName(), currentReport.reviewerName()),
                List.of(buildKeyFact("详情报告", currentReport.finalDiagnosis())),
                currentReport.finalDiagnosis()));
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_PUBLISH",
                "发布",
                currentReport.reportStatus(),
                stringify(currentReport.publishedAt()),
                currentReport.signedByName(),
                List.of(
                    buildKeyFact("发布人", currentReport.signedByName()),
                    buildKeyFact("发布时间", stringify(currentReport.publishedAt()))),
                null));
        }
        reportTrackingAggregate.revisions().stream().findFirst().ifPresent(revision -> reportNodes.add(
            buildLifecycleNode(
                "REPORT",
                "REPORT_REVISION",
                "修订",
                revision.requestStatus(),
                stringify(firstPresent(revision.reviewedAt(), revision.requestedAt())),
                firstPresent(revision.reviewedByName(), revision.requestedByName()),
                List.of(
                    buildKeyFact("修订时间", stringify(firstPresent(revision.reviewedAt(), revision.requestedAt()))),
                    buildKeyFact("修订人", firstPresent(revision.reviewedByName(), revision.requestedByName())),
                    buildKeyFact("驳回状态", revision.rejectReason() == null ? null : revision.requestStatus()),
                    buildKeyFact("驳回时间", revision.rejectReason() == null ? null : stringify(revision.reviewedAt())),
                    buildKeyFact("驳回人", revision.rejectReason() == null ? null : revision.reviewedByName())),
                revision.requestReason())));
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
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId,
        List<TrackingEvent> recentEvents
    ) {
        TrackingEvent registrationEvent = findLatestEvent(
            recentEvents,
            specimen.id(),
            List.of("SPECIMEN_COLLECTION", "SPECIMEN_REGISTER", "SPECIMEN_REGISTRATION"),
            List.of("REGISTERED"));
        TrackingEvent removalEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("REMOVAL"), List.of("COMPLETED"));
        TrackingEvent fixationEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("FIXATION"), List.of("COMPLETED", "STARTED"));
        TrackingEvent confirmationEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("CONFIRMATION"), List.of("COMPLETED"));
        TrackingEvent checkInEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("CHECK_IN"), List.of("CHECKED_IN"));
        TrackingEvent outboundEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("TRANSPORT"), List.of("HANDED_OVER", "ORDER_CREATED"));
        TrackingEvent receiptEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("RECEIPT"), List.of("RECEIVED", "DIRECT_RECEIVE"));
        List<DiagnosticReportViews.LifecycleNodeView> specimenEvents = List.of(
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_REGISTRATION",
                "标本登记",
                specimen.registeredAt() == null ? "PENDING" : "COMPLETED",
                stringify(specimen.registeredAt()),
                specimen.registeredByName(),
                registrationEvent,
                List.of(
                    buildKeyFact("登记状态", specimen.specimenStatus() == null ? null : specimen.specimenStatus().name()),
                    buildKeyFact("登记时间", stringify(specimen.registeredAt())),
                    buildKeyFact("登记人", specimen.registeredByName()),
                    buildKeyFact("送检类型", specimen.specimenType()),
                    buildKeyFact("标本名称", specimen.specimenNameStandardized()),
                    buildKeyFact("类型", specimen.specimenType()),
                    buildKeyFact("来源部位", specimen.specimenSite()),
                    buildKeyFact("标本大小", specimen.specimenSize()),
                    buildKeyFact("核对状态", specimen.verificationStatus()),
                    buildKeyFact("评价", specimen.registrationEvaluationItems())),
                specimen.remarks()),
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_REMOVAL",
                "离体确认",
                specimen.specimenRemovalAt() == null ? "PENDING" : "COMPLETED",
                stringify(specimen.specimenRemovalAt()),
                specimen.specimenRemovalOperatorName(),
                removalEvent,
                List.of(
                    buildKeyFact("离体操作人", specimen.specimenRemovalOperatorName()),
                    buildKeyFact("离体时间", stringify(specimen.specimenRemovalAt()))),
                null),
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_FIXATION",
                "标本固定",
                specimen.fixationStatus() == null ? null : specimen.fixationStatus().name(),
                stringify(fixationEvent == null ? null : fixationEvent.eventTime()),
                fixationEvent == null ? null : fixationEvent.operatorName(),
                fixationEvent,
                List.of(
                    buildKeyFact("标本固定液", null),
                    buildKeyFact("标本固定人", fixationEvent == null ? null : fixationEvent.operatorName()),
                    buildKeyFact("固定时间", stringify(fixationEvent == null ? null : fixationEvent.eventTime()))),
                null),
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_CONFIRMATION",
                "标本确认",
                specimen.verificationStatus(),
                stringify(specimen.specimenConfirmedAt()),
                specimen.verifiedByName(),
                confirmationEvent,
                List.of(
                    buildKeyFact("标本确认人", specimen.verifiedByName()),
                    buildKeyFact("标本确认时间", stringify(specimen.specimenConfirmedAt()))),
                null),
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_CHECK_IN",
                "标本入库",
                specimen.checkInStatus(),
                stringify(specimen.checkedInAt()),
                specimen.checkedInByName(),
                checkInEvent,
                List.of(
                    buildKeyFact("入库操作人", specimen.checkedInByName()),
                    buildKeyFact("入库时间", stringify(specimen.checkedInAt()))),
                null),
            buildLifecycleNode(
                "SPECIMEN",
                "SPECIMEN_OUTBOUND",
                "标本出库",
                outboundEvent == null ? null : outboundEvent.eventStatus(),
                stringify(outboundEvent == null ? null : outboundEvent.eventTime()),
                outboundEvent == null ? null : outboundEvent.operatorName(),
                outboundEvent,
                List.of(
                    buildKeyFact("出库操作人", outboundEvent == null ? null : outboundEvent.operatorName()),
                    buildKeyFact("出库时间", stringify(outboundEvent == null ? null : outboundEvent.eventTime()))),
                null),
            buildLifecycleNode(
                "TECHNICAL",
                "SPECIMEN_RECEIPT",
                "标本接收",
                specimen.receiptStatus(),
                stringify(receiptEvent == null ? null : receiptEvent.eventTime()),
                receiptEvent == null ? null : receiptEvent.operatorName(),
                receiptEvent,
                List.of(
                    buildKeyFact("物流人员", outboundEvent == null ? null : outboundEvent.operatorName()),
                    buildKeyFact("签收人员", receiptEvent == null ? null : receiptEvent.operatorName()),
                    buildKeyFact("签收时间", stringify(receiptEvent == null ? null : receiptEvent.eventTime())),
                    buildKeyFact("接收状态", specimen.receiptStatus())),
                null));
        List<DiagnosticReportViews.LifecycleBlockView> blockViews = blocks.stream()
            .map(block -> toLifecycleBlockView(
                block,
                embeddingBoxesByNo.get(block.embeddingBoxNo()),
                embeddingBoxArchiveByObjectId,
                slidesByEmbeddingBoxId,
                slideArchiveByObjectId,
                recentEvents))
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
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId,
        List<TrackingEvent> recentEvents
    ) {
        ArchiveRepository.ObjectArchiveSummary blockArchive =
            embeddingBox == null ? null : embeddingBoxArchiveByObjectId.get(embeddingBox.id());
        TrackingEvent grossingEvent = findLatestEvent(
            recentEvents, block.specimenId(), List.of("GROSSING"), List.of("COMPLETED"));
        TrackingEvent dehydrationEvent = findLatestEvent(
            recentEvents, block.specimenId(), List.of("DEHYDRATION"), List.of("COMPLETED", "STARTED"));
        TrackingEvent embeddingEvent = findLatestEvent(
            recentEvents, block.specimenId(), List.of("EMBEDDING"), List.of("COMPLETED", "STARTED"));
        List<DiagnosticReportViews.LifecycleNodeView> blockEvents = List.of(
            buildLifecycleNode(
                "TECHNICAL",
                "GROSSING",
                "取材描写",
                block.blockCode() == null ? "PENDING" : "COMPLETED",
                stringify(grossingEvent == null ? null : grossingEvent.eventTime()),
                grossingEvent == null ? null : grossingEvent.operatorName(),
                grossingEvent,
                List.of(
                    buildKeyFact("取材状态", block.blockCode() == null ? null : "COMPLETED"),
                    buildKeyFact("取材时间", stringify(grossingEvent == null ? null : grossingEvent.eventTime())),
                    buildKeyFact("包埋盒盒号", block.embeddingBoxNo()),
                    buildKeyFact("包埋备注", block.embeddingRemarks()),
                    buildKeyFact("大体描写信息", firstPresent(block.blockDescription(), block.grossDescription()))),
                block.grossDescription()),
            buildLifecycleNode(
                "TECHNICAL",
                "DEHYDRATION",
                "脱水",
                dehydrationEvent == null ? null : dehydrationEvent.eventStatus(),
                stringify(dehydrationEvent == null ? null : dehydrationEvent.eventTime()),
                dehydrationEvent == null ? null : dehydrationEvent.operatorName(),
                dehydrationEvent,
                List.of(
                    buildKeyFact("脱水开始时间", stringify(dehydrationEvent == null ? null : dehydrationEvent.eventTime())),
                    buildKeyFact("脱水完成时间", stringify(dehydrationEvent == null ? null : dehydrationEvent.eventTime())),
                    buildKeyFact("脱水状态", dehydrationEvent == null ? null : dehydrationEvent.eventStatus()),
                    buildKeyFact("脱水操作人", dehydrationEvent == null ? null : dehydrationEvent.operatorName())),
                null),
            buildLifecycleNode(
                "TECHNICAL",
                "EMBEDDING",
                "包埋",
                embeddingEvent == null ? null : embeddingEvent.eventStatus(),
                stringify(embeddingEvent == null ? null : embeddingEvent.eventTime()),
                embeddingEvent == null ? null : embeddingEvent.operatorName(),
                embeddingEvent,
                List.of(
                    buildKeyFact("包埋状态", embeddingEvent == null ? null : embeddingEvent.eventStatus()),
                    buildKeyFact("包埋时间", stringify(embeddingEvent == null ? null : embeddingEvent.eventTime())),
                    buildKeyFact("包埋人员", embeddingEvent == null ? null : embeddingEvent.operatorName()),
                    buildKeyFact("切片备注", embeddingBox == null ? null : embeddingBox.sliceNotice()),
                    buildKeyFact("取材评价", null)),
                null),
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
            .map(slide -> toLifecycleSlideView(slide, slideArchiveByObjectId.get(slide.id()), recentEvents))
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
        ArchiveRepository.ObjectArchiveSummary slideArchive,
        List<TrackingEvent> recentEvents
    ) {
        TrackingEvent slicingPrintEvent = findLatestEvent(
            recentEvents, slide.specimenId(), List.of("SLICING"), List.of("PRINTED", "SLIDE_PRINTED"));
        TrackingEvent slicingEvent = findLatestEvent(
            recentEvents, slide.specimenId(), List.of("SLICING"), List.of("COMPLETED"));
        TrackingEvent stainingEvent = findLatestEvent(
            recentEvents, slide.specimenId(), List.of("STAINING"), List.of("COMPLETED"));
        List<DiagnosticReportViews.LifecycleNodeView> slideEvents = List.of(
            buildLifecycleNode(
                "TECHNICAL",
                "SLICING",
                "切片",
                slide.slideStatus(),
                stringify(firstPresent(
                    slicingEvent == null ? null : slicingEvent.eventTime(),
                    slicingPrintEvent == null ? null : slicingPrintEvent.eventTime())),
                firstPresent(
                    slicingEvent == null ? null : slicingEvent.operatorName(),
                    slicingPrintEvent == null ? null : slicingPrintEvent.operatorName()),
                firstPresent(slicingEvent, slicingPrintEvent),
                List.of(
                    buildKeyFact("玻片打印状态", slicingPrintEvent == null ? null : slicingPrintEvent.eventStatus()),
                    buildKeyFact("打印时间", stringify(slicingPrintEvent == null ? null : slicingPrintEvent.eventTime())),
                    buildKeyFact("打印操作人", slicingPrintEvent == null ? null : slicingPrintEvent.operatorName()),
                    buildKeyFact("完成切片时间", stringify(slicingEvent == null ? null : slicingEvent.eventTime())),
                    buildKeyFact("完成切片人", slicingEvent == null ? null : slicingEvent.operatorName())),
                null),
            buildLifecycleNode(
                "TECHNICAL",
                "STAINING",
                "染色出片",
                stainingEvent == null ? null : stainingEvent.eventStatus(),
                stringify(stainingEvent == null ? null : stainingEvent.eventTime()),
                stainingEvent == null ? null : stainingEvent.operatorName(),
                stainingEvent,
                List.of(
                    buildKeyFact("染色出片时间", stringify(stainingEvent == null ? null : stainingEvent.eventTime())),
                    buildKeyFact("出片操作人", stainingEvent == null ? null : stainingEvent.operatorName()),
                    buildKeyFact("出片是否超时", null),
                    buildKeyFact("超时时长", null)),
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
        return buildLifecycleNode(
            stageCode,
            nodeCode,
            title,
            status,
            occurredAt,
            operatorName,
            null,
            keyFacts,
            eventContent);
    }

    private DiagnosticReportViews.LifecycleNodeView buildLifecycleNode(
        String stageCode,
        String nodeCode,
        String title,
        String status,
        String occurredAt,
        String operatorName,
        TrackingEvent auditEvent,
        List<DiagnosticReportViews.KeyFactView> keyFacts,
        String eventContent
    ) {
        return new DiagnosticReportViews.LifecycleNodeView(
            stageCode,
            nodeCode,
            title,
            normalizeLifecycleStatus(status),
            firstPresent(occurredAt, stringify(auditEvent == null ? null : auditEvent.eventTime())),
            firstPresent(operatorName, auditEvent == null ? null : auditEvent.operatorName()),
            auditEvent == null ? null : auditEvent.operatorIp(),
            auditEvent == null ? null : auditEvent.operatorDevice(),
            keyFacts,
            firstPresent(eventContent, auditEvent == null ? null : auditEvent.eventContent()));
    }

    private DiagnosticReportViews.KeyFactView buildKeyFact(String label, String value) {
        return new DiagnosticReportViews.KeyFactView(label, value);
    }

    private TrackingEvent findLatestEvent(
        List<TrackingEvent> events,
        String specimenId,
        List<String> nodeCodes,
        List<String> eventTypes
    ) {
        return events.stream()
            .filter(event -> specimenId == null || specimenId.equals(event.specimenId()))
            .filter(event -> nodeCodes.isEmpty() || nodeCodes.contains(normalizeCode(event.nodeCode())))
            .filter(event -> eventTypes.isEmpty() || eventTypes.contains(normalizeCode(event.eventType())))
            .max((left, right) -> {
                LocalDateTime leftTime = left.eventTime();
                LocalDateTime rightTime = right.eventTime();
                if (leftTime == null && rightTime == null) {
                    return 0;
                }
                if (leftTime == null) {
                    return -1;
                }
                if (rightTime == null) {
                    return 1;
                }
                return leftTime.compareTo(rightTime);
            })
            .orElse(null);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
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
