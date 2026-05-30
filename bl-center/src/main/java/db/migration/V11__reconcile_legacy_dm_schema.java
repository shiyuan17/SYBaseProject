package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__reconcile_legacy_dm_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new V11LegacyDmSchemaSupport(context.getConnection()).migrate();
    }
}
