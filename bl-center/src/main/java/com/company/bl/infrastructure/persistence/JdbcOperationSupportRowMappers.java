package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.OperationSupportRepository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class JdbcOperationSupportRowMappers {

    private JdbcOperationSupportRowMappers() {
    }

    static OperationSupportRepository.Reagent mapReagent(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.Reagent(
            rs.getString("id"),
            rs.getString("reagent_code"),
            rs.getString("reagent_name"),
            rs.getString("specification"),
            rs.getString("unit"),
            rs.getString("manufacturer"),
            rs.getString("reagent_type"),
            rs.getString("reagent_usage"),
            rs.getString("order_dict_item_id"),
            rs.getString("order_item_name"),
            rs.getString("clone_no"),
            rs.getString("recommended_dilution"),
            rs.getString("application_dilution"),
            rs.getString("template_status"),
            rs.getObject("validity_days") == null ? null : rs.getInt("validity_days"),
            rs.getBigDecimal("default_low_stock_threshold"),
            rs.getBigDecimal("default_stock_threshold"),
            rs.getObject("default_near_expiry_days") == null ? null : rs.getInt("default_near_expiry_days"),
            rs.getBigDecimal("stain_capacity"),
            rs.getBigDecimal("stain_threshold"),
            rs.getInt("enabled") == 1,
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")),
            rs.getString("created_by_user_id"),
            rs.getString("created_by_name"),
            rs.getString("updated_by_user_id"),
            rs.getString("updated_by_name"),
            rs.getString("remarks"));
    }

    static OperationSupportRepository.ReagentStock mapReagentStock(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.ReagentStock(
            rs.getString("id"),
            rs.getString("reagent_id"),
            rs.getString("reagent_code"),
            rs.getString("reagent_name"),
            rs.getString("reagent_type"),
            rs.getString("order_dict_item_id"),
            rs.getString("order_item_name"),
            rs.getString("batch_no"),
            rs.getBigDecimal("initial_quantity"),
            rs.getBigDecimal("stock_quantity"),
            rs.getBigDecimal("remaining_quantity"),
            rs.getString("stock_status"),
            toLocalDate(rs.getDate("production_date")),
            toLocalDateTime(rs.getTimestamp("inbound_at")),
            toLocalDate(rs.getDate("expiry_date")),
            rs.getString("storage_location"),
            rs.getBigDecimal("low_stock_threshold"),
            rs.getObject("near_expiry_days") == null ? null : rs.getInt("near_expiry_days"),
            rs.getObject("test_reminder_threshold") == null ? null : rs.getInt("test_reminder_threshold"),
            rs.getObject("expiry_reminder_threshold") == null ? null : rs.getInt("expiry_reminder_threshold"),
            rs.getString("recommended_dilution"),
            rs.getString("application_dilution"),
            rs.getBigDecimal("stain_capacity"),
            rs.getBigDecimal("stain_threshold"),
            rs.getObject("validity_days") == null ? null : rs.getInt("validity_days"),
            toLocalDateTime(rs.getTimestamp("tested_at")),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("finished_at")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")),
            rs.getString("created_by_user_id"),
            rs.getString("created_by_name"),
            rs.getString("updated_by_user_id"),
            rs.getString("updated_by_name"),
            rs.getString("remarks"));
    }

    static OperationSupportRepository.ReagentStockEvent mapReagentStockEvent(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.ReagentStockEvent(
            rs.getString("id"),
            rs.getString("stock_id"),
            rs.getString("event_type"),
            rs.getBigDecimal("quantity_delta"),
            rs.getBigDecimal("quantity_before"),
            rs.getBigDecimal("quantity_after"),
            toLocalDateTime(rs.getTimestamp("occurred_at")),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("remarks"));
    }

    static OperationSupportRepository.EquipmentRecord mapEquipmentRecord(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.EquipmentRecord(
            rs.getString("id"),
            rs.getString("equipment_code"),
            rs.getString("equipment_name"),
            rs.getString("equipment_category"),
            rs.getString("model_no"),
            rs.getString("equipment_status"),
            rs.getString("location_description"),
            toLocalDateTime(rs.getTimestamp("enabled_at")),
            toLocalDateTime(rs.getTimestamp("next_maintenance_at")),
            rs.getObject("quantity") == null ? null : rs.getInt("quantity"),
            toLocalDate(rs.getDate("purchase_date")),
            rs.getString("purchaser_name"),
            rs.getString("purchaser_code"),
            rs.getString("management_unit"),
            rs.getString("management_code"),
            rs.getString("use_unit"),
            rs.getString("principal_code"),
            rs.getString("principal_name"),
            rs.getString("user_name"),
            toLocalDate(rs.getDate("production_date")),
            toLocalDate(rs.getDate("warranty_end_date")),
            rs.getString("factory_no"),
            rs.getString("depreciation_method"),
            rs.getObject("service_life_years") == null ? null : rs.getInt("service_life_years"),
            rs.getBigDecimal("price"),
            rs.getString("manufacturer"),
            rs.getString("port_no"),
            rs.getString("ip_address"),
            rs.getString("common_startup_time"),
            rs.getString("common_shutdown_time"),
            rs.getString("common_usage_content"),
            rs.getInt("commonly_used") == 1,
            rs.getBigDecimal("set_temperature"),
            rs.getBigDecimal("current_temperature"),
            rs.getString("rfid"),
            rs.getString("remarks"));
    }

    static OperationSupportRepository.EquipmentMaintenanceLog mapEquipmentMaintenanceLog(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.EquipmentMaintenanceLog(
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

    static OperationSupportRepository.EquipmentUsageRecord mapEquipmentUsageRecord(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.EquipmentUsageRecord(
            rs.getString("id"),
            rs.getString("equipment_id"),
            rs.getString("equipment_category_snapshot"),
            rs.getString("equipment_name_snapshot"),
            rs.getInt("commonly_used") == 1,
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("ended_at")),
            rs.getBigDecimal("runtime_hours"),
            rs.getObject("diagnosis_count") == null ? null : rs.getInt("diagnosis_count"),
            rs.getString("equipment_condition"),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("usage_content"),
            rs.getString("remarks"));
    }

    static OperationSupportRepository.WhiteSlideStock mapWhiteSlideStock(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.WhiteSlideStock(
            rs.getString("id"),
            rs.getString("stock_no"),
            rs.getString("stock_code"),
            rs.getString("specification"),
            rs.getInt("quantity_available"),
            rs.getInt("quantity_borrowed"),
            rs.getString("status"),
            rs.getString("remarks"));
    }

    static OperationSupportRepository.WhiteSlideLoan mapWhiteSlideLoan(ResultSet rs, int rowNum) throws SQLException {
        return new OperationSupportRepository.WhiteSlideLoan(
            rs.getString("id"),
            rs.getString("loan_no"),
            rs.getString("stock_id"),
            rs.getString("stock_no"),
            rs.getString("stock_code"),
            rs.getInt("quantity"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("patient_name"),
            rs.getString("embedding_box_no"),
            rs.getString("slice_purpose"),
            rs.getString("slice_thickness"),
            rs.getString("borrower_name"),
            rs.getString("borrower_identity_no"),
            rs.getString("borrower_unit"),
            rs.getString("borrower_phone"),
            rs.getBigDecimal("unit_price"),
            rs.getBigDecimal("amount"),
            rs.getInt("save_direct_print") == 1,
            rs.getString("loan_status"),
            rs.getString("wax_block_usage"),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            toLocalDateTime(rs.getTimestamp("loaned_at")),
            toLocalDateTime(rs.getTimestamp("returned_at")),
            rs.getString("returned_by_user_id"),
            rs.getString("returned_by_name"),
            rs.getString("remarks"));
    }

    static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
