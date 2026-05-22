package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V40__seed_masterdata_code_numbering_rules extends BaseJavaMigration {

    private static final List<NumberingRuleSeed> RULE_SEEDS = List.of(
        new NumberingRuleSeed("NR_DEPARTMENT_CODE", "RULE_DEPARTMENT_CODE", "DEPARTMENT_CODE", "DEPT-", "科室编码"),
        new NumberingRuleSeed("NR_BODY_PART_CODE", "RULE_BODY_PART_CODE", "BODY_PART_CODE", "BP-", "部位编码"),
        new NumberingRuleSeed("NR_ROLE_CODE", "RULE_ROLE_CODE", "ROLE_CODE", "ROLE-", "角色编码"),
        new NumberingRuleSeed("NR_USER_CODE", "RULE_USER_CODE", "USER_CODE", "USER-", "用户编码"),
        new NumberingRuleSeed("NR_LOGIN_TAG_CODE", "RULE_LOGIN_TAG_CODE", "LOGIN_TAG_CODE", "LT-", "登录标签编码"),
        new NumberingRuleSeed("NR_ORDER_CATEGORY_CODE", "RULE_ORDER_CATEGORY_CODE", "ORDER_CATEGORY_CODE", "ODC-", "医嘱分类编码"),
        new NumberingRuleSeed("NR_ORDER_ITEM_CODE", "RULE_ORDER_ITEM_CODE", "ORDER_ITEM_CODE", "ODI-", "医嘱条目编码"),
        new NumberingRuleSeed("NR_CHARGE_ITEM_CODE", "RULE_CHARGE_ITEM_CODE", "CHARGE_ITEM_CODE", "OCI-", "收费项目编码"),
        new NumberingRuleSeed("NR_PACKAGE_CODE", "RULE_PACKAGE_CODE", "PACKAGE_CODE", "PKG-", "套餐编码"),
        new NumberingRuleSeed("NR_TEMPLATE_CATEGORY_CODE", "RULE_TEMPLATE_CATEGORY_CODE", "TEMPLATE_CATEGORY_CODE", "STC-", "模板分类编码"),
        new NumberingRuleSeed("NR_TEMPLATE_CODE", "RULE_TEMPLATE_CODE", "TEMPLATE_CODE", "TPL-", "模板编码"),
        new NumberingRuleSeed("NR_GUIDELINE_CATEGORY_CODE", "RULE_GUIDELINE_CATEGORY_CODE", "GUIDELINE_CATEGORY_CODE", "SGC-", "规范分类编码"),
        new NumberingRuleSeed("NR_GUIDELINE_CODE", "RULE_GUIDELINE_CODE", "GUIDELINE_CODE", "GL-", "规范编码"),
        new NumberingRuleSeed("NR_CONFIG_CATEGORY_CODE", "RULE_CONFIG_CATEGORY_CODE", "CONFIG_CATEGORY_CODE", "CFG-", "配置分类编码")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (NumberingRuleSeed seed : RULE_SEEDS) {
            if (!exists(connection, seed)) {
                insert(connection, seed);
            }
        }
    }

    private boolean exists(Connection connection, NumberingRuleSeed seed) throws Exception {
        try (PreparedStatement query = connection.prepareStatement("""
            select 1
            from numbering_rules
            where id = ? or rule_code = ? or biz_type = ?
            """)) {
            query.setString(1, seed.id());
            query.setString(2, seed.ruleCode());
            query.setString(3, seed.bizType());
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insert(Connection connection, NumberingRuleSeed seed) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into numbering_rules
                (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type,
                 enabled, remarks, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            insert.setString(1, seed.id());
            insert.setString(2, seed.ruleCode());
            insert.setString(3, seed.bizType());
            insert.setString(4, seed.prefixPattern());
            insert.setString(5, "");
            insert.setInt(6, 4);
            insert.setString(7, "NONE");
            insert.setString(8, "GLOBAL");
            insert.setInt(9, 1);
            insert.setString(10, seed.remarks());
            insert.setTimestamp(11, now);
            insert.setTimestamp(12, now);
            insert.executeUpdate();
        }
    }

    private record NumberingRuleSeed(
        String id,
        String ruleCode,
        String bizType,
        String prefixPattern,
        String remarks
    ) {
    }
}
