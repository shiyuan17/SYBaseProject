package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V79__create_patients_registry extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (tableExists(connection, "PATIENTS")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                create table patients (
                    id varchar(64) not null,
                    patient_no varchar(64),
                    name varchar(100),
                    gender varchar(16),
                    age varchar(32),
                    inpatient_no varchar(64),
                    outpatient_no varchar(64),
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    constraint pk_patients primary key (id)
                )
                """);
            statement.execute("create index idx_patients_patient_no on patients (patient_no)");
            statement.execute("create index idx_patients_inpatient_no on patients (inpatient_no)");
            statement.execute("create index idx_patients_outpatient_no on patients (outpatient_no)");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, tableName, null)) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = metadata.getTables(null, null, tableName.toLowerCase(), null)) {
            return tables.next();
        }
    }
}
