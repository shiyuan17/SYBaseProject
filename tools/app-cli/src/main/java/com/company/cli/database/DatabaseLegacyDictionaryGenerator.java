package com.company.cli.database;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
class DatabaseLegacyDictionaryGenerator {

    private final DatabaseDictionaryTargetResolver targetResolver;
    private final DatabaseDictionaryMetadataLoader metadataLoader;
    private final DatabaseLegacyDictionaryHtmlRenderer htmlRenderer;

    DatabaseDictionaryGenerationResult generate(DatabaseDictionaryRequest request) {
        List<DatabaseConnectionTarget> targets = targetResolver.resolve(request.targets());
        List<DatabaseSourceReport> sources = targets.stream()
            .map(metadataLoader::load)
            .map(this::retainLegacyVisibleOwners)
            .toList();
        DatabaseDictionaryReport report = buildReport(request.scope(), sources);
        return new DatabaseDictionaryGenerationResult(htmlRenderer.render(report), report);
    }

    private DatabaseSourceReport retainLegacyVisibleOwners(DatabaseSourceReport source) {
        List<DatabaseOwnerReport> filteredOwners = source.owners().stream()
            .filter(owner -> owner.owner().equalsIgnoreCase(source.username()))
            .map(this::retainLegacyVisibleTables)
            .filter(owner -> !owner.tables().isEmpty())
            .toList();
        if (filteredOwners.isEmpty()) {
            return source;
        }
        return new DatabaseSourceReport(source.labels(), source.url(), source.username(), filteredOwners);
    }

    private DatabaseOwnerReport retainLegacyVisibleTables(DatabaseOwnerReport owner) {
        List<DatabaseTableReport> tables = owner.tables().stream()
            .filter(this::isLegacyVisibleTable)
            .toList();
        return new DatabaseOwnerReport(owner.owner(), tables);
    }

    private boolean isLegacyVisibleTable(DatabaseTableReport table) {
        String tableName = table.tableName().toUpperCase();
        return !tableName.startsWith("##")
            && !tableName.startsWith("SREF_CON_")
            && !"FLYWAY_SCHEMA_HISTORY".equals(tableName);
    }

    private DatabaseDictionaryReport buildReport(DatabaseDictionaryScope scope, List<DatabaseSourceReport> sources) {
        int ownerCount = sources.stream().mapToInt(source -> source.owners().size()).sum();
        int tableCount = sources.stream().flatMap(source -> source.owners().stream()).mapToInt(owner -> owner.tables().size()).sum();
        int columnCount = sources.stream().flatMap(source -> source.owners().stream())
            .flatMap(owner -> owner.tables().stream()).mapToInt(table -> table.columns().size()).sum();
        int indexCount = sources.stream().flatMap(source -> source.owners().stream())
            .flatMap(owner -> owner.tables().stream()).mapToInt(table -> table.indexes().size()).sum();
        int foreignKeyCount = sources.stream().flatMap(source -> source.owners().stream())
            .flatMap(owner -> owner.tables().stream()).mapToInt(table -> table.foreignKeys().size()).sum();

        DatabaseDictionarySummary summary = new DatabaseDictionarySummary(
            sources.size(),
            ownerCount,
            tableCount,
            columnCount,
            indexCount,
            foreignKeyCount);

        DatabaseDictionaryReportStatus status = tableCount == 0
            ? DatabaseDictionaryReportStatus.WARNING
            : DatabaseDictionaryReportStatus.OK;
        return new DatabaseDictionaryReport(Instant.now(), status, scope, sources, summary);
    }
}
