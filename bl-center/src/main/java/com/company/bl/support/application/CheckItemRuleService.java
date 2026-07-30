package com.company.bl.support.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.infrastructure.CheckItemRuleRepository;
import com.company.bl.support.infrastructure.CheckItemRuleRepository.CheckItemRuleRow;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CheckItemRuleService {

    private static final int PATHOLOGY_NO_MAX_LENGTH = 64;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{(YYYY|YY|MM|DD|0:D(?:[1-9]|1[0-2]))}");
    private static final List<String> APPLICATION_TYPES = List.of(
        "ROUTINE", "FROZEN", "CYTOLOGY", "CONSULTATION", "MOLECULAR_PATHOLOGY",
        "IMMUNE_FLUORESCENCE", "IHC", "GENE_TEST", "FISH", "HPV",
        "GYNECOLOGY_LBC_CYTOLOGY", "NON_GYNECOLOGY_LBC_CYTOLOGY", "GYNECOLOGY_LBC_DNA",
        "GYNECOLOGY_LBC_HPV", "CYTOLOGY_SMEAR", "RAPID", "SUPPLEMENTAL_REPORT",
        "TECHNICAL_ORDER", "CYTOLOGY_CONSULTATION", "DIFFICULT_CONSULTATION",
        "ELECTRON_MICROSCOPY", "LIVER_BIOPSY", "NGS", "PUNCTURE_BIOPSY", "RESEARCH"
    );
    private static final Set<String> APPLICATION_TYPE_SET = Set.copyOf(APPLICATION_TYPES);

    private final CheckItemRuleRepository repository;
    private final Clock clock;

    @Autowired
    public CheckItemRuleService(CheckItemRuleRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    CheckItemRuleService(CheckItemRuleRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CheckItemRuleView> listRules() {
        LocalDate today = LocalDate.now(clock);
        return repository.findAll().stream().map(row -> toView(row, today)).toList();
    }

    @Transactional
    public CheckItemRuleView createRule(SaveCheckItemRuleCommand command) {
        String applicationType = normalizeKnownApplicationType(command.applicationType());
        if (repository.findByApplicationType(applicationType) != null) {
            throw conflict("A check-item rule already exists for application type " + applicationType);
        }
        ValidatedRule validated = validate(command, applicationType);
        LocalDateTime now = LocalDateTime.now(clock);
        CheckItemRuleRow row = new CheckItemRuleRow(
            "NR_CHECK_ITEM_" + applicationType,
            "RULE_CHECK_ITEM_" + applicationType,
            CheckItemRuleRepository.BIZ_PREFIX + applicationType,
            applicationType,
            validated.template(),
            command.autoIncrement(),
            toResetPolicy(command.numberPeriod()),
            validated.sequenceLength(),
            now,
            now);
        try {
            repository.insert(row, validated.literalPrefix(), validated.datePattern());
        } catch (DataIntegrityViolationException exception) {
            throw conflict("A check-item rule already exists for application type " + applicationType);
        }
        repository.setCounterValue(row.ruleCode(), periodKey(command.numberPeriod(), LocalDate.now(clock)),
            command.currentMaxSequence());
        return toView(row, LocalDate.now(clock));
    }

    @Transactional
    public CheckItemRuleView updateRule(String id, SaveCheckItemRuleCommand command) {
        CheckItemRuleRow current = requireRuleForUpdate(id);
        String applicationType = normalizeKnownApplicationType(command.applicationType());
        if (!current.applicationType().equals(applicationType)) {
            throw invalid("Application type cannot be changed after a rule is created");
        }
        ValidatedRule validated = validate(command, applicationType);
        CheckItemRuleRow updated = new CheckItemRuleRow(
            current.id(), current.ruleCode(), current.bizType(), current.applicationType(),
            validated.template(), command.autoIncrement(), toResetPolicy(command.numberPeriod()),
            validated.sequenceLength(), current.createdAt(), LocalDateTime.now(clock));
        repository.update(updated, validated.literalPrefix(), validated.datePattern());
        repository.setCounterValue(updated.ruleCode(), periodKey(command.numberPeriod(), LocalDate.now(clock)),
            command.currentMaxSequence());
        return toView(updated, LocalDate.now(clock));
    }

    @Transactional
    public void deleteRule(String id) {
        CheckItemRuleRow row = requireRuleForUpdate(id);
        if (repository.hasPathologyNumberForApplicationType(row.applicationType())
            || repository.hasAdvancedCounter(row.ruleCode())) {
            throw conflict("A check-item rule that has generated or used pathology numbers cannot be deleted");
        }
        repository.delete(row);
    }

    @Transactional(readOnly = true)
    public PathologyNumberPreview preview(String rawApplicationType) {
        String applicationType = normalizeApplicationType(rawApplicationType);
        CheckItemRuleRow rule = repository.findByApplicationType(applicationType);
        if (rule == null || !rule.autoIncrement()) {
            return new PathologyNumberPreview(applicationType, rule != null, false, true, null);
        }
        LocalDate today = LocalDate.now(clock);
        TemplateDefinition template = parseTemplate(rule.formatTemplate(), true, fromResetPolicy(rule.resetPolicy()));
        long sequence = repository.currentCounterValue(rule.ruleCode(), periodKey(fromResetPolicy(rule.resetPolicy()), today)) + 1;
        String suggested = nextUnusedPreview(template, today, sequence);
        return new PathologyNumberPreview(applicationType, true, true, false, suggested);
    }

    @Transactional
    public String generatePathologyNo(String rawApplicationType) {
        String applicationType = normalizeApplicationType(rawApplicationType);
        CheckItemRuleRow rule = repository.findByApplicationTypeForUpdate(applicationType);
        if (rule == null || !rule.autoIncrement()) {
            throw invalid("Pathology number must be entered manually for application type " + applicationType);
        }
        LocalDate today = LocalDate.now(clock);
        String period = fromResetPolicy(rule.resetPolicy());
        TemplateDefinition template = parseTemplate(rule.formatTemplate(), true, period);
        String periodKey = periodKey(period, today);
        for (int attempt = 0; attempt < 100; attempt++) {
            long sequence = repository.nextCounterValue(rule.ruleCode(), periodKey);
            String pathologyNo = template.format(today, sequence);
            if (!repository.pathologyNumberExists(pathologyNo, null)) {
                return pathologyNo;
            }
        }
        throw new BlBusinessException(BlErrorCode.NUMBERING_GENERATION_FAILED, 409,
            "Unable to allocate a unique pathology number");
    }

    @Transactional
    public void validateAndAcceptCandidate(String caseId, String rawApplicationType, String rawPathologyNo) {
        String applicationType = normalizeApplicationType(rawApplicationType);
        String pathologyNo = normalizePathologyNo(rawPathologyNo);
        CheckItemRuleRow rule = repository.findByApplicationTypeForUpdate(applicationType);
        if (repository.pathologyNumberExists(pathologyNo, caseId)) {
            throw conflict("Pathology number already exists");
        }
        if (rule == null || !rule.autoIncrement()) {
            if (rule != null) {
                repository.markRuleUsed(rule.ruleCode());
            }
            return;
        }
        LocalDate today = LocalDate.now(clock);
        String period = fromResetPolicy(rule.resetPolicy());
        TemplateDefinition template = parseTemplate(rule.formatTemplate(), true, period);
        Long sequence = template.extractSequence(pathologyNo, today);
        if (sequence == null) {
            throw invalid("Pathology number does not match selected application type");
        }
        repository.advanceCounterToAtLeast(rule.ruleCode(), periodKey(period, today), sequence);
    }

    @Transactional(readOnly = true)
    public boolean matchesPathologyNoRule(String rawApplicationType, String pathologyNo) {
        if (pathologyNo == null || pathologyNo.isBlank()) {
            return false;
        }
        String applicationType = normalizeApplicationType(rawApplicationType);
        CheckItemRuleRow rule = repository.findByApplicationType(applicationType);
        if (rule == null || !rule.autoIncrement()) {
            return false;
        }
        String period = fromResetPolicy(rule.resetPolicy());
        TemplateDefinition template = parseTemplate(rule.formatTemplate(), true, period);
        return template.extractSequence(pathologyNo.trim(), LocalDate.now(clock)) != null;
    }

    private ValidatedRule validate(SaveCheckItemRuleCommand command, String applicationType) {
        String period = normalizePeriod(command.numberPeriod());
        if (command.currentMaxSequence() < 0) {
            throw invalid("Current maximum sequence must not be negative");
        }
        String template = command.prefixRule() == null ? "" : command.prefixRule().trim();
        TemplateDefinition definition = parseTemplate(template, command.autoIncrement(), period);
        if (definition.hasSequence() && command.currentMaxSequence() > definition.maximumSequence()) {
            throw invalid("Current maximum sequence exceeds the configured sequence width");
        }
        long historicalFloor = command.autoIncrement()
            ? historicalMaximum(definition, LocalDate.now(clock)) : 0L;
        if (command.currentMaxSequence() < historicalFloor) {
            throw invalid("Current maximum sequence must not be lower than historical maximum " + historicalFloor);
        }
        return new ValidatedRule(applicationType, template, definition.literalPrefix(),
            definition.datePattern(), definition.sequenceLength());
    }

    private long historicalMaximum(TemplateDefinition template, LocalDate date) {
        return repository.findPathologyNumbers().stream()
            .map(pathologyNo -> template.extractSequence(pathologyNo, date))
            .filter(sequence -> sequence != null)
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
    }

    private String nextUnusedPreview(TemplateDefinition template, LocalDate date, long firstSequence) {
        for (long sequence = firstSequence; sequence < firstSequence + 100; sequence++) {
            String candidate = template.format(date, sequence);
            if (!repository.pathologyNumberExists(candidate, null)) {
                return candidate;
            }
        }
        throw new BlBusinessException(BlErrorCode.NUMBERING_GENERATION_FAILED, 409,
            "Unable to preview a unique pathology number");
    }

    private TemplateDefinition parseTemplate(String template, boolean autoIncrement, String period) {
        if (template == null || template.isBlank()) {
            if (autoIncrement) {
                throw invalid("Prefix rule is required when automatic increment is enabled");
            }
            return TemplateDefinition.empty();
        }
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuilder regex = new StringBuilder("^");
        int cursor = 0;
        int sequenceCount = 0;
        int sequenceLength = 1;
        int renderedLength = 0;
        StringBuilder datePattern = new StringBuilder();
        while (matcher.find()) {
            String literal = template.substring(cursor, matcher.start());
            if (literal.indexOf('{') >= 0 || literal.indexOf('}') >= 0) {
                throw invalid("Prefix rule contains an unsupported placeholder");
            }
            regex.append(Pattern.quote(literal));
            renderedLength += literal.length();
            String token = matcher.group(1);
            switch (token) {
                case "YYYY" -> { regex.append("%YYYY%"); datePattern.append("yyyy"); renderedLength += 4; }
                case "YY" -> { regex.append("%YY%"); datePattern.append("yy"); renderedLength += 2; }
                case "MM" -> { regex.append("%MM%"); datePattern.append("MM"); renderedLength += 2; }
                case "DD" -> { regex.append("%DD%"); datePattern.append("dd"); renderedLength += 2; }
                default -> {
                    sequenceCount++;
                    sequenceLength = Integer.parseInt(token.substring(3));
                    regex.append("(\\d{").append(sequenceLength).append("})");
                    renderedLength += sequenceLength;
                }
            }
            cursor = matcher.end();
        }
        String suffix = template.substring(cursor);
        if (suffix.indexOf('{') >= 0 || suffix.indexOf('}') >= 0) {
            throw invalid("Prefix rule contains an unsupported placeholder");
        }
        regex.append(Pattern.quote(suffix)).append('$');
        renderedLength += suffix.length();
        if (sequenceCount > 1 || (autoIncrement && sequenceCount != 1)) {
            throw invalid("Automatic prefix rule must contain exactly one sequence placeholder");
        }
        if (renderedLength > PATHOLOGY_NO_MAX_LENGTH) {
            throw invalid("Rendered pathology number must not exceed 64 characters");
        }
        validatePeriodTokens(template, period);
        int firstToken = template.indexOf('{');
        String literalPrefix = firstToken < 0 ? template : template.substring(0, firstToken);
        return new TemplateDefinition(template, regex.toString(), literalPrefix, datePattern.toString(), sequenceLength,
            sequenceCount == 1);
    }

    private void validatePeriodTokens(String template, String period) {
        boolean hasYear = template.contains("{YYYY}") || template.contains("{YY}");
        if (("YEAR".equals(period) || "MONTH".equals(period) || "DAY".equals(period)) && !hasYear) {
            throw invalid("The selected numbering period requires a year placeholder");
        }
        if (("MONTH".equals(period) || "DAY".equals(period)) && !template.contains("{MM}")) {
            throw invalid("The selected numbering period requires a month placeholder");
        }
        if ("DAY".equals(period) && !template.contains("{DD}")) {
            throw invalid("The selected numbering period requires a day placeholder");
        }
    }

    private CheckItemRuleView toView(CheckItemRuleRow row, LocalDate date) {
        String period = fromResetPolicy(row.resetPolicy());
        return new CheckItemRuleView(row.id(), row.applicationType(), period, row.autoIncrement(),
            row.formatTemplate() == null ? "" : row.formatTemplate(),
            repository.currentCounterValue(row.ruleCode(), periodKey(period, date)));
    }

    private CheckItemRuleRow requireRuleForUpdate(String id) {
        CheckItemRuleRow rule = repository.findByIdForUpdate(id);
        if (rule == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Check-item rule not found");
        }
        return rule;
    }

    private String normalizeKnownApplicationType(String value) {
        String normalized = normalizeApplicationType(value);
        if (!APPLICATION_TYPE_SET.contains(normalized)) {
            throw invalid("Unsupported application type " + normalized);
        }
        return normalized;
    }

    private String normalizeApplicationType(String value) {
        if (value == null || value.isBlank()) {
            return "ROUTINE";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePathologyNo(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("Pathology number must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > PATHOLOGY_NO_MAX_LENGTH) {
            throw invalid("Pathology number must not exceed 64 characters");
        }
        return normalized;
    }

    private String normalizePeriod(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("YEAR", "MONTH", "DAY", "NONE").contains(normalized)) {
            throw invalid("Number period must be YEAR, MONTH, DAY, or NONE");
        }
        return normalized;
    }

    private String toResetPolicy(String period) {
        return switch (normalizePeriod(period)) {
            case "YEAR" -> "YEARLY";
            case "MONTH" -> "MONTHLY";
            case "DAY" -> "DAILY";
            default -> "NONE";
        };
    }

    private String fromResetPolicy(String resetPolicy) {
        return switch (resetPolicy == null ? "" : resetPolicy.toUpperCase(Locale.ROOT)) {
            case "YEARLY" -> "YEAR";
            case "MONTHLY" -> "MONTH";
            case "DAILY" -> "DAY";
            default -> "NONE";
        };
    }

    private String periodKey(String period, LocalDate date) {
        return switch (normalizePeriod(period)) {
            case "YEAR" -> date.format(DateTimeFormatter.ofPattern("yyyy"));
            case "MONTH" -> date.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "DAY" -> date.format(DateTimeFormatter.BASIC_ISO_DATE);
            default -> "GLOBAL";
        };
    }

    private BlBusinessException invalid(String message) {
        return new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
    }

    private BlBusinessException conflict(String message) {
        return new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, message);
    }

    public record CheckItemRuleView(
        String id,
        String applicationType,
        String numberPeriod,
        boolean autoIncrement,
        String prefixRule,
        long currentMaxSequence
    ) {
    }

    public record SaveCheckItemRuleCommand(
        String applicationType,
        String numberPeriod,
        boolean autoIncrement,
        String prefixRule,
        long currentMaxSequence
    ) {
    }

    public record PathologyNumberPreview(
        String applicationType,
        boolean configured,
        boolean autoIncrement,
        boolean manualRequired,
        String suggestedPathologyNo
    ) {
    }

    private record ValidatedRule(
        String applicationType,
        String template,
        String literalPrefix,
        String datePattern,
        int sequenceLength
    ) {
    }

    private record TemplateDefinition(
        String template,
        String dateRegexTemplate,
        String literalPrefix,
        String datePattern,
        int sequenceLength,
        boolean hasSequence
    ) {
        static TemplateDefinition empty() {
            return new TemplateDefinition("", "^$", "", "", 1, false);
        }

        String format(LocalDate date, long sequence) {
            if (sequence < 0 || sequence > maximumSequence()) {
                throw new BlBusinessException(
                    BlErrorCode.NUMBERING_GENERATION_FAILED,
                    409,
                    "Pathology number sequence exceeds the configured sequence width"
                );
            }
            String result = template
                .replace("{YYYY}", date.format(DateTimeFormatter.ofPattern("yyyy")))
                .replace("{YY}", date.format(DateTimeFormatter.ofPattern("yy")))
                .replace("{MM}", date.format(DateTimeFormatter.ofPattern("MM")))
                .replace("{DD}", date.format(DateTimeFormatter.ofPattern("dd")));
            Matcher matcher = Pattern.compile("\\{0:D(?:[1-9]|1[0-2])}").matcher(result);
            return matcher.replaceFirst(String.format("%0" + sequenceLength + "d", sequence));
        }

        long maximumSequence() {
            long maximum = 1;
            for (int i = 0; i < sequenceLength; i++) {
                maximum *= 10;
            }
            return maximum - 1;
        }

        Long extractSequence(String pathologyNo, LocalDate date) {
            if (!hasSequence || pathologyNo == null) {
                return null;
            }
            String regex = dateRegexTemplate
                .replace("%YYYY%", date.format(DateTimeFormatter.ofPattern("yyyy")))
                .replace("%YY%", date.format(DateTimeFormatter.ofPattern("yy")))
                .replace("%MM%", date.format(DateTimeFormatter.ofPattern("MM")))
                .replace("%DD%", date.format(DateTimeFormatter.ofPattern("dd")));
            Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(pathologyNo);
            return matcher.matches() ? Long.parseLong(matcher.group(1)) : null;
        }
    }
}
