package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ApplicationRegistrationWorkbenchOperatingOptionsResponse", description = "手术楼与手术间选项")
public record ApplicationRegistrationWorkbenchOperatingOptionsResponse(
    @Schema(description = "手术楼选项")
    List<OperatingBuildingResponse> buildings
) {

    public record OperatingBuildingResponse(
        @Schema(description = "手术楼编码")
        String buildingId,
        @Schema(description = "手术楼名称")
        String buildingName,
        @Schema(description = "楼层数")
        int floors,
        @Schema(description = "位置")
        String location,
        @Schema(description = "手术间选项")
        List<OperatingRoomResponse> operatingRooms
    ) {
    }

    public record OperatingRoomResponse(
        @Schema(description = "手术楼编码")
        String buildingId,
        @Schema(description = "洁净级别")
        String cleanLevel,
        @Schema(description = "楼层")
        int floor,
        @Schema(description = "手术间编码")
        String roomId,
        @Schema(description = "手术间名称")
        String roomName,
        @Schema(description = "手术间类型")
        String roomType
    ) {
    }
}
