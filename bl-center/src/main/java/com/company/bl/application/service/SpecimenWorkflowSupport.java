package com.company.bl.application.service;

import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;

@Component
class SpecimenWorkflowSupport {

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ApplicationRepository applicationRepository;
    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;
    private final SpecimenWorkflowQueryRepository specimenWorkflowRepository;

    SpecimenWorkflowSupport(ApplicationRepository applicationRepository,
                            ApplicationRegistrationWorkbenchRepository workbenchRepository,
                            SpecimenWorkflowQueryRepository specimenWorkflowRepository) {
        this.applicationRepository = applicationRepository;
        this.workbenchRepository = workbenchRepository;
        this.specimenWorkflowRepository = specimenWorkflowRepository;
    }

    Application getApplication(String applicationId) {
        return applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new ApplicationDomainException(com.company.bl.domain.enums.ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }

    Specimen getSpecimen(String barcode) {
        return specimenWorkflowRepository.findSpecimenByBarcode(barcode)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen barcode not found"));
    }

    TransportOrder getTransportOrder(String transportOrderId) {
        return specimenWorkflowRepository.findTransportOrderById(transportOrderId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Transport order not found"));
    }

    List<Specimen> getSpecimensByApplicationId(String applicationId) {
        return specimenWorkflowRepository.findSpecimensByApplicationId(applicationId);
    }

    List<Specimen> getSpecimensByLabelPrintBatchNoAndStatuses(String labelPrintBatchNo, List<String> labelPrintStatuses) {
        return specimenWorkflowRepository.findSpecimensByLabelPrintBatchNoAndStatuses(labelPrintBatchNo, labelPrintStatuses);
    }

    Optional<PathologyCase> findPathologyCaseByApplicationId(String applicationId) {
        return specimenWorkflowRepository.findPathologyCaseByApplicationId(applicationId);
    }

    List<TransportOrderItem> getTransportOrderItems(String transportOrderId) {
        return specimenWorkflowRepository.findTransportOrderItems(transportOrderId);
    }

    Specimen resolveSpecimenForRemoval(String identifierType, String identifier) {
        if ("BARCODE".equals(identifierType)) {
            return getSpecimen(identifier);
        }
        if ("SPECIMEN_NO".equals(identifierType)) {
            List<Specimen> specimens = specimenWorkflowRepository.findSpecimensBySpecimenNo(identifier);
            if (specimens.isEmpty()) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen specimenNo not found");
            }
            if (specimens.size() > 1) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen number matches multiple records");
            }
            return specimens.get(0);
        }
        throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported specimen identifier type");
    }

    void validateApplicationCanRegister(Application application) {
        ApplicationStatus status = application.getStatus();
        if (status == ApplicationStatus.DRAFT || status == ApplicationStatus.SUBMITTED) {
            return;
        }
        throw new BlBusinessException(
            BlErrorCode.OPERATION_NOT_ALLOWED,
            409,
            "Application status does not allow specimen registration: " + status.name());
    }

    void ensureBarcodeAvailable(String barcode) {
        if (specimenWorkflowRepository.findSpecimenByBarcode(barcode).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen barcode already exists");
        }
    }

    boolean isReceiptTerminalStatus(SpecimenStatus status) {
        return status == SpecimenStatus.RECEIVED
            || status == SpecimenStatus.REJECTED
            || status == SpecimenStatus.RETURNED;
    }

    boolean isTransportOrderReadyForReceipt(TransportOrderStatus status) {
        return status == TransportOrderStatus.PRINTED
            || status == TransportOrderStatus.HANDED_OVER
            || status == TransportOrderStatus.PARTIALLY_RECEIVED;
    }

    boolean isTransportItemTerminal(TransportItemStatus status) {
        return status == TransportItemStatus.COMPLETED || status == TransportItemStatus.RETURNED;
    }

