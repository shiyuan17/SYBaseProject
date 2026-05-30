package com.company.bl.masterdata.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicalOrderServiceTest {

    @Mock
    private MedicalOrderDictionaryService medicalOrderDictionaryService;

    @Mock
    private MedicalOrderChargeService medicalOrderChargeService;

    @Mock
    private MedicalOrderPackageService medicalOrderPackageService;

    @Test
    void updateMedicalOrderCategoryShouldDelegateToDictionaryService() {
        MedicalOrderService service = new MedicalOrderService(
            medicalOrderDictionaryService,
            medicalOrderChargeService,
            medicalOrderPackageService);
        MedicalOrderService.UpdateMedicalOrderCategoryCommand command =
            new MedicalOrderService.UpdateMedicalOrderCategoryCommand(null, "CAT-CODE", "Category", 1, true);

        service.updateMedicalOrderCategory("CAT-1", command);

        verify(medicalOrderDictionaryService).updateMedicalOrderCategory("CAT-1", command);
    }

    @Test
    void createChargeItemShouldDelegateToChargeService() {
        MedicalOrderService service = new MedicalOrderService(
            medicalOrderDictionaryService,
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
