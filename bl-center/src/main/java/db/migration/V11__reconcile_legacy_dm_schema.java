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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class V11__reconcile_legacy_dm_schema extends BaseJavaMigration {

    private static final String DEFAULT_PASSWORD = "0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134";
    private static final String DEFAULT_PASSWORD_ALGO = "SM3";
    private static final String DEFAULT_PASSWORD_SALT = "9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1";

    private static final List<MenuSeed> WORKFLOW_MENUS = List.of(
        new MenuSeed("MENU_M2_WORKFLOW", null, "M2_WORKFLOW", "M2 Workflow", "DIRECTORY", "/workflow", "WorkflowRoot", "m2", 110),
        new MenuSeed("MENU_M2_CLINICAL", "MENU_M2_WORKFLOW", "M2_CLINICAL", "Clinical Register", "MENU", "/api/v1/specimens/register", "ClinicalRegister", "m2:clinical", 111),
        new MenuSeed("MENU_M2_FIXATION", "MENU_M2_WORKFLOW", "M2_FIXATION", "Fixation Verify", "MENU", "/api/v1/specimen-fixations", "FixationVerify", "m2:fixation", 112),
        new MenuSeed("MENU_M2_TRANSPORT", "MENU_M2_WORKFLOW", "M2_TRANSPORT", "Transport Handover", "MENU", "/api/v1/transport-orders", "TransportHandover", "m2:transport", 113),
        new MenuSeed("MENU_M2_RECEIPT", "MENU_M2_WORKFLOW", "M2_RECEIPT", "Specimen Receipt", "MENU", "/api/v1/specimen-receipts", "SpecimenReceipt", "m2:receipt", 114),
        new MenuSeed("MENU_M2_TRACKING", "MENU_M2_WORKFLOW", "M2_TRACKING", "Tracking Query", "MENU", "/api/v1/applications/{id}/tracking", "TrackingQuery", "m2:tracking", 115),
        new MenuSeed("MENU_M3_WORKFLOW", null, "M3_WORKFLOW", "M3 Workflow", "DIRECTORY", "/technical-workflow", "TechnicalWorkflowRoot", "m3", 120),
        new MenuSeed("MENU_M3_GROSSING", "MENU_M3_WORKFLOW", "M3_GROSSING", "Grossing", "MENU", "/api/v1/grossings", "Grossing", "m3:grossing", 121),
        new MenuSeed("MENU_M3_DEHYDRATION", "MENU_M3_WORKFLOW", "M3_DEHYDRATION", "Dehydration", "MENU", "/api/v1/dehydration-batches", "Dehydration", "m3:dehydration", 122),
        new MenuSeed("MENU_M3_EMBEDDING", "MENU_M3_WORKFLOW", "M3_EMBEDDING", "Embedding", "MENU", "/api/v1/embeddings", "Embedding", "m3:embedding", 123),
        new MenuSeed("MENU_M3_SLICING", "MENU_M3_WORKFLOW", "M3_SLICING", "Slicing", "MENU", "/api/v1/slicings", "Slicing", "m3:slicing", 124),
        new MenuSeed("MENU_M3_STAINING", "MENU_M3_WORKFLOW", "M3_STAINING", "Staining", "MENU", "/api/v1/slide-stainings", "Staining", "m3:staining", 125),
        new MenuSeed("MENU_M3_REWORK", "MENU_M3_WORKFLOW", "M3_REWORK", "Rework", "MENU", "/api/v1/rework-orders", "Rework", "m3:rework", 126),
        new MenuSeed("MENU_M3_TRACKING", "MENU_M3_WORKFLOW", "M3_TRACKING", "Technical Tracking", "MENU", "/api/v1/pathology-cases/{id}/technical-tracking", "TechnicalTracking", "m3:tracking", 127),
        new MenuSeed("MENU_M3_TASKS", "MENU_M3_WORKFLOW", "M3_TASKS", "Technical Tasks", "MENU", "/api/v1/technical-tasks/pending", "TechnicalTasks", "m3:tasks", 128)
    );

    private static final List<PermissionSeed> WORKFLOW_PERMISSIONS = List.of(
        new PermissionSeed("PERM_SPECIMEN_REGISTER", "PERM_SPECIMEN_REGISTER", "Specimen register", "MENU_M2_CLINICAL", "REGISTER", "POST", "/api/v1/specimens/register", "M2", 110),
        new PermissionSeed("PERM_FIXATION_VERIFY", "PERM_FIXATION_VERIFY", "Fixation verify", "MENU_M2_FIXATION", "VERIFY", "POST", "/api/v1/specimen-fixations", "M2", 111),
        new PermissionSeed("PERM_TRANSPORT_HANDOVER", "PERM_TRANSPORT_HANDOVER", "Transport handover", "MENU_M2_TRANSPORT", "HANDOVER", "POST", "/api/v1/transport-orders", "M2", 112),
        new PermissionSeed("PERM_SPECIMEN_RECEIVE", "PERM_SPECIMEN_RECEIVE", "Specimen receive", "MENU_M2_RECEIPT", "RECEIVE", "POST", "/api/v1/specimen-receipts", "M2", 113),
        new PermissionSeed("PERM_SPECIMEN_TRACKING_QUERY", "PERM_SPECIMEN_TRACKING_QUERY", "Tracking query", "MENU_M2_TRACKING", "QUERY", "GET", "/api/v1/applications/{id}/tracking", "M2", 114),
        new PermissionSeed("PERM_CLINICAL_IMPORT", "PERM_CLINICAL_IMPORT", "Clinical import", "MENU_M2_CLINICAL", "IMPORT", "POST", "/api/v1/clinical-applications/import", "M2", 115),
        new PermissionSeed("PERM_M3_GROSSING", "PERM_M3_GROSSING", "Grossing operate", "MENU_M3_GROSSING", "OPERATE", "POST", "/api/v1/grossings", "M3", 121),
        new PermissionSeed("PERM_M3_DEHYDRATION", "PERM_M3_DEHYDRATION", "Dehydration operate", "MENU_M3_DEHYDRATION", "OPERATE", "POST", "/api/v1/dehydration-batches", "M3", 122),
        new PermissionSeed("PERM_M3_EMBEDDING", "PERM_M3_EMBEDDING", "Embedding operate", "MENU_M3_EMBEDDING", "OPERATE", "POST", "/api/v1/embeddings", "M3", 123),
        new PermissionSeed("PERM_M3_SLICING", "PERM_M3_SLICING", "Slicing operate", "MENU_M3_SLICING", "OPERATE", "POST", "/api/v1/slicings", "M3", 124),
        new PermissionSeed("PERM_M3_STAINING", "PERM_M3_STAINING", "Staining operate", "MENU_M3_STAINING", "OPERATE", "POST", "/api/v1/slide-stainings", "M3", 125),
        new PermissionSeed("PERM_M3_REWORK", "PERM_M3_REWORK", "Rework operate", "MENU_M3_REWORK", "OPERATE", "POST", "/api/v1/rework-orders", "M3", 126),
        new PermissionSeed("PERM_M3_TECH_TRACKING_QUERY", "PERM_M3_TECH_TRACKING_QUERY", "Technical tracking query", "MENU_M3_TRACKING", "QUERY", "GET", "/api/v1/pathology-cases/{id}/technical-tracking", "M3", 127),
        new PermissionSeed("PERM_M3_TECH_TASK_QUERY", "PERM_M3_TECH_TASK_QUERY", "Technical task query", "MENU_M3_TASKS", "QUERY", "GET", "/api/v1/technical-tasks/pending", "M3", 128)
    );

    private static final List<RoleSeed> WORKFLOW_ROLES = List.of(
        new RoleSeed("ROLE_M2_CLINICAL_REGISTER", "M2_CLINICAL_REGISTER", "M2 Clinical Register", "BUSINESS", "DEPARTMENT", "M2 clinical register workstation"),
        new RoleSeed("ROLE_M2_FIXATION_VERIFY", "M2_FIXATION_VERIFY", "M2 Fixation Verify", "BUSINESS", "DEPARTMENT", "M2 fixation workstation"),
        new RoleSeed("ROLE_M2_TRANSPORT_HANDOVER", "M2_TRANSPORT_HANDOVER", "M2 Transport Handover", "BUSINESS", "DEPARTMENT", "M2 transport workstation"),
        new RoleSeed("ROLE_M2_SPECIMEN_RECEIVE", "M2_SPECIMEN_RECEIVE", "M2 Specimen Receive", "BUSINESS", "DEPARTMENT", "M2 specimen receipt workstation"),
        new RoleSeed("ROLE_M2_TRACKING_QUERY", "M2_TRACKING_QUERY", "M2 Tracking Query", "BUSINESS", "DEPARTMENT", "M2 tracking query workstation"),
        new RoleSeed("ROLE_M2_CLINICAL_IMPORT", "M2_CLINICAL_IMPORT", "M2 Clinical Import", "BUSINESS", "DEPARTMENT", "M2 clinical import workstation"),
        new RoleSeed("ROLE_M3_GROSSING", "M3_GROSSING", "M3 Grossing", "BUSINESS", "DEPARTMENT", "M3 grossing workstation"),
        new RoleSeed("ROLE_M3_DEHYDRATION", "M3_DEHYDRATION", "M3 Dehydration", "BUSINESS", "DEPARTMENT", "M3 dehydration workstation"),
        new RoleSeed("ROLE_M3_EMBEDDING", "M3_EMBEDDING", "M3 Embedding", "BUSINESS", "DEPARTMENT", "M3 embedding workstation"),
        new RoleSeed("ROLE_M3_SLICING", "M3_SLICING", "M3 SLICING", "BUSINESS", "DEPARTMENT", "M3 slicing workstation"),
        new RoleSeed("ROLE_M3_STAINING", "M3_STAINING", "M3 Staining", "BUSINESS", "DEPARTMENT", "M3 staining workstation"),
        new RoleSeed("ROLE_M3_REWORK", "M3_REWORK", "M3 Rework", "BUSINESS", "DEPARTMENT", "M3 rework workstation"),
        new RoleSeed("ROLE_M3_TRACKING", "M3_TRACKING", "M3 Tracking", "BUSINESS", "DEPARTMENT", "M3 technical tracking workstation")
    );

    private static final List<RolePermissionSeed> WORKFLOW_ROLE_PERMISSIONS = List.of(
        new RolePermissionSeed("RP_M2_ADMIN_REGISTER", "ROLE_PATHOLOGY_ADMIN", "PERM_SPECIMEN_REGISTER"),
        new RolePermissionSeed("RP_M2_ADMIN_FIXATION", "ROLE_PATHOLOGY_ADMIN", "PERM_FIXATION_VERIFY"),
        new RolePermissionSeed("RP_M2_ADMIN_TRANSPORT", "ROLE_PATHOLOGY_ADMIN", "PERM_TRANSPORT_HANDOVER"),
        new RolePermissionSeed("RP_M2_ADMIN_RECEIVE", "ROLE_PATHOLOGY_ADMIN", "PERM_SPECIMEN_RECEIVE"),
        new RolePermissionSeed("RP_M2_ADMIN_TRACKING", "ROLE_PATHOLOGY_ADMIN", "PERM_SPECIMEN_TRACKING_QUERY"),
        new RolePermissionSeed("RP_M2_ADMIN_IMPORT", "ROLE_PATHOLOGY_ADMIN", "PERM_CLINICAL_IMPORT"),
        new RolePermissionSeed("RP_M2_REGISTER_ROLE", "ROLE_M2_CLINICAL_REGISTER", "PERM_SPECIMEN_REGISTER"),
        new RolePermissionSeed("RP_M2_FIXATION_ROLE", "ROLE_M2_FIXATION_VERIFY", "PERM_FIXATION_VERIFY"),
        new RolePermissionSeed("RP_M2_TRANSPORT_ROLE", "ROLE_M2_TRANSPORT_HANDOVER", "PERM_TRANSPORT_HANDOVER"),
        new RolePermissionSeed("RP_M2_RECEIVE_ROLE", "ROLE_M2_SPECIMEN_RECEIVE", "PERM_SPECIMEN_RECEIVE"),
        new RolePermissionSeed("RP_M2_TRACKING_ROLE", "ROLE_M2_TRACKING_QUERY", "PERM_SPECIMEN_TRACKING_QUERY"),
        new RolePermissionSeed("RP_M2_IMPORT_ROLE", "ROLE_M2_CLINICAL_IMPORT", "PERM_CLINICAL_IMPORT"),
        new RolePermissionSeed("RP_M3_ADMIN_GROSSING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_GROSSING"),
        new RolePermissionSeed("RP_M3_ADMIN_DEHYDRATION", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_DEHYDRATION"),
        new RolePermissionSeed("RP_M3_ADMIN_EMBEDDING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_EMBEDDING"),
        new RolePermissionSeed("RP_M3_ADMIN_SLICING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_SLICING"),
        new RolePermissionSeed("RP_M3_ADMIN_STAINING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_STAINING"),
        new RolePermissionSeed("RP_M3_ADMIN_REWORK", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_REWORK"),
        new RolePermissionSeed("RP_M3_ADMIN_TRACKING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_TECH_TRACKING_QUERY"),
        new RolePermissionSeed("RP_M3_ADMIN_TASKS", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_GROSSING_ROLE", "ROLE_M3_GROSSING", "PERM_M3_GROSSING"),
        new RolePermissionSeed("RP_M3_GROSSING_TASK_ROLE", "ROLE_M3_GROSSING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_DEHYDRATION_ROLE", "ROLE_M3_DEHYDRATION", "PERM_M3_DEHYDRATION"),
        new RolePermissionSeed("RP_M3_DEHYDRATION_TASK_ROLE", "ROLE_M3_DEHYDRATION", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_EMBEDDING_ROLE", "ROLE_M3_EMBEDDING", "PERM_M3_EMBEDDING"),
        new RolePermissionSeed("RP_M3_EMBEDDING_TASK_ROLE", "ROLE_M3_EMBEDDING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_SLICING_ROLE", "ROLE_M3_SLICING", "PERM_M3_SLICING"),
        new RolePermissionSeed("RP_M3_SLICING_TASK_ROLE", "ROLE_M3_SLICING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_STAINING_ROLE", "ROLE_M3_STAINING", "PERM_M3_STAINING"),
        new RolePermissionSeed("RP_M3_STAINING_TASK_ROLE", "ROLE_M3_STAINING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_REWORK_ROLE", "ROLE_M3_REWORK", "PERM_M3_REWORK"),
        new RolePermissionSeed("RP_M3_REWORK_TASK_ROLE", "ROLE_M3_REWORK", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_TRACKING_ROLE", "ROLE_M3_TRACKING", "PERM_M3_TECH_TRACKING_QUERY")
    );

    private static final List<UserSeed> STANDARD_USERS = List.of(
        new UserSeed("USER_M1_ADMIN", "U-M1-ADMIN", "m1.admin", "M1 Admin", "PATHOLOGY_ADMIN", true),
        new UserSeed("USER_M1_DOCTOR", "U-M1-DOCTOR", "m1.doctor", "M1 Doctor", "PATHOLOGY_DOCTOR", true),
        new UserSeed("USER_M1_TECHNICIAN", "U-M1-TECH", "m1.technician", "M1 Technician", "PATHOLOGY_TECHNICIAN", true),
        new UserSeed("USER_M1_ARCHIVE", "U-M1-ARCHIVE", "m1.archive", "M1 Archive", "ARCHIVE_MANAGER", true),
        new UserSeed("USER_M1_REAGENT", "U-M1-REAGENT", "m1.reagent", "M1 Reagent", "REAGENT_DEVICE_MANAGER", true),
        new UserSeed("USER_M1_QUALITY", "U-M1-QUALITY", "m1.quality", "M1 Quality", "QUALITY_MANAGER", true),
        new UserSeed("USER_M1_NO_PERMISSION", "U-M1-NOAUTH", "m1.noauth", "M1 No Permission", null, true),
        new UserSeed("USER_M2_ADMIN", "U-M2-ADMIN", "m2.admin", "M2 Admin", "PATHOLOGY_ADMIN", true),
        new UserSeed("USER_M2_REGISTER", "U-M2-REGISTER", "m2.register", "M2 Register", "M2_CLINICAL_REGISTER", true),
        new UserSeed("USER_M2_FIXATION", "U-M2-FIXATION", "m2.fixation", "M2 Fixation", "M2_FIXATION_VERIFY", true),
        new UserSeed("USER_M2_TRANSPORT", "U-M2-TRANSPORT", "m2.transport", "M2 Transport", "M2_TRANSPORT_HANDOVER", true),
        new UserSeed("USER_M2_RECEIVE", "U-M2-RECEIVE", "m2.receive", "M2 Receive", "M2_SPECIMEN_RECEIVE", true),
        new UserSeed("USER_M2_TRACKING", "U-M2-TRACKING", "m2.tracking", "M2 Tracking", "M2_TRACKING_QUERY", true),
        new UserSeed("USER_M2_IMPORT", "U-M2-IMPORT", "m2.import", "M2 Import", "M2_CLINICAL_IMPORT", true),
        new UserSeed("USER_M2_NO_PERMISSION", "U-M2-NOAUTH", "m2.noauth", "M2 No Permission", null, true),
        new UserSeed("USER_M3_GROSSING", "U-M3-GROSSING", "m3.grossing", "M3 Grossing", "M3_GROSSING", true),
        new UserSeed("USER_M3_DEHYDRATION", "U-M3-DEHYDRATION", "m3.dehydration", "M3 Dehydration", "M3_DEHYDRATION", true),
        new UserSeed("USER_M3_EMBEDDING", "U-M3-EMBEDDING", "m3.embedding", "M3 Embedding", "M3_EMBEDDING", true),
        new UserSeed("USER_M3_SLICING", "U-M3-SLICING", "m3.slicing", "M3 Slicing", "M3_SLICING", true),
        new UserSeed("USER_M3_STAINING", "U-M3-STAINING", "m3.staining", "M3 Staining", "M3_STAINING", true),
        new UserSeed("USER_M3_REWORK", "U-M3-REWORK", "m3.rework", "M3 Rework", "M3_REWORK", true),
        new UserSeed("USER_M3_TRACKING", "U-M3-TRACKING", "m3.tracking", "M3 Tracking", "M3_TRACKING", true)
    );

    private static final List<UserRoleSeed> STANDARD_USER_ROLES = List.of(
        new UserRoleSeed("UR_M1_ADMIN", "USER_M1_ADMIN", "ROLE_PATHOLOGY_ADMIN"),
        new UserRoleSeed("UR_M1_DOCTOR", "USER_M1_DOCTOR", "ROLE_PATHOLOGY_DOCTOR"),
        new UserRoleSeed("UR_M1_TECHNICIAN", "USER_M1_TECHNICIAN", "ROLE_PATHOLOGY_TECHNICIAN"),
        new UserRoleSeed("UR_M1_ARCHIVE", "USER_M1_ARCHIVE", "ROLE_ARCHIVE_MANAGER"),
        new UserRoleSeed("UR_M1_REAGENT", "USER_M1_REAGENT", "ROLE_REAGENT_DEVICE_MANAGER"),
        new UserRoleSeed("UR_M1_QUALITY", "USER_M1_QUALITY", "ROLE_QUALITY_MANAGER"),
        new UserRoleSeed("UR_M2_ADMIN", "USER_M2_ADMIN", "ROLE_PATHOLOGY_ADMIN"),
        new UserRoleSeed("UR_M2_REGISTER", "USER_M2_REGISTER", "ROLE_M2_CLINICAL_REGISTER"),
        new UserRoleSeed("UR_M2_FIXATION", "USER_M2_FIXATION", "ROLE_M2_FIXATION_VERIFY"),
        new UserRoleSeed("UR_M2_TRANSPORT", "USER_M2_TRANSPORT", "ROLE_M2_TRANSPORT_HANDOVER"),
        new UserRoleSeed("UR_M2_RECEIVE", "USER_M2_RECEIVE", "ROLE_M2_SPECIMEN_RECEIVE"),
        new UserRoleSeed("UR_M2_TRACKING", "USER_M2_TRACKING", "ROLE_M2_TRACKING_QUERY"),
        new UserRoleSeed("UR_M2_IMPORT", "USER_M2_IMPORT", "ROLE_M2_CLINICAL_IMPORT"),
        new UserRoleSeed("UR_M3_GROSSING", "USER_M3_GROSSING", "ROLE_M3_GROSSING"),
        new UserRoleSeed("UR_M3_DEHYDRATION", "USER_M3_DEHYDRATION", "ROLE_M3_DEHYDRATION"),
        new UserRoleSeed("UR_M3_EMBEDDING", "USER_M3_EMBEDDING", "ROLE_M3_EMBEDDING"),
        new UserRoleSeed("UR_M3_SLICING", "USER_M3_SLICING", "ROLE_M3_SLICING"),
        new UserRoleSeed("UR_M3_STAINING", "USER_M3_STAINING", "ROLE_M3_STAINING"),
        new UserRoleSeed("UR_M3_REWORK", "USER_M3_REWORK", "ROLE_M3_REWORK"),
        new UserRoleSeed("UR_M3_TRACKING", "USER_M3_TRACKING", "ROLE_M3_TRACKING")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureUserLoginSchema(connection);
        ensureTechnicalPendingTasks(connection);
        reconcileWorkflowAuthorization(connection);
        reconcileUsers(connection);
        reconcileRoleMenus(connection);
    }

    private void ensureUserLoginSchema(Connection connection) throws SQLException {
        ensureColumn(connection, "USERS", "PASSWORD_ALGO",
            "ALTER TABLE users ADD password_algo VARCHAR(32)");
        ensureColumn(connection, "USERS", "PASSWORD_SALT",
            "ALTER TABLE users ADD password_salt VARCHAR(64)");
        ensureTable(connection, "AUTH_ACCESS_TOKENS", """
            CREATE TABLE auth_access_tokens (
                jti VARCHAR(128) NOT NULL,
                user_id VARCHAR(64) NOT NULL,
                issued_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                revoked_at TIMESTAMP,
                client_ip VARCHAR(64),
                client_device VARCHAR(200),
                CONSTRAINT pk_auth_access_tokens PRIMARY KEY (jti),
                CONSTRAINT fk_auth_access_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
            )
            """);
        ensureIndex(connection, "AUTH_ACCESS_TOKENS", "IDX_AUTH_ACCESS_TOKENS_USER_ID",
            "CREATE INDEX idx_auth_access_tokens_user_id ON auth_access_tokens (user_id)");
    }

    private void ensureTechnicalPendingTasks(Connection connection) throws SQLException {
        if (!tableExists(connection, "TECHNICAL_PENDING_TASKS")) {
            execute(connection, """
                CREATE TABLE technical_pending_tasks (
                    id VARCHAR(64) NOT NULL,
                    application_id VARCHAR(64) NOT NULL,
                    case_id VARCHAR(64) NOT NULL,
                    specimen_id VARCHAR(64),
                    task_type VARCHAR(64) NOT NULL,
                    task_status VARCHAR(32) NOT NULL,
                    object_type VARCHAR(32),
                    object_id VARCHAR(64),
                    parent_task_id VARCHAR(64),
                    payload VARCHAR(2000),
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    remarks VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT pk_technical_pending_tasks PRIMARY KEY (id),
                    CONSTRAINT fk_technical_pending_tasks_application FOREIGN KEY (application_id) REFERENCES applications (id),
                    CONSTRAINT fk_technical_pending_tasks_case FOREIGN KEY (case_id) REFERENCES pathology_cases (id),
                    CONSTRAINT fk_technical_pending_tasks_specimen FOREIGN KEY (specimen_id) REFERENCES specimens (id),
                    CONSTRAINT fk_technical_pending_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES technical_pending_tasks (id)
                )
                """);
            return;
        }

        if (indexExists(connection, "TECHNICAL_PENDING_TASKS", "UK_TECHNICAL_PENDING_TASKS_CASE_TYPE")) {
            execute(connection, "ALTER TABLE technical_pending_tasks DROP CONSTRAINT uk_technical_pending_tasks_case_type");
        }
        ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "SPECIMEN_ID",
            "ALTER TABLE technical_pending_tasks ADD COLUMN specimen_id VARCHAR(64)");
        ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "OBJECT_TYPE",
            "ALTER TABLE technical_pending_tasks ADD COLUMN object_type VARCHAR(32)");
        ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "OBJECT_ID",
            "ALTER TABLE technical_pending_tasks ADD COLUMN object_id VARCHAR(64)");
        ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "PARENT_TASK_ID",
            "ALTER TABLE technical_pending_tasks ADD COLUMN parent_task_id VARCHAR(64)");
        ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "STARTED_AT",
            "ALTER TABLE technical_pending_tasks ADD COLUMN started_at TIMESTAMP");
        ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "COMPLETED_AT",
            "ALTER TABLE technical_pending_tasks ADD COLUMN completed_at TIMESTAMP");
        ensureColumn(connection, "TECHNICAL_PENDING_TASKS", "REMARKS",
            "ALTER TABLE technical_pending_tasks ADD COLUMN remarks VARCHAR(500)");
        execute(connection, """
            UPDATE technical_pending_tasks
            SET object_type = 'CASE',
                object_id = case_id
            WHERE object_type IS NULL
            """);
        if (!foreignKeyExists(connection, "TECHNICAL_PENDING_TASKS", "FK_TECHNICAL_PENDING_TASKS_SPECIMEN")) {
            execute(connection, """
                ALTER TABLE technical_pending_tasks ADD CONSTRAINT fk_technical_pending_tasks_specimen
                FOREIGN KEY (specimen_id) REFERENCES specimens (id)
                """);
        }
        if (!foreignKeyExists(connection, "TECHNICAL_PENDING_TASKS", "FK_TECHNICAL_PENDING_TASKS_PARENT")) {
            execute(connection, """
                ALTER TABLE technical_pending_tasks ADD CONSTRAINT fk_technical_pending_tasks_parent
                FOREIGN KEY (parent_task_id) REFERENCES technical_pending_tasks (id)
                """);
        }
    }

    private void reconcileWorkflowAuthorization(Connection connection) throws SQLException {
        for (MenuSeed menu : WORKFLOW_MENUS) {
            upsertMenu(connection, menu);
        }
        for (PermissionSeed permission : WORKFLOW_PERMISSIONS) {
            upsertPermission(connection, permission);
        }
        for (RoleSeed role : WORKFLOW_ROLES) {
            upsertRole(connection, role);
        }

        deleteRolePermissions(connection);
        for (RolePermissionSeed seed : WORKFLOW_ROLE_PERMISSIONS) {
            insertRolePermission(connection, seed);
        }
    }

    private void reconcileUsers(Connection connection) throws SQLException {
        for (UserSeed user : STANDARD_USERS) {
            upsertUser(connection, user);
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM user_roles WHERE user_id = ?")) {
            for (UserSeed user : STANDARD_USERS) {
                statement.setString(1, user.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        for (UserRoleSeed seed : STANDARD_USER_ROLES) {
            insertUserRole(connection, seed);
        }
    }

    private void reconcileRoleMenus(Connection connection) throws SQLException {
        Set<String> reconciledRoleIds = new LinkedHashSet<>();
        reconciledRoleIds.add("ROLE_PATHOLOGY_ADMIN");
        for (RoleSeed role : WORKFLOW_ROLES) {
            reconciledRoleIds.add(role.id());
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM role_menus WHERE role_id = ?")) {
            for (String roleId : reconciledRoleIds) {
                statement.setString(1, roleId);
                statement.addBatch();
            }
            statement.executeBatch();
        }

        for (MenuIdentity menu : loadAdminMenus(connection)) {
            insertRoleMenu(connection, "RM_ADMIN_" + menu.menuCode(), "ROLE_PATHOLOGY_ADMIN", menu.id());
        }

        for (RoleSeed role : WORKFLOW_ROLES) {
            String rootMenuId = role.roleCode().startsWith("M2_") ? "MENU_M2_WORKFLOW" : "MENU_M3_WORKFLOW";
            LinkedHashSet<MenuIdentity> menus = new LinkedHashSet<>(loadMenusForRole(connection, role.id()));
            menus.add(loadMenuIdentity(connection, rootMenuId));
            for (MenuIdentity menu : menus) {
                insertRoleMenu(connection, "RM_" + role.roleCode() + "_" + menu.menuCode(), role.id(), menu.id());
            }
        }
    }

    private List<MenuIdentity> loadAdminMenus(Connection connection) throws SQLException {
        List<MenuIdentity> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
            select id, menu_code
            from menus
            where id in ('MENU_SYSTEM', 'MENU_M2_WORKFLOW', 'MENU_M3_WORKFLOW')
               or parent_id in ('MENU_SYSTEM', 'MENU_M2_WORKFLOW', 'MENU_M3_WORKFLOW')
            order by sort_order, menu_code
            """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new MenuIdentity(resultSet.getString("id"), resultSet.getString("menu_code")));
            }
        }
        return result;
    }

    private List<MenuIdentity> loadMenusForRole(Connection connection, String roleId) throws SQLException {
        List<MenuIdentity> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
            select distinct menus.id, menus.menu_code
            from role_permissions
            join permissions on permissions.id = role_permissions.permission_id
            join menus on menus.id = permissions.menu_id
            where role_permissions.role_id = ?
            order by menus.menu_code
            """)) {
            statement.setString(1, roleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new MenuIdentity(resultSet.getString("id"), resultSet.getString("menu_code")));
                }
            }
        }
        return result;
    }

    private MenuIdentity loadMenuIdentity(Connection connection, String menuId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id, menu_code FROM menus WHERE id = ?")) {
            statement.setString(1, menuId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Missing menu required for reconciliation: " + menuId);
                }
                return new MenuIdentity(resultSet.getString("id"), resultSet.getString("menu_code"));
            }
        }
    }

    private void upsertMenu(Connection connection, MenuSeed seed) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            UPDATE menus
            SET parent_id = ?,
                menu_code = ?,
                menu_name = ?,
                menu_type = ?,
                path = ?,
                component_name = ?,
                permission_prefix = ?,
                sort_order = ?,
                visible = 1,
                enabled = 1,
                updated_at = ?
            WHERE id = ?
            """)) {
            bindMenu(update, seed, false);
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix,
                 sort_order, visible, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?)
            """)) {
            bindMenu(insert, seed, true);
            insert.executeUpdate();
        }
    }

    private void bindMenu(PreparedStatement statement, MenuSeed seed, boolean includeIdFirst) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        int index = 1;
        if (includeIdFirst) {
            statement.setString(index++, seed.id());
        }
        if (seed.parentId() == null) {
            statement.setNull(index++, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index++, seed.parentId());
        }
        statement.setString(index++, seed.menuCode());
        statement.setString(index++, seed.menuName());
        statement.setString(index++, seed.menuType());
        statement.setString(index++, seed.path());
        statement.setString(index++, seed.componentName());
        statement.setString(index++, seed.permissionPrefix());
        statement.setInt(index++, seed.sortOrder());
        statement.setTimestamp(index++, Timestamp.valueOf(now));
        if (includeIdFirst) {
            statement.setTimestamp(index++, Timestamp.valueOf(now));
        } else {
            statement.setString(index, seed.id());
        }
    }

    private void upsertPermission(Connection connection, PermissionSeed seed) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            UPDATE permissions
            SET permission_code = ?,
                permission_name = ?,
                menu_id = ?,
                action_key = ?,
                http_method = ?,
                resource_path = ?,
                permission_group = ?,
                sort_order = ?,
                enabled = 1,
                updated_at = ?
            WHERE id = ?
            """)) {
            LocalDateTime now = LocalDateTime.now();
            update.setString(1, seed.permissionCode());
            update.setString(2, seed.permissionName());
            update.setString(3, seed.menuId());
            update.setString(4, seed.actionKey());
            update.setString(5, seed.httpMethod());
            update.setString(6, seed.resourcePath());
            update.setString(7, seed.permissionGroup());
            update.setInt(8, seed.sortOrder());
            update.setTimestamp(9, Timestamp.valueOf(now));
            update.setString(10, seed.id());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO permissions
                (id, permission_code, permission_name, menu_id, action_key, http_method, resource_path,
                 permission_group, sort_order, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            LocalDateTime now = LocalDateTime.now();
            insert.setString(1, seed.id());
            insert.setString(2, seed.permissionCode());
            insert.setString(3, seed.permissionName());
            insert.setString(4, seed.menuId());
            insert.setString(5, seed.actionKey());
            insert.setString(6, seed.httpMethod());
            insert.setString(7, seed.resourcePath());
            insert.setString(8, seed.permissionGroup());
            insert.setInt(9, seed.sortOrder());
            insert.setTimestamp(10, Timestamp.valueOf(now));
            insert.setTimestamp(11, Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void upsertRole(Connection connection, RoleSeed seed) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            UPDATE roles
            SET role_code = ?,
                role_name = ?,
                role_type = ?,
                data_scope = ?,
                remarks = ?,
                enabled = 1,
                updated_at = ?
            WHERE id = ?
            """)) {
            LocalDateTime now = LocalDateTime.now();
            update.setString(1, seed.roleCode());
            update.setString(2, seed.roleName());
            update.setString(3, seed.roleType());
            update.setString(4, seed.dataScope());
            update.setString(5, seed.remarks());
            update.setTimestamp(6, Timestamp.valueOf(now));
            update.setString(7, seed.id());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO roles
                (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, 1, ?, ?)
            """)) {
            LocalDateTime now = LocalDateTime.now();
            insert.setString(1, seed.id());
            insert.setString(2, seed.roleCode());
            insert.setString(3, seed.roleName());
            insert.setString(4, seed.roleType());
            insert.setString(5, seed.dataScope());
            insert.setString(6, seed.remarks());
            insert.setTimestamp(7, Timestamp.valueOf(now));
            insert.setTimestamp(8, Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void deleteRolePermissions(Connection connection) throws SQLException {
        try (PreparedStatement byPermission = connection.prepareStatement(
            "DELETE FROM role_permissions WHERE permission_id = ?")) {
            for (PermissionSeed permission : WORKFLOW_PERMISSIONS) {
                byPermission.setString(1, permission.id());
                byPermission.addBatch();
            }
            byPermission.executeBatch();
        }
        try (PreparedStatement byId = connection.prepareStatement(
            "DELETE FROM role_permissions WHERE id = ?")) {
            for (RolePermissionSeed seed : WORKFLOW_ROLE_PERMISSIONS) {
                byId.setString(1, seed.id());
                byId.addBatch();
            }
            byId.executeBatch();
        }
    }

    private void insertRolePermission(Connection connection, RolePermissionSeed seed) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO role_permissions
                (id, role_id, permission_id, assigned_at)
            VALUES
                (?, ?, ?, ?)
            """)) {
            insert.setString(1, seed.id());
            insert.setString(2, seed.roleId());
            insert.setString(3, seed.permissionId());
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void upsertUser(Connection connection, UserSeed seed) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            UPDATE users
            SET user_code = ?,
                login_name = ?,
                name = ?,
                password = ?,
                password_algo = ?,
                password_salt = ?,
                role = ?,
                enabled = ?,
                updated_at = ?
            WHERE id = ?
            """)) {
            LocalDateTime now = LocalDateTime.now();
            update.setString(1, seed.userCode());
            update.setString(2, seed.loginName());
            update.setString(3, seed.name());
            update.setString(4, DEFAULT_PASSWORD);
            update.setString(5, DEFAULT_PASSWORD_ALGO);
            update.setString(6, DEFAULT_PASSWORD_SALT);
            if (seed.roleCode() == null) {
                update.setNull(7, java.sql.Types.VARCHAR);
            } else {
                update.setString(7, seed.roleCode());
            }
            update.setInt(8, seed.enabled() ? 1 : 0);
            update.setTimestamp(9, Timestamp.valueOf(now));
            update.setString(10, seed.id());
            if (update.executeUpdate() > 0) {
                return;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO users
                (id, user_code, login_name, name, password, password_algo, password_salt, role, enabled, created_at, updated_at)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            LocalDateTime now = LocalDateTime.now();
            insert.setString(1, seed.id());
            insert.setString(2, seed.userCode());
            insert.setString(3, seed.loginName());
            insert.setString(4, seed.name());
            insert.setString(5, DEFAULT_PASSWORD);
            insert.setString(6, DEFAULT_PASSWORD_ALGO);
            insert.setString(7, DEFAULT_PASSWORD_SALT);
            if (seed.roleCode() == null) {
                insert.setNull(8, java.sql.Types.VARCHAR);
            } else {
                insert.setString(8, seed.roleCode());
            }
            insert.setInt(9, seed.enabled() ? 1 : 0);
            insert.setTimestamp(10, Timestamp.valueOf(now));
            insert.setTimestamp(11, Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void insertUserRole(Connection connection, UserRoleSeed seed) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO user_roles
                (id, user_id, role_id, is_primary, assigned_at, assigned_by_name)
            VALUES
                (?, ?, ?, 1, ?, 'system')
            """)) {
            insert.setString(1, seed.id());
            insert.setString(2, seed.userId());
            insert.setString(3, seed.roleId());
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void insertRoleMenu(Connection connection, String id, String roleId, String menuId) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO role_menus
                (id, role_id, menu_id, assigned_at)
            VALUES
                (?, ?, ?, ?)
            """)) {
            insert.setString(1, id);
            insert.setString(2, roleId);
            insert.setString(3, menuId);
            insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void ensureTable(Connection connection, String tableName, String ddl) throws SQLException {
        if (!tableExists(connection, tableName)) {
            execute(connection, ddl);
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private void ensureIndex(Connection connection, String tableName, String indexName, String ddl) throws SQLException {
        if (!indexExists(connection, tableName, indexName)) {
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

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getIndexInfo(null, null, candidate, false, false)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("TABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("INDEX_NAME"), indexName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String fkName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String candidate : identifierCandidates(tableName)) {
            try (ResultSet resultSet = metaData.getImportedKeys(null, null, candidate)) {
                while (resultSet.next()) {
                    if (identifierEquals(resultSet.getString("FKTABLE_NAME"), tableName)
                        && identifierEquals(resultSet.getString("FK_NAME"), fkName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean identifierEquals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private List<String> identifierCandidates(String identifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.toUpperCase());
        candidates.add(identifier.toLowerCase());
        return new ArrayList<>(candidates);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record MenuSeed(
        String id,
        String parentId,
        String menuCode,
        String menuName,
        String menuType,
        String path,
        String componentName,
        String permissionPrefix,
        int sortOrder
    ) {
    }

    private record PermissionSeed(
        String id,
        String permissionCode,
        String permissionName,
        String menuId,
        String actionKey,
        String httpMethod,
        String resourcePath,
        String permissionGroup,
        int sortOrder
    ) {
    }

    private record RoleSeed(
        String id,
        String roleCode,
        String roleName,
        String roleType,
        String dataScope,
        String remarks
    ) {
    }

    private record RolePermissionSeed(String id, String roleId, String permissionId) {
    }

    private record UserSeed(
        String id,
        String userCode,
        String loginName,
        String name,
        String roleCode,
        boolean enabled
    ) {
    }

    private record UserRoleSeed(String id, String userId, String roleId) {
    }

    private record MenuIdentity(String id, String menuCode) {
    }
}
