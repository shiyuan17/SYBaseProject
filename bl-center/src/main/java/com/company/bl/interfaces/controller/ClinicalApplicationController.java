package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ClinicalApplicationImportAppService;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.ImportClinicalApplicationRequest;
import com.company.bl.interfaces.vo.ApplicationIdResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinical-applications")
@RequiredArgsConstructor
public class ClinicalApplicationController {

    private final ClinicalApplicationImportAppService clinicalApplicationImportAppService;

    @RequirePermission(M2PermissionCodes.CLINICAL_IMPORT)
    @PostMapping("/import")
    public ResponseEntity<ApplicationIdResponse> importApplication(@Valid @RequestBody ImportClinicalApplicationRequest request) {
        return ResponseEntity.status(201).body(new ApplicationIdResponse(
            clinicalApplicationImportAppService.importApplication(
                new ClinicalApplicationImportAppService.ImportClinicalApplicationCommand(
                    request.getThirdPartySource(),
                    request.getExternalOrderNo()))
                .value()));
    }
}
