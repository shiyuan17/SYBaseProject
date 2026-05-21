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
import java.util.UUID;

public class V28__grant_m3_grossing_reference_query_permissions extends BaseJavaMigration {

    private static final String ROLE_M3_GROSSING = "ROLE_M3_GROSSING";

    private static final List<String> PERMISSION_CODES = List.of(
        "PERM_SYS_BODY_PART_QUERY",
        "PERM_SYS_TEMPLATE_QUERY"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (String permissionCode : PERMISSION_CODES) {
            String permissionId = findPermissionIdByCode(connection, permissionCode);
            if (permissionId == null) {
                throw new SQLException("Missing permission required for M3 grossing reference query grant: " + permissionCode);
            }
            ensureRolePermission(connection, ROLE_M3_GROSSING, permissionId);
        }
    }

    private String findPermissionIdByCode(Connection connection, String permissionCode) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "select id from permissions where permission_code = ? and enabled = 1")) {
            query.setString(1, permissionCode);
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private void ensureRolePermission(Connection connection, String roleId, String permissionId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
            "select 1 from role_permissions where role_id = ? and permission_id = ?")) {
            query.setString(1, roleId);
            query.setString(2, permissionId);
            try (ResultSet resultSet = query.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_permissions
                (id, role_id, permission_id, assigned_at)
            values
                (?, ?, ?, ?)
            """)) {
            insert.setString(1, "RP_M3_REF_" + UUID.randomUUID().toString().replace("-", ""));
            insert.setString(2, roleId);
            insert.setString(3, permissionId);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }
}