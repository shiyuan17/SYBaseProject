package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class V100__repair_builtin_workflow_role_names extends BaseJavaMigration {

    private static final List<RoleDisplayName> BUILTIN_WORKFLOW_ROLES = List.of(
        new RoleDisplayName("ROLE_M2_CLINICAL_REGISTER", "标本登记员"),
        new RoleDisplayName("ROLE_M2_FIXATION_VERIFY", "固定核验员"),
        new RoleDisplayName("ROLE_M2_TRANSPORT_HANDOVER", "转运交接员"),
        new RoleDisplayName("ROLE_M2_SPECIMEN_RECEIVE", "标本接收员"),
        new RoleDisplayName("ROLE_M2_TRACKING_QUERY", "标本追踪员"),
        new RoleDisplayName("ROLE_M2_CLINICAL_IMPORT", "临床导入员"),
        new RoleDisplayName("ROLE_M3_GROSSING", "取材员"),
        new RoleDisplayName("ROLE_M3_DEHYDRATION", "脱水员"),
        new RoleDisplayName("ROLE_M3_EMBEDDING", "包埋员"),
        new RoleDisplayName("ROLE_M3_SLICING", "切片员"),
        new RoleDisplayName("ROLE_M3_STAINING", "染色员"),
        new RoleDisplayName("ROLE_M3_REWORK", "返工员"),
        new RoleDisplayName("ROLE_M3_TRACKING", "技术追踪员"),
        new RoleDisplayName("ROLE_M4_ASSIGN", "诊断分派员"),
        new RoleDisplayName("ROLE_M4_DIAGNOSIS", "诊断医生"),
        new RoleDisplayName("ROLE_M4_REVIEW", "审核医生"),
        new RoleDisplayName("ROLE_M4_SIGN", "签发医生"),
        new RoleDisplayName("ROLE_M4_TRACKING", "报告追踪员"),
        new RoleDisplayName("ROLE_M4_MEDICAL_ORDER_EXECUTE", "医嘱执行员")
    );

    @Override
    public void migrate(Context context) throws Exception {
        repairBuiltinWorkflowRoleNames(context.getConnection());
    }

    private void repairBuiltinWorkflowRoleNames(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            update roles
            set role_name = ?
            where id = ?
            """)) {
            for (RoleDisplayName role : BUILTIN_WORKFLOW_ROLES) {
                statement.setString(1, role.displayName);
                statement.setString(2, role.id);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static final class RoleDisplayName {
        private final String id;
        private final String displayName;

        private RoleDisplayName(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }
}
