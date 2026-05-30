package com.company.bl.masterdata.application;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.MedicalOrderJdbcRepository;
import com.company.bl.masterdata.infrastructure.MedicalOrderPageJdbcRepository;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalOrderPackageServiceTest {

    @Mock
    private MedicalOrderJdbcRepository repository;

    @Mock
    private MedicalOrderPageJdbcRepository pageRepository;

    @Mock
    private NumberingService numberingService;

    @Mock
    private OperationAuditService operationAuditService;

    @Test
    void updatePackageShouldRejectMissingPackage() {
        MedicalOrderPackageService service = new MedicalOrderPackageService(
            repository,
            pageRepository,
            numberingService,
            operationAuditService);
        when(repository.findPackageById("PKG-1")).thenReturn(null);
        when(operationAuditService.audit(anyString(), anyString(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(3)).get());

        assertThatThrownBy(() -> service.updatePackage(
            "PKG-1",
            new MedicalOrderService.UpdatePackageCommand(null, "Package", "PUBLIC", "USER-1", true, null, java.util.List.of())))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Package not found");
    }
}
