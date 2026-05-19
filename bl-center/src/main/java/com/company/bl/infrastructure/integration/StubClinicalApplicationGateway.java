package com.company.bl.infrastructure.integration;

import com.company.bl.application.gateway.ClinicalApplicationGateway;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import org.springframework.stereotype.Component;

@Component
public class StubClinicalApplicationGateway implements ClinicalApplicationGateway {

    @Override
    public ImportedClinicalApplication fetch(String thirdPartySource, String externalOrderNo) {
        if (externalOrderNo != null && externalOrderNo.startsWith("MOCK-")) {
            return new ImportedClinicalApplication(
                externalOrderNo,
                thirdPartySource,
                "PAT-" + externalOrderNo.substring(5),
                "Mock Patient",
                "F",
                "42",
                "HOSP-001",
                "Mock Hospital",
                "DEPT-OR",
                "手术室",
                "DOC-001",
                "Mock Doctor",
                "Mock diagnosis",
                "Mock symptom",
                "甲状腺",
                "ROUTINE");
        }
        throw new BlBusinessException(
            BlErrorCode.EXTERNAL_INTEGRATION_UNAVAILABLE,
            503,
            "Clinical application integration placeholder only supports external order numbers prefixed with MOCK-");
    }
}
