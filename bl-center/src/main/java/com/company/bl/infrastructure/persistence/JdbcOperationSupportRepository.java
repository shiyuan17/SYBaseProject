package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.OperationSupportRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    public List<Reagent> findReagents(String keyword, Boolean enabled, String reagentType, String templateStatus) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select r.id, r.reagent_code, r.reagent_name, r.specification, r.unit, r.manufacturer,
                   r.reagent_type, r.reagent_usage, r.order_dict_item_id, moi.order_item_name,
                   r.clone_no, r.recommended_dilution, r.application_dilution, r.template_status, r.validity_days,
                   r.default_low_stock_threshold, r.default_stock_threshold, r.default_near_expiry_days,
                   r.stain_capacity, r.stain_threshold, r.enabled, r.created_at, r.updated_at,
                   r.created_by_user_id, r.created_by_name, r.updated_by_user_id, r.updated_by_name, r.remarks
            from reagents r
            left join medical_order_dict_items moi on moi.id = r.order_dict_item_id
            where (:enabled is null or r.enabled = :enabled)
              and (:reagentType is null or r.reagent_type = :reagentType)
              and (:templateStatus is null or r.template_status = :templateStatus)
              and coalesce(r.template_status, 'ENABLED') <> 'DELETED'
              and (:keywordLike is null or upper(r.reagent_code) like :keywordLike
                   or upper(r.reagent_name) like :keywordLike
                   or upper(coalesce(moi.order_item_name, '')) like :keywordLike)
            order by r.reagent_code asc
            """, new MapSqlParameterSource()
            .addValue("enabled", enabled == null ? null : (enabled ? 1 : 0))
            .addValue("reagentType", JdbcOperationSupportRowMappers.blankToNull(reagentType))
            .addValue("templateStatus", JdbcOperationSupportRowMappers.blankToNull(templateStatus))
            .addValue("keywordLike", like), JdbcOperationSupportRowMappers::mapReagent);
    }

    @Override
    public Optional<Reagent> findReagentById(String reagentId) {
        return jdbcTemplate.query("""
            select r.id, r.reagent_code, r.reagent_name, r.specification, r.unit, r.manufacturer,
                   r.reagent_type, r.reagent_usage, r.order_dict_item_id, moi.order_item_name,
                   r.clone_no, r.recommended_dilution, r.application_dilution, r.template_status, r.validity_days,
                   r.default_low_stock_threshold, r.default_stock_threshold, r.default_near_expiry_days,
                   r.stain_capacity, r.stain_threshold, r.enabled, r.created_at, r.updated_at,
                   r.created_by_user_id, r.created_by_name, r.updated_by_user_id, r.updated_by_name, r.remarks
            from reagents r
            left join medical_order_dict_items moi on moi.id = r.order_dict_item_id
            where r.id = :id
            """, Map.of("id", reagentId), JdbcOperationSupportRowMappers::mapReagent).stream().findFirst();
    }

    @Override
    public Optional<Reagent> findReagentByCodeOrName(String reagentCode, String reagentName) {
        return jdbcTemplate.query("""
            select r.id, r.reagent_code, r.reagent_name, r.specification, r.unit, r.manufacturer,
                   r.reagent_type, r.reagent_usage, r.order_dict_item_id, moi.order_item_name,
                   r.clone_no, r.recommended_dilution, r.application_dilution, r.template_status, r.validity_days,
                   r.default_low_stock_threshold, r.default_stock_threshold, r.default_near_expiry_days,
                   r.stain_capacity, r.stain_threshold, r.enabled, r.created_at, r.updated_at,
                   r.created_by_user_id, r.created_by_name, r.updated_by_user_id, r.updated_by_name, r.remarks
            from reagents r
            left join medical_order_dict_items moi on moi.id = r.order_dict_item_id
            where (:reagentCode is not null and r.reagent_code = :reagentCode)
               or (:reagentName is not null and r.reagent_name = :reagentName)
            order by case when r.reagent_code = :reagentCode then 0 else 1 end
            """, new MapSqlParameterSource()
            .addValue("reagentCode", JdbcOperationSupportRowMappers.blankToNull(reagentCode))
            .addValue("reagentName", JdbcOperationSupportRowMappers.blankToNull(reagentName)), JdbcOperationSupportRowMappers::mapReagent).stream().findFirst();
    }

    @Override
    public boolean existsMedicalOrderItem(String orderDictItemId) {
        if (JdbcOperationSupportRowMappers.blankToNull(orderDictItemId) == null) {
            return true;
        }
        return !jdbcTemplate.query("""
            select id
            from medical_order_dict_items
            where id = :id
            """, Map.of("id", orderDictItemId), (rs, rowNum) -> rs.getString("id")).isEmpty();
    }

    @Override
    public void insertReagent(CreateReagentCommand command) {
        jdbcTemplate.update("""
            insert into reagents
                (id, reagent_code, reagent_name, specification, unit, manufacturer,
                 reagent_type, reagent_usage, order_dict_item_id, clone_no, recommended_dilution, application_dilution,
                 template_status, validity_days, default_low_stock_threshold, default_stock_threshold,
                 default_near_expiry_days, stain_capacity, stain_threshold, enabled, remarks,
                 created_by_user_id, created_by_name, updated_by_user_id, updated_by_name, created_at, updated_at)
            values
                (:id, :reagentCode, :reagentName, :specification, :unit, :manufacturer,
                 :reagentType, :reagentUsage, :orderDictItemId, :cloneNo, :recommendedDilution, :applicationDilution,
                 :templateStatus, :validityDays, :defaultLowStockThreshold, :defaultStockThreshold,
                 :defaultNearExpiryDays, :stainCapacity, :stainThreshold, :enabled, :remarks,
                 :createdByUserId, :createdByName, :updatedByUserId, :updatedByName, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reagentCode", command.reagentCode())
            .addValue("reagentName", command.reagentName())
            .addValue("specification", command.specification())
            .addValue("unit", command.unit())
            .addValue("manufacturer", command.manufacturer())
            .addValue("reagentType", command.reagentType())
            .addValue("reagentUsage", command.reagentUsage())
            .addValue("orderDictItemId", command.orderDictItemId())
            .addValue("cloneNo", command.cloneNo())
            .addValue("recommendedDilution", command.recommendedDilution())
            .addValue("applicationDilution", command.applicationDilution())
            .addValue("templateStatus", command.templateStatus())
            .addValue("validityDays", command.validityDays())
            .addValue("defaultLowStockThreshold", command.defaultLowStockThreshold())
            .addValue("defaultStockThreshold", command.defaultStockThreshold())
            .addValue("defaultNearExpiryDays", command.defaultNearExpiryDays())
            .addValue("stainCapacity", command.stainCapacity())
            .addValue("stainThreshold", command.stainThreshold())
            .addValue("enabled", command.enabled() ? 1 : 0)
            .addValue("remarks", command.remarks())
            .addValue("createdByUserId", command.createdByUserId())
            .addValue("createdByName", command.createdByName())
            .addValue("updatedByUserId", command.updatedByUserId())
            .addValue("updatedByName", command.updatedByName())
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
                reagent_type = :reagentType,
                reagent_usage = :reagentUsage,
                order_dict_item_id = :orderDictItemId,
                clone_no = :cloneNo,
                recommended_dilution = :recommendedDilution,
                application_dilution = :applicationDilution,
                template_status = :templateStatus,
                validity_days = :validityDays,
                default_low_stock_threshold = :defaultLowStockThreshold,
                default_stock_threshold = :defaultStockThreshold,
                default_near_expiry_days = :defaultNearExpiryDays,
                stain_capacity = :stainCapacity,
                stain_threshold = :stainThreshold,
                enabled = :enabled,
                remarks = :remarks,
                updated_by_user_id = :updatedByUserId,
                updated_by_name = :updatedByName,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reagentName", command.reagentName())
            .addValue("specification", command.specification())
            .addValue("unit", command.unit())
            .addValue("manufacturer", command.manufacturer())
            .addValue("reagentType", command.reagentType())
            .addValue("reagentUsage", command.reagentUsage())
            .addValue("orderDictItemId", command.orderDictItemId())
            .addValue("cloneNo", command.cloneNo())
            .addValue("recommendedDilution", command.recommendedDilution())
            .addValue("applicationDilution", command.applicationDilution())
            .addValue("templateStatus", command.templateStatus())
            .addValue("validityDays", command.validityDays())
            .addValue("defaultLowStockThreshold", command.defaultLowStockThreshold())
            .addValue("defaultStockThreshold", command.defaultStockThreshold())
            .addValue("defaultNearExpiryDays", command.defaultNearExpiryDays())
            .addValue("stainCapacity", command.stainCapacity())
            .addValue("stainThreshold", command.stainThreshold())
            .addValue("enabled", command.enabled() ? 1 : 0)
            .addValue("remarks", command.remarks())
            .addValue("updatedByUserId", command.updatedByUserId())
            .addValue("updatedByName", command.updatedByName())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public List<ReagentStock> findReagentStocks(String keyword, String stockStatus, String reagentType, LocalDate dateFrom, LocalDate dateTo) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select rs.id, rs.reagent_id, r.reagent_code, r.reagent_name, r.reagent_type,
                   r.order_dict_item_id, moi.order_item_name, rs.batch_no, rs.initial_quantity,
                   rs.stock_quantity, rs.remaining_quantity, rs.stock_status, rs.production_date, rs.inbound_at,
                   rs.expiry_date, rs.storage_location, rs.low_stock_threshold, rs.near_expiry_days,
                   rs.test_reminder_threshold, rs.expiry_reminder_threshold, rs.recommended_dilution,
                   rs.application_dilution, rs.stain_capacity, rs.stain_threshold, rs.validity_days,
                   rs.tested_at, rs.started_at, rs.finished_at, rs.created_at, rs.updated_at,
                   rs.created_by_user_id, rs.created_by_name, rs.updated_by_user_id, rs.updated_by_name, rs.remarks
            from reagent_stocks rs
            join reagents r on r.id = rs.reagent_id
            left join medical_order_dict_items moi on moi.id = r.order_dict_item_id
            where (:stockStatus is null or rs.stock_status = :stockStatus)
              and (:reagentType is null or r.reagent_type = :reagentType)
              and (:dateFrom is null or cast(rs.inbound_at as date) >= :dateFrom)
              and (:dateTo is null or cast(rs.inbound_at as date) <= :dateTo)
              and (:keywordLike is null or upper(r.reagent_code) like :keywordLike
                   or upper(r.reagent_name) like :keywordLike
                   or upper(rs.batch_no) like :keywordLike
                   or upper(coalesce(moi.order_item_name, '')) like :keywordLike)
            order by r.reagent_code asc, rs.batch_no asc
            """, new MapSqlParameterSource()
            .addValue("stockStatus", JdbcOperationSupportRowMappers.blankToNull(stockStatus))
            .addValue("reagentType", JdbcOperationSupportRowMappers.blankToNull(reagentType))
            .addValue("dateFrom", dateFrom)
            .addValue("dateTo", dateTo)
            .addValue("keywordLike", like), JdbcOperationSupportRowMappers::mapReagentStock);
    }

    @Override
    public Optional<ReagentStock> findReagentStockById(String stockId) {
        return jdbcTemplate.query("""
            select rs.id, rs.reagent_id, r.reagent_code, r.reagent_name, r.reagent_type,
                   r.order_dict_item_id, moi.order_item_name, rs.batch_no, rs.initial_quantity,
                   rs.stock_quantity, rs.remaining_quantity, rs.stock_status, rs.production_date, rs.inbound_at,
                   rs.expiry_date, rs.storage_location, rs.low_stock_threshold, rs.near_expiry_days,
                   rs.test_reminder_threshold, rs.expiry_reminder_threshold, rs.recommended_dilution,
                   rs.application_dilution, rs.stain_capacity, rs.stain_threshold, rs.validity_days,
                   rs.tested_at, rs.started_at, rs.finished_at, rs.created_at, rs.updated_at,
                   rs.created_by_user_id, rs.created_by_name, rs.updated_by_user_id, rs.updated_by_name, rs.remarks
            from reagent_stocks rs
            join reagents r on r.id = rs.reagent_id
            left join medical_order_dict_items moi on moi.id = r.order_dict_item_id
            where rs.id = :id
            """, Map.of("id", stockId), JdbcOperationSupportRowMappers::mapReagentStock).stream().findFirst();
    }

    @Override
    public void insertReagentStock(CreateReagentStockCommand command) {
        jdbcTemplate.update("""
            insert into reagent_stocks
                (id, reagent_id, batch_no, initial_quantity, stock_quantity, remaining_quantity, stock_status,
                 production_date, inbound_at, expiry_date, storage_location, low_stock_threshold, near_expiry_days,
                 test_reminder_threshold, expiry_reminder_threshold, recommended_dilution, application_dilution,
                 stain_capacity, stain_threshold, validity_days, remarks, created_by_user_id, created_by_name,
                 updated_by_user_id, updated_by_name, created_at, updated_at)
            values
                (:id, :reagentId, :batchNo, :initialQuantity, :stockQuantity, :remainingQuantity, :stockStatus,
                 :productionDate, :inboundAt, :expiryDate, :storageLocation, :lowStockThreshold, :nearExpiryDays,
                 :testReminderThreshold, :expiryReminderThreshold, :recommendedDilution, :applicationDilution,
                 :stainCapacity, :stainThreshold, :validityDays, :remarks, :createdByUserId, :createdByName,
                 :updatedByUserId, :updatedByName, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reagentId", command.reagentId())
            .addValue("batchNo", command.batchNo())
            .addValue("initialQuantity", command.initialQuantity())
            .addValue("stockQuantity", command.stockQuantity())
            .addValue("remainingQuantity", command.remainingQuantity())
            .addValue("stockStatus", command.stockStatus())
            .addValue("productionDate", command.productionDate())
            .addValue("inboundAt", command.inboundAt())
            .addValue("expiryDate", command.expiryDate())
            .addValue("storageLocation", command.storageLocation())
            .addValue("lowStockThreshold", command.lowStockThreshold())
            .addValue("nearExpiryDays", command.nearExpiryDays())
            .addValue("testReminderThreshold", command.testReminderThreshold())
            .addValue("expiryReminderThreshold", command.expiryReminderThreshold())
            .addValue("recommendedDilution", command.recommendedDilution())
            .addValue("applicationDilution", command.applicationDilution())
            .addValue("stainCapacity", command.stainCapacity())
            .addValue("stainThreshold", command.stainThreshold())
            .addValue("validityDays", command.validityDays())
            .addValue("remarks", command.remarks())
            .addValue("createdByUserId", command.createdByUserId())
            .addValue("createdByName", command.createdByName())
            .addValue("updatedByUserId", command.updatedByUserId())
            .addValue("updatedByName", command.updatedByName())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateReagentStock(UpdateReagentStockCommand command) {
        jdbcTemplate.update("""
            update reagent_stocks
            set initial_quantity = :initialQuantity,
                stock_quantity = :stockQuantity,
                remaining_quantity = :remainingQuantity,
                stock_status = :stockStatus,
                production_date = :productionDate,
                inbound_at = :inboundAt,
                expiry_date = :expiryDate,
                storage_location = :storageLocation,
                low_stock_threshold = :lowStockThreshold,
                near_expiry_days = :nearExpiryDays,
                test_reminder_threshold = :testReminderThreshold,
                expiry_reminder_threshold = :expiryReminderThreshold,
                recommended_dilution = :recommendedDilution,
                application_dilution = :applicationDilution,
                stain_capacity = :stainCapacity,
                stain_threshold = :stainThreshold,
                validity_days = :validityDays,
                remarks = :remarks,
                updated_by_user_id = :updatedByUserId,
                updated_by_name = :updatedByName,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("initialQuantity", command.initialQuantity())
            .addValue("stockQuantity", command.stockQuantity())
            .addValue("remainingQuantity", command.remainingQuantity())
            .addValue("stockStatus", command.stockStatus())
            .addValue("productionDate", command.productionDate())
            .addValue("inboundAt", command.inboundAt())
            .addValue("expiryDate", command.expiryDate())
            .addValue("storageLocation", command.storageLocation())
            .addValue("lowStockThreshold", command.lowStockThreshold())
            .addValue("nearExpiryDays", command.nearExpiryDays())
            .addValue("testReminderThreshold", command.testReminderThreshold())
            .addValue("expiryReminderThreshold", command.expiryReminderThreshold())
            .addValue("recommendedDilution", command.recommendedDilution())
            .addValue("applicationDilution", command.applicationDilution())
            .addValue("stainCapacity", command.stainCapacity())
            .addValue("stainThreshold", command.stainThreshold())
            .addValue("validityDays", command.validityDays())
            .addValue("remarks", command.remarks())
            .addValue("updatedByUserId", command.updatedByUserId())
            .addValue("updatedByName", command.updatedByName())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateReagentStockState(UpdateReagentStockStateCommand command) {
        jdbcTemplate.update("""
            update reagent_stocks
            set stock_quantity = :stockQuantity,
                remaining_quantity = :remainingQuantity,
                stock_status = :stockStatus,
                tested_at = coalesce(:testedAt, tested_at),
                started_at = coalesce(:startedAt, started_at),
                finished_at = coalesce(:finishedAt, finished_at),
                remarks = :remarks,
                updated_by_user_id = :updatedByUserId,
                updated_by_name = :updatedByName,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("stockQuantity", command.stockQuantity())
            .addValue("remainingQuantity", command.remainingQuantity())
            .addValue("stockStatus", command.stockStatus())
            .addValue("testedAt", command.testedAt())
            .addValue("startedAt", command.startedAt())
            .addValue("finishedAt", command.finishedAt())
            .addValue("remarks", command.remarks())
            .addValue("updatedByUserId", command.updatedByUserId())
            .addValue("updatedByName", command.updatedByName())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void insertReagentStockEvent(CreateReagentStockEventCommand command) {
        jdbcTemplate.update("""
            insert into reagent_stock_events
                (id, stock_id, event_type, quantity_delta, quantity_before, quantity_after,
                 occurred_at, operator_user_id, operator_name, remarks, created_at)
            values
                (:id, :stockId, :eventType, :quantityDelta, :quantityBefore, :quantityAfter,
                 :occurredAt, :operatorUserId, :operatorName, :remarks, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("stockId", command.stockId())
            .addValue("eventType", command.eventType())
            .addValue("quantityDelta", command.quantityDelta())
            .addValue("quantityBefore", command.quantityBefore())
            .addValue("quantityAfter", command.quantityAfter())
            .addValue("occurredAt", command.occurredAt())
            .addValue("operatorUserId", command.operatorUserId())
            .addValue("operatorName", command.operatorName())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt()));
    }

    @Override
    public List<ReagentStockEvent> findReagentStockEvents(String stockId) {
        return jdbcTemplate.query("""
            select id, stock_id, event_type, quantity_delta, quantity_before, quantity_after,
                   occurred_at, operator_user_id, operator_name, remarks
            from reagent_stock_events
            where stock_id = :stockId
            order by occurred_at desc, created_at desc
            """, Map.of("stockId", stockId), JdbcOperationSupportRowMappers::mapReagentStockEvent);
    }

    @Override
    public List<ReagentWarning> findReagentWarnings(LocalDate today) {
        return jdbcTemplate.query("""
            select rs.id as stock_id, r.reagent_code, r.reagent_name, rs.batch_no,
                   coalesce(rs.remaining_quantity, rs.stock_quantity) as stock_quantity,
                   coalesce(rs.low_stock_threshold, r.default_stock_threshold, r.default_low_stock_threshold) as low_stock_threshold,
                   rs.expiry_date,
                   coalesce(rs.near_expiry_days, rs.expiry_reminder_threshold, r.default_near_expiry_days) as near_expiry_days
            from reagent_stocks rs
            join reagents r on r.id = rs.reagent_id
            where rs.stock_status not in ('DISABLED', 'FINISHED')
              and (
                (
                    coalesce(rs.low_stock_threshold, r.default_stock_threshold, r.default_low_stock_threshold) is not null
                    and coalesce(rs.remaining_quantity, rs.stock_quantity) <= coalesce(rs.low_stock_threshold, r.default_stock_threshold, r.default_low_stock_threshold)
                )
                or (
                    rs.expiry_date is not null
                    and coalesce(rs.near_expiry_days, rs.expiry_reminder_threshold, r.default_near_expiry_days) is not null
                    and rs.expiry_date <= :warningDate
                )
            )
            order by r.reagent_code asc, rs.batch_no asc
            """, new MapSqlParameterSource()
            .addValue("warningDate", Date.valueOf(today.plusDays(3650))), (rs, rowNum) -> {
            String warningType;
            BigDecimal threshold = rs.getBigDecimal("low_stock_threshold");
            LocalDate expiryDate = JdbcOperationSupportRowMappers.toLocalDate(rs.getDate("expiry_date"));
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
                   location_description, enabled_at, next_maintenance_at, quantity, purchase_date,
                   purchaser_name, purchaser_code, management_unit, management_code, use_unit,
                   principal_code, principal_name, user_name, production_date, warranty_end_date,
                   factory_no, depreciation_method, service_life_years, price, manufacturer,
                   port_no, ip_address, common_startup_time, common_shutdown_time, common_usage_content,
                   commonly_used, set_temperature, current_temperature, rfid, remarks
            from equipment_records
            where (:equipmentStatus is null or equipment_status = :equipmentStatus)
              and (:keywordLike is null or upper(equipment_code) like :keywordLike
                   or upper(equipment_name) like :keywordLike
                   or upper(coalesce(management_code, '')) like :keywordLike
                   or upper(coalesce(model_no, '')) like :keywordLike
                   or upper(coalesce(manufacturer, '')) like :keywordLike
                   or upper(coalesce(rfid, '')) like :keywordLike)
            order by equipment_code asc
            """, new MapSqlParameterSource()
            .addValue("equipmentStatus", equipmentStatus)
            .addValue("keywordLike", like), JdbcOperationSupportRowMappers::mapEquipmentRecord);
    }

    @Override
    public Optional<EquipmentRecord> findEquipmentRecordById(String equipmentId) {
        return jdbcTemplate.query("""
            select id, equipment_code, equipment_name, equipment_category, model_no, equipment_status,
                   location_description, enabled_at, next_maintenance_at, quantity, purchase_date,
                   purchaser_name, purchaser_code, management_unit, management_code, use_unit,
                   principal_code, principal_name, user_name, production_date, warranty_end_date,
                   factory_no, depreciation_method, service_life_years, price, manufacturer,
                   port_no, ip_address, common_startup_time, common_shutdown_time, common_usage_content,
                   commonly_used, set_temperature, current_temperature, rfid, remarks
            from equipment_records
            where id = :id
            """, Map.of("id", equipmentId), JdbcOperationSupportRowMappers::mapEquipmentRecord).stream().findFirst();
    }

    @Override
    public void insertEquipmentRecord(CreateEquipmentRecordCommand command) {
        jdbcTemplate.update("""
            insert into equipment_records
                (id, equipment_code, equipment_name, equipment_category, model_no, equipment_status,
                 location_description, enabled_at, next_maintenance_at, quantity, purchase_date,
                 purchaser_name, purchaser_code, management_unit, management_code, use_unit,
                 principal_code, principal_name, user_name, production_date, warranty_end_date,
                 factory_no, depreciation_method, service_life_years, price, manufacturer,
                 port_no, ip_address, common_startup_time, common_shutdown_time, common_usage_content,
                 commonly_used, set_temperature, current_temperature, rfid, remarks, created_at, updated_at)
            values
                (:id, :equipmentCode, :equipmentName, :equipmentCategory, :modelNo, :equipmentStatus,
                 :locationDescription, :enabledAt, :nextMaintenanceAt, :quantity, :purchaseDate,
                 :purchaserName, :purchaserCode, :managementUnit, :managementCode, :useUnit,
                 :principalCode, :principalName, :userName, :productionDate, :warrantyEndDate,
                 :factoryNo, :depreciationMethod, :serviceLifeYears, :price, :manufacturer,
                 :portNo, :ipAddress, :commonStartupTime, :commonShutdownTime, :commonUsageContent,
                 :commonlyUsed, :setTemperature, :currentTemperature, :rfid, :remarks, :createdAt, :updatedAt)
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
            .addValue("quantity", command.quantity())
            .addValue("purchaseDate", command.purchaseDate())
            .addValue("purchaserName", command.purchaserName())
            .addValue("purchaserCode", command.purchaserCode())
            .addValue("managementUnit", command.managementUnit())
            .addValue("managementCode", command.managementCode())
            .addValue("useUnit", command.useUnit())
            .addValue("principalCode", command.principalCode())
            .addValue("principalName", command.principalName())
            .addValue("userName", command.userName())
            .addValue("productionDate", command.productionDate())
            .addValue("warrantyEndDate", command.warrantyEndDate())
            .addValue("factoryNo", command.factoryNo())
            .addValue("depreciationMethod", command.depreciationMethod())
            .addValue("serviceLifeYears", command.serviceLifeYears())
            .addValue("price", command.price())
            .addValue("manufacturer", command.manufacturer())
            .addValue("portNo", command.portNo())
            .addValue("ipAddress", command.ipAddress())
            .addValue("commonStartupTime", command.commonStartupTime())
            .addValue("commonShutdownTime", command.commonShutdownTime())
            .addValue("commonUsageContent", command.commonUsageContent())
            .addValue("commonlyUsed", command.commonlyUsed() ? 1 : 0)
            .addValue("setTemperature", command.setTemperature())
            .addValue("currentTemperature", command.currentTemperature())
            .addValue("rfid", command.rfid())
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
                quantity = :quantity,
                purchase_date = :purchaseDate,
                purchaser_name = :purchaserName,
                purchaser_code = :purchaserCode,
                management_unit = :managementUnit,
                management_code = :managementCode,
                use_unit = :useUnit,
                principal_code = :principalCode,
                principal_name = :principalName,
                user_name = :userName,
                production_date = :productionDate,
                warranty_end_date = :warrantyEndDate,
                factory_no = :factoryNo,
                depreciation_method = :depreciationMethod,
                service_life_years = :serviceLifeYears,
                price = :price,
                manufacturer = :manufacturer,
                port_no = :portNo,
                ip_address = :ipAddress,
                common_startup_time = :commonStartupTime,
                common_shutdown_time = :commonShutdownTime,
                common_usage_content = :commonUsageContent,
                commonly_used = :commonlyUsed,
                set_temperature = :setTemperature,
                current_temperature = :currentTemperature,
                rfid = :rfid,
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
            .addValue("quantity", command.quantity())
            .addValue("purchaseDate", command.purchaseDate())
            .addValue("purchaserName", command.purchaserName())
            .addValue("purchaserCode", command.purchaserCode())
            .addValue("managementUnit", command.managementUnit())
            .addValue("managementCode", command.managementCode())
            .addValue("useUnit", command.useUnit())
            .addValue("principalCode", command.principalCode())
            .addValue("principalName", command.principalName())
            .addValue("userName", command.userName())
            .addValue("productionDate", command.productionDate())
            .addValue("warrantyEndDate", command.warrantyEndDate())
            .addValue("factoryNo", command.factoryNo())
            .addValue("depreciationMethod", command.depreciationMethod())
            .addValue("serviceLifeYears", command.serviceLifeYears())
            .addValue("price", command.price())
            .addValue("manufacturer", command.manufacturer())
            .addValue("portNo", command.portNo())
            .addValue("ipAddress", command.ipAddress())
            .addValue("commonStartupTime", command.commonStartupTime())
            .addValue("commonShutdownTime", command.commonShutdownTime())
            .addValue("commonUsageContent", command.commonUsageContent())
            .addValue("commonlyUsed", command.commonlyUsed() ? 1 : 0)
            .addValue("setTemperature", command.setTemperature())
            .addValue("currentTemperature", command.currentTemperature())
            .addValue("rfid", command.rfid())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateEquipmentStatusBatch(List<String> equipmentIds, String equipmentStatus, LocalDateTime updatedAt) {
        jdbcTemplate.update("""
            update equipment_records
            set equipment_status = :equipmentStatus,
                updated_at = :updatedAt
            where id in (:equipmentIds)
            """, new MapSqlParameterSource()
            .addValue("equipmentStatus", equipmentStatus)
            .addValue("updatedAt", updatedAt)
            .addValue("equipmentIds", equipmentIds));
    }

    @Override
    public List<EquipmentMaintenanceLog> findEquipmentMaintenanceLogs(String equipmentId) {
        return jdbcTemplate.query("""
            select id, equipment_id, maintenance_type, maintenance_status, performed_at,
                   performed_by_user_id, performed_by_name, description, next_maintenance_at, remarks
            from equipment_maintenance_logs
            where equipment_id = :equipmentId
            order by performed_at desc, created_at desc
            """, Map.of("equipmentId", equipmentId), JdbcOperationSupportRowMappers::mapEquipmentMaintenanceLog);
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
            LocalDateTime nextMaintenanceAt = JdbcOperationSupportRowMappers.toLocalDateTime(rs.getTimestamp("next_maintenance_at"));
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

    @Override
    public List<EquipmentRecord> findCommonlyUsedEquipmentRecords() {
        return jdbcTemplate.query("""
            select id, equipment_code, equipment_name, equipment_category, model_no, equipment_status,
                   location_description, enabled_at, next_maintenance_at, quantity, purchase_date,
                   purchaser_name, purchaser_code, management_unit, management_code, use_unit,
                   principal_code, principal_name, user_name, production_date, warranty_end_date,
                   factory_no, depreciation_method, service_life_years, price, manufacturer,
                   port_no, ip_address, common_startup_time, common_shutdown_time, common_usage_content,
                   commonly_used, set_temperature, current_temperature, rfid, remarks
            from equipment_records
            where commonly_used = 1
              and equipment_status = 'ACTIVE'
            order by equipment_code asc
            """, JdbcOperationSupportRowMappers::mapEquipmentRecord);
    }

    @Override
    public void insertEquipmentUsageRecord(CreateEquipmentUsageRecordCommand command) {
        jdbcTemplate.update("""
            insert into equipment_usage_records
                (id, equipment_id, equipment_category_snapshot, equipment_name_snapshot, commonly_used,
                 started_at, ended_at, runtime_hours, diagnosis_count, equipment_condition,
                 operator_user_id, operator_name, usage_content, remarks, created_at, updated_at)
            values
                (:id, :equipmentId, :equipmentCategorySnapshot, :equipmentNameSnapshot, :commonlyUsed,
                 :startedAt, :endedAt, :runtimeHours, :diagnosisCount, :equipmentCondition,
                 :operatorUserId, :operatorName, :usageContent, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("equipmentId", command.equipmentId())
            .addValue("equipmentCategorySnapshot", command.equipmentCategorySnapshot())
            .addValue("equipmentNameSnapshot", command.equipmentNameSnapshot())
            .addValue("commonlyUsed", command.commonlyUsed() ? 1 : 0)
            .addValue("startedAt", command.startedAt())
            .addValue("endedAt", command.endedAt())
            .addValue("runtimeHours", command.runtimeHours())
            .addValue("diagnosisCount", command.diagnosisCount())
            .addValue("equipmentCondition", command.equipmentCondition())
            .addValue("operatorUserId", command.operatorUserId())
            .addValue("operatorName", command.operatorName())
            .addValue("usageContent", command.usageContent())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public Optional<EquipmentUsageRecord> findEquipmentUsageRecordById(String usageRecordId) {
        return jdbcTemplate.query("""
            select id, equipment_id, equipment_category_snapshot, equipment_name_snapshot, commonly_used,
                   started_at, ended_at, runtime_hours, diagnosis_count, equipment_condition,
                   operator_user_id, operator_name, usage_content, remarks
            from equipment_usage_records
            where id = :id
            """, Map.of("id", usageRecordId), JdbcOperationSupportRowMappers::mapEquipmentUsageRecord).stream().findFirst();
    }

    @Override
    public List<WhiteSlideStock> findWhiteSlideStocks(String keyword, String status) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select id, stock_no, stock_code, specification, quantity_available, quantity_borrowed, status, remarks
            from white_slide_stocks
            where (:status is null or status = :status)
              and (:keywordLike is null or upper(stock_no) like :keywordLike
                   or upper(stock_code) like :keywordLike
                   or upper(coalesce(specification, '')) like :keywordLike
                   or upper(coalesce(remarks, '')) like :keywordLike)
            order by stock_no asc
            """, new MapSqlParameterSource()
            .addValue("status", JdbcOperationSupportRowMappers.blankToNull(status))
            .addValue("keywordLike", like), JdbcOperationSupportRowMappers::mapWhiteSlideStock);
    }

    @Override
    public Optional<WhiteSlideStock> findWhiteSlideStockById(String stockId) {
        return jdbcTemplate.query("""
            select id, stock_no, stock_code, specification, quantity_available, quantity_borrowed, status, remarks
            from white_slide_stocks
            where id = :id
            """, Map.of("id", stockId), JdbcOperationSupportRowMappers::mapWhiteSlideStock).stream().findFirst();
    }

    @Override
    public void updateWhiteSlideStockQuantities(UpdateWhiteSlideStockQuantitiesCommand command) {
        jdbcTemplate.update("""
            update white_slide_stocks
            set quantity_available = :quantityAvailable,
                quantity_borrowed = :quantityBorrowed,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("quantityAvailable", command.quantityAvailable())
            .addValue("quantityBorrowed", command.quantityBorrowed())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public List<WhiteSlideLoan> findWhiteSlideLoans(String keyword, String loanStatus) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select l.id, l.loan_no, l.stock_id, s.stock_no, s.stock_code, l.quantity, l.case_id, l.pathology_no,
                   l.patient_name, l.embedding_box_no, l.slice_purpose, l.slice_thickness, l.borrower_name,
                   l.borrower_identity_no, l.borrower_unit, l.borrower_phone, l.unit_price, l.amount,
                   l.save_direct_print, l.loan_status, l.wax_block_usage, l.operator_user_id, l.operator_name,
                   l.loaned_at, l.returned_at, l.returned_by_user_id, l.returned_by_name, l.remarks
            from white_slide_loans l
            join white_slide_stocks s on s.id = l.stock_id
            where (:loanStatus is null or l.loan_status = :loanStatus)
              and (:keywordLike is null or upper(l.loan_no) like :keywordLike
                   or upper(coalesce(l.pathology_no, '')) like :keywordLike
                   or upper(coalesce(l.patient_name, '')) like :keywordLike
                   or upper(coalesce(l.embedding_box_no, '')) like :keywordLike
                   or upper(coalesce(l.borrower_name, '')) like :keywordLike
                   or upper(coalesce(l.borrower_phone, '')) like :keywordLike
                   or upper(coalesce(l.borrower_unit, '')) like :keywordLike
                   or upper(coalesce(l.borrower_identity_no, '')) like :keywordLike
                   or upper(coalesce(l.slice_purpose, '')) like :keywordLike
                   or upper(coalesce(s.stock_no, '')) like :keywordLike
                   or upper(coalesce(s.stock_code, '')) like :keywordLike)
            order by l.loaned_at desc, l.loan_no desc
            """, new MapSqlParameterSource()
            .addValue("loanStatus", JdbcOperationSupportRowMappers.blankToNull(loanStatus))
            .addValue("keywordLike", like), JdbcOperationSupportRowMappers::mapWhiteSlideLoan);
    }

    @Override
    public Optional<WhiteSlideLoan> findWhiteSlideLoanById(String loanId) {
        return jdbcTemplate.query("""
            select l.id, l.loan_no, l.stock_id, s.stock_no, s.stock_code, l.quantity, l.case_id, l.pathology_no,
                   l.patient_name, l.embedding_box_no, l.slice_purpose, l.slice_thickness, l.borrower_name,
                   l.borrower_identity_no, l.borrower_unit, l.borrower_phone, l.unit_price, l.amount,
                   l.save_direct_print, l.loan_status, l.wax_block_usage, l.operator_user_id, l.operator_name,
                   l.loaned_at, l.returned_at, l.returned_by_user_id, l.returned_by_name, l.remarks
            from white_slide_loans l
            join white_slide_stocks s on s.id = l.stock_id
            where l.id = :id
            """, Map.of("id", loanId), JdbcOperationSupportRowMappers::mapWhiteSlideLoan).stream().findFirst();
    }

    @Override
    public void insertWhiteSlideLoan(CreateWhiteSlideLoanCommand command) {
        jdbcTemplate.update("""
            insert into white_slide_loans
                (id, loan_no, stock_id, quantity, case_id, pathology_no, patient_name, embedding_box_no,
                 slice_purpose, slice_thickness, borrower_name, borrower_identity_no, borrower_unit, borrower_phone,
                 unit_price, amount, save_direct_print, loan_status, wax_block_usage, operator_user_id, operator_name,
                 loaned_at, remarks, created_at, updated_at)
            values
                (:id, :loanNo, :stockId, :quantity, :caseId, :pathologyNo, :patientName, :embeddingBoxNo,
                 :slicePurpose, :sliceThickness, :borrowerName, :borrowerIdentityNo, :borrowerUnit, :borrowerPhone,
                 :unitPrice, :amount, :saveDirectPrint, :loanStatus, :waxBlockUsage, :operatorUserId, :operatorName,
                 :loanedAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("loanNo", command.loanNo())
            .addValue("stockId", command.stockId())
            .addValue("quantity", command.quantity())
            .addValue("caseId", command.caseId())
            .addValue("pathologyNo", command.pathologyNo())
            .addValue("patientName", command.patientName())
            .addValue("embeddingBoxNo", command.embeddingBoxNo())
            .addValue("slicePurpose", command.slicePurpose())
            .addValue("sliceThickness", command.sliceThickness())
            .addValue("borrowerName", command.borrowerName())
            .addValue("borrowerIdentityNo", command.borrowerIdentityNo())
            .addValue("borrowerUnit", command.borrowerUnit())
            .addValue("borrowerPhone", command.borrowerPhone())
            .addValue("unitPrice", command.unitPrice())
            .addValue("amount", command.amount())
            .addValue("saveDirectPrint", command.saveDirectPrint() ? 1 : 0)
            .addValue("loanStatus", command.loanStatus())
            .addValue("waxBlockUsage", command.waxBlockUsage())
            .addValue("operatorUserId", command.operatorUserId())
            .addValue("operatorName", command.operatorName())
            .addValue("loanedAt", command.loanedAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    @Override
    public void updateWhiteSlideLoanReturned(UpdateWhiteSlideLoanReturnedCommand command) {
        jdbcTemplate.update("""
            update white_slide_loans
            set loan_status = :loanStatus,
                returned_at = :returnedAt,
                returned_by_user_id = :returnedByUserId,
                returned_by_name = :returnedByName,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("loanStatus", command.loanStatus())
            .addValue("returnedAt", command.returnedAt())
            .addValue("returnedByUserId", command.returnedByUserId())
            .addValue("returnedByName", command.returnedByName())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

}
