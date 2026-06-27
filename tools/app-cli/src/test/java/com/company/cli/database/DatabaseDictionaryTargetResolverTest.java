package com.company.cli.database;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseDictionaryTargetResolverTest {

    private final MockEnvironment environment = new MockEnvironment();
    private final DatabaseDictionaryTargetResolver resolver = new DatabaseDictionaryTargetResolver(environment);

    @Test
    void shouldFallbackToDefaultLocalDatasourceAndDeduplicateMatchingTargets() {
        List<DatabaseConnectionTarget> targets = resolver.resolve(List.of("auth-center", "bl-center"));

        assertThat(targets).hasSize(1);
        DatabaseConnectionTarget target = targets.get(0);
        assertThat(target.labels()).containsExactly("auth-center", "bl-center");
        assertThat(target.driverClassName()).isEqualTo("dm.jdbc.driver.DmDriver");
        assertThat(target.url()).isEqualTo("jdbc:dm://127.0.0.1:5236");
        assertThat(target.username()).isEqualTo("SYSDBA");
        assertThat(target.password()).isEqualTo("Dm.2027.Pwd.");
    }

    @Test
    void shouldFailWhenDatasourceConfigurationIsPartial() {
        environment.setProperty("BL_CENTER_DATASOURCE_URL", "jdbc:dm://127.0.0.1:5236");
        environment.setProperty("BL_CENTER_DATASOURCE_USERNAME", "SYSDBA");

        assertThatThrownBy(() -> resolver.resolve(List.of("bl-center")))
            .isInstanceOf(DatabaseDictionaryException.class)
            .hasMessageContaining("BL_CENTER_DATASOURCE_PASSWORD");
    }

    @Test
    void shouldKeepDistinctTargetsWhenConnectionKeyDiffers() {
        environment.setProperty("AUTH_CENTER_DATASOURCE_URL", "jdbc:dm://127.0.0.1:5236");
        environment.setProperty("AUTH_CENTER_DATASOURCE_USERNAME", "SYSDBA");
        environment.setProperty("AUTH_CENTER_DATASOURCE_PASSWORD", "Dm.2027.Pwd.");
        environment.setProperty("BL_CENTER_DATASOURCE_URL", "jdbc:dm://127.0.0.2:5236");
        environment.setProperty("BL_CENTER_DATASOURCE_USERNAME", "SYSDBA");
        environment.setProperty("BL_CENTER_DATASOURCE_PASSWORD", "Dm.2027.Pwd.");

        List<DatabaseConnectionTarget> targets = resolver.resolve(List.of("auth-center", "bl-center"));

        assertThat(targets).hasSize(2);
        assertThat(targets)
            .extracting(DatabaseConnectionTarget::labels)
            .containsExactly(List.of("auth-center"), List.of("bl-center"));
    }
}
