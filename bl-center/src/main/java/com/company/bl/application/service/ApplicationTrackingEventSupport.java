package com.company.bl.application.service;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
class ApplicationTrackingEventSupport {

    private final TechnicalWorkflowRepository technicalWorkflowRepository;

    ApplicationTrackingEventSupport(TechnicalWorkflowRepository technicalWorkflowRepository) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
    }

    void writeCreateEvent(Application application) {
        writeEvent(application, "APPLICATION_CREATE", "CREATE", "创建申请单 ");
    }

    void writeUpdateEvent(Application application) {
        writeEvent(application, "APPLICATION_UPDATE", "UPDATE", "更新申请单 ");
    }

    void writeVoidEvent(Application application) {
        writeEvent(application, "APPLICATION_VOID", "VOID", "作废申请单 ");
    }

    private void writeEvent(
        Application application,
        String nodeCode,
        String eventType,
        String eventContentPrefix
    ) {
        ResolvedOperator operator = resolveCurrentOperator();
        technicalWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            application.getId().value(),
            null,
            null,
            null,
            nodeCode,
            eventType,
            "SUCCESS",
            LocalDateTime.now(),
            operator.userId(),
            operator.name(),
            null,
            eventContentPrefix + application.getApplicationNo(),
            null
        ));
    }

    private ResolvedOperator resolveCurrentOperator() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes)) {
            return ResolvedOperator.system();
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String operatorUserId = trimToNull(attributeValue(request, ApiPermissionContext.CURRENT_USER_ID));
        String operatorName = trimToNull(attributeValue(request, ApiPermissionContext.CURRENT_OPERATOR_NAME));
        String loginName = trimToNull(attributeValue(request, ApiPermissionContext.CURRENT_LOGIN_NAME));
        if (operatorUserId == null && operatorName == null && loginName == null) {
            return ResolvedOperator.system();
        }
        return new ResolvedOperator(
            operatorUserId,
            operatorName == null ? (loginName == null ? "system" : loginName) : operatorName
        );
    }

    private String attributeValue(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        return value == null ? null : value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ResolvedOperator(String userId, String name) {
        private static ResolvedOperator system() {
            return new ResolvedOperator(null, "system");
        }
    }
}
