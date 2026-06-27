package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PathologyScreenDashboardSupportTest {

    @Test
    void shouldCalculateCriticalValueTenMinuteRateWithoutDatabaseSpecificFunctions() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        PathologyScreenDashboardSupport support = support(jdbcTemplate);
        String[] querySql = new String[1];

        doAnswer(invocation -> {
            querySql[0] = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            RowMapper<Object> rowMapper = invocation.getArgument(2);
            return List.of(
                rowMapper.mapRow(notificationRow(
                    LocalDateTime.of(2026, 6, 1, 10, 0),
                    LocalDateTime.of(2026, 6, 1, 10, 8)), 0),
                rowMapper.mapRow(notificationRow(
                    LocalDateTime.of(2026, 6, 1, 11, 0),
                    LocalDateTime.of(2026, 6, 1, 11, 10)), 1),
                rowMapper.mapRow(notificationRow(
                    LocalDateTime.of(2026, 6, 1, 12, 0),
                    LocalDateTime.of(2026, 6, 1, 12, 12)), 2));
        }).when(jdbcTemplate).query(
            anyString(),
            any(MapSqlParameterSource.class),
            org.mockito.ArgumentMatchers.<RowMapper<Object>>any());

        Object snapshot = invokeCriticalValueTenMinuteRate(
            support,
            LocalDateTime.of(2026, 6, 1, 0, 0),
            LocalDateTime.of(2026, 6, 30, 23, 59, 59));

        assertThat(querySql[0]).doesNotContain("dateadd(");
        assertThat(snapshotValue(snapshot)).isEqualByComparingTo("66.67");
        assertThat(snapshotStatus(snapshot)).isEqualTo("AVAILABLE");
        assertThat(snapshotSourceNote(snapshot)).contains("10 分钟");
    }

    @Test
    void shouldReturnPartialWhenCriticalValueNotificationIsEmpty() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        PathologyScreenDashboardSupport support = support(jdbcTemplate);

        when(jdbcTemplate.query(
            anyString(),
            any(MapSqlParameterSource.class),
            org.mockito.ArgumentMatchers.<RowMapper<Object>>any()))
            .thenReturn(List.of());

        Object snapshot = invokeCriticalValueTenMinuteRate(
            support,
            LocalDateTime.of(2026, 6, 1, 0, 0),
            LocalDateTime.of(2026, 6, 30, 23, 59, 59));

        assertThat(snapshotValue(snapshot)).isEqualByComparingTo("0.00");
        assertThat(snapshotStatus(snapshot)).isEqualTo("PARTIAL");
    }

    private static PathologyScreenDashboardSupport support(NamedParameterJdbcTemplate jdbcTemplate) {
        return new PathologyScreenDashboardSupport(
            mock(M6JdbcRepository.class),
            jdbcTemplate,
            new StatisticsQualitySupport(mock(M6JdbcRepository.class), jdbcTemplate));
    }

    private static ResultSet notificationRow(LocalDateTime createdAt, LocalDateTime readAt) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("created_at")).thenReturn(Timestamp.valueOf(createdAt));
        when(resultSet.getObject("read_at")).thenReturn(Timestamp.valueOf(readAt));
        return resultSet;
    }

    private static Object invokeCriticalValueTenMinuteRate(
        PathologyScreenDashboardSupport support,
        LocalDateTime from,
        LocalDateTime to
    ) throws Exception {
        Method method = PathologyScreenDashboardSupport.class.getDeclaredMethod(
            "criticalValueTenMinuteRate",
            LocalDateTime.class,
            LocalDateTime.class);
        method.setAccessible(true);
        return method.invoke(support, from, to);
    }

    private static BigDecimal snapshotValue(Object snapshot) throws Exception {
        return (BigDecimal) snapshotMethod("value").invoke(snapshot);
    }

    private static String snapshotStatus(Object snapshot) throws Exception {
        return (String) snapshotMethod("status").invoke(snapshot);
    }

    private static String snapshotSourceNote(Object snapshot) throws Exception {
        return (String) snapshotMethod("sourceNote").invoke(snapshot);
    }

    private static Method snapshotMethod(String name) throws Exception {
        Method method = Class.forName(
            "com.company.bl.integration.application.PathologyScreenDashboardSupport$RateSnapshot")
            .getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }
}
