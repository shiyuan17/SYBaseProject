package db.migration;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class V110__backfill_dm_table_and_column_commentsTest {

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
