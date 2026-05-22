package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V39__normalize_role_authorization_labels extends BaseJavaMigration {

    private static final List<MenuSeed> MENU_SEEDS = List.of(
        new MenuSeed(
            "MENU_DEPARTMENTS",
            "DEPARTMENTS",
            "科室字典",
            List.of("Department Dictionary"))
    );

    private static final List<PermissionSeed> PERMISSION_SEEDS = List.of(
        new PermissionSeed(
            "PERM_SYS_DEPARTMENT_QUERY",
            "查询科室字典",
            List.of("Query Department Dictionary")),
        new PermissionSeed(
            "PERM_SYS_DEPARTMENT_CREATE",
            "维护科室字典",
            List.of("Maintain Department Dictionary"))
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (MenuSeed seed : MENU_SEEDS) {
            normalizeMenuName(connection, seed);
        }
        for (PermissionSeed seed : PERMISSION_SEEDS) {
            normalizePermissionName(connection, seed);
        }
    }

    private void normalizeMenuName(Connection connection, MenuSeed seed) throws Exception {
        StringBuilder sql = new StringBuilder("""
            update menus
            set menu_name = ?,
                updated_at = ?
            where (id = ? or menu_code = ?)
              and (
                    menu_name is null
                 or trim(menu_name) = ''
            """);
        for (int index = 0; index < seed.legacyNames().size(); index++) {
            sql.append(" or trim(menu_name) = ?");
        }
        sql.append("\n              )");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setString(parameterIndex++, seed.normalizedName());
            statement.setTimestamp(parameterIndex++, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(parameterIndex++, seed.id());
            statement.setString(parameterIndex++, seed.code());
            for (String legacyName : seed.legacyNames()) {
                statement.setString(parameterIndex++, legacyName);
            }
            statement.executeUpdate();
        }
    }

    private void normalizePermissionName(Connection connection, PermissionSeed seed) throws Exception {
        StringBuilder sql = new StringBuilder("""
            update permissions
            set permission_name = ?,
                updated_at = ?
            where (id = ? or permission_code = ?)
              and (
                    permission_name is null
                 or trim(permission_name) = ''
            """);
        for (int index = 0; index < seed.legacyNames().size(); index++) {
            sql.append(" or trim(permission_name) = ?");
        }
        sql.append("\n              )");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setString(parameterIndex++, seed.normalizedName());
            statement.setTimestamp(parameterIndex++, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(parameterIndex++, seed.code());
            statement.setString(parameterIndex++, seed.code());
            for (String legacyName : seed.legacyNames()) {
                statement.setString(parameterIndex++, legacyName);
            }
            statement.executeUpdate();
        }
    }

    private record MenuSeed(
        String id,
        String code,
        String normalizedName,
        List<String> legacyNames
    ) {
    }

    private record PermissionSeed(
        String code,
        String normalizedName,
        List<String> legacyNames
    ) {
    }
}
