package com.company.bl.interfaces.controller;

import com.company.bl.application.service.GrossingMediaStorageService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.vo.GrossingMediaAssetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/grossing-media-assets")
@Tag(name = "技术流程", description = "取材影像上传与访问接口")
public class GrossingMediaAssetController {

    private final GrossingMediaStorageService grossingMediaStorageService;

    public GrossingMediaAssetController(GrossingMediaStorageService grossingMediaStorageService) {
        this.grossingMediaStorageService = grossingMediaStorageService;
    }

    @Operation(summary = "上传取材影像", description = "上传标本摄影像并返回可归档的附件 URL。")
    @RequirePermission(M3PermissionCodes.GROSSING)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GrossingMediaAssetResponse upload(@RequestParam("file") MultipartFile file) {
        return grossingMediaStorageService.store(file);
    }

    @Operation(summary = "读取取材影像", description = "读取已上传的取材影像文件。")
    @GetMapping("/files/{dateDirectory}/{storedFileName}")
    public ResponseEntity<Resource> read(@PathVariable String dateDirectory,
                                         @PathVariable String storedFileName) {
        GrossingMediaStorageService.StoredGrossingMedia media =
            grossingMediaStorageService.load(dateDirectory, storedFileName);
        return ResponseEntity.ok()
            .contentType(media.contentType())
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(media.resource());
    }
}
