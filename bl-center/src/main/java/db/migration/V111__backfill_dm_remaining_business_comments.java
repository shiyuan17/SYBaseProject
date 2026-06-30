package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class V111__backfill_dm_remaining_business_comments extends BaseJavaMigration {

    private static final String OWNER = "SYSDBA";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isDmDatabase(connection)) {
            return;
        }
        new DmCommentBackfillSupport(OWNER).backfill(connection, commentDefinitions());
    }

    private boolean isDmDatabase(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toUpperCase().contains("DM");
    }

    static List<DmTableCommentDefinition> commentDefinitions() {
        return List.of(
            table("APPLICATIONS", "病理申请单表",
                column("PATIENT_NAME", "患者姓名"),
                column("PATIENT_GENDER", "患者性别"),
                column("PATIENT_AGE", "患者年龄")),
            table("APPLICATION_REGISTRATION_WORKBENCH", "前台申请登记工作台扩展表",
                column("TECHNICAL_HISTORY_SUMMARY_OVERRIDE", "技术病史摘要补充"),
                column("TECHNICAL_CLINICAL_EXAM_SURGERY_OVERRIDE", "技术临床检查及手术补充"),
                column("TECHNICAL_LAB_IMAGING_OVERRIDE", "技术检验及影像补充"),
                column("TECHNICAL_SUBMISSION_REQUIREMENT_OVERRIDE", "技术送检要求补充"),
                column("TECHNICAL_INFECTIOUS_PAST_HISTORY_OVERRIDE", "技术传染病史补充"),
                column("TECHNICAL_EXTERNAL_PATHOLOGY_DIAGNOSIS_OVERRIDE", "技术院外病理诊断补充"))
        );
    }

    private static DmTableCommentDefinition table(String tableName,
                                                  String comment,
                                                  DmColumnCommentDefinition... columns) {
        return new DmTableCommentDefinition(tableName, comment, List.of(columns));
    }

    private static DmColumnCommentDefinition column(String columnName, String comment) {
        return new DmColumnCommentDefinition(columnName, comment);
    }
}
