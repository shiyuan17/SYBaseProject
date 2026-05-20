package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ClinicalApplicationImportAppService;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.ImportClinicalApplicationRequest;
import com.company.bl.interfaces.vo.ApplicationIdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "临床送检", description = "第三方临床申请导入接口")
public class ClinicalApplicationController {

    private final ClinicalApplicationImportAppService clinicalApplicationImportAppService;

    @Operation(summary = "导入临床申请单", description = "按第三方来源和外部单号导入临床申请。")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "导入成功", useReturnTypeSchema = true))
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
