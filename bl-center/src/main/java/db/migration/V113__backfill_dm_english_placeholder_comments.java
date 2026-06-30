package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class V113__backfill_dm_english_placeholder_comments extends BaseJavaMigration {

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
        List<DmTableCommentDefinition> tables = new ArrayList<>();
        tables.addAll(V110__backfill_dm_table_and_column_comments.commentDefinitions());
        tables.addAll(V111__backfill_dm_remaining_business_comments.commentDefinitions());
        tables.addAll(V112__backfill_dm_more_business_comments.commentDefinitions());
        tables.addAll(List.of(
            table("CASE_MEDIA_ASSETS", "病例统一附件与影像归档表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("DEHYDRATION_BATCHES", "脱水批次表",
                column("CASE_ID", "病例ID"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("DEHYDRATION_BATCH_ITEMS", "脱水批次明细表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("EMBEDDINGS", "包埋记录表",
                column("SAMPLING_BLOCK_ID", "取材块ID"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("EMBEDDING_BOXES", "包埋盒表",
                column("SAMPLING_BLOCK_ID", "取材块ID"),
                column("UPDATED_AT", "更新时间")),
            table("MATERIAL_LOANS", "蜡块/玻片/申请单借阅记录表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间"),
                column("BORROWER_PHONE", "借阅人电话"),
                column("BORROWER_UNIT", "借阅单位"),
                column("DEPOSIT_AMOUNT", "押金金额")),
            table("MEDICAL_ORDER_QC_EVALUATIONS", "医嘱质控评价表",
                column("REMARKS", "备注")),
            table("PATHOLOGY_CASES", "病理病例主表",
                column("CASE_STATUS", "病例状态")),
            table("REPORT_REVISION_REQUESTS", "病理报告修订申请审批表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("REPORT_VERSIONS", "病理报告历史版本表",
                column("PRINT_STATUS", "打印状态"),
                column("PRINTED_AT", "打印时间"),
                column("DELIVERY_STATUS", "发放状态"),
                column("ISSUED_AT", "签发时间"),
                column("RECALLED_AT", "召回时间"),
                column("PLANNED_ISSUE_AT", "计划签发时间"),
                column("DELIVERY_SCHEDULE_STATUS", "发放排程状态")),
            table("REWORK_ORDERS", "返工医嘱表",
                column("SAMPLING_BLOCK_ID", "取材块ID"),
                column("EMBEDDING_BOX_ID", "包埋盒ID"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("SAMPLINGS", "取材记录表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间"),
                column("SIZE_TEXT", "大小描述"),
                column("CUT_SURFACE_FEATURE", "切面特征"),
                column("MARGIN_MARKING", "切缘标记")),
            table("SAMPLING_BLOCKS", "取材块明细表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间"),
                column("EMBEDDING_BOX_NAME", "包埋盒名称"),
                column("EMBEDDING_BOX_STATUS", "包埋盒状态"),
                column("EMBEDDING_REMARKS", "包埋备注")),
            table("SLICINGS", "切片批次表",
                column("EMBEDDING_BOX_ID", "包埋盒ID"),
                column("SLICE_COUNT_PER_SLIDE", "每张玻片切片数"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间"),
                column("TASK_ID", "任务ID")),
            table("SLIDES", "物理玻片表",
                column("EMBEDDING_BOX_ID", "包埋盒ID"),
                column("UPDATED_AT", "更新时间")),
            table("SLIDE_QC_EVALUATIONS", "玻片质控评价表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("SLIDE_STAININGS", "玻片染色记录表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("SPECIMEN_COLLECTION_RECORDS", "标本采集登记记录表",
                column("TERMINAL_CODE", "终端编码"),
                column("PRINTER_CODE", "打印机编码")),
            table("SPECIMEN_FIXATION_RECORDS", "标本固定核对记录表",
                column("APPLICATION_ID", "申请单ID"),
                column("TERMINAL_CODE", "终端编码"),
                column("VERIFICATION_STARTED_AT", "核对开始时间"),
                column("VERIFICATION_COMPLETED_AT", "核对完成时间")),
            table("SPECIMEN_RECEIPTS", "标本接收与拒收记录表",
                column("APPLICATION_ID", "申请单ID"),
                column("TRANSPORT_ORDER_ID", "转运单ID"),
                column("TERMINAL_CODE", "终端编码"),
                column("QUALITY_CHECK_RESULT", "质检结果"),
                column("QUALITY_ISSUE_CODES", "质检问题编码集"),
                column("LOGISTICS_STAFF_NAME", "物流交接人姓名")),
            table("SPECIMEN_STORAGE_RECORDS", "标本/蜡块/玻片实物流转与存储记录表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间"),
                column("ARCHIVE_EXPIRES_AT", "归档到期时间"),
                column("ARCHIVE_REMINDER_DAYS", "归档提醒天数")),
            table("TRANSPORT_ORDERS", "标本转运单主表",
                column("TERMINAL_CODE", "终端编码"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间"),
                column("OUTBOUND_USER_ID", "出库人用户ID"),
                column("OUTBOUND_USER_NAME", "出库人姓名")),
            table("TRANSPORT_ORDER_ITEMS", "标本转运单明细表",
                column("APPLICATION_ID", "申请单ID"))
        ));
        return List.copyOf(tables);
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
