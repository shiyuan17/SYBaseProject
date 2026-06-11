package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class V86__extend_reagent_inventory_legacy_fields extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        extendReagents(connection);
        extendReagentStocks(connection);
        ensureStockEvents(connection);
    }

    private void extendReagents(Connection connection) throws SQLException {
        if (!tableExists(connection, "REAGENTS")) {
            return;
        }
        ensureColumn(connection, "REAGENTS", "REAGENT_TYPE",
            "ALTER TABLE reagents ADD COLUMN reagent_type VARCHAR(64)");
        ensureColumn(connection, "REAGENTS", "REAGENT_USAGE",
            "ALTER TABLE reagents ADD COLUMN reagent_usage VARCHAR(200)");
        ensureColumn(connection, "REAGENTS", "ORDER_DICT_ITEM_ID",
            "ALTER TABLE reagents ADD COLUMN order_dict_item_id VARCHAR(64)");
        ensureColumn(connection, "REAGENTS", "CLONE_NO",
            "ALTER TABLE reagents ADD COLUMN clone_no VARCHAR(100)");
        ensureColumn(connection, "REAGENTS", "RECOMMENDED_DILUTION",
            "ALTER TABLE reagents ADD COLUMN recommended_dilution VARCHAR(100)");
        ensureColumn(connection, "REAGENTS", "APPLICATION_DILUTION",
            "ALTER TABLE reagents ADD COLUMN application_dilution VARCHAR(100)");
        ensureColumn(connection, "REAGENTS", "TEMPLATE_STATUS",
            "ALTER TABLE reagents ADD COLUMN template_status VARCHAR(32)");
        ensureColumn(connection, "REAGENTS", "VALIDITY_DAYS",
            "ALTER TABLE reagents ADD COLUMN validity_days INTEGER");
        ensureColumn(connection, "REAGENTS", "DEFAULT_STOCK_THRESHOLD",
            "ALTER TABLE reagents ADD COLUMN default_stock_threshold DECIMAL(18, 2)");
        ensureColumn(connection, "REAGENTS", "STAIN_CAPACITY",
            "ALTER TABLE reagents ADD COLUMN stain_capacity DECIMAL(18, 2)");
        ensureColumn(connection, "REAGENTS", "STAIN_THRESHOLD",
            "ALTER TABLE reagents ADD COLUMN stain_threshold DECIMAL(18, 2)");
        ensureColumn(connection, "REAGENTS", "CREATED_BY_USER_ID",
            "ALTER TABLE reagents ADD COLUMN created_by_user_id VARCHAR(64)");
        ensureColumn(connection, "REAGENTS", "CREATED_BY_NAME",
            "ALTER TABLE reagents ADD COLUMN created_by_name VARCHAR(100)");
        ensureColumn(connection, "REAGENTS", "UPDATED_BY_USER_ID",
            "ALTER TABLE reagents ADD COLUMN updated_by_user_id VARCHAR(64)");
        ensureColumn(connection, "REAGENTS", "UPDATED_BY_NAME",
            "ALTER TABLE reagents ADD COLUMN updated_by_name VARCHAR(100)");

        execute(connection, """
            update reagents
            set template_status = case when enabled = 1 then 'ENABLED' else 'DISABLED' end
            where template_status is null
            """);
        execute(connection, """
            update reagents
            set default_stock_threshold = default_low_stock_threshold
            where default_stock_threshold is null and default_low_stock_threshold is not null
            """);
    }

    private void extendReagentStocks(Connection connection) throws SQLException {
        if (!tableExists(connection, "REAGENT_STOCKS")) {
            return;
        }
        ensureColumn(connection, "REAGENT_STOCKS", "INITIAL_QUANTITY",
            "ALTER TABLE reagent_stocks ADD COLUMN initial_quantity DECIMAL(18, 2)");
        ensureColumn(connection, "REAGENT_STOCKS", "REMAINING_QUANTITY",
            "ALTER TABLE reagent_stocks ADD COLUMN remaining_quantity DECIMAL(18, 2)");
        ensureColumn(connection, "REAGENT_STOCKS", "PRODUCTION_DATE",
            "ALTER TABLE reagent_stocks ADD COLUMN production_date DATE");
        ensureColumn(connection, "REAGENT_STOCKS", "INBOUND_AT",
            "ALTER TABLE reagent_stocks ADD COLUMN inbound_at TIMESTAMP");
        ensureColumn(connection, "REAGENT_STOCKS", "TEST_REMINDER_THRESHOLD",
            "ALTER TABLE reagent_stocks ADD COLUMN test_reminder_threshold INTEGER");
        ensureColumn(connection, "REAGENT_STOCKS", "EXPIRY_REMINDER_THRESHOLD",
            "ALTER TABLE reagent_stocks ADD COLUMN expiry_reminder_threshold INTEGER");
        ensureColumn(connection, "REAGENT_STOCKS", "RECOMMENDED_DILUTION",
            "ALTER TABLE reagent_stocks ADD COLUMN recommended_dilution VARCHAR(100)");
        ensureColumn(connection, "REAGENT_STOCKS", "APPLICATION_DILUTION",
            "ALTER TABLE reagent_stocks ADD COLUMN application_dilution VARCHAR(100)");
        ensureColumn(connection, "REAGENT_STOCKS", "STAIN_CAPACITY",
            "ALTER TABLE reagent_stocks ADD COLUMN stain_capacity DECIMAL(18, 2)");
        ensureColumn(connection, "REAGENT_STOCKS", "STAIN_THRESHOLD",
            "ALTER TABLE reagent_stocks ADD COLUMN stain_threshold DECIMAL(18, 2)");
        ensureColumn(connection, "REAGENT_STOCKS", "VALIDITY_DAYS",
            "ALTER TABLE reagent_stocks ADD COLUMN validity_days INTEGER");
        ensureColumn(connection, "REAGENT_STOCKS", "TESTED_AT",
            "ALTER TABLE reagent_stocks ADD COLUMN tested_at TIMESTAMP");
        ensureColumn(connection, "REAGENT_STOCKS", "STARTED_AT",
            "ALTER TABLE reagent_stocks ADD COLUMN started_at TIMESTAMP");
        ensureColumn(connection, "REAGENT_STOCKS", "FINISHED_AT",
            "ALTER TABLE reagent_stocks ADD COLUMN finished_at TIMESTAMP");
        ensureColumn(connection, "REAGENT_STOCKS", "CREATED_BY_USER_ID",
            "ALTER TABLE reagent_stocks ADD COLUMN created_by_user_id VARCHAR(64)");
        ensureColumn(connection, "REAGENT_STOCKS", "CREATED_BY_NAME",
            "ALTER TABLE reagent_stocks ADD COLUMN created_by_name VARCHAR(100)");
        ensureColumn(connection, "REAGENT_STOCKS", "UPDATED_BY_USER_ID",
            "ALTER TABLE reagent_stocks ADD COLUMN updated_by_user_id VARCHAR(64)");
        ensureColumn(connection, "REAGENT_STOCKS", "UPDATED_BY_NAME",
            "ALTER TABLE reagent_stocks ADD COLUMN updated_by_name VARCHAR(100)");

        execute(connection, """
            update reagent_stocks
            set initial_quantity = stock_quantity
            where initial_quantity is null and stock_quantity is not null
            """);
        execute(connection, """
            update reagent_stocks
            set remaining_quantity = stock_quantity
            where remaining_quantity is null and stock_quantity is not null
            """);
        execute(connection, """
            update reagent_stocks
            set inbound_at = created_at
            where inbound_at is null
            """);
    }

    private void ensureStockEvents(Connection connection) throws SQLException {
        if (tableExists(connection, "REAGENT_STOCK_EVENTS")) {
            return;
        }
        execute(connection, """
            CREATE TABLE reagent_stock_events (
                id VARCHAR(64) NOT NULL,
                stock_id VARCHAR(64) NOT NULL,
                event_type VARCHAR(32) NOT NULL,
                quantity_delta DECIMAL(18, 2),
                quantity_before DECIMAL(18, 2),
                quantity_after DECIMAL(18, 2),
                occurred_at TIMESTAMP NOT NULL,
                operator_user_id VARCHAR(64),
                operator_name VARCHAR(100),
                remarks VARCHAR(500),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT pk_reagent_stock_events PRIMARY KEY (id),
                CONSTRAINT fk_reagent_stock_events_stock FOREIGN KEY (stock_id) REFERENCES reagent_stocks (id)
            )
            """);
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getColumns(null, null, candidate, null)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("COLUMN_NAME"), columnName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> identifierCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.toUpperCase());
        candidates.add(identifier.toLowerCase());
        return new ArrayList<>(candidates);
    }

    private boolean identifierEquals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
