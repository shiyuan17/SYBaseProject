package com.company.bl.application.service;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class SpecimenManagementExportBuilder {

    private static final DateTimeFormatter EXPORT_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    byte[] buildSpecimenManagementExport(List<SpecimenWorkflowRepository.SpecimenManagementExportRow> rows) {
        List<List<String>> sheetRows = new ArrayList<>();
        sheetRows.add(List.of(
            "行号",
            "标本ID",
            "申请单号",
            "姓名",
            "年龄",
            "性别",
            "标本名称",
            "项目名称",
            "组织数量",
            "离体时间",
            "离体操作人",
            "固定时间",
            "固定操作人",
            "送检医生",
            "标本状态",
            "手术所见",
            "增加标本时间",
            "增加标本人",
            "标本确认时间",
            "标本确认人",
            "标本入库时间",
            "标本入库人",
            "标本出库时间",
            "标本出库人",
            "标本签收时间",
            "标本签收人",
            "标本拒收人",
            "标本拒收时间",
            "送检日期",
            "住院号",
            "床号",
            "所属分区",
            "打印标志/次",
            "临床描述",
            "临床诊断",
            "临床建议",
            "有无感染",
            "物流人员",
            "拒收原因",
            "送检科室"
        ));
        for (int index = 0; index < rows.size(); index++) {
            SpecimenWorkflowRepository.SpecimenManagementExportRow row = rows.get(index);
            sheetRows.add(List.of(
                Integer.toString(index + 1),
                defaultString(row.specimenId()),
                defaultString(row.applicationNo()),
                defaultString(row.patientName()),
                defaultString(row.patientAge()),
                defaultString(row.patientGender()),
                defaultString(row.specimenName()),
                defaultString(row.projectName()),
                defaultNumber(row.specimenCount()),
                formatExportDateTime(row.specimenRemovalAt()),
                defaultString(row.specimenRemovalOperatorName()),
                formatExportDateTime(row.fixationCompletedAt()),
                defaultString(row.fixationOperatorName()),
                defaultString(row.submittingDoctorName()),
                defaultString(row.displayStatus()),
                defaultString(row.surgeryFindings()),
                formatExportDateTime(row.registeredAt()),
                defaultString(row.registrationOperatorName()),
                formatExportDateTime(row.specimenConfirmedAt()),
                defaultString(row.specimenConfirmedByName()),
                formatExportDateTime(row.checkedInAt()),
                defaultString(row.checkedInByName()),
                formatExportDateTime(row.outboundAt()),
                defaultString(row.outboundUserName()),
                formatExportDateTime(row.signedAt()),
                defaultString(row.signedByName()),
                defaultString(row.rejectedByName()),
                formatExportDateTime(row.rejectedAt()),
                formatExportDate(row.submissionDate()),
                defaultString(row.inpatientNo()),
                defaultString(row.bedNo()),
                defaultString(row.wardName()),
                defaultString(row.printFlag()),
                defaultString(row.clinicalDescription()),
                defaultString(row.clinicalDiagnosis()),
                defaultString(row.clinicalSuggestion()),
                defaultString(row.infectionFlag()),
                defaultString(row.logisticsStaffName()),
                defaultString(row.rejectionReason()),
                defaultString(row.submittingDepartmentName())
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
            throw new IllegalStateException("Failed to build specimen management export", exception);
        }
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
                <sheet name="标本综合信息" sheetId="1" r:id="rId1"/>
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

    private String formatExportDate(LocalDate value) {
        return value == null ? "" : value.format(EXPORT_DATE_FORMATTER);
    }

    private String formatExportDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXPORT_TIME_FORMATTER);
    }

    private String defaultNumber(Integer value) {
        return value == null ? "" : value.toString();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
