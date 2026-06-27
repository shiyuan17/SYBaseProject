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

class V110__backfill_dm_table_and_column_commentsTest {

    @Test
    void shouldSkipBackfillForNonDmDatabase() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Context context = mock(Context.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        new V110__backfill_dm_table_and_column_comments().migrate(context);

        verify(connection, never()).prepareStatement("select 1 from all_tables where owner = ? and table_name = ?");
    }

    @Test
    void shouldIncludeKeyLegacyNullTablesInBackfillPlan() {
        Map<String, DmTableCommentDefinition> tables = V110__backfill_dm_table_and_column_comments.commentDefinitions()
            .stream()
            .collect(Collectors.toMap(DmTableCommentDefinition::tableName, Function.identity()));

        assertThat(tables).containsKeys(
            "MEDICAL_ORDER_PACKAGES",
            "USER_NOTIFICATIONS",
            "TECHNICAL_SPECIMEN_REGISTRATIONS",
            "SYSTEM_CONFIG_ITEM_DEPARTMENTS",
            "WORKSTATION_DAILY_CLEARS");

        assertThat(tables.get("MEDICAL_ORDER_PACKAGES").comment()).isEqualTo("医嘱套餐表");
        assertThat(tables.get("USER_NOTIFICATIONS").columns())
            .extracting(DmColumnCommentDefinition::columnName)
            .contains("TITLE", "CONTENT", "STATUS", "READ_AT");
        assertThat(tables.get("WORKSTATION_DAILY_CLEARS").columns())
            .extracting(DmColumnCommentDefinition::columnName)
            .containsExactlyInAnyOrder(
                "ID",
                "WORKSTATION_TYPE",
                "WORK_DATE",
                "OPERATOR_USER_ID",
                "OPERATOR_NAME",
                "CLEARED_AT",
                "CLEAR_STATUS",
                "OPERATOR_IP",
                "CREATED_AT");
    }
}
