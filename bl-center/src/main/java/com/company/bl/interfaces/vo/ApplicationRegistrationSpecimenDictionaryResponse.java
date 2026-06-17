package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ApplicationRegistrationSpecimenDictionaryResponse", description = "申请登记工作台标本字典")
public record ApplicationRegistrationSpecimenDictionaryResponse(
    @Schema(description = "系统-部位-标本分组")
    List<SpecimenDictionaryGroupResponse> groups,
    @Schema(description = "标本词条选项")
    List<SpecimenDictionaryEntryOptionResponse> entryOptions,
    @Schema(description = "常用标本选项")
    List<SpecimenDictionaryEntryOptionResponse> commonOptions,
    @Schema(description = "是否按当前用户所属科室过滤")
    boolean departmentFiltered
) {

    public record SpecimenDictionaryGroupResponse(
        @Schema(description = "系统 ID")
        String systemId,
        @Schema(description = "系统名称")
        String systemName,
        @Schema(description = "部位列表")
        List<SpecimenDictionaryPartResponse> subParts
    ) {
    }

    public record SpecimenDictionaryPartResponse(
        @Schema(description = "部位 ID")
        String partId,
        @Schema(description = "部位名称")
        String partName,
        @Schema(description = "标本名称列表")
        List<String> specimens
    ) {
    }

    public record SpecimenDictionaryEntryOptionResponse(
        @Schema(description = "系统 ID")
        String systemId,
        @Schema(description = "系统名称")
        String systemName,
        @Schema(description = "部位 ID")
        String partId,
        @Schema(description = "部位名称")
        String partName,
        @Schema(description = "标本名称")
        String specimenName,
        @Schema(description = "搜索关键字")
        List<String> searchKeywords
    ) {
    }
}
