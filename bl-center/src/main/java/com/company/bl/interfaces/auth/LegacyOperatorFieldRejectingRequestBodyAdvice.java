package com.company.bl.interfaces.auth;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@ControllerAdvice(annotations = Controller.class)
public class LegacyOperatorFieldRejectingRequestBodyAdvice implements RequestBodyAdvice {

    private final ObjectMapper objectMapper;

    public LegacyOperatorFieldRejectingRequestBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(org.springframework.core.MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.getParameterType().isAnnotationPresent(RejectLegacyOperatorFields.class)
            || methodParameter.hasMethodAnnotation(RejectLegacyOperatorFields.class)
            || methodParameter.hasParameterAnnotation(RejectLegacyOperatorFields.class);
    }

    @Override
    public @NonNull HttpInputMessage beforeBodyRead(@NonNull HttpInputMessage inputMessage,
                                                    @NonNull org.springframework.core.MethodParameter parameter,
                                                    @NonNull Type targetType,
                                                    @NonNull Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        byte[] body = inputMessage.getBody().readAllBytes();
        rejectLegacyOperatorFields(body);
        return new BufferedHttpInputMessage(inputMessage.getHeaders(), body);
    }

    @Override
    public Object afterBodyRead(@NonNull Object body,
                                @NonNull HttpInputMessage inputMessage,
                                @NonNull org.springframework.core.MethodParameter parameter,
                                @NonNull Type targetType,
                                @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body,
                                  @NonNull HttpInputMessage inputMessage,
                                  @NonNull org.springframework.core.MethodParameter parameter,
                                  @NonNull Type targetType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    private void rejectLegacyOperatorFields(byte[] body) throws IOException {
        if (body.length == 0) {
            return;
        }
        JsonNode root = objectMapper.readTree(body);
        if (root != null && root.isObject()) {
            if (root.has("operatorUserId")) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400,
                    "operatorUserId is no longer accepted; operator identity is derived from the authenticated user");
            }
            if (root.has("operatorName")) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400,
                    "operatorName is no longer accepted; operator identity is derived from the authenticated user");
            }
        }
    }

    private record BufferedHttpInputMessage(HttpHeaders headers, byte[] body) implements HttpInputMessage {
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }
    }
}
