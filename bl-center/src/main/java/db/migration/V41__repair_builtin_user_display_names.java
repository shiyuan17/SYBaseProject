package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class V41__repair_builtin_user_display_names extends BaseJavaMigration {

    private static final List<UserDisplayName> BUILTIN_USERS = List.of(
        new UserDisplayName("USER_M1_ADMIN", "病理科管理员"),
        new UserDisplayName("USER_M1_DOCTOR", "病理医生"),
        new UserDisplayName("USER_M1_TECHNICIAN", "病理技师"),
        new UserDisplayName("USER_M1_ARCHIVE", "归档管理员"),
        new UserDisplayName("USER_M1_REAGENT", "试剂设备管理员"),
        new UserDisplayName("USER_M1_QUALITY", "质控管理员"),
        new UserDisplayName("USER_M1_NO_PERMISSION", "无权限用户"),
        new UserDisplayName("USER_M2_ADMIN", "病理科管理员"),
        new UserDisplayName("USER_M2_REGISTER", "标本登记员"),
        new UserDisplayName("USER_M2_FIXATION", "固定核验员"),
        new UserDisplayName("USER_M2_TRANSPORT", "转运交接员"),
        new UserDisplayName("USER_M2_RECEIVE", "标本接收员"),
        new UserDisplayName("USER_M2_TRACKING", "标本追踪员"),
        new UserDisplayName("USER_M2_IMPORT", "临床导入员"),
        new UserDisplayName("USER_M2_NO_PERMISSION", "无权限用户"),
        new UserDisplayName("USER_M3_GROSSING", "取材员"),
        new UserDisplayName("USER_M3_DEHYDRATION", "脱水员"),
        new UserDisplayName("USER_M3_EMBEDDING", "包埋员"),
        new UserDisplayName("USER_M3_SLICING", "切片员"),
        new UserDisplayName("USER_M3_STAINING", "染色员"),
        new UserDisplayName("USER_M3_REWORK", "返工员"),
        new UserDisplayName("USER_M3_TRACKING", "技术追踪员"),
        new UserDisplayName("USER_M4_ASSIGN", "诊断分派员"),
        new UserDisplayName("USER_M4_DIAGNOSIS", "诊断医生"),
        new UserDisplayName("USER_M4_REVIEW", "审核医生"),
        new UserDisplayName("USER_M4_SIGN", "签发医生"),
        new UserDisplayName("USER_M4_TRACKING", "报告追踪员"),
        new UserDisplayName("USER_M4_ORDER_EXECUTE", "医嘱执行员"),
        new UserDisplayName("USER_M4_NO_PERMISSION", "无权限用户")
    );

    private static final List<DisplayColumnRepair> DISPLAY_COLUMN_REPAIRS = List.of(
        new DisplayColumnRepair("operation_logs", "operator_user_id", "operator_name"),
        new DisplayColumnRepair("applications", "submitting_doctor_user_id", "submitting_doctor_name"),
        new DisplayColumnRepair("pathology_cases", "received_by_user_id", "received_by_name"),
        new DisplayColumnRepair("specimens", "applicant_doctor_user_id", "applicant_doctor_name"),
        new DisplayColumnRepair("specimens", "registered_by_user_id", "registered_by_name"),
        new DisplayColumnRepair("specimen_collection_records", "collector_user_id", "collector_name"),
        new DisplayColumnRepair("specimen_fixation_records", "verified_by_user_id", "verified_by_name"),
        new DisplayColumnRepair("transport_orders", "handover_user_id", "handover_user_name"),
        new DisplayColumnRepair("transport_orders", "receiver_user_id", "receiver_user_name"),
        new DisplayColumnRepair("transport_order_items", "verified_by_user_id", "verified_by_name"),
        new DisplayColumnRepair("specimen_receipts", "received_by_user_id", "received_by_name"),
        new DisplayColumnRepair("workflow_events", "operator_user_id", "operator_name"),
        new DisplayColumnRepair("samplings", "sampled_by_user_id", "sampled_by_name"),
        new DisplayColumnRepair("dehydration_batches", "operator_user_id", "operator_name"),
        new DisplayColumnRepair("embeddings", "embedded_by_user_id", "embedded_by_name"),
        new DisplayColumnRepair("slicings", "sliced_by_user_id", "sliced_by_name"),
        new DisplayColumnRepair("slide_stainings", "stained_by_user_id", "stained_by_name"),
        new DisplayColumnRepair("rework_orders", "requested_by_user_id", "requested_by_name"),
        new DisplayColumnRepair("rework_orders", "executed_by_user_id", "executed_by_name"),
        new DisplayColumnRepair("slide_qc_evaluations", "evaluator_user_id", "evaluator_name"),
        new DisplayColumnRepair("case_media_assets", "captured_by_user_id", "captured_by_name"),
        new DisplayColumnRepair("diagnostic_tasks", "assigned_by_user_id", "assigned_by_name"),
        new DisplayColumnRepair("diagnostic_tasks", "diagnosis_doctor_user_id", "diagnosis_doctor_name"),
        new DisplayColumnRepair("diagnostic_tasks", "primary_doctor_user_id", "primary_doctor_name"),
        new DisplayColumnRepair("diagnostic_tasks", "review_doctor_user_id", "review_doctor_name"),
        new DisplayColumnRepair("diagnostic_tasks", "reviewer_user_id", "reviewer_name"),
        new DisplayColumnRepair("pathology_reports", "reviewer_user_id", "reviewer_name"),
        new DisplayColumnRepair("pathology_reports", "signed_by_user_id", "signed_by_name"),
        new DisplayColumnRepair("report_versions", "signed_by_user_id", "signed_by_name"),
        new DisplayColumnRepair("report_revision_requests", "requested_by_user_id", "requested_by_name"),
        new DisplayColumnRepair("report_revision_requests", "reviewed_by_user_id", "reviewed_by_name"),
        new DisplayColumnRepair("medical_orders", "doctor_user_id", "doctor_name"),
        new DisplayColumnRepair("medical_orders", "executor_user_id", "executor_name"),
        new DisplayColumnRepair("consultation_cases", "requested_by_user_id", "requested_by_name"),
        new DisplayColumnRepair("consultation_cases", "host_user_id", "host_name"),
        new DisplayColumnRepair("consultation_participants", "participant_user_id", "participant_name"),
        new DisplayColumnRepair("consultation_participants", "drafted_by_user_id", "drafted_by_name")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        repairBuiltinUserNames(connection);
        for (DisplayColumnRepair repair : DISPLAY_COLUMN_REPAIRS) {
            repairDisplayColumnIfPresent(connection, repair);
        }
    }

    private void repairBuiltinUserNames(Connection connection) throws SQLException {
        if (!hasColumns(connection, "users", "id", "name")) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
            update users
            set name = ?
            where id = ?
            """)) {
            for (UserDisplayName user : BUILTIN_USERS) {
                statement.setString(1, user.displayName);
                statement.setString(2, user.id);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void repairDisplayColumnIfPresent(Connection connection, DisplayColumnRepair repair) throws SQLException {
        if (!hasColumns(connection, repair.table, repair.userIdColumn, repair.displayNameColumn)) {
            return;
        }
        if (!hasColumns(connection, "users", "id", "name")) {
            return;
        }

        String placeholders = String.join(", ", Collections.nCopies(BUILTIN_USERS.size(), "?"));
        String sql = """
            update %s
            set %s = (
                select name
                from users
                where users.id = %s.%s
            )
            where %s in (%s)
              and exists (
                select 1
                from users
                where users.id = %s.%s
              )
            """.formatted(
            repair.table,
            repair.displayNameColumn,
            repair.table,
            repair.userIdColumn,
            repair.userIdColumn,
            placeholders,
            repair.table,
            repair.userIdColumn
        );
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < BUILTIN_USERS.size(); i++) {
                statement.setString(i + 1, BUILTIN_USERS.get(i).id);
            }
            statement.executeUpdate();
        }
    }

    private boolean hasColumns(Connection connection, String tableName, String... columnNames) throws SQLException {
        for (String columnName : columnNames) {
            if (!hasColumn(connection, tableName, columnName)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String tableVariant : nameVariants(tableName)) {
            for (String columnVariant : nameVariants(columnName)) {
                try (ResultSet columns = metaData.getColumns(null, null, tableVariant, columnVariant)) {
                    if (columns.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> nameVariants(String name) {
        return List.of(name, name.toUpperCase(Locale.ROOT), name.toLowerCase(Locale.ROOT));
    }

    private static final class UserDisplayName {
        private final String id;
        private final String displayName;

        private UserDisplayName(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    private static final class DisplayColumnRepair {
        private final String table;
        private final String userIdColumn;
        private final String displayNameColumn;

        private DisplayColumnRepair(String table, String userIdColumn, String displayNameColumn) {
            this.table = table;
            this.userIdColumn = userIdColumn;
            this.displayNameColumn = displayNameColumn;
        }
    }
}
