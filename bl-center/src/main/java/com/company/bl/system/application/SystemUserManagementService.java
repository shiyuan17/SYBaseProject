package com.company.bl.system.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemUserManagementService {

    private final SystemUserManagementQuerySupport querySupport;
    private final SystemUserManagementMutationSupport mutationSupport;
    private final SystemUserManagementImportSupport importSupport;

    public SystemUserManagementService(SystemUserManagementQuerySupport querySupport,
                                       SystemUserManagementMutationSupport mutationSupport,
                                       SystemUserManagementImportSupport importSupport) {
        this.querySupport = querySupport;
        this.mutationSupport = mutationSupport;
        this.importSupport = importSupport;
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PagedResult<SystemManagementService.UserView> listUsers(int page,
                                                                                           int size,
                                                                                           Boolean enabled,
                                                                                           String keyword) {
        return querySupport.listUsers(page, size, enabled, keyword);
    }

    @Transactional
    public SystemManagementService.UserView createUser(SystemManagementService.CreateUserCommand command) {
        return mutationSupport.createUser(command);
    }

    @Transactional
    public SystemManagementService.UserView updateUser(String userId, SystemManagementService.UpdateUserCommand command) {
        return mutationSupport.updateUser(userId, command);
    }

    @Transactional
    public SystemManagementService.UserView updateUserEnabled(String userId, boolean enabled) {
        return mutationSupport.updateUserEnabled(userId, enabled);
    }

    @Transactional
    public SystemManagementService.UserView assignUserRoles(String userId, SystemManagementService.AssignUserRolesCommand command) {
        return mutationSupport.assignUserRoles(userId, command);
    }

    @Transactional
    public void recordUserLogin(SystemManagementService.RecordUserLoginCommand command) {
        mutationSupport.recordUserLogin(command);
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PagedResult<SystemManagementService.UserLoginLogView> listUserLoginLogs(String userId,
                                                                                                            int page,
                                                                                                            int size) {
        return querySupport.listUserLoginLogs(userId, page, size);
    }

    @Transactional(readOnly = true)
    public byte[] exportUsers(Boolean enabled, String keyword) {
        return querySupport.exportUsers(enabled, keyword);
    }

    @Transactional
    public SystemManagementService.ImportResult importUsers(byte[] content) {
        return importSupport.importUsers(content);
    }

    @Transactional(readOnly = true)
    public SystemManagementService.PrintLoginTagView printLoginTag(String userId) {
        return querySupport.printLoginTag(userId);
    }
}
