package db.migration;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V113__backfill_dm_english_placeholder_commentsTest {

    @Test
    void shouldSkipBackfillForNonDmDatabase() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Context context = mock(Context.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        new V113__backfill_dm_english_placeholder_comments().migrate(context);

        verify(connection, never()).prepareStatement("select 1 from all_tables where owner = ? and table_name = ?");
    }

    @Test
    void shouldIncludeEnglishPlaceholderRepairTargets() {
        Map<String, DmTableCommentDefinition> tables = V113__backfill_dm_english_placeholder_comments.commentDefinitions()
            .stream()
            .collect(Collectors.toMap(DmTableCommentDefinition::tableName, Function.identity(), (left, right) -> right));

        assertThat(tables).containsKeys(
            "WORKFLOW_EVENTS",
            "SPECIMENS",
            "MEDICAL_ORDERS",
            "REAGENTS",
            "REAGENT_STOCKS",
            "TRANSPORT_ORDERS");

        assertThat(tables.get("WORKFLOW_EVENTS").columns())
            .extracting(DmColumnCommentDefinition::columnName)
            .contains("APPLICATION_ID", "EVENT_TYPE", "CREATED_AT", "OPERATOR_DEVICE");
        assertThat(tables.get("SPECIMENS").columns())
            .extracting(DmColumnCommentDefinition::columnName)
            .contains("TERMINAL_CODE", "CHECK_IN_STATUS", "SPECIMEN_REMOVAL_OPERATOR_NAME");
        assertThat(tables.get("REAGENTS").columns())
            .extracting(DmColumnCommentDefinition::columnName)
            .contains("REAGENT_TYPE", "APPLICATION_DILUTION", "UPDATED_BY_NAME");
    }
}
