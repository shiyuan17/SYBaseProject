package com.company.bl.support.application;

import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class OperationAuditService {

    private final SupportJdbcRepository supportJdbcRepository;

    public OperationAuditService(SupportJdbcRepository supportJdbcRepository) {
        this.supportJdbcRepository = supportJdbcRepository;
    }

    public <T> T audit(String moduleCode,
                       String businessType,
                       String operationName,
                       Supplier<T> action,
                       Function<T, String> businessIdResolver,
                       Supplier<String> contentResolver) {
        return audit(moduleCode, businessType, operationName, action, businessIdResolver, null, contentResolver);
    }

    public <T> T audit(String moduleCode,
                       String businessType,
                       String operationName,
                       Supplier<T> action,
                       Function<T, String> businessIdResolver,
                       Supplier<String> failureBusinessIdSupplier,
                       Supplier<String> contentResolver) {
        ResolvedOperator operator = resolveCurrentOperator();
        return audit(moduleCode, businessType, operationName, action, businessIdResolver, failureBusinessIdSupplier,
            operator.userId(), operator.name(), contentResolver);
    }

    public <T> T audit(String moduleCode,
                       String businessType,
                       String operationName,
                       Supplier<T> action,
                       Function<T, String> businessIdResolver,
                       Supplier<String> failureBusinessIdSupplier,
                       String operatorUserId,
                       String operatorName,
                       Supplier<String> contentResolver) {
        try {
            T result = action.get();
            supportJdbcRepository.insertOperationLog(new SupportJdbcRepository.OperationLogRow(
                "OP-" + UUID.randomUUID(),
                moduleCode,
                businessType,
                businessIdResolver == null ? null : businessIdResolver.apply(result),
                operationName,
                "SUCCESS",
                operatorUserId,
                operatorName,
                null,
                LocalDateTime.now(),
                contentResolver == null ? null : contentResolver.get(),
                null));
            return result;
        } catch (RuntimeException exception) {
            supportJdbcRepository.insertOperationLog(new SupportJdbcRepository.OperationLogRow(
                "OP-" + UUID.randomUUID(),
                moduleCode,
                businessType,
                failureBusinessIdSupplier == null ? null : failureBusinessIdSupplier.get(),
                operationName,
                "FAILED",
                operatorUserId,
                operatorName,
                null,
                LocalDateTime.now(),
                contentResolver == null ? null : contentResolver.get(),
                exception.getMessage()));
            throw exception;
        }
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
            operatorName == null ? (loginName == null ? "system" : loginName) : operatorName);
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
