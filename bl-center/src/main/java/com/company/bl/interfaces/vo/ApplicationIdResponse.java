package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApplicationIdResponse", description = "申请单标识响应")
public record ApplicationIdResponse(@Schema(description = "申请单 ID") String id) {
}
