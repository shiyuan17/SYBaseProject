package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class V102__seed_specimen_dictionary_system_config extends BaseJavaMigration {

    private static final String ROOT_CATEGORY_ID = "SCC_SPECIMEN_DICTIONARY";
    private static final String ROOT_CATEGORY_CODE = "SPECIMEN_DICTIONARY";
    private static final String ROOT_CATEGORY_NAME = "标本字典";
    private static final String ROOT_CATEGORY_TYPE = "SPECIMEN_DICTIONARY";

    private static final String DEPT_ORTHO = "DEPT-ORTHO";
    private static final String DEPT_EMERGENCY = "DEPT-EMERGENCY";
    private static final String DEPT_GENERAL = "DEPT-GENERAL";
    private static final String DEPT_GYNE = "DEPT-GYNE";

    private static final List<DepartmentSeed> DEPARTMENTS = List.of(
        new DepartmentSeed(DEPT_ORTHO, "DEPT_CLINICAL", "ORTHO", "骨科", 40),
        new DepartmentSeed(DEPT_EMERGENCY, "DEPT_CLINICAL", "EMERGENCY", "急诊科", 50),
        new DepartmentSeed(DEPT_GENERAL, "DEPT_CLINICAL", "GENERAL_SURGERY", "普外科", 60),
        new DepartmentSeed(DEPT_GYNE, "DEPT_CLINICAL", "GYNECOLOGY", "妇科", 70)
    );

    private static final List<CategorySeed> CATEGORIES = List.of(
        new CategorySeed("SCC_SPEC_DICT_SYS_001", ROOT_CATEGORY_ID, "SPECIMEN_SYSTEM_BONE_SOFT", "骨、关节及软组织", 10),
        new CategorySeed("SCC_SPEC_DICT_PART_101", "SCC_SPEC_DICT_SYS_001", "SPECIMEN_PART_OSTEOMYELITIS", "骨髓炎", 10),
        new CategorySeed("SCC_SPEC_DICT_PART_102", "SCC_SPEC_DICT_SYS_001", "SPECIMEN_PART_JOINT", "关节", 20),
        new CategorySeed("SCC_SPEC_DICT_SYS_002", ROOT_CATEGORY_ID, "SPECIMEN_SYSTEM_BREAST_SKIN", "乳腺及皮肤", 20),
        new CategorySeed("SCC_SPEC_DICT_PART_201", "SCC_SPEC_DICT_SYS_002", "SPECIMEN_PART_SKIN", "皮肤", 10),
        new CategorySeed("SCC_SPEC_DICT_PART_202", "SCC_SPEC_DICT_SYS_002", "SPECIMEN_PART_BREAST", "乳腺", 20),
        new CategorySeed("SCC_SPEC_DICT_SYS_003", ROOT_CATEGORY_ID, "SPECIMEN_SYSTEM_GYNE", "妇科", 30),
        new CategorySeed("SCC_SPEC_DICT_PART_301", "SCC_SPEC_DICT_SYS_003", "SPECIMEN_PART_CERVIX", "宫颈", 10),
        new CategorySeed("SCC_SPEC_DICT_PART_302", "SCC_SPEC_DICT_SYS_003", "SPECIMEN_PART_UTERUS", "子宫", 20)
    );

    private static final List<ItemSeed> ITEMS = List.of(
        new ItemSeed("SCI_SPEC_DICT_001", "SCC_SPEC_DICT_PART_101", "specimen.dictionary.item.001", "右侧胫骨感染病灶", 10, Set.of(DEPT_ORTHO, DEPT_EMERGENCY)),
        new ItemSeed("SCI_SPEC_DICT_002", "SCC_SPEC_DICT_PART_101", "specimen.dictionary.item.002", "右股骨骨髓炎病灶", 20, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_003", "SCC_SPEC_DICT_PART_101", "specimen.dictionary.item.003", "右腓骨骨髓炎病灶", 30, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_004", "SCC_SPEC_DICT_PART_101", "specimen.dictionary.item.004", "左侧胫骨感染病灶", 40, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_005", "SCC_SPEC_DICT_PART_101", "specimen.dictionary.item.005", "左股骨骨髓炎病灶", 50, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_006", "SCC_SPEC_DICT_PART_101", "specimen.dictionary.item.006", "左腓骨骨髓炎病灶", 60, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_007", "SCC_SPEC_DICT_PART_102", "specimen.dictionary.item.007", "膝关节滑膜组织", 10, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_008", "SCC_SPEC_DICT_PART_102", "specimen.dictionary.item.008", "髋关节滑膜组织", 20, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_009", "SCC_SPEC_DICT_PART_102", "specimen.dictionary.item.009", "踝关节病灶组织", 30, Set.of(DEPT_ORTHO)),
        new ItemSeed("SCI_SPEC_DICT_010", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.010", "背部黑毛痣", 10, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_011", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.011", "背部溃疡组织", 20, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_012", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.012", "背部皮肤肿物", 30, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_013", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.013", "额部黑毛痣", 40, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_014", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.014", "额部皮肤肿物", 50, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_015", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.015", "腹部皮肤活检", 60, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_016", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.016", "颈部皮肤肿物", 70, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_017", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.017", "皮肤溃疡组织", 80, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_018", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.018", "头部皮肤肿物", 90, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_019", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.019", "头皮肿物", 100, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_020", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.020", "右面部黑毛痣", 110, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_021", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.021", "右前臂皮肤", 120, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_022", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.022", "左手背", 130, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_023", "SCC_SPEC_DICT_PART_201", "specimen.dictionary.item.023", "左前臂黑毛痣", 140, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_024", "SCC_SPEC_DICT_PART_202", "specimen.dictionary.item.024", "右乳外上象限结节", 10, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_025", "SCC_SPEC_DICT_PART_202", "specimen.dictionary.item.025", "左乳腺肿物", 20, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_026", "SCC_SPEC_DICT_PART_202", "specimen.dictionary.item.026", "乳头乳晕区组织", 30, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_027", "SCC_SPEC_DICT_PART_202", "specimen.dictionary.item.027", "乳腺钙化灶", 40, Set.of(DEPT_GENERAL)),
        new ItemSeed("SCI_SPEC_DICT_028", "SCC_SPEC_DICT_PART_301", "specimen.dictionary.item.028", "宫颈 3 点位组织", 10, Set.of(DEPT_GYNE)),
        new ItemSeed("SCI_SPEC_DICT_029", "SCC_SPEC_DICT_PART_301", "specimen.dictionary.item.029", "宫颈 6 点位组织", 20, Set.of(DEPT_GYNE)),
        new ItemSeed("SCI_SPEC_DICT_030", "SCC_SPEC_DICT_PART_301", "specimen.dictionary.item.030", "宫颈锥切标本", 30, Set.of(DEPT_GYNE)),
        new ItemSeed("SCI_SPEC_DICT_031", "SCC_SPEC_DICT_PART_301", "specimen.dictionary.item.031", "宫颈活检组织", 40, Set.of(DEPT_GYNE)),
        new ItemSeed("SCI_SPEC_DICT_032", "SCC_SPEC_DICT_PART_302", "specimen.dictionary.item.032", "宫腔内容物", 10, Set.of(DEPT_GYNE)),
        new ItemSeed("SCI_SPEC_DICT_033", "SCC_SPEC_DICT_PART_302", "specimen.dictionary.item.033", "子宫内膜组织", 20, Set.of(DEPT_GYNE)),
        new ItemSeed("SCI_SPEC_DICT_034", "SCC_SPEC_DICT_PART_302", "specimen.dictionary.item.034", "子宫肌瘤组织", 30, Set.of(DEPT_GYNE))
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureItemDepartmentTable(connection);
        seedDepartments(connection);
        ensureRootCategory(connection);
        seedCategories(connection);
        seedItems(connection);
        seedItemDepartments(connection);
    }

    private void ensureItemDepartmentTable(Connection connection) throws SQLException {
        if (tableExists(connection, "SYSTEM_CONFIG_ITEM_DEPARTMENTS")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE system_config_item_departments (
                    id VARCHAR(64) NOT NULL,
                    config_item_id VARCHAR(64) NOT NULL,
                    department_id VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    CONSTRAINT pk_system_config_item_departments PRIMARY KEY (id),
                    CONSTRAINT uk_system_config_item_departments UNIQUE (config_item_id, department_id),
                    CONSTRAINT fk_system_config_item_departments_item FOREIGN KEY (config_item_id) REFERENCES system_config_items (id)
                )
                """);
        }
    }

    private void seedDepartments(Connection connection) throws SQLException {
        for (DepartmentSeed department : DEPARTMENTS) {
            if (exists(connection, "select 1 from department_dict where id = ?", department.id())) {
                continue;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into department_dict
                    (id, parent_id, department_code, department_name, sort_order, enabled, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, 1, ?, ?)
                """)) {
                insert.setString(1, department.id());
                insert.setString(2, department.parentId());
                insert.setString(3, department.code());
                insert.setString(4, department.name());
                insert.setInt(5, department.sortOrder());
                insert.setTimestamp(6, now());
                insert.setTimestamp(7, now());
                insert.executeUpdate();
            }
        }
    }

    private void ensureRootCategory(Connection connection) throws SQLException {
        if (exists(connection, "select 1 from system_config_categories where id = ?", ROOT_CATEGORY_ID)
            || exists(connection, "select 1 from system_config_categories where category_code = ?", ROOT_CATEGORY_CODE)) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into system_config_categories
                (id, parent_id, category_code, category_name, category_type, sort_order, enabled, created_at, updated_at)
            values
                (?, 'SCC_ROOT', ?, ?, ?, 30, 1, ?, ?)
            """)) {
            insert.setString(1, ROOT_CATEGORY_ID);
            insert.setString(2, ROOT_CATEGORY_CODE);
            insert.setString(3, ROOT_CATEGORY_NAME);
            insert.setString(4, ROOT_CATEGORY_TYPE);
            insert.setTimestamp(5, now());
            insert.setTimestamp(6, now());
            insert.executeUpdate();
        }
    }

    private void seedCategories(Connection connection) throws SQLException {
        for (CategorySeed category : CATEGORIES) {
            if (exists(connection, "select 1 from system_config_categories where id = ?", category.id())
                || exists(connection, "select 1 from system_config_categories where category_code = ?", category.code())) {
                continue;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_categories
                    (id, parent_id, category_code, category_name, category_type, sort_order, enabled, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, 1, ?, ?)
                """)) {
                insert.setString(1, category.id());
                insert.setString(2, category.parentId());
                insert.setString(3, category.code());
                insert.setString(4, category.name());
                insert.setString(5, ROOT_CATEGORY_TYPE);
                insert.setInt(6, category.sortOrder());
                insert.setTimestamp(7, now());
                insert.setTimestamp(8, now());
                insert.executeUpdate();
            }
        }
    }

    private void seedItems(Connection connection) throws SQLException {
        for (ItemSeed item : ITEMS) {
            if (exists(connection, "select 1 from system_config_items where id = ?", item.id())
                || exists(connection, "select 1 from system_config_items where config_key = ?", item.configKey())) {
                continue;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                insert into system_config_items
                    (id, category_id, config_key, config_name, config_value, value_type, sort_order,
                     enabled, remarks, created_at, updated_at)
                values
                    (?, ?, ?, ?, null, 'SPECIMEN_DICTIONARY_ITEM', ?, 1, null, ?, ?)
                """)) {
                insert.setString(1, item.id());
                insert.setString(2, item.partCategoryId());
                insert.setString(3, item.configKey());
                insert.setString(4, item.specimenName());
                insert.setInt(5, item.sortOrder());
                insert.setTimestamp(6, now());
                insert.setTimestamp(7, now());
                insert.executeUpdate();
            }
        }
    }

    private void seedItemDepartments(Connection connection) throws SQLException {
        for (ItemSeed item : ITEMS) {
            for (String departmentId : item.departmentIds()) {
                if (exists(connection,
                    "select 1 from system_config_item_departments where config_item_id = ? and department_id = ?",
                    item.id(),
                    departmentId)) {
                    continue;
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                    insert into system_config_item_departments
                        (id, config_item_id, department_id, created_at)
                    values
                        (?, ?, ?, ?)
                    """)) {
                    insert.setString(1, item.id() + "_" + departmentId);
                    insert.setString(2, item.id());
                    insert.setString(3, departmentId);
                    insert.setTimestamp(4, now());
                    insert.executeUpdate();
                }
            }
        }
    }

    private boolean exists(Connection connection, String sql, String... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select count(*)
            from information_schema.tables
            where upper(table_name) = upper(?)
            """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ignored) {
        }
        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, null, null)) {
            while (resultSet.next()) {
                String existing = resultSet.getString("TABLE_NAME");
                if (existing != null && existing.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private record DepartmentSeed(String id, String parentId, String code, String name, int sortOrder) {
    }

    private record CategorySeed(String id, String parentId, String code, String name, int sortOrder) {
    }

    private record ItemSeed(
        String id,
        String partCategoryId,
        String configKey,
        String specimenName,
        int sortOrder,
        Set<String> departmentIds
    ) {
    }
}
