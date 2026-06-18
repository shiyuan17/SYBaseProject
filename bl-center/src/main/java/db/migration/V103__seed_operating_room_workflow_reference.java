package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V103__seed_operating_room_workflow_reference extends BaseJavaMigration {

    private static final String ROOT_CATEGORY_CODE = "WORKFLOW_REFERENCE";
    private static final String ROOT_CATEGORY_ID = "SCC_WORKFLOW_REFERENCE";
    private static final String CATEGORY_ID = "SCC_WORKFLOW_REFERENCE_OPERATING_ROOM";
    private static final String CATEGORY_CODE = "OPERATING_ROOM";
    private static final String CATEGORY_NAME = "手术楼/手术室";
    private static final String CATEGORY_TYPE = "WORKFLOW_REFERENCE";
    private static final String ITEM_VALUE_TYPE = "STRING";

    private static final List<BuildingSeed> BUILDINGS = List.of(
        new BuildingSeed(
            "SCC_WORKFLOW_REFERENCE_OPERATING_ROOM_B001",
            "B001",
            "惠侨楼",
            10,
            List.of(
                new RoomSeed("SCI_WORKFLOW_OPERATING_ROOM_B001_OR101", "WORKFLOW_REFERENCE.OPERATING_ROOM.B001.OR101", "手术室 1", "OR-101", 10),
                new RoomSeed("SCI_WORKFLOW_OPERATING_ROOM_B001_OR102", "WORKFLOW_REFERENCE.OPERATING_ROOM.B001.OR102", "手术室 2", "OR-102", 20),
                new RoomSeed("SCI_WORKFLOW_OPERATING_ROOM_B001_OR103", "WORKFLOW_REFERENCE.OPERATING_ROOM.B001.OR103", "手术室 3", "OR-103", 30)
            )),
        new BuildingSeed(
            "SCC_WORKFLOW_REFERENCE_OPERATING_ROOM_B002",
            "B002",
            "门诊医技楼",
            20,
            List.of(
                new RoomSeed("SCI_WORKFLOW_OPERATING_ROOM_B002_OR201", "WORKFLOW_REFERENCE.OPERATING_ROOM.B002.OR201", "日间手术室 A", "OR-201", 10),
                new RoomSeed("SCI_WORKFLOW_OPERATING_ROOM_B002_OR202", "WORKFLOW_REFERENCE.OPERATING_ROOM.B002.OR202", "日间手术室 B", "OR-202", 20)
            )),
        new BuildingSeed(
            "SCC_WORKFLOW_REFERENCE_OPERATING_ROOM_B003",
            "B003",
            "感染病楼",
            30,
            List.of(
                new RoomSeed("SCI_WORKFLOW_OPERATING_ROOM_B003_OR301", "WORKFLOW_REFERENCE.OPERATING_ROOM.B003.OR301", "负压手术室", "OR-301", 10)
            ))
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "SYSTEM_CONFIG_CATEGORIES")
            || !tableExists(connection, "SYSTEM_CONFIG_ITEMS")) {
            return;
        }

        String rootCategoryId = findCategoryIdByCode(connection, ROOT_CATEGORY_CODE);
        if (rootCategoryId == null) {
            rootCategoryId = ROOT_CATEGORY_ID;
        }
        ensureCategory(connection, CATEGORY_ID, rootCategoryId, CATEGORY_CODE, CATEGORY_NAME, 190);
        for (BuildingSeed building : BUILDINGS) {
            ensureCategory(
                connection,
                building.id(),
                CATEGORY_ID,
                building.code(),
                building.name(),
                building.sortOrder());
            for (RoomSeed room : building.rooms()) {
                ensureItem(connection, room.id(), building.id(), room.configKey(), room.roomName(), room.roomId(), room.sortOrder());
            }
        }
    }

    private void ensureCategory(
        Connection connection,
        String id,
        String parentId,
        String code,
        String name,
        int sortOrder
    ) throws SQLException {
        String existingId = findCategoryIdByCode(connection, code);
        if (existingId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_categories
                    (id, parent_id, category_code, category_name, category_type, sort_order, enabled, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, 1, ?, ?)
                """)) {
                Timestamp now = now();
                insert.setString(1, id);
                insert.setString(2, parentId);
                insert.setString(3, code);
                insert.setString(4, name);
                insert.setString(5, CATEGORY_TYPE);
                insert.setInt(6, sortOrder);
                insert.setTimestamp(7, now);
                insert.setTimestamp(8, now);
                insert.executeUpdate();
            }
            return;
        }

        try (PreparedStatement update = connection.prepareStatement("""
            update system_config_categories
            set parent_id = ?,
                category_name = ?,
                category_type = ?,
                sort_order = ?,
                enabled = 1,
                updated_at = ?
            where id = ?
            """)) {
            update.setString(1, parentId);
            update.setString(2, name);
            update.setString(3, CATEGORY_TYPE);
            update.setInt(4, sortOrder);
            update.setTimestamp(5, now());
            update.setString(6, existingId);
            update.executeUpdate();
        }
    }

    private void ensureItem(
        Connection connection,
        String id,
        String categoryId,
        String configKey,
        String configName,
        String configValue,
        int sortOrder
    ) throws SQLException {
        String existingId = findItemIdByKey(connection, configKey);
        if (existingId == null) {
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_items
                    (id, category_id, config_key, config_name, config_value, value_type, sort_order,
                     enabled, remarks, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                """)) {
                Timestamp now = now();
                insert.setString(1, id);
                insert.setString(2, categoryId);
                insert.setString(3, configKey);
                insert.setString(4, configName);
                insert.setString(5, configValue);
                insert.setString(6, ITEM_VALUE_TYPE);
                insert.setInt(7, sortOrder);
                insert.setString(8, "手术楼/手术室预置项");
                insert.setTimestamp(9, now);
                insert.setTimestamp(10, now);
                insert.executeUpdate();
            }
            return;
        }

        try (PreparedStatement update = connection.prepareStatement("""
            update system_config_items
            set category_id = ?,
                config_name = ?,
                config_value = ?,
                value_type = ?,
                sort_order = ?,
                enabled = 1,
                remarks = ?,
                updated_at = ?
            where id = ?
            """)) {
            update.setString(1, categoryId);
            update.setString(2, configName);
            update.setString(3, configValue);
            update.setString(4, ITEM_VALUE_TYPE);
            update.setInt(5, sortOrder);
            update.setString(6, "手术楼/手术室预置项");
            update.setTimestamp(7, now());
            update.setString(8, existingId);
            update.executeUpdate();
        }
    }

    private String findCategoryIdByCode(Connection connection, String categoryCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select id
            from system_config_categories
            where category_code = ?
            """)) {
            statement.setString(1, categoryCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private String findItemIdByKey(Connection connection, String configKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select id
            from system_config_items
            where config_key = ?
            """)) {
            statement.setString(1, configKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
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
            try (ResultSet resultSet = connection.getMetaData().getTables(null, null, null, null)) {
                while (resultSet.next()) {
                    String existing = resultSet.getString("TABLE_NAME");
                    if (existing != null && existing.equalsIgnoreCase(tableName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private record BuildingSeed(
        String id,
        String code,
        String name,
        int sortOrder,
        List<RoomSeed> rooms
    ) {
    }

    private record RoomSeed(
        String id,
        String configKey,
        String roomName,
        String roomId,
        int sortOrder
    ) {
    }
}
