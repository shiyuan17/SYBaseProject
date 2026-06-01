package com.company.bl.system.application;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserManagementServiceTest {

    @Mock
    private SystemUserJdbcRepository systemUserJdbcRepository;

    @Mock
    private NumberingService numberingService;

    @Mock
    private OperationAuditService operationAuditService;

    @Mock
    private Sm3PasswordEncoder sm3PasswordEncoder;

    @Test
    void createUserShouldMapDuplicateKeyToConflictException() {
        SystemUserManagementMutationSupport mutationSupport = new SystemUserManagementMutationSupport(
            systemUserJdbcRepository,
            numberingService,
            operationAuditService,
            sm3PasswordEncoder);
        SystemUserManagementService service = new SystemUserManagementService(
            mock(SystemUserManagementQuerySupport.class),
            mutationSupport,
            mock(SystemUserManagementImportSupport.class));
        when(numberingService.generateUserCode()).thenReturn("USER-CODE");
        when(numberingService.generateLoginTagCode()).thenReturn("TAG-CODE");
        when(systemUserJdbcRepository.insertUser(any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(operationAuditService.audit(anyString(), anyString(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(3)).get());

        assertThatThrownBy(() -> service.createUser(new SystemManagementService.CreateUserCommand(
            null,
            "login",
            "name",
            "password",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true)))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void assignUserRolesShouldRejectMoreThanOnePrimaryRole() {
        SystemUserManagementMutationSupport mutationSupport = new SystemUserManagementMutationSupport(
            systemUserJdbcRepository,
            numberingService,
            operationAuditService,
            sm3PasswordEncoder);
        SystemUserManagementService service = new SystemUserManagementService(
            mock(SystemUserManagementQuerySupport.class),
            mutationSupport,
            mock(SystemUserManagementImportSupport.class));
        when(systemUserJdbcRepository.findUserById("USER-1")).thenReturn(user("USER-1"));
        when(operationAuditService.audit(anyString(), anyString(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(3)).get());

        assertThatThrownBy(() -> service.assignUserRoles(
            "USER-1",
            new SystemManagementService.AssignUserRolesCommand(List.of(
                new SystemManagementService.RoleAssignmentInput("ROLE-1", true),
                new SystemManagementService.RoleAssignmentInput("ROLE-2", true)))))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Only one primary role");
    }

    private static SystemJdbcRepository.UserRow user(String userId) {
        return new SystemJdbcRepository.UserRow(
            userId,
            "USER-CODE",
            "login",
            "name",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null);
    }
}
