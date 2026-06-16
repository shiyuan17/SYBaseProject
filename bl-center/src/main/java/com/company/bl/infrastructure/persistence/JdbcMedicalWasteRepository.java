package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.MedicalWasteRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcMedicalWasteRepository implements MedicalWasteRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcMedicalWasteRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SpecimenBatch> findSpecimenBatches(String keyword, String createdByName, LocalDate dateFrom, LocalDate dateTo) {
        return jdbcTemplate.query("""
            select id, bag_name, grossing_station_id, grossing_station_name, grossing_operator_id, grossing_operator_name,
                   grossing_date, grossing_period, weight_kg, label_count, printed_at, printed_by_user_id, printed_by_name,
                   destroyed_at, destroyed_by_user_id, destroyed_by_name
            from medical_waste_specimen_batch
            where (:keywordLike is null or upper(bag_name) like :keywordLike)
              and (:createdByName is null or grossing_operator_name = :createdByName)
              and (:dateFrom is null or grossing_date >= :dateFrom)
              and (:dateTo is null or grossing_date <= :dateTo)
            order by grossing_date desc, printed_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("keywordLike", like(keyword))
            .addValue("createdByName", blankToNull(createdByName))
            .addValue("dateFrom", dateFrom)
            .addValue("dateTo", dateTo), this::mapSpecimenBatch);
    }

    @Override
    public List<SpecimenPreviewLabel> findSpecimenPreviewLabels(String grossingStationName,
                                                                String grossingOperatorName,
                                                                LocalDate grossingDate,
                                                                String grossingPeriod) {
        int startHour = "PM".equals(grossingPeriod) ? 12 : 0;
        int endHour = "PM".equals(grossingPeriod) ? 24 : 12;
        return jdbcTemplate.query("""
            select s.id as source_label_id,
                   a.patient_id,
                   a.patient_name,
                   pc.pathology_no,
                   coalesce(s.specimen_name_standardized, s.specimen_no) as specimen_name
            from samplings sm
            join specimens s on s.id = sm.specimen_id
            join pathology_cases pc on pc.id = sm.case_id
            join applications a on a.id = pc.application_id
            left join technical_pending_tasks t
              on t.case_id = sm.case_id
             and t.task_type = 'GROSSING'
            where sm.sampled_at is not null
              and cast(sm.sampled_at as date) = :grossingDate
              and extract(hour from sm.sampled_at) >= :startHour
              and extract(hour from sm.sampled_at) < :endHour
              and sm.sampled_by_name = :grossingOperatorName
              and coalesce(t.station_name, '') = :grossingStationName
            order by a.patient_id asc, pc.pathology_no asc, s.specimen_no asc, s.id asc
            """, new MapSqlParameterSource()
            .addValue("grossingDate", grossingDate)
            .addValue("startHour", startHour)
            .addValue("endHour", endHour)
            .addValue("grossingOperatorName", grossingOperatorName)
            .addValue("grossingStationName", grossingStationName), this::mapSpecimenPreviewLabel);
    }

    @Override
    public List<OptionItem> findGrossingStations() {
        return jdbcTemplate.query("""
            select distinct station_name
            from technical_pending_tasks
            where task_type = 'GROSSING'
              and station_name is not null
              and trim(station_name) <> ''
            order by station_name asc
            """, (rs, rowNum) -> new OptionItem(rs.getString("station_name"), rs.getString("station_name")));
    }

    @Override
    public List<OptionItem> findGrossingOperators() {
        return jdbcTemplate.query("""
            select distinct sampled_by_name
            from samplings
            where sampled_by_name is not null
              and trim(sampled_by_name) <> ''
            order by sampled_by_name asc
            """, (rs, rowNum) -> new OptionItem(rs.getString("sampled_by_name"), rs.getString("sampled_by_name")));
    }

    @Override
    public void insertSpecimenBatch(CreateSpecimenBatchCommand command) {
        jdbcTemplate.update("""
            insert into medical_waste_specimen_batch
                (id, bag_name, grossing_station_id, grossing_station_name, grossing_operator_id, grossing_operator_name,
                 grossing_date, grossing_period, weight_kg, label_count, printed_at, printed_by_user_id, printed_by_name)
            values
                (:id, :bagName, :grossingStationId, :grossingStationName, :grossingOperatorId, :grossingOperatorName,
                 :grossingDate, :grossingPeriod, :weightKg, :labelCount, :printedAt, :printedByUserId, :printedByName)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("bagName", command.bagName())
            .addValue("grossingStationId", command.grossingStationId())
            .addValue("grossingStationName", command.grossingStationName())
            .addValue("grossingOperatorId", command.grossingOperatorId())
            .addValue("grossingOperatorName", command.grossingOperatorName())
            .addValue("grossingDate", command.grossingDate())
            .addValue("grossingPeriod", command.grossingPeriod())
            .addValue("weightKg", command.weightKg())
            .addValue("labelCount", command.labelCount())
            .addValue("printedAt", command.printedAt())
            .addValue("printedByUserId", command.printedByUserId())
            .addValue("printedByName", command.printedByName()));
    }

    @Override
    public void insertSpecimenBatchLabels(List<CreateSpecimenBatchLabelCommand> commands) {
        for (CreateSpecimenBatchLabelCommand command : commands) {
            jdbcTemplate.update("""
                insert into medical_waste_specimen_batch_label
                    (id, batch_id, source_label_id, patient_id, patient_name, pathology_no, specimen_name)
                values
                    (:id, :batchId, :sourceLabelId, :patientId, :patientName, :pathologyNo, :specimenName)
                """, new MapSqlParameterSource()
                .addValue("id", command.id())
                .addValue("batchId", command.batchId())
                .addValue("sourceLabelId", command.sourceLabelId())
                .addValue("patientId", command.patientId())
                .addValue("patientName", command.patientName())
                .addValue("pathologyNo", command.pathologyNo())
                .addValue("specimenName", command.specimenName()));
        }
    }

    @Override
    public Optional<SpecimenBatch> findSpecimenBatchById(String batchId) {
        return jdbcTemplate.query("""
            select id, bag_name, grossing_station_id, grossing_station_name, grossing_operator_id, grossing_operator_name,
                   grossing_date, grossing_period, weight_kg, label_count, printed_at, printed_by_user_id, printed_by_name,
                   destroyed_at, destroyed_by_user_id, destroyed_by_name
            from medical_waste_specimen_batch
            where id = :id
            """, Map.of("id", batchId), this::mapSpecimenBatch).stream().findFirst();
    }

    @Override
    public void destroySpecimenBatch(String batchId, String destroyedByUserId, String destroyedByName, LocalDateTime destroyedAt) {
        jdbcTemplate.update("""
            update medical_waste_specimen_batch
            set destroyed_at = :destroyedAt,
                destroyed_by_user_id = :destroyedByUserId,
                destroyed_by_name = :destroyedByName
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", batchId)
            .addValue("destroyedAt", destroyedAt)
            .addValue("destroyedByUserId", destroyedByUserId)
            .addValue("destroyedByName", destroyedByName));
    }

    @Override
    public List<ReagentBag> findReagentBags(String keyword, LocalDate dateFrom, LocalDate dateTo) {
        return jdbcTemplate.query("""
            select id, bag_name, waste_type, weight_kg, volume_ml, source, remarks,
                   created_at, created_by_user_id, created_by_name, printed_at, printed_by_user_id, printed_by_name,
                   handed_over_at, handed_over_by_user_id, handed_over_by_name, handover_remarks, updated_at
            from medical_waste_reagent_bag
            where (:keywordLike is null or upper(bag_name) like :keywordLike)
              and (:dateFrom is null or cast(created_at as date) >= :dateFrom)
              and (:dateTo is null or cast(created_at as date) <= :dateTo)
            order by created_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("keywordLike", like(keyword))
            .addValue("dateFrom", dateFrom)
            .addValue("dateTo", dateTo), this::mapReagentBag);
    }

    @Override
    public Optional<ReagentBag> findReagentBagById(String bagId) {
        return jdbcTemplate.query("""
            select id, bag_name, waste_type, weight_kg, volume_ml, source, remarks,
                   created_at, created_by_user_id, created_by_name, printed_at, printed_by_user_id, printed_by_name,
                   handed_over_at, handed_over_by_user_id, handed_over_by_name, handover_remarks, updated_at
            from medical_waste_reagent_bag
            where id = :id
            """, Map.of("id", bagId), this::mapReagentBag).stream().findFirst();
    }

    @Override
    public void insertReagentBag(CreateReagentBagCommand command) {
        jdbcTemplate.update("""
            insert into medical_waste_reagent_bag
                (id, bag_name, waste_type, weight_kg, volume_ml, source, remarks,
                 created_at, created_by_user_id, created_by_name,
                 printed_at, printed_by_user_id, printed_by_name, updated_at)
            values
                (:id, :bagName, :wasteType, :weightKg, :volumeMl, :source, :remarks,
                 :createdAt, :createdByUserId, :createdByName,
                 :printedAt, :printedByUserId, :printedByName, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("bagName", command.bagName())
            .addValue("wasteType", command.wasteType())
            .addValue("weightKg", command.weightKg())
            .addValue("volumeMl", command.volumeMl())
            .addValue("source", command.source())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("createdByUserId", command.createdByUserId())
            .addValue("createdByName", command.createdByName())
            .addValue("printedAt", command.printedAt())
            .addValue("printedByUserId", command.printedByUserId())
            .addValue("printedByName", command.printedByName())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateReagentBag(UpdateReagentBagCommand command) {
        jdbcTemplate.update("""
            update medical_waste_reagent_bag
            set bag_name = :bagName,
                waste_type = :wasteType,
                weight_kg = :weightKg,
                volume_ml = :volumeMl,
                source = :source,
                remarks = :remarks,
                printed_at = :printedAt,
                printed_by_user_id = :printedByUserId,
                printed_by_name = :printedByName,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("bagName", command.bagName())
            .addValue("wasteType", command.wasteType())
            .addValue("weightKg", command.weightKg())
            .addValue("volumeMl", command.volumeMl())
            .addValue("source", command.source())
            .addValue("remarks", command.remarks())
            .addValue("printedAt", command.printedAt())
            .addValue("printedByUserId", command.printedByUserId())
            .addValue("printedByName", command.printedByName())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void handoverReagentBag(String bagId,
                                   String handedOverByUserId,
                                   String handedOverByName,
                                   LocalDateTime handedOverAt,
                                   String handoverRemarks,
                                   LocalDateTime updatedAt) {
        jdbcTemplate.update("""
            update medical_waste_reagent_bag
            set handed_over_at = :handedOverAt,
                handed_over_by_user_id = :handedOverByUserId,
                handed_over_by_name = :handedOverByName,
                handover_remarks = :handoverRemarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", bagId)
            .addValue("handedOverAt", handedOverAt)
            .addValue("handedOverByUserId", handedOverByUserId)
            .addValue("handedOverByName", handedOverByName)
            .addValue("handoverRemarks", handoverRemarks)
            .addValue("updatedAt", updatedAt));
    }

    private SpecimenBatch mapSpecimenBatch(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenBatch(
            rs.getString("id"),
            rs.getString("bag_name"),
            JdbcResultSetUtils.getNullableString(rs, "grossing_station_id"),
            rs.getString("grossing_station_name"),
            JdbcResultSetUtils.getNullableString(rs, "grossing_operator_id"),
            rs.getString("grossing_operator_name"),
            rs.getObject("grossing_date", LocalDate.class),
            rs.getString("grossing_period"),
            rs.getBigDecimal("weight_kg"),
            rs.getObject("label_count", Integer.class),
            JdbcResultSetUtils.getNullableLocalDateTime(rs, "printed_at"),
            rs.getString("printed_by_user_id"),
            rs.getString("printed_by_name"),
            JdbcResultSetUtils.getNullableLocalDateTime(rs, "destroyed_at"),
            JdbcResultSetUtils.getNullableString(rs, "destroyed_by_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "destroyed_by_name"));
    }

    private SpecimenPreviewLabel mapSpecimenPreviewLabel(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenPreviewLabel(
            rs.getString("source_label_id"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "patient_name"),
            JdbcResultSetUtils.getNullableString(rs, "pathology_no"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_name"));
    }

    private ReagentBag mapReagentBag(ResultSet rs, int rowNum) throws SQLException {
        return new ReagentBag(
            rs.getString("id"),
            rs.getString("bag_name"),
            rs.getString("waste_type"),
            rs.getBigDecimal("weight_kg"),
            rs.getBigDecimal("volume_ml"),
            JdbcResultSetUtils.getNullableString(rs, "source"),
            JdbcResultSetUtils.getNullableString(rs, "remarks"),
            JdbcResultSetUtils.getNullableLocalDateTime(rs, "created_at"),
            rs.getString("created_by_user_id"),
            rs.getString("created_by_name"),
            JdbcResultSetUtils.getNullableLocalDateTime(rs, "printed_at"),
            rs.getString("printed_by_user_id"),
            rs.getString("printed_by_name"),
            JdbcResultSetUtils.getNullableLocalDateTime(rs, "handed_over_at"),
            JdbcResultSetUtils.getNullableString(rs, "handed_over_by_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "handed_over_by_name"),
            JdbcResultSetUtils.getNullableString(rs, "handover_remarks"),
            JdbcResultSetUtils.getNullableLocalDateTime(rs, "updated_at"));
    }

    private String like(String keyword) {
        String normalized = blankToNull(keyword);
        return normalized == null ? null : "%" + normalized.toUpperCase() + "%";
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
