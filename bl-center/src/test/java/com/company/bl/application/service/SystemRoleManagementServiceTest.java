package com.company.bl.application.service;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.system.application.SystemManagementService;
import com.company.bl.system.application.SystemRoleManagementService;
import com.company.bl.system.infrastructure.SystemRoleJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemRoleManagementServiceTest {

    @Mock
    private SystemRoleJdbcRepository systemRoleJdbcRepository;

    @Mock
    private NumberingService numberingService;

    @Mock
    private OperationAuditService operationAuditService;

    @Test
    void deleteRoleShouldRejectAssignedRole() {
        SystemRoleManagementService service = new SystemRoleManagementService(
            systemRoleJdbcRepository,
            numberingService,
            operationAuditService);
        when(systemRoleJdbcRepository.findRoleById("ROLE-1")).thenReturn(role("ROLE-1"));
        when(systemRoleJdbcRepository.countRoleAssignments("ROLE-1")).thenReturn(1L);
        when(operationAuditService.audit(anyString(), anyString(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(3)).get());

        assertThatThrownBy(() -> service.deleteRole("ROLE-1"))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("still assigned to users");
    }

    @Test
    void createRoleShouldMapDuplicateKeyToConflictException() {
        SystemRoleManagementService service = new SystemRoleManagementService(
            systemRoleJdbcRepository,
            numberingService,
            operationAuditService);
        when(numberingService.generateRoleCode()).thenReturn("ROLE-CODE");
        when(systemRoleJdbcRepository.insertRole(any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(operationAuditService.audit(anyString(), anyString(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(3)).get());

        assertThatThrownBy(() -> service.createRole(new SystemManagementService.CreateRoleCommand(
            null,
            "Role",
            "ADMIN",
            "ALL",
            "remark",
            true)))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Role code already exists");
    }

    private static SystemRoleJdbcRepository.RoleRow role(String roleId) {
        return new SystemRoleJdbcRepository.RoleRow(
            roleId,
            "ROLE-1",
            "Role",
            "ADMIN",
            "ALL",
            "remark",
            true,
            null,
            null);
    }
}
