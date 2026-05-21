package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class V21__seed_m3_timeout_configs extends BaseJavaMigration {

    private static final String CATEGORY_ID = "SCC_GENERAL";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        upsertConfigItem(connection, "SCI_M3_GROSSING_TIMEOUT", "technical.timeout.grossingMinutes",
            "取材任务超时分钟数", "240", "INTEGER", 30, "M3 取材任务超时阈值");
        upsertConfigItem(connection, "SCI_M3_DEHYDRATION_TIMEOUT", "technical.timeout.dehydrationMinutes",
            "脱水任务超时分钟数", "720", "INTEGER", 31, "M3 脱水任务超时阈值");
        upsertConfigItem(connection, "SCI_M3_STAINING_TIMEOUT", "technical.timeout.stainingMinutes",
            "染色任务超时分钟数", "240", "INTEGER", 32, "M3 染色任务超时阈值");
    }

    private void upsertConfigItem(Connection connection,
                                  String id,
                                  String configKey,
                                  String configName,
                                  String configValue,
                                  String valueType,
                                  int sortOrder,
                                  String remarks) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update system_config_items
            set category_id = ?, config_name = ?, config_value = ?, value_type = ?, sort_order = ?,
                enabled = 1, remarks = ?, updated_at = ?
            where id = ? or config_key = ?
            """)) {
            update.setString(1, CATEGORY_ID);
            update.setString(2, configName);
            update.setString(3, configValue);
            update.setString(4, valueType);
            update.setInt(5, sortOrder);
            update.setString(6, remarks);
            update.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(8, id);
            update.setString(9, configKey);
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into system_config_items
                (id, category_id, config_key, config_name, config_value, value_type, sort_order,
                 enabled, remarks, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
            """)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            insert.setString(1, id);
            insert.setString(2, CATEGORY_ID);
            insert.setString(3, configKey);
            insert.setString(4, configName);
            insert.setString(5, configValue);
            insert.setString(6, valueType);
            insert.setInt(7, sortOrder);
            insert.setString(8, remarks);
            insert.setTimestamp(9, now);
            insert.setTimestamp(10, now);
            insert.executeUpdate();
        }
    }
}
