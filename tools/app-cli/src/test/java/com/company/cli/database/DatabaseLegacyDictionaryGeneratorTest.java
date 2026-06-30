package com.company.cli.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseLegacyDictionaryGeneratorTest {

    @Test
    void shouldRetainOnlyCurrentOwnerTablesInLegacyReport() {
        DatabaseDictionaryTargetResolver targetResolver = new StubTargetResolver();
        DatabaseDictionaryMetadataLoader metadataLoader = new StubMetadataLoader();
        DatabaseLegacyDictionaryGenerator generator =
            new DatabaseLegacyDictionaryGenerator(targetResolver, metadataLoader, new DatabaseLegacyDictionaryHtmlRenderer());

        DatabaseDictionaryGenerationResult result = generator.generate(new DatabaseDictionaryRequest(
            List.of("bl-center"),
            Path.of("DM数据表.html"),
            DatabaseDictionaryScope.VISIBLE_ALL));

        assertThat(result.report().sources()).hasSize(1);
        DatabaseSourceReport source = result.report().sources().get(0);
        assertThat(source.username()).isEqualTo("SYSDBA");
        assertThat(source.owners()).extracting(DatabaseOwnerReport::owner).containsExactly("SYSDBA");
        assertThat(result.html()).contains("业务表");
        assertThat(result.html()).doesNotContain("SYSCONTEXTINDEXES");
        assertThat(result.html()).doesNotContain("CTISYS");
    }

    private static final class StubTargetResolver extends DatabaseDictionaryTargetResolver {

        private StubTargetResolver() {
            super(new org.springframework.mock.env.MockEnvironment());
        }

        @Override
        List<DatabaseConnectionTarget> resolve(List<String> requestedTargets) {
            return List.of(new DatabaseConnectionTarget(
                List.of("bl-center"),
                "dm.jdbc.driver.DmDriver",
                "jdbc:dm://127.0.0.1:5236",
                "SYSDBA",
                "Dm.2027.Pwd."));
        }
    }

    private static final class StubMetadataLoader extends DatabaseDictionaryMetadataLoader {

        @Override
        DatabaseSourceReport load(DatabaseConnectionTarget target) {
            DatabaseTableReport businessTable = new DatabaseTableReport(
                "SYSDBA",
                "BUSINESS_TABLE",
                "业务表",
                List.of(new DatabaseColumnReport("ID", "VARCHAR2(64)", false, null, "主键ID", true, List.of("PK"), List.of())),
                List.of(),
                List.of(),
                List.of());
            DatabaseTableReport systemTable = new DatabaseTableReport(
                "CTISYS",
                "SYSCONTEXTINDEXES",
                null,
                List.of(new DatabaseColumnReport("NAME", "VARCHAR(128)", true, null, null, false, List.of(), List.of())),
                List.of(),
                List.of(),
                List.of());
            return new DatabaseSourceReport(
                List.of("bl-center"),
                target.url(),
                target.username(),
                List.of(
                    new DatabaseOwnerReport("SYSDBA", List.of(businessTable)),
                    new DatabaseOwnerReport("CTISYS", List.of(systemTable))));
        }
    }
}
