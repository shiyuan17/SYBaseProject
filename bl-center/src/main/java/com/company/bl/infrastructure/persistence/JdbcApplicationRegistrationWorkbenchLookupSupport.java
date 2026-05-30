package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

class JdbcApplicationRegistrationWorkbenchLookupSupport {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private volatile Boolean patientsTableAvailable;

    JdbcApplicationRegistrationWorkbenchLookupSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<ApplicationRegistrationWorkbenchRepository.WorkbenchApplicationRow> findApplicationByKeyword(String keyword, String queryType) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return Optional.empty();
        }
        String normalizedQueryType = normalizeQueryType(queryType);
        String keywordLike = "%" + normalizedKeyword.toUpperCase(Locale.ROOT) + "%";
        return jdbcTemplate.query(
                buildLookupSql(normalizedQueryType),
                new MapSqlParameterSource()
                    .addValue("keyword", normalizedKeyword)
                    .addValue("keywordLike", keywordLike),
                this::mapWorkbenchApplicationRow)
            .stream()
            .findFirst();
    }

    private String normalizeQueryType(String queryType) {
        if (queryType == null || queryType.isBlank()) {
            return "AUTO";
        }
        return queryType.trim().toUpperCase(Locale.ROOT);
    }

    private String buildLookupSql(String queryType) {
        if (hasPatientsTable()) {
            return switch (queryType) {
                case "APPLICATION_NO" -> """
                    select
                        a.id as application_id,
                        a.application_no,
                        a.patient_id,
                        a.patient_name,
                        a.patient_gender,
                        a.patient_age,
                        a.submitting_department_name,
                        a.submitting_doctor_name,
                        a.clinical_diagnosis,
                        a.remarks,
                        a.status,
                        a.application_date,
                        a.submission_date
                    from applications a
                    where upper(coalesce(a.application_no, '')) = upper(:keyword)
                       or upper(coalesce(a.application_no, '')) like :keywordLike
                    order by case
                        when upper(coalesce(a.application_no, '')) = upper(:keyword) then 0
                        else 1
                    end,
                    a.updated_at desc
                    fetch next 1 rows only
                    """;
                case "INPATIENT_NO" -> """
                    select
                        a.id as application_id,
                        a.application_no,
                        a.patient_id,
                        a.patient_name,
                        a.patient_gender,
                        a.patient_age,
                        a.submitting_department_name,
                        a.submitting_doctor_name,
                        a.clinical_diagnosis,
                        a.remarks,
                        a.status,
                        a.application_date,
                        a.submission_date
                    from applications a
                    left join application_registration_workbench w on w.application_id = a.id
                    left join patients p
                        on p.id = a.patient_id
                        or p.patient_no = a.patient_id
                    where upper(coalesce(w.inpatient_no, '')) = upper(:keyword)
                       or upper(coalesce(p.patient_no, '')) = upper(:keyword)
                       or upper(coalesce(p.inpatient_no, '')) = upper(:keyword)
                       or upper(coalesce(p.outpatient_no, '')) = upper(:keyword)
                       or upper(coalesce(w.inpatient_no, '')) like :keywordLike
                       or upper(coalesce(p.patient_no, '')) like :keywordLike
                       or upper(coalesce(p.inpatient_no, '')) like :keywordLike
                       or upper(coalesce(p.outpatient_no, '')) like :keywordLike
                    order by case
                        when upper(coalesce(w.inpatient_no, '')) = upper(:keyword) then 0
                        when upper(coalesce(p.patient_no, '')) = upper(:keyword) then 0
                        when upper(coalesce(p.inpatient_no, '')) = upper(:keyword) then 0
                        when upper(coalesce(p.outpatient_no, '')) = upper(:keyword) then 0
                        else 1
                    end,
                    a.updated_at desc
                    fetch next 1 rows only
                    """;
                case "PATIENT_NAME" -> """
                    select
                        a.id as application_id,
                        a.application_no,
                        a.patient_id,
                        a.patient_name,
                        a.patient_gender,
                        a.patient_age,
                        a.submitting_department_name,
                        a.submitting_doctor_name,
                        a.clinical_diagnosis,
                        a.remarks,
                        a.status,
                        a.application_date,
                        a.submission_date
                    from applications a
                    where upper(coalesce(a.patient_name, '')) like :keywordLike
                    order by a.updated_at desc
                    fetch next 1 rows only
                    """;
                default -> """
                    select
                        a.id as application_id,
                        a.application_no,
                        a.patient_id,
                        a.patient_name,
                        a.patient_gender,
                        a.patient_age,
                        a.submitting_department_name,
                        a.submitting_doctor_name,
                        a.clinical_diagnosis,
                        a.remarks,
                        a.status,
                        a.application_date,
                        a.submission_date
                    from applications a
                    left join application_registration_workbench w on w.application_id = a.id
                    left join patients p
                        on p.id = a.patient_id
                        or p.patient_no = a.patient_id
                    where a.id = :keyword
                       or upper(a.application_no) = upper(:keyword)
                       or upper(coalesce(a.external_order_no, '')) = upper(:keyword)
                       or upper(coalesce(a.patient_id, '')) = upper(:keyword)
                       or upper(coalesce(w.inpatient_no, '')) = upper(:keyword)
                       or upper(coalesce(p.patient_no, '')) = upper(:keyword)
                       or upper(coalesce(p.inpatient_no, '')) = upper(:keyword)
                       or upper(coalesce(p.outpatient_no, '')) = upper(:keyword)
                       or upper(coalesce(a.application_no, '')) like :keywordLike
                       or upper(coalesce(a.external_order_no, '')) like :keywordLike
                       or upper(coalesce(a.patient_id, '')) like :keywordLike
                       or upper(coalesce(a.patient_name, '')) like :keywordLike
                       or upper(coalesce(w.inpatient_no, '')) like :keywordLike
                       or upper(coalesce(p.patient_no, '')) like :keywordLike
                       or upper(coalesce(p.inpatient_no, '')) like :keywordLike
                       or upper(coalesce(p.outpatient_no, '')) like :keywordLike
                    order by case
                        when a.id = :keyword then 0
                        when upper(a.application_no) = upper(:keyword) then 0
                        when upper(coalesce(a.external_order_no, '')) = upper(:keyword) then 0
                        when upper(coalesce(a.patient_id, '')) = upper(:keyword) then 0
                        when upper(coalesce(w.inpatient_no, '')) = upper(:keyword) then 0
                        when upper(coalesce(p.patient_no, '')) = upper(:keyword) then 0
                        when upper(coalesce(p.inpatient_no, '')) = upper(:keyword) then 0
                        when upper(coalesce(p.outpatient_no, '')) = upper(:keyword) then 0
                        else 1
                    end,
                    a.updated_at desc
                    fetch next 1 rows only
                    """;
            };
        }

        return switch (queryType) {
            case "APPLICATION_NO" -> """
                select
                    a.id as application_id,
                    a.application_no,
                    a.patient_id,
                    a.patient_name,
                    a.patient_gender,
                    a.patient_age,
                    a.submitting_department_name,
                    a.submitting_doctor_name,
                    a.clinical_diagnosis,
                    a.remarks,
                    a.status,
                    a.application_date,
                    a.submission_date
                from applications a
                where upper(coalesce(a.application_no, '')) = upper(:keyword)
                   or upper(coalesce(a.application_no, '')) like :keywordLike
                order by case
                    when upper(coalesce(a.application_no, '')) = upper(:keyword) then 0
                    else 1
                end,
                a.updated_at desc
                fetch next 1 rows only
                """;
            case "INPATIENT_NO" -> """
                select
                    a.id as application_id,
                    a.application_no,
                    a.patient_id,
                    a.patient_name,
                    a.patient_gender,
                    a.patient_age,
                    a.submitting_department_name,
                    a.submitting_doctor_name,
                    a.clinical_diagnosis,
                    a.remarks,
                    a.status,
                    a.application_date,
                    a.submission_date
                from applications a
                left join application_registration_workbench w on w.application_id = a.id
                where upper(coalesce(w.inpatient_no, '')) = upper(:keyword)
                   or upper(coalesce(w.inpatient_no, '')) like :keywordLike
                order by case
                    when upper(coalesce(w.inpatient_no, '')) = upper(:keyword) then 0
                    else 1
                end,
                a.updated_at desc
                fetch next 1 rows only
                """;
            case "PATIENT_NAME" -> """
                select
                    a.id as application_id,
                    a.application_no,
                    a.patient_id,
                    a.patient_name,
                    a.patient_gender,
                    a.patient_age,
                    a.submitting_department_name,
                    a.submitting_doctor_name,
                    a.clinical_diagnosis,
                    a.remarks,
                    a.status,
                    a.application_date,
                    a.submission_date
                from applications a
                where upper(coalesce(a.patient_name, '')) like :keywordLike
                order by a.updated_at desc
                fetch next 1 rows only
                """;
            default -> """
                select
                    a.id as application_id,
                    a.application_no,
                    a.patient_id,
                    a.patient_name,
                    a.patient_gender,
                    a.patient_age,
                    a.submitting_department_name,
                    a.submitting_doctor_name,
                    a.clinical_diagnosis,
                    a.remarks,
                    a.status,
                    a.application_date,
                    a.submission_date
                from applications a
                left join application_registration_workbench w on w.application_id = a.id
                where a.id = :keyword
                   or upper(a.application_no) = upper(:keyword)
                   or upper(coalesce(a.external_order_no, '')) = upper(:keyword)
                   or upper(coalesce(a.patient_id, '')) = upper(:keyword)
                   or upper(coalesce(w.inpatient_no, '')) = upper(:keyword)
                   or upper(coalesce(a.application_no, '')) like :keywordLike
                   or upper(coalesce(a.external_order_no, '')) like :keywordLike
                   or upper(coalesce(a.patient_id, '')) like :keywordLike
                   or upper(coalesce(a.patient_name, '')) like :keywordLike
                   or upper(coalesce(w.inpatient_no, '')) like :keywordLike
                order by case
                    when a.id = :keyword then 0
                    when upper(a.application_no) = upper(:keyword) then 0
                    when upper(coalesce(a.external_order_no, '')) = upper(:keyword) then 0
                    when upper(coalesce(a.patient_id, '')) = upper(:keyword) then 0
                    when upper(coalesce(w.inpatient_no, '')) = upper(:keyword) then 0
                    else 1
                end,
                a.updated_at desc
                fetch next 1 rows only
                """;
        };
    }

    private boolean hasPatientsTable() {
        Boolean cached = patientsTableAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            tableExists(connection.getMetaData(), "PATIENTS"));
        patientsTableAvailable = Boolean.TRUE.equals(resolved);
        return patientsTableAvailable;
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, tableName, null)) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet tables = metadata.getTables(null, null, tableName.toLowerCase(), null)) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private ApplicationRegistrationWorkbenchRepository.WorkbenchApplicationRow mapWorkbenchApplicationRow(ResultSet rs, int rowNum)
        throws SQLException {
        return new ApplicationRegistrationWorkbenchRepository.WorkbenchApplicationRow(
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_id"),
            rs.getString("patient_name"),
            rs.getString("patient_gender"),
            rs.getString("patient_age"),
            rs.getString("submitting_department_name"),
            rs.getString("submitting_doctor_name"),
            rs.getString("clinical_diagnosis"),
            rs.getString("remarks"),
            rs.getString("status"),
            rs.getDate("application_date") == null ? null : rs.getDate("application_date").toLocalDate(),
            rs.getDate("submission_date") == null ? null : rs.getDate("submission_date").toLocalDate());
    }
}
