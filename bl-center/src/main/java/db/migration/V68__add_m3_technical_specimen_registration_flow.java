package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class V68__add_m3_technical_specimen_registration_flow extends BaseJavaMigration {

    private static final String TABLE_NAME = "TECHNICAL_SPECIMEN_REGISTRATIONS";
    private static final String MENU_ID = "MENU_M3_SPECIMEN_REGISTRATION";
    private static final String MENU_PARENT_ID = "MENU_M3_WORKFLOW";
    private static final String ADMIN_ROLE_ID = "ROLE_PATHOLOGY_ADMIN";
    private static final String RECEIPT_ROLE_ID = "ROLE_M2_SPECIMEN_RECEIVE";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureRegistrationTable(connection);
        upsertMenu(connection);
        ensureRoleMenu(connection, "RM_M3_SPEC_REG_ADMIN", ADMIN_ROLE_ID, MENU_ID);
        ensureRoleMenu(connection, "RM_M3_SPEC_REG_RECEIPT", RECEIPT_ROLE_ID, MENU_ID);
    }

    private void ensureRegistrationTable(Connection connection) throws SQLException {
        if (tableExists(connection, TABLE_NAME)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                create table technical_specimen_registrations (
                    case_id varchar(64) not null,
                    application_id varchar(64) not null,
                    registration_status varchar(32) not null,
                    registered_by_user_id varchar(64),
                    registered_by_name varchar(128),
                    registered_at timestamp,
                    remarks varchar(500),
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    constraint pk_technical_specimen_registrations primary key (case_id),
                    constraint fk_technical_specimen_reg_case foreign key (case_id) references pathology_cases (id)
                )
                """);
            statement.execute("""
                create index idx_tech_spec_reg_status_created
                    on technical_specimen_registrations (registration_status, created_at)
                """);
            statement.execute("""
                create index idx_tech_spec_reg_application
                    on technical_specimen_registrations (application_id)
                """);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, null)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getTables(null, null, tableName.toLowerCase(), null)) {
            return resultSet.next();
        }
    }

    private void upsertMenu(Connection connection) throws SQLException {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        try (PreparedStatement update = connection.prepareStatement("""
            update menus
            set parent_id = ?, menu_code = ?, menu_name = ?, menu_type = ?, path = ?, component_name = ?,
                permission_prefix = ?, sort_order = ?, visible = 1, enabled = 1, updated_at = ?
            where id = ?
            """)) {
            update.setString(1, MENU_PARENT_ID);
            update.setString(2, "M3_SPECIMEN_REGISTRATION");
            update.setString(3, "标本登记");
            update.setString(4, "MENU");
            update.setString(5, "/api/v1/technical-specimen-registrations/pending");
            update.setString(6, "TechnicalSpecimenRegistration");
            update.setString(7, "m2:receipt");
            update.setInt(8, 121);
            update.setTimestamp(9, now);
            update.setString(10, MENU_ID);
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            insert into menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix,
                 sort_order, visible, enabled, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)
            """)) {
            insert.setString(1, MENU_ID);
            insert.setString(2, MENU_PARENT_ID);
            insert.setString(3, "M3_SPECIMEN_REGISTRATION");
            insert.setString(4, "标本登记");
            insert.setString(5, "MENU");
            insert.setString(6, "/api/v1/technical-specimen-registrations/pending");
            insert.setString(7, "TechnicalSpecimenRegistration");
            insert.setString(8, "m2:receipt");
            insert.setInt(9, 121);
            insert.setTimestamp(10, now);
            insert.setTimestamp(11, now);
            insert.executeUpdate();
        }
    }

    private void ensureRoleMenu(Connection connection, String id, String roleId, String menuId) throws SQLException {
        if (exists(connection, roleId, menuId)) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into role_menus
                (id, role_id, menu_id, assigned_at)
            values
                (?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, roleId);
            insert.setString(3, menuId);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String roleId, String menuId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select 1
            from role_menus
            where role_id = ?
              and menu_id = ?
            """)) {
            statement.setString(1, roleId);
            statement.setString(2, menuId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
