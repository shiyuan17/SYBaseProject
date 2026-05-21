package com.company.bl.support.infrastructure;

import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class SupportJdbcRepository {

    private static final int FAILURE_REASON_MAX_LENGTH = 500;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public SupportJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemDefaultZone());
    }

    SupportJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public List<NumberingRuleRow> findNumberingRules() {
        return jdbcTemplate.query("""
            select id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type,
                   enabled, remarks, created_at, updated_at
            from numbering_rules
            order by biz_type
            """, this::mapNumberingRule);
    }

    public NumberingRuleRow findNumberingRuleById(String id) {
        List<NumberingRuleRow> rows = jdbcTemplate.query("""
            select id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type,
                   enabled, remarks, created_at, updated_at
            from numbering_rules
            where id = :id
            """, Map.of("id", id), this::mapNumberingRule);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public NumberingRuleRow findNumberingRuleByBizType(String bizType) {
        List<NumberingRuleRow> rows = jdbcTemplate.query("""
            select id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type,
                   enabled, remarks, created_at, updated_at
            from numbering_rules
            where biz_type = :bizType
            """, Map.of("bizType", bizType), this::mapNumberingRule);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateNumberingRule(NumberingRuleRow row) {
        jdbcTemplate.update("""
            update numbering_rules
            set prefix_pattern = :prefixPattern,
                date_pattern = :datePattern,
                seq_length = :seqLength,
                reset_policy = :resetPolicy,
                scope_type = :scopeType,
                enabled = :enabled,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("prefixPattern", row.prefixPattern())
            .addValue("datePattern", row.datePattern())
            .addValue("seqLength", row.seqLength())
            .addValue("resetPolicy", row.resetPolicy())
            .addValue("scopeType", row.scopeType())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("remarks", row.remarks())
            .addValue("updatedAt", LocalDateTime.now(clock)));
    }

    public long nextCounterValue(String ruleCode, String periodKey, String scopeKey) {
        for (int attempt = 0; attempt < 10; attempt++) {
            CounterRow current = findCounter(ruleCode, periodKey, scopeKey);
            if (current == null) {
                try {
                    jdbcTemplate.update("""
                        insert into numbering_counters
                            (id, rule_code, period_key, scope_key, current_value, version, updated_at)
                        values
                            (:id, :ruleCode, :periodKey, :scopeKey, :currentValue, :version, :updatedAt)
                        """, new MapSqlParameterSource()
                        .addValue("id", "NC-" + UUID.randomUUID())
                        .addValue("ruleCode", ruleCode)
                        .addValue("periodKey", periodKey)
                        .addValue("scopeKey", scopeKey)
                        .addValue("currentValue", 1L)
                        .addValue("version", 0)
                        .addValue("updatedAt", LocalDateTime.now(clock)));
                    return 1L;
                } catch (DataAccessException ignored) {
                    continue;
                }
            }

            long nextValue = current.currentValue() + 1;
            int updated = jdbcTemplate.update("""
                update numbering_counters
                set current_value = :currentValue,
                    version = :nextVersion,
                    updated_at = :updatedAt
                where id = :id and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", current.id())
                .addValue("currentValue", nextValue)
                .addValue("nextVersion", current.version() + 1)
                .addValue("updatedAt", LocalDateTime.now(clock))
                .addValue("version", current.version()));
            if (updated == 1) {
                return nextValue;
            }
        }
        throw new IllegalStateException("Failed to update numbering counter after retries");
    }

    public void insertOperationLog(OperationLogRow row) {
        jdbcTemplate.update("""
            insert into operation_logs
                (id, module_code, business_type, business_id, operation_name, operation_result,
                 operator_user_id, operator_name, operator_ip, operation_at, operation_content, failure_reason)
            values
                (:id, :moduleCode, :businessType, :businessId, :operationName, :operationResult,
                 :operatorUserId, :operatorName, :operatorIp, :operationAt, :operationContent, :failureReason)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("moduleCode", row.moduleCode())
            .addValue("businessType", row.businessType())
            .addValue("businessId", row.businessId())
            .addValue("operationName", row.operationName())
            .addValue("operationResult", row.operationResult())
            .addValue("operatorUserId", row.operatorUserId())
            .addValue("operatorName", row.operatorName())
            .addValue("operatorIp", row.operatorIp())
            .addValue("operationAt", row.operationAt())
            .addValue("operationContent", row.operationContent())
            .addValue("failureReason", truncate(row.failureReason(), FAILURE_REASON_MAX_LENGTH)));
    }

    public List<Map<String, Object>> findOperationLogs(String moduleCode) {
        return jdbcTemplate.queryForList("""
            select id, module_code, business_type, business_id, operation_name, operation_result,
                   operator_name, operation_at, failure_reason
            from operation_logs
            where (:moduleCode is null or module_code = :moduleCode)
            order by operation_at desc
            """, new MapSqlParameterSource().addValue("moduleCode", moduleCode));
    }

    public String resolveDatePart(String pattern, LocalDateTime now) {
        if (pattern == null || pattern.isBlank()) {
            return "";
        }
        return now.format(DateTimeFormatter.ofPattern(pattern));
    }

    private CounterRow findCounter(String ruleCode, String periodKey, String scopeKey) {
        List<CounterRow> rows = jdbcTemplate.query("""
            select id, current_value, version
            from numbering_counters
            where rule_code = :ruleCode and period_key = :periodKey and scope_key = :scopeKey
            """, Map.of("ruleCode", ruleCode, "periodKey", periodKey, "scopeKey", scopeKey), (rs, rowNum) ->
            new CounterRow(
                rs.getString("id"),
                rs.getLong("current_value"),
                rs.getInt("version")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private NumberingRuleRow mapNumberingRule(ResultSet rs, int rowNum) throws SQLException {
        return new NumberingRuleRow(
            rs.getString("id"),
            rs.getString("rule_code"),
            rs.getString("biz_type"),
            rs.getString("prefix_pattern"),
            rs.getString("date_pattern"),
            rs.getInt("seq_length"),
            rs.getString("reset_policy"),
            rs.getString("scope_type"),
            rs.getInt("enabled") == 1,
            rs.getString("remarks"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());
    }

    public record NumberingRuleRow(
        String id,
        String ruleCode,
        String bizType,
        String prefixPattern,
        String datePattern,
        int seqLength,
        String resetPolicy,
        String scopeType,
        boolean enabled,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record OperationLogRow(
        String id,
        String moduleCode,
        String businessType,
        String businessId,
        String operationName,
        String operationResult,
        String operatorUserId,
        String operatorName,
        String operatorIp,
        LocalDateTime operationAt,
        String operationContent,
        String failureReason
    ) {
    }

    private record CounterRow(String id, long currentValue, int version) {
    }
}
