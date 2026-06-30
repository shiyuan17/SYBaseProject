package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class V112__backfill_dm_more_business_comments extends BaseJavaMigration {

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
            table("ARCHIVE_CABINETS", "归档柜主表",
                column("LAYER_COUNT", "层数"),
                column("SLOT_COUNT_PER_LAYER", "每层格位数"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("ARCHIVE_POSITIONS", "归档柜位表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("BILLING_RECORDS", "病理计费留痕表",
                column("EXTERNAL_SYSTEM", "外部系统标识"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("CONSULTATION_CASES", "病理会诊流程表",
                column("HOST_USER_ID", "发起方用户ID"),
                column("HOST_NAME", "发起方姓名"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("CONSULTATION_PARTICIPANTS", "会诊参与人明细表",
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间")),
            table("MEDICAL_ORDERS", "通用病理医嘱表",
                column("EXECUTOR_USER_ID", "执行人用户ID"),
                column("EXECUTOR_NAME", "执行人姓名"),
                column("ACCEPTED_AT", "接单时间"),
                column("COMPLETED_AT", "完成时间"),
                column("CANCELLED_AT", "取消时间"),
                column("REMARKS", "备注"),
                column("ORDER_ITEM_ID", "医嘱项目ID"),
                column("ORDER_ITEM_CODE", "医嘱项目编码"),
                column("ORDER_ITEM_NAME", "医嘱项目名称"),
                column("ORDER_CATEGORY_ID", "医嘱分类ID"),
                column("ORDER_CATEGORY_CODE", "医嘱分类编码"),
                column("ORDER_CATEGORY_NAME", "医嘱分类名称"),
                column("PRINTED_AT", "打印时间"),
                column("PRINTED_BY_USER_ID", "打印人用户ID"),
                column("PRINTED_BY_NAME", "打印人姓名"),
                column("RELEASED_AT", "发放时间"),
                column("RELEASED_BY_USER_ID", "发放人用户ID"),
                column("RELEASED_BY_NAME", "发放人姓名"),
                column("TERMINATED_AT", "终止时间"),
                column("TERMINATED_BY_USER_ID", "终止人用户ID"),
                column("TERMINATED_BY_NAME", "终止人姓名"),
                column("TERMINATION_REASON_CODE", "终止原因编码"),
                column("TERMINATION_REASON_LABEL", "终止原因名称"),
                column("TARGET_TYPE", "目标对象类型"),
                column("TARGET_SPECIMEN_ID", "目标标本ID"),
                column("TARGET_SPECIMEN_NO", "目标标本编号"),
                column("TARGET_BLOCK_ID", "目标蜡块ID"),
                column("TARGET_BLOCK_NO", "目标蜡块号"),
                column("TARGET_SLIDE_ID", "目标玻片ID"),
                column("TARGET_SLIDE_NO", "目标玻片号")),
            table("REAGENTS", "试剂主数据表",
                column("REAGENT_TYPE", "试剂类型"),
                column("REAGENT_USAGE", "试剂用途"),
                column("ORDER_DICT_ITEM_ID", "医嘱字典项目ID"),
                column("CLONE_NO", "克隆号"),
                column("RECOMMENDED_DILUTION", "推荐稀释比例"),
                column("APPLICATION_DILUTION", "上机稀释比例"),
                column("TEMPLATE_STATUS", "模板状态"),
                column("VALIDITY_DAYS", "有效天数"),
                column("DEFAULT_STOCK_THRESHOLD", "默认库存预警阈值"),
                column("STAIN_CAPACITY", "单瓶染色容量"),
                column("STAIN_THRESHOLD", "染色预警阈值"),
                column("CREATED_BY_USER_ID", "创建人用户ID"),
                column("CREATED_BY_NAME", "创建人姓名"),
                column("UPDATED_BY_USER_ID", "更新人用户ID"),
                column("UPDATED_BY_NAME", "更新人姓名")),
            table("REAGENT_STOCKS", "试剂库存批次表",
                column("INITIAL_QUANTITY", "初始数量"),
                column("REMAINING_QUANTITY", "剩余数量"),
                column("PRODUCTION_DATE", "生产日期"),
                column("INBOUND_AT", "入库时间"),
                column("TEST_REMINDER_THRESHOLD", "检测提醒阈值"),
                column("EXPIRY_REMINDER_THRESHOLD", "效期提醒阈值"),
                column("RECOMMENDED_DILUTION", "推荐稀释比例"),
                column("APPLICATION_DILUTION", "上机稀释比例"),
                column("STAIN_CAPACITY", "单瓶染色容量"),
                column("STAIN_THRESHOLD", "染色预警阈值"),
                column("VALIDITY_DAYS", "有效天数"),
                column("TESTED_AT", "检测时间"),
                column("STARTED_AT", "启用时间"),
                column("FINISHED_AT", "用尽时间"),
                column("CREATED_BY_USER_ID", "创建人用户ID"),
                column("CREATED_BY_NAME", "创建人姓名"),
                column("UPDATED_BY_USER_ID", "更新人用户ID"),
                column("UPDATED_BY_NAME", "更新人姓名")),
            table("SPECIMENS", "病例下标本表",
                column("SPECIMEN_STATUS", "标本状态"),
                column("LABEL_PRINT_BATCH_NO", "标签打印批次号"),
                column("LABEL_PRINT_STATUS", "标签打印状态"),
                column("TERMINAL_CODE", "终端编码"),
                column("CREATED_AT", "创建时间"),
                column("UPDATED_AT", "更新时间"),
                column("CONTAINER_NAME", "容器名称"),
                column("CONTAINER_COUNT", "容器数量"),
                column("SPECIMEN_CONFIRMED_AT", "标本确认时间"),
                column("CHECK_IN_STATUS", "签入状态"),
                column("CHECKED_IN_AT", "签入时间"),
                column("CHECKED_IN_BY_USER_ID", "签入人用户ID"),
                column("CHECKED_IN_BY_NAME", "签入人姓名"),
                column("SPECIMEN_REMOVAL_AT", "标本离体时间"),
                column("SPECIMEN_REMOVAL_OPERATOR_USER_ID", "离体登记人用户ID"),
                column("SPECIMEN_REMOVAL_OPERATOR_NAME", "离体登记人姓名"),
                column("SPECIMEN_SIZE", "标本大小"),
                column("FROZEN_FLAG", "是否冰冻"),
                column("REGISTRATION_EVALUATION_ITEMS", "登记评估项目")),
            table("WORKFLOW_EVENTS", "病理流程轨迹事件表",
                column("APPLICATION_ID", "申请单ID"),
                column("TRANSPORT_ORDER_ID", "转运单ID"),
                column("EVENT_TYPE", "事件类型"),
                column("EVENT_STATUS", "事件状态"),
                column("EVENT_TIME", "事件时间"),
                column("SOURCE_TERMINAL", "来源终端"),
                column("EVENT_CONTENT", "事件内容"),
                column("CREATED_AT", "创建时间"),
                column("OPERATOR_IP", "操作IP"),
                column("OPERATOR_DEVICE", "操作设备"))
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
