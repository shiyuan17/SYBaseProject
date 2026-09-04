package com.company.bl.application.service;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

enum OfdRenderWorkerFailure {
    MISSING_RENDER_ASSET("报告引用的图片文件已丢失，无法生成 OFD"),
    STORAGE_ACCESS_DENIED("报告文件存储不可访问，无法生成 OFD"),
    OUT_OF_MEMORY("报告版式生成异常，请重新生成"),
    TIMEOUT("报告 OFD 生成超时，请稍后重试"),
    RENDER_FAILED("报告 OFD 文件生成失败，请重试");

    private static final String DIAGNOSTIC_PREFIX = "OFD_WORKER_FAILURE=";

    private final String userMessage;

    OfdRenderWorkerFailure(String userMessage) {
        this.userMessage = userMessage;
    }

    String userMessage() {
        return userMessage;
    }

    String diagnosticLine() {
        return DIAGNOSTIC_PREFIX + name();
    }

    static OfdRenderWorkerFailure classify(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof OutOfMemoryError) {
                return OUT_OF_MEMORY;
            }
            if (current instanceof NoSuchFileException) {
                return MISSING_RENDER_ASSET;
            }
            if (current instanceof AccessDeniedException || current instanceof SecurityException) {
                return STORAGE_ACCESS_DENIED;
            }
        }
        return RENDER_FAILED;
    }

    static OfdRenderWorkerFailure fromDiagnostic(String diagnostic) {
        if (diagnostic == null || diagnostic.isBlank()) {
            return RENDER_FAILED;
        }
        if (diagnostic.contains("OutOfMemoryError") || diagnostic.contains("out of memory")) {
            return OUT_OF_MEMORY;
        }
        for (OfdRenderWorkerFailure failure : values()) {
            if (diagnostic.contains(failure.diagnosticLine())) {
                return failure;
            }
        }
        return RENDER_FAILED;
    }
}

final class OfdRenderWorkerException extends IOException {

    private final OfdRenderWorkerFailure failure;

    OfdRenderWorkerException(OfdRenderWorkerFailure failure) {
        super(failure.userMessage());
        this.failure = failure;
    }

    OfdRenderWorkerException(OfdRenderWorkerFailure failure, Throwable cause) {
        super(failure.userMessage(), cause);
        this.failure = failure;
    }

    OfdRenderWorkerFailure failure() {
        return failure;
    }
}
