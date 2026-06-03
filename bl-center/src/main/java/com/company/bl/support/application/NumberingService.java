package com.company.bl.support.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NumberingService {

    public static final String BIZ_APPLICATION_NO = "APPLICATION_NO";
    public static final String BIZ_PATHOLOGY_NO = "PATHOLOGY_NO";
    public static final String BIZ_TRANSPORT_ORDER_NO = "TRANSPORT_ORDER_NO";
    public static final String BIZ_SPECIMEN_NO = "SPECIMEN_NO";
    public static final String BIZ_BLOCK_NO = "BLOCK_NO";
    public static final String BIZ_SLIDE_NO = "SLIDE_NO";
    public static final String BIZ_REPORT_NO = "REPORT_NO";
    public static final String BIZ_DEPARTMENT_CODE = "DEPARTMENT_CODE";
    public static final String BIZ_BODY_PART_CODE = "BODY_PART_CODE";
    public static final String BIZ_ROLE_CODE = "ROLE_CODE";
    public static final String BIZ_USER_CODE = "USER_CODE";
    public static final String BIZ_LOGIN_TAG_CODE = "LOGIN_TAG_CODE";
    public static final String BIZ_ORDER_CATEGORY_CODE = "ORDER_CATEGORY_CODE";
    public static final String BIZ_ORDER_ITEM_CODE = "ORDER_ITEM_CODE";
    public static final String BIZ_CHARGE_ITEM_CODE = "CHARGE_ITEM_CODE";
    public static final String BIZ_PACKAGE_CODE = "PACKAGE_CODE";
    public static final String BIZ_TEMPLATE_CATEGORY_CODE = "TEMPLATE_CATEGORY_CODE";
    public static final String BIZ_TEMPLATE_CODE = "TEMPLATE_CODE";
    public static final String BIZ_GUIDELINE_CATEGORY_CODE = "GUIDELINE_CATEGORY_CODE";
    public static final String BIZ_GUIDELINE_CODE = "GUIDELINE_CODE";
    public static final String BIZ_CONFIG_CATEGORY_CODE = "CONFIG_CATEGORY_CODE";
    private static final PathologyNoRule DEFAULT_PATHOLOGY_NO_RULE =
        new PathologyNoRule("BL", "yyyyMMdd", 4);
    private static final Map<String, PathologyNoRule> PATHOLOGY_NO_RULES =
        Map.ofEntries(
            Map.entry("CONSULTATION", new PathologyNoRule("HZ", "yy", 5)),
            Map.entry("CYTOLOGY", new PathologyNoRule("XB", "yy", 5)),
            Map.entry(
                "CYTOLOGY_CONSULTATION",
                new PathologyNoRule("XH", "yy", 5)
            ),
            Map.entry("CYTOLOGY_SMEAR", new PathologyNoRule("GP", "yy", 5)),
            Map.entry(
                "DIFFICULT_CONSULTATION",
                new PathologyNoRule("YN", "yy", 5)
            ),
            Map.entry("ELECTRON_MICROSCOPY", new PathologyNoRule("EM", "yy", 5)),
            Map.entry("FISH", new PathologyNoRule("FISH", "yy", 5)),
            Map.entry("FROZEN", new PathologyNoRule("BD", "yyyyMMdd", 4)),
            Map.entry("GENE_TEST", new PathologyNoRule("JY", "yy", 5)),
            Map.entry(
                "GYNECOLOGY_LBC_CYTOLOGY",
                new PathologyNoRule("FY", "yy", 5)
            ),
            Map.entry("GYNECOLOGY_LBC_DNA", new PathologyNoRule("FD", "yy", 5)),
            Map.entry("GYNECOLOGY_LBC_HPV", new PathologyNoRule("FH", "yy", 5)),
            Map.entry("HPV", new PathologyNoRule("HPV", "yy", 5)),
            Map.entry("IHC", new PathologyNoRule("IH", "yy", 5)),
            Map.entry(
                "IMMUNE_FLUORESCENCE",
                new PathologyNoRule("IF", "yy", 5)
            ),
            Map.entry("LIVER_BIOPSY", new PathologyNoRule("GC", "yy", 5)),
            Map.entry(
                "MOLECULAR_PATHOLOGY",
                new PathologyNoRule("FZ", "yy", 5)
            ),
            Map.entry("NGS", new PathologyNoRule("NGS", "yy", 5)),
            Map.entry(
                "NON_GYNECOLOGY_LBC_CYTOLOGY",
                new PathologyNoRule("NF", "yy", 5)
            ),
            Map.entry("PUNCTURE_BIOPSY", new PathologyNoRule("CC", "yy", 5)),
            Map.entry("RAPID", new PathologyNoRule("KS", "yy", 5)),
            Map.entry("RESEARCH", new PathologyNoRule("KY", "yy", 5)),
            Map.entry("ROUTINE", DEFAULT_PATHOLOGY_NO_RULE),
            Map.entry("SUPPLEMENTAL_REPORT", new PathologyNoRule("MS", "yy", 5)),
            Map.entry("TECHNICAL_ORDER", new PathologyNoRule("JS", "yy", 5))
        );

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
        return generate(BIZ_APPLICATION_NO, "GLOBAL");
    }

    @Transactional
    public String generatePathologyNo() {
        return generate(BIZ_PATHOLOGY_NO, "GLOBAL");
    }

    @Transactional
    public String generatePathologyNo(String applicationType) {
        PathologyNoRule rule = resolvePathologyNoRule(applicationType);
        return generatePathologyNoWithRule(rule);
    }

    public boolean matchesPathologyNoRule(String applicationType, String pathologyNo) {
        if (blank(pathologyNo)) {
            return false;
        }
        PathologyNoRule rule = resolvePathologyNoRule(applicationType);
        String dateDigits = "yy".equals(rule.datePattern()) ? "\\d{2}" : "\\d{8}";
        return Pattern.compile(
            "^" + Pattern.quote(rule.prefix()) + dateDigits + "\\d{" + rule.seqLength() + "}$",
            Pattern.CASE_INSENSITIVE
        ).matcher(pathologyNo.trim()).matches();
    }

    @Transactional
    public String generateTransportOrderNo() {
        return generate(BIZ_TRANSPORT_ORDER_NO, "GLOBAL");
    }

    @Transactional
    public String generateSpecimenNo(String scopeKey) {
        return generate(BIZ_SPECIMEN_NO, normalizeScope(scopeKey));
    }

    @Transactional
    public String generateBlockNo(String scopeKey) {
        return generate(BIZ_BLOCK_NO, normalizeScope(scopeKey));
    }

    @Transactional
    public String generateSlideNo() {
        return generate(BIZ_SLIDE_NO, "GLOBAL");
    }

    @Transactional
    public String generateReportNo() {
        return generate(BIZ_REPORT_NO, "GLOBAL");
    }

    @Transactional
    public String generateMasterDataCode(String bizType) {
        return generate(bizType, "GLOBAL");
    }

    @Transactional
    public String generateDepartmentCode() {
        return generateMasterDataCode(BIZ_DEPARTMENT_CODE);
    }

    @Transactional
    public String generateBodyPartCode() {
        return generateMasterDataCode(BIZ_BODY_PART_CODE);
    }

    @Transactional
    public String generateRoleCode() {
        return generateMasterDataCode(BIZ_ROLE_CODE);
    }

    @Transactional
    public String generateUserCode() {
        return generateMasterDataCode(BIZ_USER_CODE);
    }

    @Transactional
    public String generateLoginTagCode() {
        return generateMasterDataCode(BIZ_LOGIN_TAG_CODE);
    }

    @Transactional
    public String generateOrderCategoryCode() {
        return generateMasterDataCode(BIZ_ORDER_CATEGORY_CODE);
    }

    @Transactional
    public String generateOrderItemCode() {
        return generateMasterDataCode(BIZ_ORDER_ITEM_CODE);
    }

    @Transactional
    public String generateChargeItemCode() {
        return generateMasterDataCode(BIZ_CHARGE_ITEM_CODE);
    }

    @Transactional
    public String generatePackageCode() {
        return generateMasterDataCode(BIZ_PACKAGE_CODE);
    }

    @Transactional
    public String generateTemplateCategoryCode() {
        return generateMasterDataCode(BIZ_TEMPLATE_CATEGORY_CODE);
    }

    @Transactional
    public String generateTemplateCode() {
        return generateMasterDataCode(BIZ_TEMPLATE_CODE);
    }

    @Transactional
    public String generateGuidelineCategoryCode() {
        return generateMasterDataCode(BIZ_GUIDELINE_CATEGORY_CODE);
    }

    @Transactional
    public String generateGuidelineCode() {
        return generateMasterDataCode(BIZ_GUIDELINE_CODE);
    }

    @Transactional
    public String generateConfigCategoryCode() {
        return generateMasterDataCode(BIZ_CONFIG_CATEGORY_CODE);
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
        long nextValue = supportJdbcRepository.nextCounterValue(rule.ruleCode(), periodKey, resolveScopeKey(rule, scopeKey));
        return (blank(rule.prefixPattern()) ? "" : rule.prefixPattern())
            + datePart
            + pad(nextValue, rule.seqLength());
    }

    private String generatePathologyNoWithRule(PathologyNoRule rule) {
        SupportJdbcRepository.NumberingRuleRow baseRule =
            supportJdbcRepository.findNumberingRuleByBizType(BIZ_PATHOLOGY_NO);
        if (baseRule == null || !baseRule.enabled()) {
            throw new BlBusinessException(
                BlErrorCode.NUMBERING_GENERATION_FAILED,
                409,
                "Enabled numbering rule not found for biz type " + BIZ_PATHOLOGY_NO
            );
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String datePart = now.format(DateTimeFormatter.ofPattern(rule.datePattern()));
        String periodKey = resolvePeriodKey(baseRule.resetPolicy(), now, datePart);
        long nextValue = supportJdbcRepository.nextCounterValue(
            baseRule.ruleCode(),
            periodKey,
            resolveScopeKey(baseRule, "PATHOLOGY:" + rule.prefix())
        );
        return rule.prefix() + datePart + pad(nextValue, rule.seqLength());
    }

    private PathologyNoRule resolvePathologyNoRule(String applicationType) {
        if (blank(applicationType)) {
            return DEFAULT_PATHOLOGY_NO_RULE;
        }
        return PATHOLOGY_NO_RULES.getOrDefault(
            applicationType.trim().toUpperCase(),
            DEFAULT_PATHOLOGY_NO_RULE
        );
    }

    private String resolveScopeKey(SupportJdbcRepository.NumberingRuleRow rule, String requestedScopeKey) {
        if ("GLOBAL".equalsIgnoreCase(rule.scopeType())) {
            return "GLOBAL";
        }
        return normalizeScope(requestedScopeKey);
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

    @Schema(name = "NumberingRuleView", description = "业务编号规则")
    public record NumberingRuleView(
        @Schema(description = "规则 ID") String id,
        @Schema(description = "规则编码") String ruleCode,
        @Schema(description = "业务类型") String bizType,
        @Schema(description = "前缀模式") String prefixPattern,
        @Schema(description = "日期模式") String datePattern,
        @Schema(description = "流水号长度") int seqLength,
        @Schema(description = "重置策略") String resetPolicy,
        @Schema(description = "作用域类型") String scopeType,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "备注") String remarks,
        @Schema(description = "创建时间") String createdAt,
        @Schema(description = "更新时间") String updatedAt
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

    private record PathologyNoRule(
        String prefix,
        String datePattern,
        int seqLength
    ) {
    }
}
