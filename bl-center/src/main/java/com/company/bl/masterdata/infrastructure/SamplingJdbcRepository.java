package com.company.bl.masterdata.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class SamplingJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SamplingJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TemplateCategoryRow> findTemplateCategories() {
        return jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, sort_order, enabled
            from sampling_template_categories
            order by sort_order, category_code
            """, this::mapTemplateCategory);
    }

    public List<TemplateRow> findTemplates() {
        return jdbcTemplate.query("""
            select id, category_id, template_code, template_name, template_content, split_part_count,
                   applicable_specimen_type, enabled
            from sampling_templates
            order by template_code
            """, this::mapTemplate);
    }

    public List<TemplateSiteRow> findTemplateSites() {
        return jdbcTemplate.query("""
            select rel.template_id, rel.body_part_id, body.part_name
            from sampling_template_site_rel rel
            join body_part_dict body on body.id = rel.body_part_id
            order by rel.sort_order, rel.body_part_id
            """, this::mapTemplateSite);
    }

    public TemplateCategoryRow insertTemplateCategory(CreateTemplateCategoryRow row) {
        jdbcTemplate.update("""
            insert into sampling_template_categories
                (id, parent_id, category_code, category_name, sort_order, enabled, created_at, updated_at)
            values
                (:id, :parentId, :categoryCode, :categoryName, :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findTemplateCategoryById(row.id());
    }

    public TemplateRow insertTemplate(CreateTemplateRow row) {
        jdbcTemplate.update("""
            insert into sampling_templates
                (id, category_id, template_code, template_name, template_content, split_part_count,
                 applicable_specimen_type, enabled, created_at, updated_at)
            values
                (:id, :categoryId, :templateCode, :templateName, :templateContent, :splitPartCount,
                 :applicableSpecimenType, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("categoryId", row.categoryId())
            .addValue("templateCode", row.templateCode())
            .addValue("templateName", row.templateName())
            .addValue("templateContent", row.templateContent())
            .addValue("splitPartCount", row.splitPartCount())
            .addValue("applicableSpecimenType", row.applicableSpecimenType())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        replaceTemplateSites(row.id(), row.bodyPartIds());
        return findTemplateById(row.id());
    }

    public TemplateCategoryRow findTemplateCategoryById(String id) {
        List<TemplateCategoryRow> rows = jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, sort_order, enabled
            from sampling_template_categories where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapTemplateCategory);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public TemplateRow findTemplateById(String id) {
        List<TemplateRow> rows = jdbcTemplate.query("""
            select id, category_id, template_code, template_name, template_content, split_part_count,
                   applicable_specimen_type, enabled
            from sampling_templates where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapTemplate);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateTemplateCategory(String id, UpdateTemplateCategoryRow row) {
        jdbcTemplate.update("""
            update sampling_template_categories
            set parent_id = :parentId,
                category_code = :categoryCode,
                category_name = :categoryName,
                sort_order = :sortOrder,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateTemplate(String id, UpdateTemplateRow row) {
        jdbcTemplate.update("""
            update sampling_templates
            set category_id = :categoryId,
                template_code = :templateCode,
                template_name = :templateName,
                template_content = :templateContent,
                split_part_count = :splitPartCount,
                applicable_specimen_type = :applicableSpecimenType,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("categoryId", row.categoryId())
            .addValue("templateCode", row.templateCode())
            .addValue("templateName", row.templateName())
            .addValue("templateContent", row.templateContent())
            .addValue("splitPartCount", row.splitPartCount())
            .addValue("applicableSpecimenType", row.applicableSpecimenType())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
        replaceTemplateSites(id, row.bodyPartIds());
    }

    public void replaceTemplateSites(String templateId, List<String> bodyPartIds) {
        jdbcTemplate.update("delete from sampling_template_site_rel where template_id = :templateId",
            new MapSqlParameterSource().addValue("templateId", templateId));
        int sortOrder = 0;
        for (String bodyPartId : bodyPartIds) {
            jdbcTemplate.update("""
                insert into sampling_template_site_rel
                    (id, template_id, body_part_id, sort_order, created_at)
                values
                    (:id, :templateId, :bodyPartId, :sortOrder, :createdAt)
                """, new MapSqlParameterSource()
                .addValue("id", "TSR-" + UUID.randomUUID())
                .addValue("templateId", templateId)
                .addValue("bodyPartId", bodyPartId)
                .addValue("sortOrder", ++sortOrder)
                .addValue("createdAt", LocalDateTime.now()));
        }
    }

    public void updateTemplateEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update sampling_templates set enabled = :enabled, updated_at = :updatedAt where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public long countTemplateCategoryChildren(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from sampling_template_categories
            where parent_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public long countTemplatesByCategory(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from sampling_templates
            where category_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public void deleteTemplateCategory(String id) {
        jdbcTemplate.update("delete from sampling_template_categories where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    public void deleteTemplate(String id) {
        jdbcTemplate.update("delete from sampling_template_site_rel where template_id = :templateId",
            new MapSqlParameterSource().addValue("templateId", id));
        jdbcTemplate.update("delete from sampling_templates where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    public List<GuidelineCategoryRow> findGuidelineCategories() {
        return jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, sort_order, enabled
            from sampling_guideline_categories
            order by sort_order, category_code
            """, this::mapGuidelineCategory);
    }

    public List<GuidelineRow> findGuidelines() {
        return jdbcTemplate.query("""
            select id, category_id, guideline_code, guideline_name, guideline_content, version_no, enabled
            from sampling_guidelines
            order by guideline_code
            """, this::mapGuideline);
    }

    public GuidelineCategoryRow insertGuidelineCategory(CreateGuidelineCategoryRow row) {
        jdbcTemplate.update("""
            insert into sampling_guideline_categories
                (id, parent_id, category_code, category_name, sort_order, enabled, created_at, updated_at)
            values
                (:id, :parentId, :categoryCode, :categoryName, :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findGuidelineCategoryById(row.id());
    }

    public GuidelineRow insertGuideline(CreateGuidelineRow row) {
        jdbcTemplate.update("""
            insert into sampling_guidelines
                (id, category_id, guideline_code, guideline_name, guideline_content, version_no,
                 enabled, created_at, updated_at)
            values
                (:id, :categoryId, :guidelineCode, :guidelineName, :guidelineContent, :versionNo,
                 :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("categoryId", row.categoryId())
            .addValue("guidelineCode", row.guidelineCode())
            .addValue("guidelineName", row.guidelineName())
            .addValue("guidelineContent", row.guidelineContent())
            .addValue("versionNo", row.versionNo())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findGuidelineById(row.id());
    }

    public GuidelineCategoryRow findGuidelineCategoryById(String id) {
        List<GuidelineCategoryRow> rows = jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, sort_order, enabled
            from sampling_guideline_categories where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapGuidelineCategory);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public GuidelineRow findGuidelineById(String id) {
        List<GuidelineRow> rows = jdbcTemplate.query("""
            select id, category_id, guideline_code, guideline_name, guideline_content, version_no, enabled
            from sampling_guidelines where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapGuideline);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateGuidelineCategory(String id, UpdateGuidelineCategoryRow row) {
        jdbcTemplate.update("""
            update sampling_guideline_categories
            set parent_id = :parentId,
                category_code = :categoryCode,
                category_name = :categoryName,
                sort_order = :sortOrder,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateGuideline(String id, UpdateGuidelineRow row) {
        jdbcTemplate.update("""
            update sampling_guidelines
            set category_id = :categoryId,
                guideline_code = :guidelineCode,
                guideline_name = :guidelineName,
                guideline_content = :guidelineContent,
                version_no = :versionNo,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("categoryId", row.categoryId())
            .addValue("guidelineCode", row.guidelineCode())
            .addValue("guidelineName", row.guidelineName())
            .addValue("guidelineContent", row.guidelineContent())
            .addValue("versionNo", row.versionNo())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateGuidelineEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update sampling_guidelines set enabled = :enabled, updated_at = :updatedAt where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public long countGuidelineCategoryChildren(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from sampling_guideline_categories
            where parent_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public long countGuidelinesByCategory(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from sampling_guidelines
            where category_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public void deleteGuidelineCategory(String id) {
        jdbcTemplate.update("delete from sampling_guideline_categories where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    public void deleteGuideline(String id) {
        jdbcTemplate.update("delete from sampling_guidelines where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    private TemplateCategoryRow mapTemplateCategory(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateCategoryRow(rs.getString("id"), rs.getString("parent_id"), rs.getString("category_code"),
            rs.getString("category_name"), rs.getInt("sort_order"), rs.getInt("enabled") == 1);
    }

    private TemplateRow mapTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateRow(rs.getString("id"), rs.getString("category_id"), rs.getString("template_code"),
            rs.getString("template_name"), rs.getString("template_content"), rs.getInt("split_part_count"),
            rs.getString("applicable_specimen_type"), rs.getInt("enabled") == 1);
    }

    private TemplateSiteRow mapTemplateSite(ResultSet rs, int rowNum) throws SQLException {
        return new TemplateSiteRow(rs.getString("template_id"), rs.getString("body_part_id"), rs.getString("part_name"));
    }

    private GuidelineCategoryRow mapGuidelineCategory(ResultSet rs, int rowNum) throws SQLException {
        return new GuidelineCategoryRow(rs.getString("id"), rs.getString("parent_id"), rs.getString("category_code"),
            rs.getString("category_name"), rs.getInt("sort_order"), rs.getInt("enabled") == 1);
    }

    private GuidelineRow mapGuideline(ResultSet rs, int rowNum) throws SQLException {
        return new GuidelineRow(rs.getString("id"), rs.getString("category_id"), rs.getString("guideline_code"),
            rs.getString("guideline_name"), rs.getString("guideline_content"), rs.getString("version_no"),
            rs.getInt("enabled") == 1);
    }

    public record TemplateCategoryRow(String id, String parentId, String categoryCode, String categoryName,
                                      int sortOrder, boolean enabled) {
    }

    public record TemplateRow(String id, String categoryId, String templateCode, String templateName,
                              String templateContent, int splitPartCount, String applicableSpecimenType,
                              boolean enabled) {
    }

    public record TemplateSiteRow(String templateId, String bodyPartId, String bodyPartName) {
    }

    public record CreateTemplateCategoryRow(String id, String parentId, String categoryCode, String categoryName,
                                            int sortOrder, boolean enabled, LocalDateTime createdAt,
                                            LocalDateTime updatedAt) {
    }

    public record UpdateTemplateCategoryRow(String parentId, String categoryCode, String categoryName,
                                            int sortOrder, boolean enabled) {
    }

    public record CreateTemplateRow(String id, String categoryId, String templateCode, String templateName,
                                    String templateContent, int splitPartCount, String applicableSpecimenType,
                                    boolean enabled, List<String> bodyPartIds, LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
    }

    public record UpdateTemplateRow(String categoryId, String templateCode, String templateName,
                                    String templateContent, int splitPartCount, String applicableSpecimenType,
                                    boolean enabled, List<String> bodyPartIds) {
    }

    public record GuidelineCategoryRow(String id, String parentId, String categoryCode, String categoryName,
                                       int sortOrder, boolean enabled) {
    }

    public record GuidelineRow(String id, String categoryId, String guidelineCode, String guidelineName,
                               String guidelineContent, String versionNo, boolean enabled) {
    }

    public record CreateGuidelineCategoryRow(String id, String parentId, String categoryCode, String categoryName,
                                             int sortOrder, boolean enabled, LocalDateTime createdAt,
                                             LocalDateTime updatedAt) {
    }

    public record UpdateGuidelineCategoryRow(String parentId, String categoryCode, String categoryName,
                                             int sortOrder, boolean enabled) {
    }

    public record CreateGuidelineRow(String id, String categoryId, String guidelineCode, String guidelineName,
                                     String guidelineContent, String versionNo, boolean enabled,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record UpdateGuidelineRow(String categoryId, String guidelineCode, String guidelineName,
                                     String guidelineContent, String versionNo, boolean enabled) {
    }
}
