package com.company.bl.interfaces.controller;

import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.TechnicalTaskAssignRequest;
import com.company.bl.interfaces.dto.TechnicalTaskClaimRequest;
import com.company.bl.interfaces.dto.TechnicalTaskPriorityRequest;
import com.company.bl.interfaces.dto.TechnicalTaskRemarksRequest;
import com.company.bl.interfaces.dto.TechnicalTaskReleaseRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalTaskControllerPermissionAnnotationTest {

    @Test
    void shouldUseActionLevelPermissionsForTechnicalTaskMutations() throws Exception {
        assertPermission("listPending", M3PermissionCodes.TECHNICAL_TASK_QUERY,
            int.class,
            int.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            LocalDateTime.class,
            LocalDateTime.class,
            boolean.class,
            boolean.class);
        assertPermission("assign", M3PermissionCodes.TECHNICAL_TASK_ASSIGN,
            String.class,
            TechnicalTaskAssignRequest.class,
            HttpServletRequest.class);
        assertPermission("claim", M3PermissionCodes.TECHNICAL_TASK_CLAIM,
            String.class,
            TechnicalTaskClaimRequest.class,
            HttpServletRequest.class);
        assertPermission("release", M3PermissionCodes.TECHNICAL_TASK_RELEASE,
            String.class,
            TechnicalTaskReleaseRequest.class,
            HttpServletRequest.class);
        assertPermission("priority", M3PermissionCodes.TECHNICAL_TASK_PRIORITY,
            String.class,
            TechnicalTaskPriorityRequest.class,
            HttpServletRequest.class);
        assertPermission("remarks", M3PermissionCodes.TECHNICAL_TASK_REMARKS,
            String.class,
            TechnicalTaskRemarksRequest.class,
            HttpServletRequest.class);
    }

    private static void assertPermission(String methodName,
                                         String expectedPermission,
                                         Class<?>... parameterTypes) throws Exception {
        Method method = TechnicalTaskController.class.getDeclaredMethod(methodName, parameterTypes);
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        assertThat(annotation)
            .as("%s should declare @RequirePermission", methodName)
            .isNotNull();
        assertThat(annotation.value())
            .as("%s should use the expected permission code", methodName)
            .isEqualTo(expectedPermission);
    }
}
