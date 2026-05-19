package com.company.bl.support.application;

import com.company.bl.support.infrastructure.SupportJdbcRepository;
import org.springframework.stereotype.Service;

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
        try {
            T result = action.get();
            supportJdbcRepository.insertOperationLog(new SupportJdbcRepository.OperationLogRow(
                "OP-" + UUID.randomUUID(),
                moduleCode,
                businessType,
                businessIdResolver == null ? null : businessIdResolver.apply(result),
                operationName,
                "SUCCESS",
                null,
                "system",
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
                null,
                "system",
                null,
                LocalDateTime.now(),
                contentResolver == null ? null : contentResolver.get(),
                exception.getMessage()));
            throw exception;
        }
    }
}
