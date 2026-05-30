package com.company.bl.masterdata.application;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.MedicalOrderChargeJdbcRepository;
import com.company.bl.masterdata.infrastructure.MedicalOrderPageJdbcRepository;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
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
class MedicalOrderChargeServiceTest {

    @Mock
    private MedicalOrderChargeJdbcRepository repository;

    @Mock
    private MedicalOrderPageJdbcRepository pageRepository;

    @Mock
    private NumberingService numberingService;

    @Mock
    private OperationAuditService operationAuditService;

    @Test
    void createChargeItemShouldMapDuplicateKeyToConflictException() {
        MedicalOrderChargeService service = new MedicalOrderChargeService(
            repository,
            pageRepository,
            numberingService,
            operationAuditService);
        when(numberingService.generateChargeItemCode()).thenReturn("CHG-1");
        when(repository.insertChargeItem(any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(operationAuditService.audit(anyString(), anyString(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(3)).get());

        assertThatThrownBy(() -> service.createChargeItem(new MedicalOrderService.CreateChargeItemCommand(
            "ITEM-1",
            null,
            "Charge",
            "spec",
            "unit",
            java.math.BigDecimal.ONE,
            1,
            true)))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Charge item code already exists");
    }
}
