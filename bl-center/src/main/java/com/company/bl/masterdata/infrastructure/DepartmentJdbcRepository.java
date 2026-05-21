package com.company.bl.masterdata.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class DepartmentJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DepartmentJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DepartmentRow> findDepartments() {
        return jdbcTemplate.query("""
            select id, parent_id, department_code, department_name, sort_order, enabled
            from department_dict
            order by sort_order, department_code
            """, this::mapDepartment);
    }

    public DepartmentRow insertDepartment(CreateDepartmentRow row) {
        jdbcTemplate.update("""
            insert into department_dict
                (id, parent_id, department_code, department_name, sort_order, enabled, created_at, updated_at)
            values
                (:id, :parentId, :departmentCode, :departmentName, :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("parentId", row.parentId())
            .addValue("departmentCode", row.departmentCode())
            .addValue("departmentName", row.departmentName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findDepartmentById(row.id());
    }

    public DepartmentRow findDepartmentById(String id) {
        List<DepartmentRow> rows = jdbcTemplate.query("""
            select id, parent_id, department_code, department_name, sort_order, enabled
            from department_dict
            where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapDepartment);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateDepartmentEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update department_dict
            set enabled = :enabled, updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateDepartment(String id, UpdateDepartmentRow row) {
        jdbcTemplate.update("""
            update department_dict
            set parent_id = :parentId,
                department_code = :departmentCode,
                department_name = :departmentName,
                sort_order = :sortOrder,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("parentId", row.parentId())
            .addValue("departmentCode", row.departmentCode())
            .addValue("departmentName", row.departmentName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public long countChildren(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from department_dict
            where parent_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public long countUserReferences(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from users
            where department_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public void deleteDepartment(String id) {
        jdbcTemplate.update("""
            delete from department_dict
            where id = :id
            """, new MapSqlParameterSource().addValue("id", id));
    }

    private DepartmentRow mapDepartment(ResultSet rs, int rowNum) throws SQLException {
        return new DepartmentRow(
            rs.getString("id"),
            rs.getString("parent_id"),
            rs.getString("department_code"),
            rs.getString("department_name"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1);
    }

    public record DepartmentRow(
        String id,
        String parentId,
        String departmentCode,
        String departmentName,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record CreateDepartmentRow(
        String id,
        String parentId,
        String departmentCode,
        String departmentName,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record UpdateDepartmentRow(
        String parentId,
        String departmentCode,
        String departmentName,
        int sortOrder,
        boolean enabled
    ) {
    }
}
