package com.company.cli.database;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseLegacyDictionaryHtmlRendererTest {

    private final DatabaseLegacyDictionaryHtmlRenderer renderer = new DatabaseLegacyDictionaryHtmlRenderer();

    @Test
    void shouldRenderLegacySectionsUsingTableAndColumnComments() {
        DatabaseTableReport table = new DatabaseTableReport(
            "SYSDBA",
            "USERS",
            "系统用户表",
            List.of(
                new DatabaseColumnReport("ID", "VARCHAR2(64)", false, null, "主键ID", true, List.of("PK_USERS"), List.of()),
                new DatabaseColumnReport("LOGIN_NAME", "VARCHAR2(64)", false, null, "登录名", false, List.of(), List.of())),
            List.of(new DatabaseUniqueConstraintReport("PK_USERS", true, List.of("ID"))),
            List.of(),
            List.of());
        DatabaseDictionaryReport report = new DatabaseDictionaryReport(
            Instant.parse("2026-06-26T08:00:00Z"),
            DatabaseDictionaryReportStatus.OK,
            DatabaseDictionaryScope.VISIBLE_ALL,
            List.of(new DatabaseSourceReport(
                List.of("bl-center"),
                "jdbc:dm://127.0.0.1:5236",
                "SYSDBA",
                List.of(new DatabaseOwnerReport("SYSDBA", List.of(table))))),
            new DatabaseDictionarySummary(1, 1, 1, 2, 0, 0));

        String html = renderer.render(report);

        assertThat(html).contains("1.4.1 表清单");
        assertThat(html).contains("USERS");
        assertThat(html).contains("系统用户表");
        assertThat(html).contains("USERS[系统用户表]");
        assertThat(html).contains("<td>主键ID</td>");
        assertThat(html).contains("<td>登录名</td>");
        assertThat(html).doesNotContain("[null]");
        assertThat(html).doesNotContain(">null<");
    }

    @Test
    void shouldFallbackTableDisplayValuesToCodeWhenCommentsAreMissing() {
        DatabaseTableReport table = new DatabaseTableReport(
            "SYSDBA",
            "EMPTY_TABLE",
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of());
        DatabaseDictionaryReport report = new DatabaseDictionaryReport(
            Instant.parse("2026-06-26T08:00:00Z"),
            DatabaseDictionaryReportStatus.OK,
            DatabaseDictionaryScope.VISIBLE_ALL,
            List.of(new DatabaseSourceReport(
                List.of("bl-center"),
                "jdbc:dm://127.0.0.1:5236",
                "SYSDBA",
                List.of(new DatabaseOwnerReport("SYSDBA", List.of(table))))),
            new DatabaseDictionarySummary(1, 1, 1, 0, 0, 0));

        String html = renderer.render(report);

        assertThat(html).contains("EMPTY_TABLE[EMPTY_TABLE]");
        assertThat(html).contains("<td>EMPTY_TABLE</td><td>EMPTY_TABLE</td><td>EMPTY_TABLE</td>");
        assertThat(html).doesNotContain("[null]");
        assertThat(html).doesNotContain(">null<");
    }

    @Test
    void shouldFallbackToCodesWhenTableAndColumnCommentsAreMissing() {
        DatabaseTableReport table = new DatabaseTableReport(
            "SYSDBA",
            "READER",
            null,
            List.of(
                new DatabaseColumnReport("READER_ID", "INT", false, null, null, true, List.of("PK_READER"), List.of()),
                new DatabaseColumnReport("NAME", "VARCHAR(30)", true, null, null, false, List.of(), List.of())),
            List.of(new DatabaseUniqueConstraintReport("PK_READER", true, List.of("READER_ID"))),
            List.of(),
            List.of());
        DatabaseDictionaryReport report = new DatabaseDictionaryReport(
            Instant.parse("2026-06-29T08:00:00Z"),
            DatabaseDictionaryReportStatus.OK,
            DatabaseDictionaryScope.VISIBLE_ALL,
            List.of(new DatabaseSourceReport(
                List.of("bl-center"),
                "jdbc:dm://127.0.0.1:5236",
                "SYSDBA",
                List.of(new DatabaseOwnerReport("SYSDBA", List.of(table))))),
            new DatabaseDictionarySummary(1, 1, 1, 2, 0, 0));

        String html = renderer.render(report);

        assertThat(html).contains("READER[READER]");
        assertThat(html).contains("<td>READER</td><td>READER</td><td>READER</td>");
        assertThat(html).contains("<td>READER_ID</td><td>READER_ID</td>");
        assertThat(html).contains("<td>NAME</td><td>NAME</td>");
    }
}
