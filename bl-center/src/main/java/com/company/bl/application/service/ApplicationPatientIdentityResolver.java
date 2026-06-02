package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApplicationPatientIdentityResolver {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private volatile Boolean patientsTableAvailable;

    public Optional<PatientSummary> lookup(String patientIdentifier) {
        String normalizedIdentifier = normalize(patientIdentifier);
        if (normalizedIdentifier == null) {
            return Optional.empty();
        }
        if (!hasPatientsTable()) {
            return Optional.empty();
        }

        List<PatientSummary> matchedPatients = queryPatientsByIdentifier(normalizedIdentifier);
        if (matchedPatients.isEmpty()) {
            return Optional.empty();
        }
        ensureSingleMatch(normalizedIdentifier, matchedPatients);
        return Optional.of(matchedPatients.get(0));
    }

    public String resolveExistingOrOriginal(String patientIdentifier) {
        String normalizedIdentifier = normalize(patientIdentifier);
        if (normalizedIdentifier == null) {
            return null;
        }
        if (!hasPatientsTable()) {
            return normalizedIdentifier;
        }
        return lookup(normalizedIdentifier)
            .map(PatientSummary::patientId)
            .orElse(normalizedIdentifier);
    }

    public String resolveOrCreate(
        String patientIdentifier,
        String patientName,
        String patientGender,
        String patientAge
    ) {
        String normalizedIdentifier = normalize(patientIdentifier);
        if (normalizedIdentifier == null) {
            return null;
        }
        if (!hasPatientsTable()) {
            return normalizedIdentifier;
        }
        Optional<PatientSummary> matchedPatient = lookup(normalizedIdentifier);
        if (matchedPatient.isPresent()) {
            return matchedPatient.get().patientId();
        }

        String patientId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                insert into patients
                    (id, patient_no, name, gender, age, inpatient_no, outpatient_no, created_at, updated_at)
                values
                    (:id, :patientNo, :name, :gender, :age, null, null, :createdAt, :updatedAt)
                """,
            new MapSqlParameterSource()
                .addValue("id", patientId)
                .addValue("patientNo", normalizedIdentifier)
                .addValue("name", normalize(patientName))
                .addValue("gender", normalize(patientGender))
                .addValue("age", normalize(patientAge))
                .addValue("createdAt", now)
                .addValue("updatedAt", now));
        return patientId;
    }

    private List<PatientSummary> queryPatientsByIdentifier(String normalizedIdentifier) {
        return jdbcTemplate.query("""
                select
                    id,
                    patient_no,
                    inpatient_no,
                    outpatient_no,
                    name,
                    gender,
                    age
                from patients
                where upper(id) = upper(:identifier)
                   or upper(coalesce(patient_no, '')) = upper(:identifier)
                   or upper(coalesce(inpatient_no, '')) = upper(:identifier)
                   or upper(coalesce(outpatient_no, '')) = upper(:identifier)
                order by case
                    when upper(id) = upper(:identifier) then 0
                    when upper(coalesce(patient_no, '')) = upper(:identifier) then 1
                    when upper(coalesce(inpatient_no, '')) = upper(:identifier) then 2
                    when upper(coalesce(outpatient_no, '')) = upper(:identifier) then 3
                    else 4
                end
                fetch next 2 rows only
                """,
            new MapSqlParameterSource().addValue("identifier", normalizedIdentifier),
            (rs, rowNum) -> new PatientSummary(
                rs.getString("id"),
                resolvePatientIdentifier(
                    rs.getString("id"),
                    rs.getString("patient_no"),
                    rs.getString("inpatient_no"),
                    rs.getString("outpatient_no")),
                normalize(rs.getString("name")),
                normalize(rs.getString("gender")),
                normalize(rs.getString("age"))));
    }

    private void ensureSingleMatch(String normalizedIdentifier, List<PatientSummary> matchedPatients) {
        if (matchedPatients.size() > 1 &&
            !Objects.equals(matchedPatients.get(0).patientId(), matchedPatients.get(1).patientId())) {
            throw new BlBusinessException(
                BlErrorCode.RESOURCE_CONFLICT,
                409,
                "患者标识匹配到多位患者，请改用更精确的患者编号或主键ID");
        }
    }

    private boolean hasPatientsTable() {
        Boolean cached = patientsTableAvailable;
        if (Boolean.TRUE.equals(cached)) {
            return true;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            tableExists(connection.getMetaData(), "PATIENTS"));
        if (Boolean.TRUE.equals(resolved)) {
            patientsTableAvailable = true;
            return true;
        }
        return false;
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

    private String resolvePatientIdentifier(
        String patientId,
        String patientNo,
        String inpatientNo,
        String outpatientNo
    ) {
        String normalizedPatientNo = normalize(patientNo);
        if (normalizedPatientNo != null) {
            return normalizedPatientNo;
        }
        String normalizedInpatientNo = normalize(inpatientNo);
        if (normalizedInpatientNo != null) {
            return normalizedInpatientNo;
        }
        String normalizedOutpatientNo = normalize(outpatientNo);
        if (normalizedOutpatientNo != null) {
            return normalizedOutpatientNo;
        }
        return patientId;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record PatientSummary(
        String patientId,
        String patientIdentifier,
        String patientName,
        String patientGender,
        String patientAge
    ) {
    }
}
