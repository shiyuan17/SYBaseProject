package com.company.bl.support.infrastructure;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class CheckItemRuleRepository {

    public static final String BIZ_PREFIX = "CHECK_ITEM_PATHOLOGY_NO:";
    private static final String GLOBAL_SCOPE = "GLOBAL";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SupportJdbcRepository supportJdbcRepository;

    public CheckItemRuleRepository(NamedParameterJdbcTemplate jdbcTemplate,
                                   SupportJdbcRepository supportJdbcRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.supportJdbcRepository = supportJdbcRepository;
    }

    public List<CheckItemRuleRow> findAll() {
        return jdbcTemplate.query("""
            select id, rule_code, biz_type, format_template, "AUTO_INCREMENT", reset_policy,
                   seq_length, created_at, updated_at
            from numbering_rules
            where biz_type like 'CHECK_ITEM_PATHOLOGY_NO:%'
            order by biz_type
            """, this::mapRule);
    }

    public CheckItemRuleRow findById(String id) {
        return first(jdbcTemplate.query("""
            select id, rule_code, biz_type, format_template, "AUTO_INCREMENT", reset_policy,
                   seq_length, created_at, updated_at
            from numbering_rules
            where id = :id and biz_type like 'CHECK_ITEM_PATHOLOGY_NO:%'
            """, Map.of("id", id), this::mapRule));
    }

    public CheckItemRuleRow findByIdForUpdate(String id) {
        return first(jdbcTemplate.query("""
            select id, rule_code, biz_type, format_template, "AUTO_INCREMENT", reset_policy,
                   seq_length, created_at, updated_at
            from numbering_rules
            where id = :id and biz_type like 'CHECK_ITEM_PATHOLOGY_NO:%'
            for update
            """, Map.of("id", id), this::mapRule));
    }

    public CheckItemRuleRow findByApplicationType(String applicationType) {
        return first(jdbcTemplate.query("""
            select id, rule_code, biz_type, format_template, "AUTO_INCREMENT", reset_policy,
                   seq_length, created_at, updated_at
            from numbering_rules
            where biz_type = :bizType
            """, Map.of("bizType", BIZ_PREFIX + applicationType), this::mapRule));
    }

    public CheckItemRuleRow findByApplicationTypeForUpdate(String applicationType) {
        return first(jdbcTemplate.query("""
            select id, rule_code, biz_type, format_template, "AUTO_INCREMENT", reset_policy,
                   seq_length, created_at, updated_at
            from numbering_rules
            where biz_type = :bizType
            for update
            """, Map.of("bizType", BIZ_PREFIX + applicationType), this::mapRule));
    }

    public void insert(CheckItemRuleRow row, String prefix, String datePattern) {
        jdbcTemplate.update("""
            insert into numbering_rules
                (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy,
                 scope_type, enabled, remarks, format_template, "AUTO_INCREMENT", created_at, updated_at)
            values
                (:id, :ruleCode, :bizType, :prefix, :datePattern, :seqLength, :resetPolicy,
                 'GLOBAL', 1, :remarks, :formatTemplate, :autoIncrement, :createdAt, :updatedAt)
            """, ruleParameters(row, prefix, datePattern));
    }

    public void update(CheckItemRuleRow row, String prefix, String datePattern) {
        jdbcTemplate.update("""
            update numbering_rules
            set prefix_pattern = :prefix,
                date_pattern = :datePattern,
                seq_length = :seqLength,
                reset_policy = :resetPolicy,
                format_template = :formatTemplate,
                "AUTO_INCREMENT" = :autoIncrement,
                updated_at = :updatedAt
            where id = :id and biz_type = :bizType
            """, ruleParameters(row, prefix, datePattern));
    }

    public void delete(CheckItemRuleRow row) {
        jdbcTemplate.update("delete from numbering_counters where rule_code = :ruleCode",
            Map.of("ruleCode", row.ruleCode()));
        jdbcTemplate.update("delete from numbering_rules where id = :id and biz_type = :bizType",
            Map.of("id", row.id(), "bizType", row.bizType()));
    }

    public long currentCounterValue(String ruleCode, String periodKey) {
        CounterState state = findCounter(ruleCode, periodKey);
        return state == null ? 0L : state.currentValue();
    }

    public long nextCounterValue(String ruleCode, String periodKey) {
        return supportJdbcRepository.nextCounterValue(ruleCode, periodKey, GLOBAL_SCOPE);
    }

    public void setCounterValue(String ruleCode, String periodKey, long requestedValue) {
        for (int attempt = 0; attempt < 10; attempt++) {
            CounterState current = findCounter(ruleCode, periodKey);
            if (current == null) {
                if (requestedValue == 0) {
                    return;
                }
                try {
                    insertCounter(ruleCode, periodKey, requestedValue);
                    return;
                } catch (DataAccessException ignored) {
                    continue;
                }
            }
            int updated = jdbcTemplate.update("""
                update numbering_counters
                set current_value = :value, version = :nextVersion, updated_at = :updatedAt
                where id = :id and version = :version
                """, new MapSqlParameterSource()
                .addValue("value", requestedValue)
                .addValue("nextVersion", current.version() + 1)
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("id", current.id())
                .addValue("version", current.version()));
            if (updated == 1) {
                return;
            }
        }
        throw new IllegalStateException("Failed to set check-item numbering counter after retries");
    }

    public void advanceCounterToAtLeast(String ruleCode, String periodKey, long requestedValue) {
        for (int attempt = 0; attempt < 10; attempt++) {
            CounterState current = findCounter(ruleCode, periodKey);
            if (current != null && current.currentValue() >= requestedValue) {
                return;
            }
            if (current == null) {
                try {
                    insertCounter(ruleCode, periodKey, requestedValue);
                    return;
                } catch (DataAccessException ignored) {
                    continue;
                }
            }
            int updated = jdbcTemplate.update("""
                update numbering_counters
                set current_value = :value, version = :nextVersion, updated_at = :updatedAt
                where id = :id and version = :version
                """, new MapSqlParameterSource()
                .addValue("value", requestedValue)
                .addValue("nextVersion", current.version() + 1)
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("id", current.id())
                .addValue("version", current.version()));
            if (updated == 1) {
                return;
            }
        }
        throw new IllegalStateException("Failed to advance check-item numbering counter after retries");
    }

    public void markRuleUsed(String ruleCode) {
        advanceCounterToAtLeast(ruleCode, "__USED__", 1L);
    }

    public List<String> findPathologyNumbers() {
        return jdbcTemplate.query("""
            select pathology_no
            from pathology_cases
            where pathology_no is not null and pathology_no <> ''
            """, (rs, rowNum) -> rs.getString(1));
    }

    public boolean pathologyNumberExists(String pathologyNo, String excludedCaseId) {
        String exclusion = excludedCaseId == null ? "" : " and id <> :excludedCaseId";
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("pathologyNo", pathologyNo);
        if (excludedCaseId != null) {
            parameters.addValue("excludedCaseId", excludedCaseId);
        }
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from pathology_cases
            where upper(pathology_no) = upper(:pathologyNo)
            """ + exclusion, parameters, Long.class);
        return count != null && count > 0;
    }

    public boolean hasPathologyNumberForApplicationType(String applicationType) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where upper(a.application_type) = :applicationType
              and pc.pathology_no is not null and pc.pathology_no <> ''
            """, Map.of("applicationType", applicationType), Long.class);
        return count != null && count > 0;
    }

    public boolean hasAdvancedCounter(String ruleCode) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from numbering_counters
            where rule_code = :ruleCode and current_value > 0
            """, Map.of("ruleCode", ruleCode), Long.class);
        return count != null && count > 0;
    }

    private CounterState findCounter(String ruleCode, String periodKey) {
        return first(jdbcTemplate.query("""
            select id, current_value, version
            from numbering_counters
            where rule_code = :ruleCode and period_key = :periodKey and scope_key = 'GLOBAL'
            """, Map.of("ruleCode", ruleCode, "periodKey", periodKey),
            (rs, rowNum) -> new CounterState(rs.getString("id"), rs.getLong("current_value"), rs.getInt("version"))));
    }

    private void insertCounter(String ruleCode, String periodKey, long value) {
        jdbcTemplate.update("""
            insert into numbering_counters
                (id, rule_code, period_key, scope_key, current_value, version, updated_at)
            values (:id, :ruleCode, :periodKey, 'GLOBAL', :value, 0, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", "NC_CHECK_ITEM_" + UUID.randomUUID().toString().replace("-", ""))
            .addValue("ruleCode", ruleCode)
            .addValue("periodKey", periodKey)
            .addValue("value", value)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    private MapSqlParameterSource ruleParameters(CheckItemRuleRow row, String prefix, String datePattern) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("ruleCode", row.ruleCode())
            .addValue("bizType", row.bizType())
            .addValue("prefix", prefix)
            .addValue("datePattern", datePattern)
            .addValue("seqLength", row.sequenceLength())
            .addValue("resetPolicy", row.resetPolicy())
            .addValue("remarks", "检查项病理号规则：" + row.applicationType())
            .addValue("formatTemplate", row.formatTemplate())
            .addValue("autoIncrement", row.autoIncrement() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private CheckItemRuleRow mapRule(ResultSet rs, int rowNum) throws SQLException {
        String bizType = rs.getString("biz_type");
        return new CheckItemRuleRow(
            rs.getString("id"),
            rs.getString("rule_code"),
            bizType,
            bizType.substring(BIZ_PREFIX.length()),
            rs.getString("format_template"),
            rs.getInt("AUTO_INCREMENT") != 0,
            rs.getString("reset_policy"),
            rs.getInt("seq_length"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private <T> T first(List<T> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    public record CheckItemRuleRow(
        String id,
        String ruleCode,
        String bizType,
        String applicationType,
        String formatTemplate,
        boolean autoIncrement,
        String resetPolicy,
        int sequenceLength,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    private record CounterState(String id, long currentValue, int version) {
    }
}
