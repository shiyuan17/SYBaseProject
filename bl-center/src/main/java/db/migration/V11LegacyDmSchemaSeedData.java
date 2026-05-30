package db.migration;

import java.util.List;

final class V11LegacyDmSchemaSeedData {

    static final String DEFAULT_PASSWORD = "0f25f2f2a516ac8fd3e67fb25854f7fe379b0cc1bec9575bc20d6a8cff979134";
    static final String DEFAULT_PASSWORD_ALGO = "SM3";
    static final String DEFAULT_PASSWORD_SALT = "9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1";

    static final List<MenuSeed> WORKFLOW_MENUS = List.of(
        new MenuSeed("MENU_M2_WORKFLOW", null, "M2_WORKFLOW", "M2 Workflow", "DIRECTORY", "/workflow", "WorkflowRoot", "m2", 110),
        new MenuSeed("MENU_M2_CLINICAL", "MENU_M2_WORKFLOW", "M2_CLINICAL", "Clinical Register", "MENU", "/api/v1/specimens/register", "ClinicalRegister", "m2:clinical", 111),
        new MenuSeed("MENU_M2_FIXATION", "MENU_M2_WORKFLOW", "M2_FIXATION", "Fixation Verify", "MENU", "/api/v1/specimen-fixations", "FixationVerify", "m2:fixation", 112),
        new MenuSeed("MENU_M2_TRANSPORT", "MENU_M2_WORKFLOW", "M2_TRANSPORT", "Transport Handover", "MENU", "/api/v1/transport-orders", "TransportHandover", "m2:transport", 113),
        new MenuSeed("MENU_M2_RECEIPT", "MENU_M2_WORKFLOW", "M2_RECEIPT", "Specimen Receipt", "MENU", "/api/v1/specimen-receipts", "SpecimenReceipt", "m2:receipt", 114),
        new MenuSeed("MENU_M2_TRACKING", "MENU_M2_WORKFLOW", "M2_TRACKING", "Tracking Query", "MENU", "/api/v1/applications/{id}/tracking", "TrackingQuery", "m2:tracking", 115),
        new MenuSeed("MENU_M3_WORKFLOW", null, "M3_WORKFLOW", "M3 Workflow", "DIRECTORY", "/technical-workflow", "TechnicalWorkflowRoot", "m3", 120),
        new MenuSeed("MENU_M3_GROSSING", "MENU_M3_WORKFLOW", "M3_GROSSING", "Grossing", "MENU", "/api/v1/grossings", "Grossing", "m3:grossing", 121),
        new MenuSeed("MENU_M3_DEHYDRATION", "MENU_M3_WORKFLOW", "M3_DEHYDRATION", "Dehydration", "MENU", "/api/v1/dehydration-batches", "Dehydration", "m3:dehydration", 122),
        new MenuSeed("MENU_M3_EMBEDDING", "MENU_M3_WORKFLOW", "M3_EMBEDDING", "Embedding", "MENU", "/api/v1/embeddings", "Embedding", "m3:embedding", 123),
        new MenuSeed("MENU_M3_SLICING", "MENU_M3_WORKFLOW", "M3_SLICING", "Slicing", "MENU", "/api/v1/slicings", "Slicing", "m3:slicing", 124),
        new MenuSeed("MENU_M3_STAINING", "MENU_M3_WORKFLOW", "M3_STAINING", "Staining", "MENU", "/api/v1/slide-stainings", "Staining", "m3:staining", 125),
        new MenuSeed("MENU_M3_REWORK", "MENU_M3_WORKFLOW", "M3_REWORK", "Rework", "MENU", "/api/v1/rework-orders", "Rework", "m3:rework", 126),
        new MenuSeed("MENU_M3_TRACKING", "MENU_M3_WORKFLOW", "M3_TRACKING", "Technical Tracking", "MENU", "/api/v1/pathology-cases/{id}/technical-tracking", "TechnicalTracking", "m3:tracking", 127),
        new MenuSeed("MENU_M3_TASKS", "MENU_M3_WORKFLOW", "M3_TASKS", "Technical Tasks", "MENU", "/api/v1/technical-tasks/pending", "TechnicalTasks", "m3:tasks", 128)
    );

