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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalOrderServiceTest {

    @Mock
    private MedicalOrderJdbcRepository repository;

    @Mock
    private MedicalOrderPageJdbcRepository pageRepository;

    @Mock
    private NumberingService numberingService;

    @Mock
    private OperationAuditService operationAuditService;

    @Mock
    private MedicalOrderChargeService medicalOrderChargeService;

    @Mock
    private MedicalOrderPackageService medicalOrderPackageService;

    @Test
    void updateMedicalOrderCategoryShouldRejectMissingCategory() {
        MedicalOrderService service = new MedicalOrderService(
            repository,
            pageRepository,
            numberingService,
            operationAuditService,
            medicalOrderChargeService,
            medicalOrderPackageService);
        when(repository.findOrderCategoryById("CAT-1")).thenReturn(null);
        when(operationAuditService.audit(anyString(), anyString(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(3)).get());

        assertThatThrownBy(() -> service.updateMedicalOrderCategory(
            "CAT-1",
            new MedicalOrderService.UpdateMedicalOrderCategoryCommand(null, "CAT-CODE", "Category", 1, true)))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Medical order category not found");
    }

    @Test
    void createChargeItemShouldDelegateToChargeService() {
        MedicalOrderService service = new MedicalOrderService(
            repository,
            pageRepository,
            numberingService,
            operationAuditService,
            medicalOrderChargeService,
            medicalOrderPackageService);
        MedicalOrderService.CreateChargeItemCommand command = new MedicalOrderService.CreateChargeItemCommand(
            "ITEM-1",
            null,
            "Charge",
            "spec",
            "unit",
            java.math.BigDecimal.ONE,
            1,
            true);

        service.createChargeItem(command);

        verify(medicalOrderChargeService).createChargeItem(command);
    }
}
