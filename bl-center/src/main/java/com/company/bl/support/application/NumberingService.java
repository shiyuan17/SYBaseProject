package com.company.bl.support.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NumberingService {

    private final SupportJdbcRepository supportJdbcRepository;
    private final OperationAuditService operationAuditService;
    private final Clock clock;

    @Autowired
    public NumberingService(SupportJdbcRepository supportJdbcRepository,
                            OperationAuditService operationAuditService) {
        this(supportJdbcRepository, operationAuditService, Clock.systemDefaultZone());
    }

    NumberingService(SupportJdbcRepository supportJdbcRepository,
                     OperationAuditService operationAuditService,
                     Clock clock) {
        this.supportJdbcRepository = supportJdbcRepository;
        this.operationAuditService = operationAuditService;
        this.clock = clock;
    }

    @Cacheable("numberingRules")
    @Transactional(readOnly = true)
    public List<NumberingRuleView> listRules() {
        return supportJdbcRepository.findNumberingRules().stream()
            .map(this::toView)
            .toList();
    }

    @CacheEvict(value = "numberingRules", allEntries = true)
    @Transactional
    public NumberingRuleView updateRule(String id, UpdateNumberingRuleCommand command) {
        return operationAuditService.audit("SUPPORT", "NUMBERING_RULE", "update_numbering_rule", () -> {
            SupportJdbcRepository.NumberingRuleRow current = supportJdbcRepository.findNumberingRuleById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Numbering rule not found");
            }
            if (command.seqLength() < 1 || command.seqLength() > 12) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Sequence length must be between 1 and 12");
            }
            SupportJdbcRepository.NumberingRuleRow updated = new SupportJdbcRepository.NumberingRuleRow(
                current.id(),
                current.ruleCode(),
                current.bizType(),
                command.prefixPattern(),
                command.datePattern(),
                command.seqLength(),
                command.resetPolicy(),
                command.scopeType(),
                command.enabled(),
                command.remarks(),
                current.createdAt(),
                LocalDateTime.now(clock));
            supportJdbcRepository.updateNumberingRule(updated);
            return toView(updated);
        }, NumberingRuleView::id, () -> id, command::ruleSummary);
    }

    @Transactional
    public String generateApplicationNo() {
        return generate("APPLICATION_NO", "GLOBAL");
    }

    @Transactional
    public String generatePathologyNo() {
        return generate("PATHOLOGY_NO", "GLOBAL");
    }

    @Transactional
    public String generateTransportOrderNo() {
        return generate("TRANSPORT_ORDER_NO", "GLOBAL");
    }

    @Transactional
    public String generateSpecimenNo(String scopeKey) {
        return generate("SPECIMEN_NO", normalizeScope(scopeKey));
    }

    @Transactional
    public String generateBlockNo(String scopeKey) {
        return generate("BLOCK_NO", normalizeScope(scopeKey));
    }

    @Transactional
    public String generateSlideNo() {
        return generate("SLIDE_NO", "GLOBAL");
    }

    private String generate(String bizType, String scopeKey) {
        SupportJdbcRepository.NumberingRuleRow rule = supportJdbcRepository.findNumberingRuleByBizType(bizType);
        if (rule == null || !rule.enabled()) {
            throw new BlBusinessException(BlErrorCode.NUMBERING_GENERATION_FAILED, 409,
                "Enabled numbering rule not found for biz type " + bizType);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String datePart = supportJdbcRepository.resolveDatePart(rule.datePattern(), now);
        String periodKey = resolvePeriodKey(rule.resetPolicy(), now, datePart);
        long nextValue = supportJdbcRepository.nextCounterValue(rule.ruleCode(), periodKey, scopeKey);
        return (blank(rule.prefixPattern()) ? "" : rule.prefixPattern())
            + datePart
            + pad(nextValue, rule.seqLength());
    }

    private String resolvePeriodKey(String resetPolicy, LocalDateTime now, String datePart) {
        if (blank(resetPolicy) || "NONE".equalsIgnoreCase(resetPolicy)) {
            return "GLOBAL";
        }
        if (!blank(datePart)) {
            return datePart;
        }
        return switch (resetPolicy.toUpperCase()) {
            case "YEARLY" -> now.format(DateTimeFormatter.ofPattern("yyyy"));
            case "MONTHLY" -> now.format(DateTimeFormatter.ofPattern("yyyyMM"));
            default -> now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        };
    }

    private String normalizeScope(String scopeKey) {
        return blank(scopeKey) ? "GLOBAL" : scopeKey;
    }

    private String pad(long nextValue, int length) {
        return String.format("%0" + length + "d", nextValue);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private NumberingRuleView toView(SupportJdbcRepository.NumberingRuleRow row) {
        return new NumberingRuleView(
            row.id(),
            row.ruleCode(),
            row.bizType(),
            row.prefixPattern(),
            row.datePattern(),
            row.seqLength(),
            row.resetPolicy(),
            row.scopeType(),
            row.enabled(),
            row.remarks(),
            row.createdAt().toString(),
            row.updatedAt().toString());
    }

    public record NumberingRuleView(
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
        String createdAt,
        String updatedAt
    ) {
    }

    public record UpdateNumberingRuleCommand(
        String prefixPattern,
        String datePattern,
        int seqLength,
        String resetPolicy,
        String scopeType,
        boolean enabled,
        String remarks
    ) {
        String ruleSummary() {
            return String.join("|",
                blankSafe(prefixPattern),
                blankSafe(datePattern),
                String.valueOf(seqLength),
                blankSafe(resetPolicy),
                blankSafe(scopeType),
                String.valueOf(enabled),
                blankSafe(remarks));
        }

        private static String blankSafe(String value) {
            return value == null ? "" : value;
        }
    }
}
