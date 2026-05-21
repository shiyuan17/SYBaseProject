package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V38__normalize_seed_department_names extends BaseJavaMigration {

    private static final List<DepartmentSeed> DEPARTMENT_SEEDS = List.of(
        new DepartmentSeed(
            "DEPT_ROOT",
            "ROOT",
            "\u5168\u90e8\u79d1\u5ba4",
            List.of("All Departments")),
        new DepartmentSeed(
            "DEPT_CLINICAL",
            "CLINICAL",
            "\u4e34\u5e8a\u79d1\u5ba4",
            List.of("Clinical Departments")),
        new DepartmentSeed(
            "DEPT_OR",
            "OR",
            "\u624b\u672f\u5ba4",
            List.of("Operating Room")),
        new DepartmentSeed(
            "DEPT_ICU",
            "ICU",
            "\u91cd\u75c7\u76d1\u62a4\u5ba4",
            List.of("Intensive Care Unit")),
        new DepartmentSeed(
            "DEPT_PATH",
            "PATHOLOGY",
            "\u75c5\u7406\u79d1",
            List.of("Pathology Department"))
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (DepartmentSeed seed : DEPARTMENT_SEEDS) {
            normalizeDepartmentName(connection, seed);
        }
    }

    private void normalizeDepartmentName(Connection connection, DepartmentSeed seed) throws Exception {
        StringBuilder sql = new StringBuilder("""
            update department_dict
            set department_name = ?,
                updated_at = ?
            where (id = ? or department_code = ?)
              and (
                    department_name is null
                 or trim(department_name) = ''
            """);
        for (int index = 0; index < seed.legacyNames().size(); index++) {
            sql.append(" or trim(department_name) = ?");
        }
        sql.append("\n              )");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setString(parameterIndex++, seed.normalizedName());
            statement.setTimestamp(parameterIndex++, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(parameterIndex++, seed.id());
            statement.setString(parameterIndex++, seed.code());
            for (String legacyName : seed.legacyNames()) {
                statement.setString(parameterIndex++, legacyName);
            }
            statement.executeUpdate();
        }
    }

    private record DepartmentSeed(
        String id,
        String code,
        String normalizedName,
        List<String> legacyNames
    ) {
    }
}
