package db.migration;

import java.sql.Connection;

import static db.migration.V44WorkflowReferenceSeedData.CLINICAL_SYMPTOM_CATEGORY_ID;
import static db.migration.V44WorkflowReferenceSeedData.CLINICAL_SYMPTOM_ITEMS;
import static db.migration.V44WorkflowReferenceSeedData.COLLECTION_MODE_CATEGORY_ID;
import static db.migration.V44WorkflowReferenceSeedData.COLLECTION_MODE_ITEMS;
import static db.migration.V44WorkflowReferenceSeedData.FIXATION_ITEMS;
import static db.migration.V44WorkflowReferenceSeedData.FIXATION_LIQUID_TYPE_CATEGORY_ID;
import static db.migration.V44WorkflowReferenceSeedData.PERMISSION_ID;
import static db.migration.V44WorkflowReferenceSeedData.ROOT_CATEGORY_CODE;
import static db.migration.V44WorkflowReferenceSeedData.ROOT_CATEGORY_ID;
import static db.migration.V44WorkflowReferenceSeedData.ROLE_IDS;
import static db.migration.V44WorkflowReferenceSeedData.SPECIMEN_TYPE_CATEGORY_ID;
import static db.migration.V44WorkflowReferenceSeedData.SPECIMEN_TYPE_ITEMS;

final class V44WorkflowReferenceSupport {

    private final V44WorkflowReferenceMetadataSupport metadataSupport;

    V44WorkflowReferenceSupport(Connection connection) {
        this.metadataSupport = new V44WorkflowReferenceMetadataSupport(connection);
    }

    void migrate() throws Exception {
        metadataSupport.ensurePermission();
        for (String roleId : ROLE_IDS) {
            metadataSupport.ensureRolePermission(roleId, PERMISSION_ID);
        }

        String rootCategoryId = metadataSupport.ensureCategory(ROOT_CATEGORY_ID, ROOT_CATEGORY_CODE, null, "Workflow reference", "WORKFLOW_REFERENCE", 100, true);
        String specimenTypeCategoryId = metadataSupport.ensureCategory(SPECIMEN_TYPE_CATEGORY_ID, "SPECIMEN_TYPE", rootCategoryId, "Specimen type", "WORKFLOW_REFERENCE", 110, true);
        String collectionModeCategoryId = metadataSupport.ensureCategory(COLLECTION_MODE_CATEGORY_ID, "COLLECTION_MODE", rootCategoryId, "Collection mode", "WORKFLOW_REFERENCE", 120, true);
        String clinicalSymptomCategoryId = metadataSupport.ensureCategory(CLINICAL_SYMPTOM_CATEGORY_ID, "CLINICAL_SYMPTOM", rootCategoryId, "Clinical symptom", "WORKFLOW_REFERENCE", 130, true);
        String fixationCategoryId = metadataSupport.ensureCategory(FIXATION_LIQUID_TYPE_CATEGORY_ID, "FIXATION_LIQUID_TYPE", rootCategoryId, "Fixation liquid type", "WORKFLOW_REFERENCE", 140, true);

        metadataSupport.ensureItems(specimenTypeCategoryId, SPECIMEN_TYPE_ITEMS);
        metadataSupport.ensureItems(collectionModeCategoryId, COLLECTION_MODE_ITEMS);
        metadataSupport.ensureItems(clinicalSymptomCategoryId, CLINICAL_SYMPTOM_ITEMS);
        metadataSupport.ensureItems(fixationCategoryId, FIXATION_ITEMS);
    }
}