    static final List<PermissionSeed> WORKFLOW_PERMISSIONS = List.of(
        new PermissionSeed("PERM_SPECIMEN_REGISTER", "PERM_SPECIMEN_REGISTER", "Specimen register", "MENU_M2_CLINICAL", "REGISTER", "POST", "/api/v1/specimens/register", "M2", 110),
        new PermissionSeed("PERM_FIXATION_VERIFY", "PERM_FIXATION_VERIFY", "Fixation verify", "MENU_M2_FIXATION", "VERIFY", "POST", "/api/v1/specimen-fixations", "M2", 111),
        new PermissionSeed("PERM_TRANSPORT_HANDOVER", "PERM_TRANSPORT_HANDOVER", "Transport handover", "MENU_M2_TRANSPORT", "HANDOVER", "POST", "/api/v1/transport-orders", "M2", 112),
        new PermissionSeed("PERM_SPECIMEN_RECEIVE", "PERM_SPECIMEN_RECEIVE", "Specimen receive", "MENU_M2_RECEIPT", "RECEIVE", "POST", "/api/v1/specimen-receipts", "M2", 113),
        new PermissionSeed("PERM_SPECIMEN_TRACKING_QUERY", "PERM_SPECIMEN_TRACKING_QUERY", "Tracking query", "MENU_M2_TRACKING", "QUERY", "GET", "/api/v1/applications/{id}/tracking", "M2", 114),
        new PermissionSeed("PERM_CLINICAL_IMPORT", "PERM_CLINICAL_IMPORT", "Clinical import", "MENU_M2_CLINICAL", "IMPORT", "POST", "/api/v1/clinical-applications/import", "M2", 115),
        new PermissionSeed("PERM_M3_GROSSING", "PERM_M3_GROSSING", "Grossing operate", "MENU_M3_GROSSING", "OPERATE", "POST", "/api/v1/grossings", "M3", 121),
        new PermissionSeed("PERM_M3_DEHYDRATION", "PERM_M3_DEHYDRATION", "Dehydration operate", "MENU_M3_DEHYDRATION", "OPERATE", "POST", "/api/v1/dehydration-batches", "M3", 122),
        new PermissionSeed("PERM_M3_EMBEDDING", "PERM_M3_EMBEDDING", "Embedding operate", "MENU_M3_EMBEDDING", "OPERATE", "POST", "/api/v1/embeddings", "M3", 123),
        new PermissionSeed("PERM_M3_SLICING", "PERM_M3_SLICING", "Slicing operate", "MENU_M3_SLICING", "OPERATE", "POST", "/api/v1/slicings", "M3", 124),
        new PermissionSeed("PERM_M3_STAINING", "PERM_M3_STAINING", "Staining operate", "MENU_M3_STAINING", "OPERATE", "POST", "/api/v1/slide-stainings", "M3", 125),
        new PermissionSeed("PERM_M3_REWORK", "PERM_M3_REWORK", "Rework operate", "MENU_M3_REWORK", "OPERATE", "POST", "/api/v1/rework-orders", "M3", 126),
        new PermissionSeed("PERM_M3_TECH_TRACKING_QUERY", "PERM_M3_TECH_TRACKING_QUERY", "Technical tracking query", "MENU_M3_TRACKING", "QUERY", "GET", "/api/v1/pathology-cases/{id}/technical-tracking", "M3", 127),
        new PermissionSeed("PERM_M3_TECH_TASK_QUERY", "PERM_M3_TECH_TASK_QUERY", "Technical task query", "MENU_M3_TASKS", "QUERY", "GET", "/api/v1/technical-tasks/pending", "M3", 128)
    );

    static final List<RoleSeed> WORKFLOW_ROLES = List.of(
        new RoleSeed("ROLE_M2_CLINICAL_REGISTER", "M2_CLINICAL_REGISTER", "M2 Clinical Register", "BUSINESS", "DEPARTMENT", "M2 clinical register workstation"),
        new RoleSeed("ROLE_M2_FIXATION_VERIFY", "M2_FIXATION_VERIFY", "M2 Fixation Verify", "BUSINESS", "DEPARTMENT", "M2 fixation workstation"),
        new RoleSeed("ROLE_M2_TRANSPORT_HANDOVER", "M2_TRANSPORT_HANDOVER", "M2 Transport Handover", "BUSINESS", "DEPARTMENT", "M2 transport workstation"),
        new RoleSeed("ROLE_M2_SPECIMEN_RECEIVE", "M2_SPECIMEN_RECEIVE", "M2 Specimen Receive", "BUSINESS", "DEPARTMENT", "M2 specimen receipt workstation"),
        new RoleSeed("ROLE_M2_TRACKING_QUERY", "M2_TRACKING_QUERY", "M2 Tracking Query", "BUSINESS", "DEPARTMENT", "M2 tracking query workstation"),
        new RoleSeed("ROLE_M2_CLINICAL_IMPORT", "M2_CLINICAL_IMPORT", "M2 Clinical Import", "BUSINESS", "DEPARTMENT", "M2 clinical import workstation"),
        new RoleSeed("ROLE_M3_GROSSING", "M3_GROSSING", "M3 Grossing", "BUSINESS", "DEPARTMENT", "M3 grossing workstation"),
        new RoleSeed("ROLE_M3_DEHYDRATION", "M3_DEHYDRATION", "M3 Dehydration", "BUSINESS", "DEPARTMENT", "M3 dehydration workstation"),
        new RoleSeed("ROLE_M3_EMBEDDING", "M3_EMBEDDING", "M3 Embedding", "BUSINESS", "DEPARTMENT", "M3 embedding workstation"),
        new RoleSeed("ROLE_M3_SLICING", "M3_SLICING", "M3 SLICING", "BUSINESS", "DEPARTMENT", "M3 slicing workstation"),
        new RoleSeed("ROLE_M3_STAINING", "M3_STAINING", "M3 Staining", "BUSINESS", "DEPARTMENT", "M3 staining workstation"),
        new RoleSeed("ROLE_M3_REWORK", "M3_REWORK", "M3 Rework", "BUSINESS", "DEPARTMENT", "M3 rework workstation"),
        new RoleSeed("ROLE_M3_TRACKING", "M3_TRACKING", "M3 Tracking", "BUSINESS", "DEPARTMENT", "M3 technical tracking workstation")
    );

