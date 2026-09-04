package com.company.bl.infrastructure.maintenance;

import com.company.bl.infrastructure.config.ReportStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "bl.report-maintenance.rebuild-and-clean", havingValue = "true")
public final class LocalReportOfdRebuildRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalReportOfdRebuildRunner.class);
    private static final String CONFIRMATION = "LOCAL_DEV_ONLY";
    private static final String LAYOUT_CODE = "routine-pathology-report-v2";
    private static final String HOSPITAL_NAME = "佛山市中医院病理科";
    private static final String REPORT_TITLE = "病理检查报告单";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ReportStorageProperties storageProperties;
    private final Environment environment;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurableApplicationContext applicationContext;

    public LocalReportOfdRebuildRunner(NamedParameterJdbcTemplate jdbcTemplate,
                                       ObjectMapper objectMapper,
                                       ReportStorageProperties storageProperties,
                                       Environment environment,
                                       org.springframework.transaction.PlatformTransactionManager transactionManager,
                                       ConfigurableApplicationContext applicationContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.storageProperties = storageProperties;
        this.environment = environment;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            preflight();
            MaintenanceResult result = transactionTemplate.execute(status -> rebuildAndDetachArtifacts());
            if (result == null) {
                throw new IllegalStateException("Maintenance transaction returned no result");
            }
            int deletedFiles = deleteFormalOfdFiles(storageProperties.getRootDir());
            log.info("Local report OFD rebuild completed reports={} versions={} artifacts={} files={}",
                result.reportCount(), result.versionCount(), result.artifactCount(), deletedFiles);
            int exitCode = result.failedFileCount() == 0 ? 0 : 3;
            SpringApplication.exit(applicationContext, () -> exitCode);
        } catch (RuntimeException exception) {
            log.error("Local report OFD rebuild refused or failed: {}", exception.getMessage());
            SpringApplication.exit(applicationContext, () -> 3);
        }
    }

    private void preflight() {
        Set<String> profiles = Set.of(environment.getActiveProfiles());
        if (!profiles.contains("dev") || profiles.contains("prod") || profiles.contains("ky")) {
            throw new IllegalStateException("Requires active dev profile only");
        }
        String confirmation = environment.getProperty("bl.report-maintenance.confirm", "");
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalStateException("Missing bl.report-maintenance.confirm=LOCAL_DEV_ONLY");
        }
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.matches("(?i)^jdbc:dm://(127\\.0\\.0\\.1|localhost)(:[0-9]+)?(?:/.*)?$")) {
            throw new IllegalStateException("Datasource is not a localhost DM development database");
        }
        Path root = storageProperties.getRootDir().toAbsolutePath().normalize();
        String rootText = root.toString().replace('\\', '/').toLowerCase();
        String userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize()
            .toString().replace('\\', '/').toLowerCase();
        if (!rootText.startsWith(userHome + "/") || !rootText.endsWith("/.sybase/bl-center/reports")) {
            throw new IllegalStateException("Report storage root is not the local development report directory");
        }
        if (rootText.contains("/data/") || rootText.startsWith("//")) {
            throw new IllegalStateException("Refusing shared or container report storage root");
        }
    }

    private MaintenanceResult rebuildAndDetachArtifacts() {
        List<ReportRow> reports = jdbcTemplate.query("""
            select p.id report_id, p.case_id, p.report_no, p.pathology_no, p.report_date,
                   p.gross_exam, p.microscopic_exam, p.clinical_diagnosis, p.final_diagnosis,
                   p.rich_text_content, p.render_snapshot, p.reviewer_name, p.signed_by_name,
                   p.signed_at, p.submitted_at, p.published_at,
                   a.patient_name, a.patient_id, a.patient_gender, a.patient_age,
                   a.submitting_department_name, a.submitting_doctor_name, a.submission_date,
                   w.phone, w.bed_no, w.inpatient_no, w.ward_name
              from pathology_reports p
              join pathology_cases pc on pc.id = p.case_id
              join applications a on a.id = pc.application_id
              left join application_registration_workbench w on w.application_id = a.id
             order by p.id
            """, Map.of(), (rs, rowNum) -> new ReportRow(
            rs.getString("report_id"), rs.getString("case_id"), rs.getString("report_no"),
            rs.getString("pathology_no"), rs.getObject("report_date", LocalDateTime.class),
            rs.getString("gross_exam"), rs.getString("microscopic_exam"), rs.getString("clinical_diagnosis"),
            rs.getString("final_diagnosis"), rs.getString("rich_text_content"), rs.getString("render_snapshot"),
            rs.getString("reviewer_name"), rs.getString("signed_by_name"), rs.getObject("signed_at", LocalDateTime.class),
            rs.getObject("submitted_at", LocalDateTime.class), rs.getObject("published_at", LocalDateTime.class),
            rs.getString("patient_name"), rs.getString("patient_id"), rs.getString("patient_gender"),
            rs.getString("patient_age"), rs.getString("submitting_department_name"), rs.getString("submitting_doctor_name"),
            rs.getObject("submission_date", LocalDate.class), rs.getString("phone"), rs.getString("bed_no"),
            rs.getString("inpatient_no"), rs.getString("ward_name")));
        Map<String, List<String>> samplingDoctors = loadSamplingDoctors(reports);
        for (ReportRow report : reports) {
            ObjectNode snapshot = rebuildSnapshot(parseSnapshot(report.renderSnapshot()), report, report.signedByName(), report.signedAt(), samplingDoctors.get(report.caseId()));
            updateReportSnapshot(report.reportId(), snapshot);
        }

        List<VersionRow> versions = jdbcTemplate.query("""
            select id, report_id, case_id, version_no, render_snapshot, final_diagnosis_snapshot,
                   signed_by_name, signed_at
              from report_versions
             order by id
            """, Map.of(), (rs, rowNum) -> new VersionRow(
            rs.getString("id"), rs.getString("report_id"), rs.getString("case_id"), rs.getInt("version_no"),
            rs.getString("render_snapshot"), rs.getString("final_diagnosis_snapshot"), rs.getString("signed_by_name"),
            rs.getObject("signed_at", LocalDateTime.class)));
        Map<String, ReportRow> reportById = reports.stream().collect(Collectors.toMap(ReportRow::reportId, row -> row));
        for (VersionRow version : versions) {
            ReportRow report = reportById.get(version.reportId());
            if (report == null) {
                continue;
            }
            ObjectNode snapshot = rebuildSnapshot(parseSnapshot(version.renderSnapshot()), report,
                version.signedByName(), version.signedAt(), samplingDoctors.get(version.caseId()));
            if (version.renderSnapshot() == null || version.renderSnapshot().isBlank()) {
                setFallbackDiagnosis(snapshot, version.finalDiagnosisSnapshot(), report);
            }
            jdbcTemplate.update("update report_versions set render_snapshot = :snapshot where id = :id",
                new MapSqlParameterSource().addValue("id", version.id()).addValue("snapshot", write(snapshot)));
        }

        List<String> storageKeys = jdbcTemplate.query("""
            select storage_key from report_version_artifacts where artifact_format = 'OFD'
            """, Map.of(), (rs, rowNum) -> rs.getString("storage_key"));
        int detached = jdbcTemplate.update("update report_versions set artifact_id = null where artifact_id is not null", Map.of());
        int artifacts = jdbcTemplate.update("delete from report_version_artifacts where artifact_format = 'OFD'", Map.of());
        return new MaintenanceResult(reports.size(), versions.size(), artifacts, detached, storageKeys.size(), 0);
    }

    private Map<String, List<String>> loadSamplingDoctors(List<ReportRow> reports) {
        List<String> caseIds = reports.stream().map(ReportRow::caseId).filter(value -> value != null && !value.isBlank()).distinct().toList();
        if (caseIds.isEmpty()) {
            return Map.of();
        }
        Map<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();
        jdbcTemplate.query("""
            select case_id, sampled_by_name
              from samplings
             where case_id in (:caseIds)
               and sampled_by_name is not null
               and trim(sampled_by_name) <> ''
             order by case_id, sampled_at asc, created_at asc, id asc
            """, new MapSqlParameterSource("caseIds", caseIds), (rs, rowNum) -> {
                grouped.computeIfAbsent(rs.getString("case_id"), ignored -> new LinkedHashSet<>())
                    .add(rs.getString("sampled_by_name").trim());
                return null;
            });
        return grouped.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private ObjectNode parseSnapshot(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode().put("schemaVersion", 1);
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            return node != null && node.isObject() ? (ObjectNode) node : objectMapper.createObjectNode().put("schemaVersion", 1);
        } catch (IOException exception) {
            return objectMapper.createObjectNode().put("schemaVersion", 1);
        }
    }

    private ObjectNode rebuildSnapshot(ObjectNode snapshot, ReportRow report, String signedByName,
                                       LocalDateTime signedAt, List<String> samplingDoctors) {
        snapshot.put("schemaVersion", 1).put("layoutCode", LAYOUT_CODE)
            .put("hospitalName", HOSPITAL_NAME).put("reportTitle", REPORT_TITLE);
        if (!snapshot.hasNonNull("templateCode")) {
            snapshot.put("templateCode", "wysiwyg-template");
        }
        ArrayNode fields = objectMapper.createArrayNode();
        addField(fields, "routine-header", "联系电话:", report.phone());
        addField(fields, "routine-row-1", "姓名:", report.patientName());
        addField(fields, "routine-row-1", "性别:", report.patientGender());
        addField(fields, "routine-row-1", "年龄:", report.patientAge());
        addField(fields, "routine-row-1", "病人ID:", report.patientId());
        addField(fields, "routine-row-1", "床号:", report.bedNo());
        addField(fields, "routine-row-1", "住院号:", report.inpatientNo());
        addField(fields, "routine-row-2", "申请科室:", report.submittingDepartmentName());
        addField(fields, "routine-row-2", "送检医师:", report.submittingDoctorName());
        addField(fields, "routine-row-2", "检查日期:", formatDate(report.submissionDate(), report.reportDate()));
        addField(fields, "routine-row-3", "临床诊断:", report.clinicalDiagnosis());
        addField(fields, "routine-row-3", "护理单元:", report.wardName());
        snapshot.set("metaFields", fields);

        ArrayNode footer = objectMapper.createArrayNode();
        addField(footer, "", "审核医师:", report.reviewerName());
        addField(footer, "", "诊断医师:", signedByName);
        addField(footer, "", "取材医师:", samplingDoctors == null ? null : String.join("、", samplingDoctors));
        addField(footer, "", "报告日期:", formatDateTime(signedAt != null ? signedAt : report.reportDate()));
        snapshot.set("footerFields", footer);
        if (!snapshot.has("sections") || !snapshot.path("sections").isArray() || snapshot.path("sections").isEmpty()) {
            ArrayNode sections = objectMapper.createArrayNode();
            addSection(sections, "肉眼所见:", report.grossExam(), 82);
            addSection(sections, "光镜所见:", report.microscopicExam(), 160);
            addSection(sections, "病理诊断及建议:", report.finalDiagnosis(), 92);
            snapshot.set("sections", sections);
        }
        if (!snapshot.has("structuredBlocks")) {
            snapshot.set("structuredBlocks", objectMapper.createObjectNode());
        }
        return snapshot;
    }

    private void setFallbackDiagnosis(ObjectNode snapshot, String finalDiagnosis, ReportRow report) {
        ArrayNode sections = (ArrayNode) snapshot.withArray("sections");
        sections.removeAll();
        addSection(sections, "肉眼所见:", report.grossExam(), 82);
        addSection(sections, "光镜所见:", report.microscopicExam(), 160);
        addSection(sections, "病理诊断及建议:", finalDiagnosis, 92);
    }

    private void addField(ArrayNode fields, String className, String label, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || "-".equals(normalized)) {
            return;
        }
        ObjectNode field = objectMapper.createObjectNode().put("label", label).put("value", normalized);
        if (!className.isBlank()) {
            field.put("class", className);
        }
        fields.add(field);
    }

    private void addSection(ArrayNode sections, String label, String value, int minHeight) {
        ObjectNode section = objectMapper.createObjectNode().put("label", label).put("value", value == null ? "" : value).put("minHeight", minHeight);
        sections.add(section);
    }

    private String formatDate(LocalDate applicationDate, LocalDateTime reportDate) {
        if (applicationDate != null) return applicationDate.format(DATE_FORMATTER);
        return reportDate == null ? null : reportDate.toLocalDate().format(DATE_FORMATTER);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private void updateReportSnapshot(String reportId, ObjectNode snapshot) {
        jdbcTemplate.update("update pathology_reports set render_snapshot = :snapshot where id = :id",
            new MapSqlParameterSource().addValue("id", reportId).addValue("snapshot", write(snapshot)));
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize report snapshot", exception);
        }
    }

    private int deleteFormalOfdFiles(Path root) {
        Path formal = root.toAbsolutePath().normalize().resolve("formal").normalize();
        if (!formal.startsWith(root.toAbsolutePath().normalize()) || !Files.isDirectory(formal)) {
            return 0;
        }
        try (var stream = Files.walk(formal)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".ofd"))
                .toList();
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
            return files.size();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to remove local formal OFD files", exception);
        }
    }

    private record ReportRow(String reportId, String caseId, String reportNo, String pathologyNo,
                             LocalDateTime reportDate, String grossExam, String microscopicExam,
                             String clinicalDiagnosis, String finalDiagnosis, String richTextContent,
                             String renderSnapshot, String reviewerName, String signedByName,
                             LocalDateTime signedAt, LocalDateTime submittedAt, LocalDateTime publishedAt,
                             String patientName, String patientId, String patientGender, String patientAge,
                             String submittingDepartmentName, String submittingDoctorName, LocalDate submissionDate,
                             String phone, String bedNo, String inpatientNo, String wardName) {}

    private record VersionRow(String id, String reportId, String caseId, int versionNo, String renderSnapshot,
                              String finalDiagnosisSnapshot, String signedByName, LocalDateTime signedAt) {}

    private record MaintenanceResult(int reportCount, int versionCount, int artifactCount, int detachedCount,
                                     int collectedArtifactCount, int failedFileCount) {}
}
