package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecimenDictionarySystemConfigV99MigrationTest {

    @Test
    void shouldSeedSpecimenDictionaryTreeAndDepartmentRelations() throws Exception {
        String url = "jdbc:h2:mem:specimen_dictionary_v99_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("102"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement,
                "select count(*) from system_config_categories where id = 'SCC_SPECIMEN_DICTIONARY' and category_code = 'SPECIMEN_DICTIONARY'"));
            assertEquals(10, queryInt(statement,
                "select count(*) from system_config_categories where category_type = 'SPECIMEN_DICTIONARY'"));
            assertEquals(34, queryInt(statement,
                "select count(*) from system_config_items where value_type = 'SPECIMEN_DICTIONARY_ITEM'"));
            assertTrue(queryInt(statement,
                "select count(*) from system_config_item_departments where config_item_id = 'SCI_SPEC_DICT_028' and department_id = 'DEPT-GYNE'") >= 1);
            assertTrue(queryInt(statement,
                "select count(*) from system_config_item_departments where config_item_id = 'SCI_SPEC_DICT_001' and department_id = 'DEPT-ORTHO'") >= 1);
            assertTrue(queryInt(statement,
                "select count(*) from department_dict where id in ('DEPT-ORTHO','DEPT-EMERGENCY','DEPT-GENERAL','DEPT-GYNE')") >= 4);
        }
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
