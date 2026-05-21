package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.OperationSupportRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcOperationSupportRepository implements OperationSupportRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOperationSupportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Reagent> findReagents(String keyword, Boolean enabled) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select id, reagent_code, reagent_name, specification, unit, manufacturer,
                   default_low_stock_threshold, default_near_expiry_days, enabled, remarks
            from reagents
            where (:enabled is null or enabled = :enabled)
              and (:keywordLike is null or upper(reagent_code) like :keywordLike or upper(reagent_name) like :keywordLike)
            order by reagent_code asc
            """, new MapSqlParameterSource()
            .addValue("enabled", enabled == null ? null : (enabled ? 1 : 0))
            .addValue("keywordLike", like), this::mapReagent);
    }

    @Override
    public Optional<Reagent> findReagentById(String reagentId) {
        return jdbcTemplate.query("""
            select id, reagent_code, reagent_name, specification, unit, manufacturer,
                   default_low_stock_threshold, default_near_expiry_days, enabled, remarks
            from reagents
            where id = :id
            """, Map.of("id", reagentId), this::mapReagent).stream().findFirst();
    }

    @Override
    public void insertReagent(CreateReagentCommand command) {
        jdbcTemplate.update("""
            insert into reagents
                (id, reagent_code, reagent_name, specification, unit, manufacturer,
                 default_low_stock_threshold, default_near_expiry_days, enabled, remarks, created_at, updated_at)
            values
                (:id, :reagentCode, :reagentName, :specification, :unit, :manufacturer,
                 :defaultLowStockThreshold, :defaultNearExpiryDays, :enabled, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reagentCode", command.reagentCode())
            .addValue("reagentName", command.reagentName())
            .addValue("specification", command.specification())
            .addValue("unit", command.unit())
            .addValue("manufacturer", command.manufacturer())
            .addValue("defaultLowStockThreshold", command.defaultLowStockThreshold())
            .addValue("defaultNearExpiryDays", command.defaultNearExpiryDays())
            .addValue("enabled", command.enabled() ? 1 : 0)
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateReagent(UpdateReagentCommand command) {
        jdbcTemplate.update("""
            update reagents
            set reagent_name = :reagentName,
                specification = :specification,
                unit = :unit,
                manufacturer = :manufacturer,
                default_low_stock_threshold = :defaultLowStockThreshold,
                default_near_expiry_days = :defaultNearExpiryDays,
                enabled = :enabled,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reagentName", command.reagentName())
            .addValue("specification", command.specification())
            .addValue("unit", command.unit())
            .addValue("manufacturer", command.manufacturer())
            .addValue("defaultLowStockThreshold", command.defaultLowStockThreshold())
            .addValue("defaultNearExpiryDays", command.defaultNearExpiryDays())
            .addValue("enabled", command.enabled() ? 1 : 0)
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public List<ReagentStock> findReagentStocks(String keyword, String stockStatus) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select rs.id, rs.reagent_id, r.reagent_code, r.reagent_name, rs.batch_no, rs.stock_quantity,
                   rs.stock_status, rs.expiry_date, rs.storage_location, rs.low_stock_threshold, rs.near_expiry_days, rs.remarks
            from reagent_stocks rs
            join reagents r on r.id = rs.reagent_id
            where (:stockStatus is null or rs.stock_status = :stockStatus)
              and (:keywordLike is null or upper(r.reagent_code) like :keywordLike
                   or upper(r.reagent_name) like :keywordLike
                   or upper(rs.batch_no) like :keywordLike)
            order by r.reagent_code asc, rs.batch_no asc
            """, new MapSqlParameterSource()
            .addValue("stockStatus", stockStatus)
            .addValue("keywordLike", like), this::mapReagentStock);
    }

    @Override
    public Optional<ReagentStock> findReagentStockById(String stockId) {
        return jdbcTemplate.query("""
            select rs.id, rs.reagent_id, r.reagent_code, r.reagent_name, rs.batch_no, rs.stock_quantity,
                   rs.stock_status, rs.expiry_date, rs.storage_location, rs.low_stock_threshold, rs.near_expiry_days, rs.remarks
            from reagent_stocks rs
            join reagents r on r.id = rs.reagent_id
            where rs.id = :id
            """, Map.of("id", stockId), this::mapReagentStock).stream().findFirst();
    }

    @Override
    public void insertReagentStock(CreateReagentStockCommand command) {
        jdbcTemplate.update("""
            insert into reagent_stocks
                (id, reagent_id, batch_no, stock_quantity, stock_status, expiry_date, storage_location,
                 low_stock_threshold, near_expiry_days, remarks, created_at, updated_at)
            values
                (:id, :reagentId, :batchNo, :stockQuantity, :stockStatus, :expiryDate, :storageLocation,
                 :lowStockThreshold, :nearExpiryDays, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reagentId", command.reagentId())
            .addValue("batchNo", command.batchNo())
            .addValue("stockQuantity", command.stockQuantity())
            .addValue("stockStatus", command.stockStatus())
            .addValue("expiryDate", command.expiryDate())
            .addValue("storageLocation", command.storageLocation())
            .addValue("lowStockThreshold", command.lowStockThreshold())
            .addValue("nearExpiryDays", command.nearExpiryDays())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateReagentStock(UpdateReagentStockCommand command) {
        jdbcTemplate.update("""
            update reagent_stocks
            set stock_quantity = :stockQuantity,
                stock_status = :stockStatus,
                expiry_date = :expiryDate,
                storage_location = :storageLocation,
                low_stock_threshold = :lowStockThreshold,
                near_expiry_days = :nearExpiryDays,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("stockQuantity", command.stockQuantity())
            .addValue("stockStatus", command.stockStatus())
            .addValue("expiryDate", command.expiryDate())
            .addValue("storageLocation", command.storageLocation())
            .addValue("lowStockThreshold", command.lowStockThreshold())
            .addValue("nearExpiryDays", command.nearExpiryDays())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public List<ReagentWarning> findReagentWarnings(LocalDate today) {
        return jdbcTemplate.query("""
            select rs.id as stock_id, r.reagent_code, r.reagent_name, rs.batch_no, rs.stock_quantity,
                   coalesce(rs.low_stock_threshold, r.default_low_stock_threshold) as low_stock_threshold,
                   rs.expiry_date,
                   coalesce(rs.near_expiry_days, r.default_near_expiry_days) as near_expiry_days
            from reagent_stocks rs
            join reagents r on r.id = rs.reagent_id
            where (
                coalesce(rs.low_stock_threshold, r.default_low_stock_threshold) is not null
                and rs.stock_quantity <= coalesce(rs.low_stock_threshold, r.default_low_stock_threshold)
            )
            or (
                rs.expiry_date is not null
                and coalesce(rs.near_expiry_days, r.default_near_expiry_days) is not null
                and rs.expiry_date <= :warningDate
            )
            order by r.reagent_code asc, rs.batch_no asc
            """, new MapSqlParameterSource()
            .addValue("warningDate", Date.valueOf(today.plusDays(3650))), (rs, rowNum) -> {
            String warningType;
            BigDecimal threshold = rs.getBigDecimal("low_stock_threshold");
            LocalDate expiryDate = toLocalDate(rs.getDate("expiry_date"));
            Integer nearExpiryDays = rs.getObject("near_expiry_days") == null ? null : rs.getInt("near_expiry_days");
            if (threshold != null && rs.getBigDecimal("stock_quantity").compareTo(threshold) <= 0) {
                warningType = "LOW_STOCK";
            } else if (expiryDate != null && nearExpiryDays != null && !expiryDate.isAfter(today.plusDays(nearExpiryDays))) {
                warningType = "NEAR_EXPIRY";
            } else {
                warningType = "NEAR_EXPIRY";
            }
            return new ReagentWarning(
                rs.getString("stock_id"),
                rs.getString("reagent_code"),
                rs.getString("reagent_name"),
                rs.getString("batch_no"),
                warningType,
                rs.getBigDecimal("stock_quantity"),
                threshold,
                expiryDate,
                nearExpiryDays);
        }).stream().filter(item -> {
            if ("LOW_STOCK".equals(item.warningType())) {
                return true;
            }
            return item.expiryDate() != null && item.nearExpiryDays() != null
                && !item.expiryDate().isAfter(today.plusDays(item.nearExpiryDays()));
        }).toList();
    }

    @Override
    public List<EquipmentRecord> findEquipmentRecords(String keyword, String equipmentStatus) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select id, equipment_code, equipment_name, equipment_category, model_no, equipment_status,
                   location_description, enabled_at, next_maintenance_at, remarks
            from equipment_records
            where (:equipmentStatus is null or equipment_status = :equipmentStatus)
              and (:keywordLike is null or upper(equipment_code) like :keywordLike
                   or upper(equipment_name) like :keywordLike)
            order by equipment_code asc
            """, new MapSqlParameterSource()
            .addValue("equipmentStatus", equipmentStatus)
            .addValue("keywordLike", like), this::mapEquipmentRecord);
    }

    @Override
    public Optional<EquipmentRecord> findEquipmentRecordById(String equipmentId) {
        return jdbcTemplate.query("""
            select id, equipment_code, equipment_name, equipment_category, model_no, equipment_status,
                   location_description, enabled_at, next_maintenance_at, remarks
            from equipment_records
            where id = :id
            """, Map.of("id", equipmentId), this::mapEquipmentRecord).stream().findFirst();
    }

    @Override
    public void insertEquipmentRecord(CreateEquipmentRecordCommand command) {
        jdbcTemplate.update("""
            insert into equipment_records
                (id, equipment_code, equipment_name, equipment_category, model_no, equipment_status,
                 location_description, enabled_at, next_maintenance_at, remarks, created_at, updated_at)
            values
                (:id, :equipmentCode, :equipmentName, :equipmentCategory, :modelNo, :equipmentStatus,
                 :locationDescription, :enabledAt, :nextMaintenanceAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("equipmentCode", command.equipmentCode())
            .addValue("equipmentName", command.equipmentName())
            .addValue("equipmentCategory", command.equipmentCategory())
            .addValue("modelNo", command.modelNo())
            .addValue("equipmentStatus", command.equipmentStatus())
            .addValue("locationDescription", command.locationDescription())
            .addValue("enabledAt", command.enabledAt())
            .addValue("nextMaintenanceAt", command.nextMaintenanceAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateEquipmentRecord(UpdateEquipmentRecordCommand command) {
        jdbcTemplate.update("""
            update equipment_records
            set equipment_name = :equipmentName,
                equipment_category = :equipmentCategory,
                model_no = :modelNo,
                equipment_status = :equipmentStatus,
                location_description = :locationDescription,
                enabled_at = :enabledAt,
                next_maintenance_at = :nextMaintenanceAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("equipmentName", command.equipmentName())
            .addValue("equipmentCategory", command.equipmentCategory())
            .addValue("modelNo", command.modelNo())
            .addValue("equipmentStatus", command.equipmentStatus())
            .addValue("locationDescription", command.locationDescription())
            .addValue("enabledAt", command.enabledAt())
            .addValue("nextMaintenanceAt", command.nextMaintenanceAt())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public List<EquipmentMaintenanceLog> findEquipmentMaintenanceLogs(String equipmentId) {
        return jdbcTemplate.query("""
            select id, equipment_id, maintenance_type, maintenance_status, performed_at,
                   performed_by_user_id, performed_by_name, description, next_maintenance_at, remarks
            from equipment_maintenance_logs
            where equipment_id = :equipmentId
            order by performed_at desc, created_at desc
            """, Map.of("equipmentId", equipmentId), this::mapEquipmentMaintenanceLog);
    }

    @Override
    public void insertEquipmentMaintenanceLog(CreateEquipmentMaintenanceLogCommand command) {
        jdbcTemplate.update("""
            insert into equipment_maintenance_logs
                (id, equipment_id, maintenance_type, maintenance_status, performed_at,
                 performed_by_user_id, performed_by_name, description, next_maintenance_at, remarks, created_at, updated_at)
            values
                (:id, :equipmentId, :maintenanceType, :maintenanceStatus, :performedAt,
                 :performedByUserId, :performedByName, :description, :nextMaintenanceAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("equipmentId", command.equipmentId())
            .addValue("maintenanceType", command.maintenanceType())
            .addValue("maintenanceStatus", command.maintenanceStatus())
            .addValue("performedAt", command.performedAt())
            .addValue("performedByUserId", command.performedByUserId())
            .addValue("performedByName", command.performedByName())
            .addValue("description", command.description())
            .addValue("nextMaintenanceAt", command.nextMaintenanceAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public List<EquipmentWarning> findEquipmentWarnings(LocalDateTime now, LocalDateTime dueSoonThreshold) {
        return jdbcTemplate.query("""
            select id, equipment_code, equipment_name, next_maintenance_at, equipment_status
            from equipment_records
            where next_maintenance_at is not null
              and next_maintenance_at <= :dueSoonThreshold
            order by next_maintenance_at asc, equipment_code asc
            """, new MapSqlParameterSource()
            .addValue("dueSoonThreshold", dueSoonThreshold), (rs, rowNum) -> {
            LocalDateTime nextMaintenanceAt = toLocalDateTime(rs.getTimestamp("next_maintenance_at"));
            String warningType = nextMaintenanceAt != null && !nextMaintenanceAt.isAfter(now) ? "OVERDUE" : "DUE_SOON";
            return new EquipmentWarning(
                rs.getString("id"),
                rs.getString("equipment_code"),
                rs.getString("equipment_name"),
                warningType,
                nextMaintenanceAt,
                rs.getString("equipment_status"));
        });
    }

    private Reagent mapReagent(ResultSet rs, int rowNum) throws SQLException {
        return new Reagent(
            rs.getString("id"),
            rs.getString("reagent_code"),
            rs.getString("reagent_name"),
            rs.getString("specification"),
            rs.getString("unit"),
            rs.getString("manufacturer"),
            rs.getBigDecimal("default_low_stock_threshold"),
            rs.getObject("default_near_expiry_days") == null ? null : rs.getInt("default_near_expiry_days"),
            rs.getInt("enabled") == 1,
            rs.getString("remarks"));
    }

    private ReagentStock mapReagentStock(ResultSet rs, int rowNum) throws SQLException {
        return new ReagentStock(
            rs.getString("id"),
            rs.getString("reagent_id"),
            rs.getString("reagent_code"),
            rs.getString("reagent_name"),
            rs.getString("batch_no"),
            rs.getBigDecimal("stock_quantity"),
            rs.getString("stock_status"),
            toLocalDate(rs.getDate("expiry_date")),
            rs.getString("storage_location"),
            rs.getBigDecimal("low_stock_threshold"),
            rs.getObject("near_expiry_days") == null ? null : rs.getInt("near_expiry_days"),
            rs.getString("remarks"));
    }

    private EquipmentRecord mapEquipmentRecord(ResultSet rs, int rowNum) throws SQLException {
        return new EquipmentRecord(
            rs.getString("id"),
            rs.getString("equipment_code"),
            rs.getString("equipment_name"),
            rs.getString("equipment_category"),
            rs.getString("model_no"),
            rs.getString("equipment_status"),
            rs.getString("location_description"),
            toLocalDateTime(rs.getTimestamp("enabled_at")),
            toLocalDateTime(rs.getTimestamp("next_maintenance_at")),
            rs.getString("remarks"));
    }

    private EquipmentMaintenanceLog mapEquipmentMaintenanceLog(ResultSet rs, int rowNum) throws SQLException {
        return new EquipmentMaintenanceLog(
            rs.getString("id"),
            rs.getString("equipment_id"),
            rs.getString("maintenance_type"),
            rs.getString("maintenance_status"),
            toLocalDateTime(rs.getTimestamp("performed_at")),
            rs.getString("performed_by_user_id"),
            rs.getString("performed_by_name"),
            rs.getString("description"),
            toLocalDateTime(rs.getTimestamp("next_maintenance_at")),
            rs.getString("remarks"));
    }

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
