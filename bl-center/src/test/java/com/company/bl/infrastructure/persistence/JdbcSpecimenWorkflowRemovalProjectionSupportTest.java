package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class JdbcSpecimenWorkflowRemovalProjectionSupportTest {

    @Test
    void shouldKeepWhitespaceBeforeResolvedCheckInStatusWhenListingSpecimenOutbounds() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcSpecimenWorkflowRemovalProjectionSupport support =
            new JdbcSpecimenWorkflowRemovalProjectionSupport(jdbcTemplate) {
                @Override
                protected boolean hasSpecimenConfirmationColumns() {
                    return true;
                }

                @Override
                protected boolean hasTransportOrderOutboundColumns() {
                    return true;
                }
            };
        String[] countSql = new String[1];
        String[] querySql = new String[1];

        doAnswer(invocation -> {
            countSql[0] = invocation.getArgument(0);
            return 0L;
        }).when(jdbcTemplate).queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class));
        doAnswer(invocation -> {
            querySql[0] = invocation.getArgument(0);
            return List.of();
        }).when(jdbcTemplate).query(
            anyString(),
            any(MapSqlParameterSource.class),
            org.mockito.ArgumentMatchers.<RowMapper<SpecimenWorkflowRepository.SpecimenOutboundRow>>any());

        support.findSpecimenOutbounds(new SpecimenWorkflowRepository.SpecimenOutboundListQuery(1, 20, null, null));

        assertThat(countSql[0]).contains("and\ncoalesce(s.check_in_status, 'NOT_CHECKED_IN')");
        assertThat(countSql[0]).doesNotContain("andcoalesce");
        assertThat(querySql[0]).contains("and\ncoalesce(s.check_in_status, 'NOT_CHECKED_IN')");
        assertThat(querySql[0]).doesNotContain("andcoalesce");
    }

    @Test
    void shouldSkipCandidateFilterWhenListingApplicationScopedSpecimenOutbounds() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcSpecimenWorkflowRemovalProjectionSupport support =
            new JdbcSpecimenWorkflowRemovalProjectionSupport(jdbcTemplate) {
                @Override
                protected boolean hasSpecimenConfirmationColumns() {
                    return true;
                }

                @Override
                protected boolean hasTransportOrderOutboundColumns() {
                    return true;
                }
            };
        String[] countSql = new String[1];

        doAnswer(invocation -> {
            countSql[0] = invocation.getArgument(0);
            return 0L;
        }).when(jdbcTemplate).queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class));
        doAnswer(invocation -> List.of()).when(jdbcTemplate).query(
            anyString(),
            any(MapSqlParameterSource.class),
            org.mockito.ArgumentMatchers.<RowMapper<SpecimenWorkflowRepository.SpecimenOutboundRow>>any());

        support.findSpecimenOutbounds(new SpecimenWorkflowRepository.SpecimenOutboundListQuery(1, 20, "APP-1", null));

        assertThat(countSql[0]).contains("and a.id = :applicationId");
        assertThat(countSql[0]).doesNotContain("latest_order.transport_order_id is not null");
    }
}
