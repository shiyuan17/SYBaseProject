package com.company.bl.interfaces;

import com.company.common.web.filter.TraceIdFilter;
import com.company.common.web.response.ApiResponse;
import com.company.common.web.response.IgnoreApiResponseWrap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wrap-test")
class ApiResponseWrapTestController {

    @GetMapping("/plain")
    Map<String, Object> plain() {
        return Map.of("value", "ok");
    }

    @GetMapping("/entity")
    ResponseEntity<Map<String, Object>> entity() {
        return ResponseEntity.status(201)
            .header("X-Test-Header", "wrapped")
            .body(Map.of("value", "created"));
    }

    @GetMapping("/already")
    ApiResponse<Map<String, Object>> alreadyWrapped(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID);
        return ApiResponse.success(Map.of("value", "already"), traceId == null ? "" : traceId.toString());
    }

    @GetMapping("/null")
    Object nullBody() {
        return null;
    }

    @GetMapping("/no-content")
    ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ignored")
    @IgnoreApiResponseWrap
    Map<String, Object> ignored() {
        return Map.of("value", "ignored");
    }

    @GetMapping(value = "/string", produces = MediaType.TEXT_PLAIN_VALUE)
    String stringBody() {
        return "raw-text";
    }

    @GetMapping(value = "/resource", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    ResponseEntity<Resource> resource() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test.txt")
            .body(new ByteArrayResource("download".getBytes(StandardCharsets.UTF_8)));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<StreamingResponseBody> stream() {
        return ResponseEntity.ok()
            .body(outputStream -> outputStream.write("stream".getBytes(StandardCharsets.UTF_8)));
    }
}
