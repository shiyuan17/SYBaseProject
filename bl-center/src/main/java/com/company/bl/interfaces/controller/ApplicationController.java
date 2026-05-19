package com.company.bl.interfaces.controller;

import com.company.bl.application.query.GetApplicationByIdQuery;
import com.company.bl.application.service.CreateApplicationAppService;
import com.company.bl.application.service.GetApplicationAppService;
import com.company.bl.interfaces.assembler.ApplicationRepresentationAssembler;
import com.company.bl.interfaces.dto.CreateApplicationRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.ApplicationIdResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final CreateApplicationAppService createApplicationAppService;
    private final GetApplicationAppService getApplicationAppService;
    private final ApplicationRepresentationAssembler applicationRepresentationAssembler;

    public ApplicationController(CreateApplicationAppService createApplicationAppService,
                                 GetApplicationAppService getApplicationAppService,
                                 ApplicationRepresentationAssembler applicationRepresentationAssembler) {
        this.createApplicationAppService = createApplicationAppService;
        this.getApplicationAppService = getApplicationAppService;
        this.applicationRepresentationAssembler = applicationRepresentationAssembler;
    }

    @PostMapping
    public ResponseEntity<ApplicationIdResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationIdResponse response = applicationRepresentationAssembler.toIdResponse(
            createApplicationAppService.create(applicationRepresentationAssembler.toCommand(request)));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ApplicationDetailResponse getById(@PathVariable("id") String id) {
        return applicationRepresentationAssembler.toDetailResponse(
            getApplicationAppService.getById(new GetApplicationByIdQuery(id)));
    }
}
