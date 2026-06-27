package com.company.cli.database;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseDictionaryHtmlRendererTest {

    private final DatabaseDictionaryHtmlRenderer renderer = new DatabaseDictionaryHtmlRenderer();

    @Test
    void shouldRenderSearchAnchorsAndTableSections() {
        DatabaseTableReport table = new DatabaseTableReport(
            "SYSDBA",
            "USERS",
            "用户表",
            List.of(new DatabaseColumnReport(
                "ID",
                "VARCHAR2(64)",
                false,
                null,
                "主键",
                true,
                List.of("PK_USERS"),
                List.of())),
            List.of(new DatabaseUniqueConstraintReport("PK_USERS", true, List.of("ID"))),
            List.of(),
            List.of(new DatabaseIndexReport("IDX_USERS_EMAIL", "NONUNIQUE", "NORMAL", List.of("EMAIL"))));
        DatabaseDictionaryReport report = new DatabaseDictionaryReport(
            Instant.parse("2026-06-26T08:00:00Z"),
            DatabaseDictionaryReportStatus.OK,
            DatabaseDictionaryScope.VISIBLE_ALL,
            List.of(new DatabaseSourceReport(
                List.of("auth-center", "bl-center"),
                "jdbc:dm://127.0.0.1:5236",
                "SYSDBA",
                List.of(new DatabaseOwnerReport("SYSDBA", List.of(table))))),
            new DatabaseDictionarySummary(1, 1, 1, 1, 1, 0));

        String html = renderer.render(report);

        assertThat(html).contains("数据库字典");
        assertThat(html).contains("id=\"table-search\"");
        assertThat(html).contains("href=\"#table-sysdba-users\"");
        assertThat(html).contains("details class=\"table-card\"");
        assertThat(html).contains("IDX_USERS_EMAIL");
        assertThat(html).contains("用户表");
    }

    @Test
    void shouldRenderWarningStateForEmptyReport() {
        DatabaseDictionaryReport report = new DatabaseDictionaryReport(
            Instant.parse("2026-06-26T08:00:00Z"),
            DatabaseDictionaryReportStatus.WARNING,
            DatabaseDictionaryScope.VISIBLE_ALL,
            List.of(new DatabaseSourceReport(
                List.of("auth-center"),
                "jdbc:dm://127.0.0.1:5236",
                "SYSDBA",
                List.of())),
            new DatabaseDictionarySummary(1, 0, 0, 0, 0, 0));

        String html = renderer.render(report);

        assertThat(html).contains("status-pill warning");
        assertThat(html).contains("未采集到任何表");
        assertThat(html).contains("visible-all");
    }
}
