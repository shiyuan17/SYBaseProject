package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V99__add_equipment_usage_records extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        createEquipmentUsageRecordsTableIfMissing(context.getConnection());
    }

    private void createEquipmentUsageRecordsTableIfMissing(Connection connection) throws SQLException {
        if (tableExists(connection, "EQUIPMENT_USAGE_RECORDS")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE equipment_usage_records (
                    id VARCHAR(64) NOT NULL,
                    equipment_id VARCHAR(64),
                    equipment_category_snapshot VARCHAR(64) NOT NULL,
                    equipment_name_snapshot VARCHAR(100) NOT NULL,
                    commonly_used INTEGER NOT NULL DEFAULT 0,
                    started_at TIMESTAMP NOT NULL,
                    ended_at TIMESTAMP NOT NULL,
                    runtime_hours DECIMAL(8, 2) NOT NULL,
                    diagnosis_count INTEGER NOT NULL DEFAULT 0,
                    equipment_condition VARCHAR(32) NOT NULL,
                    operator_user_id VARCHAR(64),
                    operator_name VARCHAR(100) NOT NULL,
                    usage_content VARCHAR(500),
                    remarks VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT pk_equipment_usage_records PRIMARY KEY (id),
                    CONSTRAINT fk_equipment_usage_records_equipment FOREIGN KEY (equipment_id) REFERENCES equipment_records (id)
                )
                """);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select count(*)
            from information_schema.tables
            where upper(table_name) = upper(?)
            """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
        }
        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, null, null)) {
            while (resultSet.next()) {
                String existing = resultSet.getString("TABLE_NAME");
                if (existing != null && existing.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