    static final List<RolePermissionSeed> WORKFLOW_ROLE_PERMISSIONS = List.of(
        new RolePermissionSeed("RP_M2_ADMIN_REGISTER", "ROLE_PATHOLOGY_ADMIN", "PERM_SPECIMEN_REGISTER"),
        new RolePermissionSeed("RP_M2_ADMIN_FIXATION", "ROLE_PATHOLOGY_ADMIN", "PERM_FIXATION_VERIFY"),
        new RolePermissionSeed("RP_M2_ADMIN_TRANSPORT", "ROLE_PATHOLOGY_ADMIN", "PERM_TRANSPORT_HANDOVER"),
        new RolePermissionSeed("RP_M2_ADMIN_RECEIVE", "ROLE_PATHOLOGY_ADMIN", "PERM_SPECIMEN_RECEIVE"),
        new RolePermissionSeed("RP_M2_ADMIN_TRACKING", "ROLE_PATHOLOGY_ADMIN", "PERM_SPECIMEN_TRACKING_QUERY"),
        new RolePermissionSeed("RP_M2_ADMIN_IMPORT", "ROLE_PATHOLOGY_ADMIN", "PERM_CLINICAL_IMPORT"),
        new RolePermissionSeed("RP_M2_REGISTER_ROLE", "ROLE_M2_CLINICAL_REGISTER", "PERM_SPECIMEN_REGISTER"),
        new RolePermissionSeed("RP_M2_FIXATION_ROLE", "ROLE_M2_FIXATION_VERIFY", "PERM_FIXATION_VERIFY"),
        new RolePermissionSeed("RP_M2_TRANSPORT_ROLE", "ROLE_M2_TRANSPORT_HANDOVER", "PERM_TRANSPORT_HANDOVER"),
        new RolePermissionSeed("RP_M2_RECEIVE_ROLE", "ROLE_M2_SPECIMEN_RECEIVE", "PERM_SPECIMEN_RECEIVE"),
        new RolePermissionSeed("RP_M2_TRACKING_ROLE", "ROLE_M2_TRACKING_QUERY", "PERM_SPECIMEN_TRACKING_QUERY"),
        new RolePermissionSeed("RP_M2_IMPORT_ROLE", "ROLE_M2_CLINICAL_IMPORT", "PERM_CLINICAL_IMPORT"),
        new RolePermissionSeed("RP_M3_ADMIN_GROSSING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_GROSSING"),
        new RolePermissionSeed("RP_M3_ADMIN_DEHYDRATION", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_DEHYDRATION"),
        new RolePermissionSeed("RP_M3_ADMIN_EMBEDDING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_EMBEDDING"),
        new RolePermissionSeed("RP_M3_ADMIN_SLICING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_SLICING"),
        new RolePermissionSeed("RP_M3_ADMIN_STAINING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_STAINING"),
        new RolePermissionSeed("RP_M3_ADMIN_REWORK", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_REWORK"),
        new RolePermissionSeed("RP_M3_ADMIN_TRACKING", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_TECH_TRACKING_QUERY"),
        new RolePermissionSeed("RP_M3_ADMIN_TASKS", "ROLE_PATHOLOGY_ADMIN", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_GROSSING_ROLE", "ROLE_M3_GROSSING", "PERM_M3_GROSSING"),
        new RolePermissionSeed("RP_M3_GROSSING_TASK_ROLE", "ROLE_M3_GROSSING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_DEHYDRATION_ROLE", "ROLE_M3_DEHYDRATION", "PERM_M3_DEHYDRATION"),
        new RolePermissionSeed("RP_M3_DEHYDRATION_TASK_ROLE", "ROLE_M3_DEHYDRATION", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_EMBEDDING_ROLE", "ROLE_M3_EMBEDDING", "PERM_M3_EMBEDDING"),
        new RolePermissionSeed("RP_M3_EMBEDDING_TASK_ROLE", "ROLE_M3_EMBEDDING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_SLICING_ROLE", "ROLE_M3_SLICING", "PERM_M3_SLICING"),
        new RolePermissionSeed("RP_M3_SLICING_TASK_ROLE", "ROLE_M3_SLICING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_STAINING_ROLE", "ROLE_M3_STAINING", "PERM_M3_STAINING"),
        new RolePermissionSeed("RP_M3_STAINING_TASK_ROLE", "ROLE_M3_STAINING", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_REWORK_ROLE", "ROLE_M3_REWORK", "PERM_M3_REWORK"),
        new RolePermissionSeed("RP_M3_REWORK_TASK_ROLE", "ROLE_M3_REWORK", "PERM_M3_TECH_TASK_QUERY"),
        new RolePermissionSeed("RP_M3_TRACKING_ROLE", "ROLE_M3_TRACKING", "PERM_M3_TECH_TRACKING_QUERY")
    );

    static final List<UserSeed> STANDARD_USERS = List.of(
        new UserSeed("USER_M1_ADMIN", "U-M1-ADMIN", "m1.admin", "M1 Admin", "PATHOLOGY_ADMIN", true),
        new UserSeed("USER_M1_DOCTOR", "U-M1-DOCTOR", "m1.doctor", "M1 Doctor", "PATHOLOGY_DOCTOR", true),
        new UserSeed("USER_M1_TECHNICIAN", "U-M1-TECH", "m1.technician", "M1 Technician", "PATHOLOGY_TECHNICIAN", true),
        new UserSeed("USER_M1_ARCHIVE", "U-M1-ARCHIVE", "m1.archive", "M1 Archive", "ARCHIVE_MANAGER", true),
        new UserSeed("USER_M1_REAGENT", "U-M1-REAGENT", "m1.reagent", "M1 Reagent", "REAGENT_DEVICE_MANAGER", true),
        new UserSeed("USER_M1_QUALITY", "U-M1-QUALITY", "m1.quality", "M1 Quality", "QUALITY_MANAGER", true),
        new UserSeed("USER_M1_NO_PERMISSION", "U-M1-NOAUTH", "m1.noauth", "M1 No Permission", null, true),
        new UserSeed("USER_M2_ADMIN", "U-M2-ADMIN", "m2.admin", "M2 Admin", "PATHOLOGY_ADMIN", true),
        new UserSeed("USER_M2_REGISTER", "U-M2-REGISTER", "m2.register", "M2 Register", "M2_CLINICAL_REGISTER", true),
        new UserSeed("USER_M2_FIXATION", "U-M2-FIXATION", "m2.fixation", "M2 Fixation", "M2_FIXATION_VERIFY", true),
        new UserSeed("USER_M2_TRANSPORT", "U-M2-TRANSPORT", "m2.transport", "M2 Transport", "M2_TRANSPORT_HANDOVER", true),
        new UserSeed("USER_M2_RECEIVE", "U-M2-RECEIVE", "m2.receive", "M2 Receive", "M2_SPECIMEN_RECEIVE", true),
        new UserSeed("USER_M2_TRACKING", "U-M2-TRACKING", "m2.tracking", "M2 Tracking", "M2_TRACKING_QUERY", true),
        new UserSeed("USER_M2_IMPORT", "U-M2-IMPORT", "m2.import", "M2 Import", "M2_CLINICAL_IMPORT", true),
        new UserSeed("USER_M2_NO_PERMISSION", "U-M2-NOAUTH", "m2.noauth", "M2 No Permission", null, true),
        new UserSeed("USER_M3_GROSSING", "U-M3-GROSSING", "m3.grossing", "M3 Grossing", "M3_GROSSING", true),
        new UserSeed("USER_M3_DEHYDRATION", "U-M3-DEHYDRATION", "m3.dehydration", "M3 Dehydration", "M3_DEHYDRATION", true),
        new UserSeed("USER_M3_EMBEDDING", "U-M3-EMBEDDING", "m3.embedding", "M3 Embedding", "M3_EMBEDDING", true),
        new UserSeed("USER_M3_SLICING", "U-M3-SLICING", "m3.slicing", "M3 Slicing", "M3_SLICING", true),
        new UserSeed("USER_M3_STAINING", "U-M3-STAINING", "m3.staining", "M3 Staining", "M3_STAINING", true),
        new UserSeed("USER_M3_REWORK", "U-M3-REWORK", "m3.rework", "M3 Rework", "M3_REWORK", true),
        new UserSeed("USER_M3_TRACKING", "U-M3-TRACKING", "m3.tracking", "M3 Tracking", "M3_TRACKING", true)
    );

    static final List<UserRoleSeed> STANDARD_USER_ROLES = List.of(
        new UserRoleSeed("UR_M1_ADMIN", "USER_M1_ADMIN", "ROLE_PATHOLOGY_ADMIN"),
        new UserRoleSeed("UR_M1_DOCTOR", "USER_M1_DOCTOR", "ROLE_PATHOLOGY_DOCTOR"),
        new UserRoleSeed("UR_M1_TECHNICIAN", "USER_M1_TECHNICIAN", "ROLE_PATHOLOGY_TECHNICIAN"),
        new UserRoleSeed("UR_M1_ARCHIVE", "USER_M1_ARCHIVE", "ROLE_ARCHIVE_MANAGER"),
        new UserRoleSeed("UR_M1_REAGENT", "USER_M1_REAGENT", "ROLE_REAGENT_DEVICE_MANAGER"),
        new UserRoleSeed("UR_M1_QUALITY", "USER_M1_QUALITY", "ROLE_QUALITY_MANAGER"),
        new UserRoleSeed("UR_M2_ADMIN", "USER_M2_ADMIN", "ROLE_PATHOLOGY_ADMIN"),
        new UserRoleSeed("UR_M2_REGISTER", "USER_M2_REGISTER", "ROLE_M2_CLINICAL_REGISTER"),
        new UserRoleSeed("UR_M2_FIXATION", "USER_M2_FIXATION", "ROLE_M2_FIXATION_VERIFY"),
        new UserRoleSeed("UR_M2_TRANSPORT", "USER_M2_TRANSPORT", "ROLE_M2_TRANSPORT_HANDOVER"),
        new UserRoleSeed("UR_M2_RECEIVE", "USER_M2_RECEIVE", "ROLE_M2_SPECIMEN_RECEIVE"),
        new UserRoleSeed("UR_M2_TRACKING", "USER_M2_TRACKING", "ROLE_M2_TRACKING_QUERY"),
        new UserRoleSeed("UR_M2_IMPORT", "USER_M2_IMPORT", "ROLE_M2_CLINICAL_IMPORT"),
        new UserRoleSeed("UR_M3_GROSSING", "USER_M3_GROSSING", "ROLE_M3_GROSSING"),
        new UserRoleSeed("UR_M3_DEHYDRATION", "USER_M3_DEHYDRATION", "ROLE_M3_DEHYDRATION"),
        new UserRoleSeed("UR_M3_EMBEDDING", "USER_M3_EMBEDDING", "ROLE_M3_EMBEDDING"),
        new UserRoleSeed("UR_M3_SLICING", "USER_M3_SLICING", "ROLE_M3_SLICING"),
        new UserRoleSeed("UR_M3_STAINING", "USER_M3_STAINING", "ROLE_M3_STAINING"),
        new UserRoleSeed("UR_M3_REWORK", "USER_M3_REWORK", "ROLE_M3_REWORK"),
        new UserRoleSeed("UR_M3_TRACKING", "USER_M3_TRACKING", "ROLE_M3_TRACKING")
    );

    private V11LegacyDmSchemaSeedData() {
    }
}

record MenuSeed(
    String id,
    String parentId,
    String menuCode,
    String menuName,
    String menuType,
    String path,
    String componentName,
    String permissionPrefix,
    int sortOrder
) {
}

record PermissionSeed(
    String id,
    String permissionCode,
    String permissionName,
    String menuId,
    String actionKey,
    String httpMethod,
    String resourcePath,
    String permissionGroup,
    int sortOrder
) {
}

record RoleSeed(
    String id,
    String roleCode,
    String roleName,
    String roleType,
    String dataScope,
    String remarks
) {
}

record RolePermissionSeed(String id, String roleId, String permissionId) {
}

record UserSeed(
    String id,
    String userCode,
    String loginName,
    String name,
    String roleCode,
    boolean enabled
) {
}

record UserRoleSeed(String id, String userId, String roleId) {
}

record MenuIdentity(String id, String menuCode) {
}
