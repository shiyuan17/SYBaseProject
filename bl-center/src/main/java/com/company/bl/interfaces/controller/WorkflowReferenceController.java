package com.company.bl.interfaces.controller;

import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.auth.WorkflowReferencePermissionCodes;
import com.company.bl.masterdata.application.WorkflowReferenceOptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作流参考数据", description = "工作流页面可直接消费的参考字典查询接口")
@RestController
@RequestMapping("/api/v1/workflow-reference-options")
public class WorkflowReferenceController {

    private final WorkflowReferenceOptionService workflowReferenceOptionService;

    public WorkflowReferenceController(
        WorkflowReferenceOptionService workflowReferenceOptionService
    ) {
        this.workflowReferenceOptionService = workflowReferenceOptionService;
    }

    @GetMapping
    @Operation(summary = "查询工作流参考选项", description = "返回标本类型、采集方式、临床症状和固定液类型的建议项。")
    @RequirePermission(WorkflowReferencePermissionCodes.QUERY)
    public WorkflowReferenceOptionService.WorkflowReferenceOptionsResponse
    listWorkflowReferenceOptions() {
        return workflowReferenceOptionService.listWorkflowReferenceOptions();
    }
}
