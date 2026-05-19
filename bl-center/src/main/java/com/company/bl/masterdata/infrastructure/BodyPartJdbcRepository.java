package com.company.bl.masterdata.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BodyPartJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BodyPartJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BodyPartRow> findBodyParts() {
        return jdbcTemplate.query("""
            select id, parent_id, part_code, part_name, part_alias, part_level, sort_order, enabled
            from body_part_dict
            order by sort_order, part_code
            """, this::mapBodyPart);
    }

    public BodyPartRow insertBodyPart(CreateBodyPartRow row) {
        jdbcTemplate.update("""
            insert into body_part_dict
                (id, parent_id, part_code, part_name, part_alias, part_level, sort_order, enabled, created_at, updated_at)
            values
                (:id, :parentId, :partCode, :partName, :partAlias, :partLevel, :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("parentId", row.parentId())
            .addValue("partCode", row.partCode())
            .addValue("partName", row.partName())
            .addValue("partAlias", row.partAlias())
            .addValue("partLevel", row.partLevel())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findBodyPartById(row.id());
    }

    public BodyPartRow findBodyPartById(String id) {
        List<BodyPartRow> rows = jdbcTemplate.query("""
            select id, parent_id, part_code, part_name, part_alias, part_level, sort_order, enabled
            from body_part_dict where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapBodyPart);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateBodyPartEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update body_part_dict
            set enabled = :enabled, updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    private BodyPartRow mapBodyPart(ResultSet rs, int rowNum) throws SQLException {
        return new BodyPartRow(
            rs.getString("id"),
            rs.getString("parent_id"),
            rs.getString("part_code"),
            rs.getString("part_name"),
            rs.getString("part_alias"),
            rs.getInt("part_level"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1);
    }

    public record BodyPartRow(String id, String parentId, String partCode, String partName, String partAlias,
                              int partLevel, int sortOrder, boolean enabled) {
    }

    public record CreateBodyPartRow(String id, String parentId, String partCode, String partName, String partAlias,
                                    int partLevel, int sortOrder, boolean enabled, LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
    }
}
