package com.company.bl.application.service;

import com.company.bl.system.application.SystemManagementService;
import com.company.bl.system.application.SystemLogQueryService;
import com.company.bl.system.application.SystemRoleManagementService;
import com.company.bl.system.application.SystemUserManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SystemManagementServiceTest {

    @Mock
    private SystemUserManagementService systemUserManagementService;

    @Mock
    private SystemRoleManagementService systemRoleManagementService;

    @Mock
    private SystemLogQueryService systemLogQueryService;

    @Test
    void createRoleShouldDelegateToRoleService() {
        SystemManagementService service = new SystemManagementService(
            systemUserManagementService,
            systemRoleManagementService,
            systemLogQueryService);

        SystemManagementService.CreateRoleCommand command = new SystemManagementService.CreateRoleCommand(
            null,
            "Role",
            "ADMIN",
            "ALL",
            "remark",
            true);

        service.createRole(command);

        verify(systemRoleManagementService).createRole(command);
    }

    @Test
    void printLoginTagShouldDelegateToUserService() {
        SystemManagementService service = new SystemManagementService(
            systemUserManagementService,
            systemRoleManagementService,
            systemLogQueryService);

        service.printLoginTag("USER-1");

        verify(systemUserManagementService).printLoginTag("USER-1");
    }
}
