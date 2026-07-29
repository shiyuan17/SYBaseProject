package com.company.bl.application.service;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SpecimenManagementExportBuilderTest {

    @Test
    void shouldBuildWorksheetWithExpectedHeadersAndSampleValues() throws IOException {
        SpecimenManagementExportBuilder builder = new SpecimenManagementExportBuilder();

        byte[] workbook = builder.buildSpecimenManagementExport(List.of(
            new SpecimenWorkflowRepository.SpecimenManagementExportRow(
                "22914",
                "APP-001",
                "申请单-001",
                "范湄妲",
                "30岁1月15天",
                "女",
                "胃大体",
                "手术标本检查与诊断(显微摄影)",
                1,
                LocalDateTime.of(2026, 7, 3, 18, 0, 7),
                "周永坚",
                LocalDateTime.of(2026, 7, 3, 19, 0, 7),
                "固定员甲",
                "张宏",
                "待入库",
                "胃体可见隆起",
                LocalDateTime.of(2026, 7, 3, 18, 0, 7),
                "周永坚",
                LocalDateTime.of(2026, 7, 3, 20, 0, 7),
                "确认员甲",
                LocalDateTime.of(2026, 7, 3, 21, 0, 7),
                "入库员甲",
                LocalDateTime.of(2026, 7, 3, 22, 0, 7),
                "出库员甲",
                LocalDateTime.of(2026, 7, 3, 23, 0, 7),
                "签收员甲",
                LocalDateTime.of(2026, 7, 4, 8, 0, 7),
                "拒收员甲",
                LocalDate.of(2026, 7, 3),
                "000092",
                "12A",
                "惠侨楼",
                "1",
                "腹痛",
                "1:糖尿病伴肾并发症",
                "尽快送检",
                "有",
                "物流员甲",
                "容器破损",
                "急诊科")));

        assertThat(workbook).isNotEmpty();
        assertThat(readZipEntry(workbook, "xl/workbook.xml")).contains("标本综合信息");

        String worksheetXml = readZipEntry(workbook, "xl/worksheets/sheet1.xml");
        assertThat(worksheetXml)
            .contains("行号")
            .contains("标本ID")
            .contains("申请单号")
            .contains("送检科室")
            .contains("22914")
            .contains("待入库")
            .contains("物流员甲")
            .contains("容器破损");
    }

    private String readZipEntry(byte[] workbook, String entryName) throws IOException {
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(workbook), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    inputStream.transferTo(outputStream);
                    return outputStream.toString(StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("Missing zip entry: " + entryName);
    }
}
