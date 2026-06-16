package com.company.bl.interfaces.openapi;

import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.auth.RequireAnyPermission;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

@Configuration
public class BlOpenApiConfiguration {

    private static final String JSON = "application/json";
    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI blOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("bl-center OpenAPI")
                .version("0.1.0-SNAPSHOT")
                .description("bl-center 病理业务模块接口说明，供 Apifox 通过 /v3/api-docs 直接导入。"))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Bearer Token，受权限控制的接口需要携带 Authorization 请求头。"))
                .addSchemas("ApiErrorResponse", apiErrorResponseSchema()));
    }

    @Bean
    public OperationCustomizer blOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!isApiMethod(handlerMethod)) {
                return operation;
            }
            ensureValidationResponse(operation);
            RequirePermission permission = findPermission(handlerMethod);
            RequireAnyPermission anyPermission = findAnyPermission(handlerMethod);
            if (permission != null || anyPermission != null) {
                operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
                if (permission != null) {
                    operation.setDescription(appendPermission(operation.getDescription(), permission.value()));
                } else {
                    operation.setDescription(appendAnyPermission(operation.getDescription(), anyPermission.value()));
                }
                ensureErrorResponse(operation, "401", "未提供 Bearer Token 或 Token 无效");
                ensureErrorResponse(operation, "403", "当前用户缺少接口权限");
            }
            return operation;
        };
    }

    @Bean
    public OpenApiCustomizer apiEnvelopeCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (!path.startsWith("/api/")) {
                    return;
                }
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    wrapSuccessResponses(operation);
                    if (path.contains("{")) {
                        ensureErrorResponse(operation, "404", "请求资源不存在");
                    }
                    switch (httpMethod) {
                        case POST, PUT, PATCH -> ensureErrorResponse(operation, "409", "资源状态冲突或当前状态不允许操作");
                        default -> {
                        }
                    }
                });
            });
        };
    }

    private void wrapSuccessResponses(Operation operation) {
        if (operation.getResponses() == null) {
            return;
        }
        for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
            String statusCode = entry.getKey();
            if (!statusCode.startsWith("2")) {
                continue;
            }
            Content content = entry.getValue().getContent();
            if (content == null) {
                continue;
            }
            MediaType mediaType = content.get(JSON);
            if (mediaType == null || mediaType.getSchema() == null) {
                continue;
            }
            Schema<?> schema = mediaType.getSchema();
            if (isWrappedSchema(schema)) {
                continue;
            }
            ObjectSchema wrapped = new ObjectSchema();
            wrapped.addProperty("code", new StringSchema().example("SUCCESS").description("业务响应码"));
            wrapped.addProperty("message", new StringSchema().example("success").description("响应消息"));
            wrapped.addProperty("traceId", new StringSchema().example("3f2d0d9b7f3f4f8f").description("链路追踪 ID"));
            wrapped.addProperty("data", schema.description("业务数据载荷"));
            mediaType.setSchema(wrapped);
        }
    }

    private boolean isWrappedSchema(Schema<?> schema) {
        if (!(schema instanceof ObjectSchema objectSchema) || objectSchema.getProperties() == null) {
            return false;
        }
        return objectSchema.getProperties().containsKey("code")
            && objectSchema.getProperties().containsKey("message")
            && objectSchema.getProperties().containsKey("traceId")
            && objectSchema.getProperties().containsKey("data");
    }

    private void ensureValidationResponse(Operation operation) {
        ensureErrorResponse(operation, "400", "请求参数校验失败或请求体格式错误");
    }

    private void ensureErrorResponse(Operation operation, String statusCode, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.computeIfAbsent(statusCode, ignored -> new ApiResponse()
            .description(description)
            .content(new Content().addMediaType(JSON,
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse")))));
    }

    private RequirePermission findPermission(HandlerMethod handlerMethod) {
        RequirePermission permission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequirePermission.class);
        if (permission != null) {
            return permission;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
    }

    private RequireAnyPermission findAnyPermission(HandlerMethod handlerMethod) {
        RequireAnyPermission permission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireAnyPermission.class);
        if (permission != null) {
            return permission;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireAnyPermission.class);
    }

    private boolean isApiMethod(HandlerMethod handlerMethod) {
        return hasApiPrefix(AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequestMapping.class))
            || hasApiPrefix(AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequestMapping.class));
    }

    private boolean hasApiPrefix(RequestMapping mapping) {
        if (mapping == null) {
            return false;
        }
        for (String path : mapping.path()) {
            if (path.startsWith("/api/")) {
                return true;
            }
        }
        for (String value : mapping.value()) {
            if (value.startsWith("/api/")) {
                return true;
            }
        }
        return false;
    }

    private String appendPermission(String description, String permissionCode) {
        String permissionLine = "权限码：`" + permissionCode + "`。";
        if (description == null || description.isBlank()) {
            return permissionLine;
        }
        if (description.contains(permissionCode)) {
            return description;
        }
        return description + "\n\n" + permissionLine;
    }

    private String appendAnyPermission(String description, String[] permissionCodes) {
        String permissionLine = "权限码（满足其一）：`" + String.join("`、`", permissionCodes) + "`。";
        if (description == null || description.isBlank()) {
            return permissionLine;
        }
        return description + "\n\n" + permissionLine;
    }

    private Schema<?> apiErrorResponseSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("code", new StringSchema().example("VALIDATION_ERROR").description("错误码"));
        schema.addProperty("message", new StringSchema().example("请求参数校验失败").description("错误描述"));
        schema.addProperty("traceId", new StringSchema().example("3f2d0d9b7f3f4f8f").description("链路追踪 ID"));
        schema.addProperty("data", new Schema<>().nullable(true).description("错误响应固定为 null"));
        return schema;
    }
}
