package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcApplicationRegistrationWorkbenchExtensionSupportTest {

    @Test
    void shouldTreatMissingTechnicalOverrideColumnsAsNull() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcApplicationRegistrationWorkbenchExtensionSupport support =
            new JdbcApplicationRegistrationWorkbenchExtensionSupport(jdbcTemplate);
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);

        when(resultSet.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(2);
        when(metadata.getColumnLabel(1)).thenReturn("technical_history_summary_override");
        when(metadata.getColumnLabel(2)).thenReturn("technical_lab_imaging_override");
        when(resultSet.getString("technical_history_summary_override")).thenReturn("人工病史摘要");
        when(resultSet.getString("technical_lab_imaging_override")).thenReturn("人工影像检查");
        when(jdbcTemplate.query(
            anyString(),
            anyMap(),
            org.mockito.ArgumentMatchers.<RowMapper<ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides>>any()
        )).thenAnswer(invocation -> {
            RowMapper<ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides> rowMapper =
                invocation.getArgument(2);
            return List.of(rowMapper.mapRow(resultSet, 0));
        });

        Optional<ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides> overrides =
            support.findTechnicalRegistrationDetailSectionOverridesByApplicationId("APP-REG-001");

        assertThat(overrides).isPresent();
        assertThat(overrides.orElseThrow()).isEqualTo(
            new ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides(
                "人工病史摘要",
                null,
                "人工影像检查",
                null,
                null,
                null));
    }
}
