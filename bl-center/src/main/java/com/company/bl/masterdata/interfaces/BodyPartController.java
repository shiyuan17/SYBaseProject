package com.company.bl.masterdata.interfaces;

import com.company.bl.interfaces.auth.M1PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.masterdata.application.BodyPartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/body-parts")
public class BodyPartController {

    private final BodyPartService bodyPartService;

    public BodyPartController(BodyPartService bodyPartService) {
        this.bodyPartService = bodyPartService;
    }

    @RequirePermission(M1PermissionCodes.BODY_PART_QUERY)
    @GetMapping
    public List<BodyPartService.BodyPartNode> listBodyParts() {
        return bodyPartService.listBodyParts();
    }

    @RequirePermission(M1PermissionCodes.BODY_PART_CREATE)
    @PostMapping
    public BodyPartService.BodyPartNode createBodyPart(@Valid @RequestBody CreateBodyPartRequest request) {
        return bodyPartService.createBodyPart(new BodyPartService.CreateBodyPartCommand(
            request.parentId(), request.partCode(), request.partName(), request.partAlias(),
            request.partLevel(), request.sortOrder(), request.enabled()));
    }

    @RequirePermission(M1PermissionCodes.BODY_PART_CREATE)
    @PatchMapping("/{id}/enabled")
    public BodyPartService.BodyPartNode updateBodyPartEnabled(@PathVariable("id") String id,
                                                              @Valid @RequestBody UpdateEnabledRequest request) {
        return bodyPartService.updateBodyPartEnabled(id, request.enabled());
    }

    public record CreateBodyPartRequest(
        String parentId,
        @NotBlank(message = "Part code must not be blank")
        @Size(max = 64, message = "Part code must not exceed 64 characters")
        String partCode,
        @NotBlank(message = "Part name must not be blank")
        @Size(max = 100, message = "Part name must not exceed 100 characters")
        String partName,
        @Size(max = 100, message = "Part alias must not exceed 100 characters")
        String partAlias,
        @Min(value = 0, message = "Part level must not be negative")
        int partLevel,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record UpdateEnabledRequest(boolean enabled) {
    }
}
