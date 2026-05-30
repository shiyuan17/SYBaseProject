package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V44__seed_workflow_reference_options extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new V44WorkflowReferenceSupport(context.getConnection()).migrate();
    }
}