    SpecimenVerificationResult buildSpecimenVerificationResult(Specimen specimen) {
        return new SpecimenVerificationResult(
            specimen.id(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenNameStandardized(),
            specimen.specimenType(),
            specimen.specimenSite(),
            specimen.collectionMode(),
            specimen.clinicalSymptom(),
            specimen.specimenCount(),
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus() == null ? null : specimen.specimenStatus().name(),
            specimen.fixationStatus() == null ? null : specimen.fixationStatus().name(),
            specimen.verificationStatus(),
            specimen.verificationStartedAt(),
            specimen.verificationCompletedAt(),
            specimen.labelPrintStatus(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            specimen.unqualifiedReason());
    }

    PendingSpecimenItem toPendingItem(SpecimenWorkflowRepository.PendingSpecimenRow row) {
        return new PendingSpecimenItem(
            row.applicationId(),
            row.applicationNo(),
            row.patientName(),
            row.submittingDepartmentId(),
            row.submittingDepartmentName(),
            row.transportOrderId(),
            row.specimenId(),
            row.specimenNo(),
            row.barcode(),
            row.containerName(),
            row.containerCount(),
            row.specimenStatus(),
            row.fixationStatus(),
            row.fixationStartedAt(),
            row.fixationCompletedAt(),
            row.fixationLiquidType(),
            row.fixationOperatorUserId(),
            row.fixationOperatorName(),
            row.verificationStatus(),
            row.verificationStartedAt(),
            row.verificationCompletedAt(),
            row.specimenConfirmedAt(),
            row.checkInStatus(),
            row.checkedInAt(),
            row.checkedInByName(),
            row.registeredAt(),
            row.latestTrackingAt(),
            row.abnormalFlag());
    }

    ApplicationListItem toApplicationListItem(ApplicationTracking tracking) {
        String latestBatchNo = resolveLatestLabelPrintBatchNo(tracking.specimens());
        String latestLabelPrintStatus = latestBatchNo == null
            ? null
            : resolveLatestBatchLabelPrintStatus(tracking.specimens(), latestBatchNo);
        ApplicationOperationState operationState = resolveApplicationOperationState(tracking.application());
        return new ApplicationListItem(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientName(),
            tracking.application().getPatientGender(),
            tracking.application().getPatientAge(),
            tracking.application().getStatus().name(),
            tracking.application().getSubmittingDepartmentName(),
            tracking.application().getSubmittingDoctorName(),
            tracking.application().getApplicationType(),
            tracking.application().getApplicationFormStatus().name(),
            tracking.currentNode(),
            tracking.abnormal(),
            tracking.specimens().size(),
            latestLabelPrintStatus,
            operationState.editable(),
            operationState.deletable(),
            operationState.voided(),
            operationState.disabledReason(),
            tracking.application().getApplicationDate(),
            tracking.application().getSubmissionDate(),
            tracking.application().getCreatedAt(),
            tracking.application().getUpdatedAt());
    }

    ApplicationOperationState resolveApplicationOperationState(Application application) {
        if (application.getStatus() == ApplicationStatus.VOIDED) {
            return new ApplicationOperationState(false, false, true, "申请单已作废，不能再编辑或作废");
        }
        if (workbenchRepository.hasStartedDownstreamWorkflow(application.getId().value())) {
            return new ApplicationOperationState(false, false, false, "申请单已进入下游流程，不能再编辑或作废");
        }
        return new ApplicationOperationState(true, true, false, null);
    }

    String resolveLatestLabelPrintBatchNo(List<Specimen> specimens) {
        String latestBatchNo = null;
        for (Specimen specimen : specimens) {
            if (!blank(specimen.labelPrintBatchNo())) {
                latestBatchNo = specimen.labelPrintBatchNo().trim();
            }
        }
        return latestBatchNo;
    }

    String resolveLatestBatchLabelPrintStatus(List<Specimen> specimens, String batchNo) {
        String resolvedStatus = null;
        int resolvedPriority = 0;
        for (Specimen specimen : specimens) {
            if (!batchNo.equals(specimen.labelPrintBatchNo())) {
                continue;
            }
            String status = normalizeStatus(specimen.labelPrintStatus());
            int priority = labelPrintStatusPriority(status);
            if (priority > resolvedPriority) {
                resolvedStatus = status;
                resolvedPriority = priority;
            }
        }
        return resolvedStatus;
    }

    String resolveLatestBatchLabelPrintMessage(List<TrackingEvent> events, List<Specimen> batchSpecimens) {
        if (batchSpecimens.isEmpty()) {
            return null;
        }
        Set<String> batchSpecimenIds = new HashSet<>();
        for (Specimen specimen : batchSpecimens) {
            batchSpecimenIds.add(specimen.id());
        }
        String latestMessage = null;
        for (TrackingEvent event : events) {
            if (!"LABEL_PRINT".equals(event.nodeCode())) {
                continue;
            }
            if (event.specimenId() == null || !batchSpecimenIds.contains(event.specimenId())) {
                continue;
            }
            latestMessage = event.eventContent();
        }
        return latestMessage;
    }

    Specimen copyWithLabelPrintStatus(Specimen specimen, String labelPrintStatus) {
        return new Specimen(
            specimen.id(),
            specimen.applicationId(),
            specimen.caseId(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenType(),
            specimen.specimenNameStandardized(),
            specimen.specimenSite(),
            specimen.collectionMode(),
            specimen.specimenCount(),
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus(),
            specimen.fixationStatus(),
            specimen.verificationStatus(),
            specimen.verificationStartedAt(),
            specimen.verificationCompletedAt(),
            specimen.specimenRemovalAt(),
            specimen.specimenRemovalOperatorUserId(),
            specimen.specimenRemovalOperatorName(),
            specimen.specimenConfirmedAt(),
            specimen.checkInStatus(),
            specimen.checkedInAt(),
            specimen.checkedInByName(),
            specimen.qualified(),
            specimen.unqualifiedReason(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            specimen.qualityIssueCodes(),
            specimen.clinicalSymptom(),
            specimen.applicantDepartmentId(),
            specimen.applicantDepartmentName(),
            specimen.applicantDoctorUserId(),
            specimen.applicantDoctorName(),
            specimen.submissionDate(),
            specimen.labelPrintBatchNo(),
            labelPrintStatus,
            specimen.registeredByUserId(),
            specimen.registeredByName(),
            specimen.registeredAt(),
            specimen.terminalCode(),
            specimen.remarks());
    }

    byte[] buildSpecimenRemovalExport(List<SpecimenWorkflowRepository.SpecimenRemovalListRow> rows) {
        List<List<String>> sheetRows = new ArrayList<>();
        sheetRows.add(List.of(
            "标本ID",
            "申请单",
            "标本编号",
            "姓名",
            "住院号",
            "性别",
            "手术间",
            "标本名称",
            "标本状态",
            "类型",
            "离体时间",
            "离体操作人",
            "添加时间",
            "添加人"
        ));
        for (SpecimenWorkflowRepository.SpecimenRemovalListRow row : rows) {
            sheetRows.add(List.of(
                defaultString(row.barcode()),
                defaultString(row.applicationNo()),
                defaultString(row.specimenNo()),
                defaultString(row.patientName()),
                defaultString(row.inpatientNo()),
                defaultString(row.patientGender()),
                defaultString(row.surgeryName()),
                defaultString(row.specimenName()),
                defaultString(row.specimenStatus()),
                defaultString(row.specimenType()),
                formatExportDateTime(row.specimenRemovalAt()),
                defaultString(row.specimenRemovalOperatorName()),
                formatExportDateTime(row.registeredAt()),
                defaultString(row.registeredByName())
            ));
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeZipEntry(zipOutputStream, "[Content_Types].xml", contentTypesXml());
            writeZipEntry(zipOutputStream, "_rels/.rels", rootRelsXml());
            writeZipEntry(zipOutputStream, "xl/workbook.xml", workbookXml());
            writeZipEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            writeZipEntry(zipOutputStream, "xl/worksheets/sheet1.xml", worksheetXml(sheetRows));
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build specimen removal export", exception);
        }
    }

    String trim(String value) {
        return value == null ? null : value.trim();
    }

    boolean blank(String value) {
        return value == null || value.isBlank();
    }

    String defaultIfBlank(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    String normalizeQualityCheckResult(String value) {
        String normalized = normalizeStatus(value);
        if (normalized == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Quality check result is required");
        }
        if (!"PASSED".equals(normalized) && !"FAILED".equals(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported quality check result");
        }
        return normalized;
    }

    List<String> normalizeQualityIssueCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String code = trim(value);
            if (code != null) {
                normalized.add(code.toUpperCase());
            }
        }
        return List.copyOf(normalized);
    }

    String joinQualityIssueCodes(List<String> values) {
        List<String> normalized = normalizeQualityIssueCodes(values);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    int normalizePage(int page) {
        return Math.max(page, 1);
    }

    int normalizeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }

    LocalDateTime parseDateFrom(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    LocalDateTime parseDateTo(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay();
    }

    LocalDate parseLocalDateFrom(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    LocalDate parseLocalDate(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    LocalDate parseLocalDateTo(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).plusDays(1);
    }

    String normalizeStatus(String value) {
        return blank(value) ? null : value.trim().toUpperCase();
    }

    String commandCheckInStatus(Specimen specimen) {
        return blank(specimen.checkInStatus()) ? "NOT_CHECKED_IN" : specimen.checkInStatus().trim().toUpperCase();
    }

    private int labelPrintStatusPriority(String status) {
        if ("FAILED".equals(status)) {
            return 3;
        }
        if ("PENDING".equals(status)) {
            return 2;
        }
        if ("SUCCESS".equals(status)) {
            return 1;
        }
        return 0;
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String contentTypesXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
            """;
    }

    private String rootRelsXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
            """;
    }

    private String workbookXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="绂讳綋鏃堕棿璁剧疆" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
            """;
    }

    private String workbookRelsXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
            """;
    }

    private String worksheetXml(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
            """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            builder.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> cells = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {
                builder.append("<c r=\"")
                    .append(excelColumnName(columnIndex))
                    .append(rowIndex + 1)
                    .append("\" t=\"inlineStr\"><is><t>")
                    .append(escapeXml(cells.get(columnIndex)))
                    .append("</t></is></c>");
            }
            builder.append("</row>");
        }
        builder.append("""
              </sheetData>
            </worksheet>
            """);
        return builder.toString();
    }

    private String excelColumnName(int columnIndex) {
        StringBuilder builder = new StringBuilder();
        int current = columnIndex;
        do {
            builder.insert(0, (char) ('A' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);
        return builder.toString();
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private String formatExportDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXPORT_TIME_FORMATTER);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
