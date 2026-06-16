package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ArchiveModels;
import com.company.bl.application.service.ArchiveQueryService;
import com.company.bl.application.service.ArchiveWorkflowService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.BatchCreateArchiveCabinetRequest;
import com.company.bl.interfaces.dto.CreateArchiveCabinetNodeRequest;
import com.company.bl.interfaces.dto.CreateArchiveCabinetRequest;
import com.company.bl.interfaces.dto.UpdateArchiveCabinetNodeRequest;
import com.company.bl.interfaces.dto.UpdateArchiveCabinetRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ArchiveCabinetController extends TechnicalControllerSupport {

    private final ArchiveQueryService archiveQueryService;
    private final ArchiveWorkflowService archiveWorkflowService;

    public ArchiveCabinetController(ArchiveQueryService archiveQueryService,
                                    ArchiveWorkflowService archiveWorkflowService) {
        this.archiveQueryService = archiveQueryService;
        this.archiveWorkflowService = archiveWorkflowService;
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_QUERY)
    @GetMapping("/archive-cabinets")
    public List<ArchiveModels.ArchiveCabinetView> listArchiveCabinets() {
        return archiveQueryService.listArchiveCabinets();
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_QUERY)
    @GetMapping("/archive-cabinet-nodes")
    public List<ArchiveModels.ArchiveCabinetNodeView> listArchiveCabinetNodes() {
        return archiveQueryService.listArchiveCabinetNodes();
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_CREATE)
    @PostMapping("/archive-cabinets")
    public ArchiveModels.ArchiveCabinetView createArchiveCabinet(@Valid @RequestBody CreateArchiveCabinetRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.createArchiveCabinet(new ArchiveModels.CreateArchiveCabinetCommand(
            request.getCabinetCode(),
            request.getCabinetName(),
            request.getCabinetType(),
            request.getLayerCount(),
            request.getSlotCountPerLayer(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getLocationDescription(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_CREATE)
    @PostMapping("/archive-cabinet-nodes")
    public ArchiveModels.ArchiveCabinetNodeView createArchiveCabinetNode(@Valid @RequestBody CreateArchiveCabinetNodeRequest request,
                                                                         HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.createArchiveCabinetNode(new ArchiveModels.CreateArchiveCabinetNodeCommand(
            request.getParentId(),
            request.getNodeCode(),
            request.getNodeType(),
            request.getCabinetType(),
            request.getCapacity(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getPathLocation(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_CREATE)
    @PostMapping("/archive-cabinets/batch")
    public List<ArchiveModels.ArchiveCabinetView> batchCreateArchiveCabinets(@Valid @RequestBody BatchCreateArchiveCabinetRequest request,
                                                                             HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.batchCreateArchiveCabinets(new ArchiveModels.BatchCreateArchiveCabinetCommand(
            request.getParentId(),
            request.getCabinetType(),
            request.getCabinetCodePrefix(),
            request.getStartNo(),
            request.getCount(),
            request.getNumberWidth(),
            request.getCabinetNamePrefix(),
            request.getLayerCount(),
            request.getSlotCountPerLayer(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getLocationDescription(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_UPDATE)
    @PatchMapping("/archive-cabinets/{id}")
    public ArchiveModels.ArchiveCabinetView updateArchiveCabinet(@PathVariable("id") String cabinetId,
                                                                 @Valid @RequestBody UpdateArchiveCabinetRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.updateArchiveCabinet(new ArchiveModels.UpdateArchiveCabinetCommand(
            cabinetId,
            request.getCabinetName(),
            request.getCabinetStatus(),
            request.getLocationDescription(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_UPDATE)
    @PatchMapping("/archive-cabinet-nodes/{id}")
    public ArchiveModels.ArchiveCabinetNodeView updateArchiveCabinetNode(@PathVariable("id") String nodeId,
                                                                         @Valid @RequestBody UpdateArchiveCabinetNodeRequest request,
                                                                         HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.updateArchiveCabinetNode(new ArchiveModels.UpdateArchiveCabinetNodeCommand(
            nodeId,
            request.getNodeCode(),
            request.getCabinetType(),
            request.getCapacity(),
            request.getPathLocation(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_DELETE)
    @DeleteMapping("/archive-cabinets/{id}")
    public ResponseEntity<Void> deleteArchiveCabinet(@PathVariable("id") String cabinetId) {
        archiveWorkflowService.deleteArchiveCabinet(cabinetId);
        return ResponseEntity.noContent().build();
    }

    @RequirePermission(M5PermissionCodes.ARCHIVE_CABINET_QUERY)
    @GetMapping("/archive-positions/available")
    public List<ArchiveModels.ArchivePositionView> listAvailablePositions(@RequestParam(required = false) String cabinetType,
                                                                          @RequestParam(required = false) String cabinetId) {
        return archiveQueryService.listAvailablePositions(cabinetType, cabinetId);
    }
}
