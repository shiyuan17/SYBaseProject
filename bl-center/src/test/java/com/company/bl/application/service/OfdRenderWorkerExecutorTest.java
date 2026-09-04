package com.company.bl.application.service;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.infrastructure.config.ReportStorageProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfdRenderWorkerExecutorTest {

    @TempDir
    Path storageRoot;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRenderExactMetadataAndMinimumHeightRegressionWithinBoundedWorker() throws Exception {
        Path temporaryDirectory = storageRoot.resolve(".tmp");
        Files.createDirectories(temporaryDirectory);
        JsonNode snapshot = exactRegressionSnapshot();
        Path target = temporaryDirectory.resolve("exact-layout-regression.ofd");

        executor().render(target, snapshot, report(snapshot.toString()), "签发医生",
            LocalDateTime.of(2026, 8, 31, 12, 0), List.of());

        assertThat(target).isRegularFile();
        try (OFDReader reader = new OFDReader(target)) {
            String extractedText = String.join("", new ContentExtractor(reader).extractAll());
            assertThat(extractedText)
                .contains("测试医院")
                .contains("字段1")
                .contains("较长的字段值用于验证换行")
                .contains("肉眼所见")
                .contains("光镜所见")
                .contains("病理诊断及建议")
                .contains("签发医生");
        }
        assertThat(readXmlEntries(target))
            .contains("Value=\"255 255 255\"")
            .contains("Value=\"221 221 221\"")
            .contains("Value=\"215 25 32\"")
            .contains("Value=\"0 128 0\"");
        try (var temporaryFiles = Files.list(temporaryDirectory)) {
            assertThat(temporaryFiles.map(Path::getFileName).map(Path::toString).toList())
                .containsExactly("exact-layout-regression.ofd");
        }
    }

    @Test
    void shouldRenderLongUnbrokenTextLargeMinHeightAndImageRowsInBoundedWorker() throws Exception {
        Path temporaryDirectory = storageRoot.resolve(".tmp");
        Files.createDirectories(temporaryDirectory);
        byte[] png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        List<DiagnosticReportRepository.ReportRenderAsset> assets = new java.util.ArrayList<>();
        StringBuilder images = new StringBuilder();
        for (int index = 0; index < 4; index++) {
            String assetId = "RRA-00000000-0000-0000-0000-00000000000" + index;
            String storageKey = "assets/image-" + index + ".png";
            Path image = storageRoot.resolve(storageKey);
            Files.createDirectories(image.getParent());
            Files.write(image, png);
            assets.add(new DiagnosticReportRepository.ReportRenderAsset(
                assetId, "CASE-1", image.getFileName().toString(), storageKey, "image/png",
                Files.size(image), "a".repeat(64), LocalDateTime.now()));
            if (index > 0) {
                images.append(',');
            }
            images.append("{\"assetId\":\"").append(assetId).append("\",\"title\":\"image.png\"}");
        }
        String unbrokenText = "A".repeat(12_000);
        JsonNode snapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "templateCode": "wysiwyg-template",
              "hospitalName": "测试医院",
              "reportTitle": "病理检查报告单",
              "metaFields": [],
              "footerFields": [],
              "sections": [{
                "label": "镜下所见：",
                "minHeight": 5000,
                "value": "%s",
                "images": [%s]
              }]
            }
            """.formatted(unbrokenText, images));
        Path target = temporaryDirectory.resolve("worker-success.ofd");

        executor().render(target, snapshot, report(snapshot.toString()), "签发医生",
            LocalDateTime.of(2026, 8, 31, 12, 0), assets);

        assertThat(target).isRegularFile();
        try (OFDReader reader = new OFDReader(target)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            assertThat(String.join("", new ContentExtractor(reader).extractAll()))
                .contains("测试医院")
                .contains("签发医生");
        }
        try (var temporaryFiles = Files.list(temporaryDirectory)) {
            assertThat(temporaryFiles.map(Path::getFileName).map(Path::toString).toList())
                .containsExactly("worker-success.ofd");
        }
    }

    @Test
    void shouldFailDeterministicallyAndRemovePartialOutputWhenAssetIsMissing() throws Exception {
        Path temporaryDirectory = storageRoot.resolve(".tmp");
        Files.createDirectories(temporaryDirectory);
        String assetId = "RRA-11111111-1111-1111-1111-111111111111";
        JsonNode snapshot = objectMapper.readTree("""
            {
              "schemaVersion": 1,
              "templateCode": "wysiwyg-template",
              "metaFields": [],
              "footerFields": [],
              "sections": [{
                "label": "镜下所见：",
                "value": "图片缺失",
                "images": [{"assetId": "%s"}]
              }]
            }
            """.formatted(assetId));
        DiagnosticReportRepository.ReportRenderAsset missingAsset =
            new DiagnosticReportRepository.ReportRenderAsset(
                assetId, "CASE-1", "missing.png", "assets/missing.png", "image/png",
                1, "a".repeat(64), LocalDateTime.now());
        Path target = temporaryDirectory.resolve("worker-failure.ofd");

        assertThatThrownBy(() -> executor().render(
            target, snapshot, report(snapshot.toString()), "签发医生", LocalDateTime.now(), List.of(missingAsset)))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("图片文件已丢失");

        assertThat(target).doesNotExist();
        try (var temporaryFiles = Files.list(temporaryDirectory)) {
            assertThat(temporaryFiles).isEmpty();
        }
    }

    @Test
    void shouldRejectAssetOutsideStorageRootWithoutStartingWorker() throws Exception {
        Path temporaryDirectory = storageRoot.resolve(".tmp");
        Files.createDirectories(temporaryDirectory);
        Path target = temporaryDirectory.resolve("worker-invalid-asset.ofd");
        DiagnosticReportRepository.ReportRenderAsset outsideAsset =
            new DiagnosticReportRepository.ReportRenderAsset(
                "RRA-22222222-2222-2222-2222-222222222222", "CASE-1", "outside.png",
                "../outside.png", "image/png", 1, "a".repeat(64), LocalDateTime.now());

        assertThatThrownBy(() -> executor().render(
            target, objectMapper.createObjectNode(), report("{}"), "签发医生", LocalDateTime.now(),
            List.of(outsideAsset)))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("图片文件已丢失");

        assertThat(target).doesNotExist();
        try (var temporaryFiles = Files.list(temporaryDirectory)) {
            assertThat(temporaryFiles).isEmpty();
        }
    }

    private OfdRenderWorkerExecutor executor() {
        ReportStorageProperties properties = new ReportStorageProperties();
        properties.setRootDir(storageRoot);
        properties.setOfdWorkerMaxHeapMb(256);
        properties.setOfdWorkerTimeout(Duration.ofSeconds(30));
        return new OfdRenderWorkerExecutor(objectMapper, properties);
    }

    private JsonNode exactRegressionSnapshot() {
        var snapshot = objectMapper.createObjectNode();
        snapshot.put("schemaVersion", 1);
        snapshot.put("templateCode", "wysiwyg-template");
        snapshot.put("hospitalName", "测试医院");
        snapshot.put("reportTitle", "病理检查报告单");
        snapshot.put("accentColor", "#d71920");
        snapshot.put("deliveredAt", "2026-08-31");
        snapshot.put("note", "注：本报告为脱敏布局回归样例。");
        var metaFields = snapshot.putArray("metaFields");
        for (int index = 1; index <= 13; index++) {
            var field = metaFields.addObject();
            field.put("label", "字段" + index + "：");
            field.put("value", index == 13 ? "" : "较长的字段值用于验证换行-" + index);
            field.put("class", index >= 9 ? "span-2" : "");
        }
        var footerFields = snapshot.putArray("footerFields");
        footerFields.addObject().put("label", "审核医师：").put("value", "审核医生");
        footerFields.addObject().put("label", "取材医师：").put("value", "取材医生");
        footerFields.addObject().put("label", "签名：").put("value", "电子签名");
        var sections = snapshot.putArray("sections");
        sections.addObject().put("label", "肉眼所见：").put("value", "送检组织一块，大小适中。")
            .put("minHeight", 82).putArray("images");
        sections.addObject().put("label", "光镜所见：").put("value", "组织结构清晰，细胞形态可见。")
            .put("minHeight", 160).putArray("images");
        sections.addObject().put("label", "病理诊断及建议：").put("value", "符合慢性炎症改变。")
            .put("minHeight", 92).putArray("images");
        return snapshot;
    }

    private String readXmlEntries(Path target) throws Exception {
        StringBuilder xml = new StringBuilder();
        try (ZipFile archive = new ZipFile(target.toFile())) {
            var entries = archive.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().endsWith(".xml")) {
                    try (var stream = archive.getInputStream(entry)) {
                        xml.append(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
        }
        return xml.toString();
    }

    private DiagnosticReportRepository.PathologyReport report(String renderSnapshot) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 12, 0);
        return new DiagnosticReportRepository.PathologyReport(
            "RPT-1", "CASE-1", "TASK-1", "RPT-20260831-001", "P20260001", "ROUTINE", 1,
            "SIGNED", 1, "ROUTINE", "张三", "DEPT-1", "病理科", now, "大体所见", "镜下所见",
            "临床诊断", "病理诊断", now, "REVIEWER-1", "审核医生", now, "SIGNER-1", "签发医生",
            now, null, "<p>报告</p>", renderSnapshot, "备注", now, now);
    }
}
