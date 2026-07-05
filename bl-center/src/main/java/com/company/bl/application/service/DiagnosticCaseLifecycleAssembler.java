package com.company.bl.application.service;

import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ArchiveRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.DiagnosticTrackingQueryRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class DiagnosticCaseLifecycleAssembler {

    LifecycleContent build(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate workbenchAggregate,
        DiagnosticTrackingQueryRepository.ReportTrackingAggregate reportTrackingAggregate,
        Map<String, ArchiveRepository.ObjectArchiveSummary> specimenArchiveByObjectId,
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId,
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId,
        Map<String, TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo,
        Map<String, List<TechnicalWorkflowRecords.SamplingBlock>> blocksBySpecimenId,
        Map<String, List<TechnicalWorkflowProcessingRecords.Slide>> slidesByEmbeddingBoxId
    ) {
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
        return new LifecycleContent(
            buildLifecycleStageGroups(workbenchAggregate, reportTrackingAggregate, specimenViews),
            specimenViews
        );
    }

    record LifecycleContent(
        List<DiagnosticReportViews.LifecycleStageGroupView> overallTimeline,
        List<DiagnosticReportViews.LifecycleSpecimenView> specimenViews
    ) {
    }

    private List<DiagnosticReportViews.LifecycleStageGroupView> buildLifecycleStageGroups(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate workbenchAggregate,
        DiagnosticTrackingQueryRepository.ReportTrackingAggregate reportTrackingAggregate,
        List<DiagnosticReportViews.LifecycleSpecimenView> specimenViews
    ) {
        List<DiagnosticReportViews.LifecycleNodeView> applicationNodes = new ArrayList<>();
        applicationNodes.add(buildLifecycleNode(
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
        findLatestEvent(
            workbenchAggregate.recentEvents(),
            null,
            List.of("APPOINTMENT"),
            List.of("FROZEN_REQUESTED"))
            .ifPresent(event -> applicationNodes.add(buildLifecycleNode(
                "APPLICATION",
                "FROZEN_REQUESTED",
                "冰冻申请",
                "COMPLETED",
                stringify(event.eventTime()),
                event.operatorName(),
                event,
                List.of(
                    buildKeyFact("申请单号", workbenchAggregate.applicationNo()),
                    buildKeyFact("申请类型", workbenchAggregate.applicationType()),
                    buildKeyFact("病理号", workbenchAggregate.pathologyNo())),
                event.eventContent())));
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
        TrackingEvent reportPrintEvent = findLatestEvent(
            reportTrackingAggregate.events(),
            null,
            List.of("REPORT_PRINT"),
            List.of("PRINT")).orElse(null);
        if (reportPrintEvent != null) {
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_PRINT",
                "打印",
                "PRINTED",
                null,
                null,
                reportPrintEvent,
                List.of(),
                null));
        }
        TrackingEvent reportScheduleIssueEvent = findLatestEvent(
            reportTrackingAggregate.events(),
            null,
            List.of("REPORT_SCHEDULE_ISSUE"),
            List.of("SCHEDULE_ISSUE")).orElse(null);
        if (reportScheduleIssueEvent != null) {
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_SCHEDULE_ISSUE",
                "计划发放",
                "COMPLETED",
                null,
                null,
                reportScheduleIssueEvent,
                List.of(),
                null));
        }
        TrackingEvent reportIssueEvent = findLatestEvent(
            reportTrackingAggregate.events(),
            null,
            List.of("REPORT_ISSUE"),
            List.of("ISSUE")).orElse(null);
        if (reportIssueEvent != null) {
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_ISSUE",
                "发放",
                "ISSUED",
                null,
                null,
                reportIssueEvent,
                List.of(),
                null));
        }
        TrackingEvent reportRecallEvent = findLatestEvent(
            reportTrackingAggregate.events(),
            null,
            List.of("REPORT_RECALL"),
            List.of("RECALL")).orElse(null);
        if (reportRecallEvent != null) {
            reportNodes.add(buildLifecycleNode(
                "REPORT",
                "REPORT_RECALL",
                "回收",
                "RECALLED",
                null,
                null,
                reportRecallEvent,
                List.of(),
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
        Specimen specimen,
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
            List.of("REGISTERED")).orElse(null);
        TrackingEvent removalEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("REMOVAL"), List.of("COMPLETED")).orElse(null);
        TrackingEvent fixationEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("FIXATION"), List.of("COMPLETED", "STARTED")).orElse(null);
        TrackingEvent confirmationEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("CONFIRMATION"), List.of("COMPLETED")).orElse(null);
        TrackingEvent checkInEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("CHECK_IN"), List.of("CHECKED_IN")).orElse(null);
        TrackingEvent outboundEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("TRANSPORT"), List.of("HANDED_OVER", "ORDER_CREATED")).orElse(null);
        TrackingEvent receiptEvent = findLatestEvent(
            recentEvents, specimen.id(), List.of("RECEIPT"), List.of("RECEIVED", "DIRECT_RECEIVE")).orElse(null);
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
            recentEvents, block.specimenId(), List.of("GROSSING"), List.of("COMPLETED")).orElse(null);
        TrackingEvent dehydrationEvent = findLatestEvent(
            recentEvents, block.specimenId(), List.of("DEHYDRATION"), List.of("COMPLETED", "STARTED")).orElse(null);
        TrackingEvent embeddingEvent = findLatestEvent(
            recentEvents, block.specimenId(), List.of("EMBEDDING"), List.of("COMPLETED", "STARTED")).orElse(null);
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
            recentEvents, slide.specimenId(), List.of("SLICING"), List.of("PRINTED", "SLIDE_PRINTED")).orElse(null);
        TrackingEvent slicingEvent = findLatestEvent(
            recentEvents, slide.specimenId(), List.of("SLICING"), List.of("COMPLETED")).orElse(null);
        TrackingEvent stainingEvent = findLatestEvent(
            recentEvents, slide.specimenId(), List.of("STAINING"), List.of("COMPLETED")).orElse(null);
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

    private Optional<TrackingEvent> findLatestEvent(
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
            });
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
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

    private static String archiveStatus(ArchiveRepository.ApplicationArchiveSummary summary) {
        return summary == null ? null : summary.archiveStatus();
    }

    private static String archiveStatus(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.archiveStatus();
    }

    private static String archiveLocation(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.archiveLocation();
    }

    private static String loanStatus(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.loanStatus();
    }

    private static String stringify(LocalDateTime time) {
        return time == null ? null : time.toString();
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
}
