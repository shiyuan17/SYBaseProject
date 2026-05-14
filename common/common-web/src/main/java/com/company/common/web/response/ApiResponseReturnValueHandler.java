package com.company.common.web.response;

import com.company.common.web.filter.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public class ApiResponseReturnValueHandler implements HandlerMethodReturnValueHandler {

    private static final String API_PREFIX = "/api/";

    private final RequestResponseBodyMethodProcessor responseBodyProcessor;
    private final HttpEntityMethodProcessor httpEntityProcessor;

    public ApiResponseReturnValueHandler(List<HttpMessageConverter<?>> messageConverters) {
        this.responseBodyProcessor = new RequestResponseBodyMethodProcessor(messageConverters);
        this.httpEntityProcessor = new HttpEntityMethodProcessor(messageConverters);
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return isResponseBodyReturnType(returnType)
            && isApiMapping(returnType)
            && !hasIgnoreAnnotation(returnType)
            && !shouldSkipByDeclaredType(returnType)
            && !isAlreadyWrappedDeclaredType(returnType);
    }

    @Override
    public void handleReturnValue(Object returnValue,
                                  MethodParameter returnType,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest) throws Exception {
        if (returnValue instanceof ResponseEntity<?> responseEntity) {
            handleResponseEntity(responseEntity, returnType, mavContainer, webRequest);
            return;
        }
        if (returnValue instanceof ApiResponse<?> || shouldSkipByRuntimeBody(returnValue)) {
            delegate(returnValue, returnType, mavContainer, webRequest);
            return;
        }
        responseBodyProcessor.handleReturnValue(wrapSuccess(returnValue, webRequest), returnType, mavContainer, webRequest);
    }

    private void handleResponseEntity(ResponseEntity<?> responseEntity,
                                      MethodParameter returnType,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest) throws Exception {
        Object body = responseEntity.getBody();
        if (responseEntity.getStatusCode().value() == 204
            || body instanceof ApiResponse<?>
            || shouldSkipByDeclaredType(returnType)
            || shouldSkipByRuntimeBody(body)) {
            httpEntityProcessor.handleReturnValue(responseEntity, returnType, mavContainer, webRequest);
            return;
        }
        ResponseEntity<?> wrappedEntity = ResponseEntity.status(responseEntity.getStatusCode())
            .headers(responseEntity.getHeaders())
            .body(wrapSuccess(body, webRequest));
        httpEntityProcessor.handleReturnValue(wrappedEntity, returnType, mavContainer, webRequest);
    }

    private boolean isResponseBodyReturnType(MethodParameter returnType) {
        return HttpEntity.class.isAssignableFrom(returnType.getParameterType())
            || returnType.hasMethodAnnotation(ResponseBody.class)
            || AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class);
    }

    private boolean isApiMapping(MethodParameter returnType) {
        RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(returnType.getMethod(), RequestMapping.class);
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(returnType.getContainingClass(), RequestMapping.class);
        return hasApiPrefix(classMapping) || hasApiPrefix(methodMapping);
    }

    private boolean hasApiPrefix(RequestMapping mapping) {
        if (mapping == null) {
            return false;
        }
        for (String path : mapping.path()) {
            if (path.startsWith(API_PREFIX)) {
                return true;
            }
        }
        for (String value : mapping.value()) {
            if (value.startsWith(API_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIgnoreAnnotation(MethodParameter returnType) {
        if (returnType.hasMethodAnnotation(IgnoreApiResponseWrap.class)) {
            return true;
        }
        return AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), IgnoreApiResponseWrap.class);
    }

    private boolean shouldSkipByDeclaredType(MethodParameter returnType) {
        Class<?> parameterType = returnType.getParameterType();
        if (isSkippableType(parameterType)) {
            return true;
        }
        if (!HttpEntity.class.isAssignableFrom(parameterType)) {
            return false;
        }
        Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
        return bodyType != null && isSkippableType(bodyType);
    }

    private boolean isAlreadyWrappedDeclaredType(MethodParameter returnType) {
        Class<?> parameterType = returnType.getParameterType();
        if (ApiResponse.class.isAssignableFrom(parameterType)) {
            return true;
        }
        if (!HttpEntity.class.isAssignableFrom(parameterType)) {
            return false;
        }
        Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
        return bodyType != null && ApiResponse.class.isAssignableFrom(bodyType);
    }

    private boolean shouldSkipByRuntimeBody(Object body) {
        return body != null && isSkippableType(body.getClass());
    }

    private boolean isSkippableType(Class<?> candidateType) {
        return CharSequence.class.isAssignableFrom(candidateType)
            || byte[].class.equals(candidateType)
            || Resource.class.isAssignableFrom(candidateType)
            || InputStreamResource.class.isAssignableFrom(candidateType)
            || StreamingResponseBody.class.isAssignableFrom(candidateType)
            || ResponseBodyEmitter.class.isAssignableFrom(candidateType)
            || SseEmitter.class.isAssignableFrom(candidateType);
    }

    private ApiResponse<Object> wrapSuccess(Object body, NativeWebRequest webRequest) {
        return ApiResponse.success(body, traceId(webRequest));
    }

    private String traceId(NativeWebRequest webRequest) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return "";
        }
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId.toString();
    }

    private void delegate(Object returnValue,
                          MethodParameter returnType,
                          ModelAndViewContainer mavContainer,
                          NativeWebRequest webRequest) throws Exception {
        if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
            httpEntityProcessor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
            return;
        }
        responseBodyProcessor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }
}
