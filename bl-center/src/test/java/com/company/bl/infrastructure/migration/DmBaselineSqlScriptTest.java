package com.company.bl.infrastructure.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DmBaselineSqlScriptTest {

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?m)^CREATE TABLE\\s+([a-zA-Z0-9_]+)");

    @Test
    void shouldKeepOnlyCurrentChineseMenuAndPermissionSeeds() throws Exception {
        String sql = readBaselineSql();

        assertTrue(sql.contains("('MENU_SYSTEM', NULL, 'SYSTEM', '系统管理'"));
        assertTrue(sql.contains("('MENU_M5_ARCHIVE', 'MENU_M5_SUPPORT', 'M5_ARCHIVE', '归档管理'"));
        assertTrue(sql.contains("('MENU_M6_STAT', 'MENU_M6_SUPPORT', 'M6_STAT', '统计分析'"));
        assertTrue(sql.contains("('PERM_M6_BILLING_RECONCILE', 'PERM_M6_BILLING_RECONCILE', '执行收费对账'"));

        assertFalse(sql.contains("MENU_SYS_DEPT"));
        assertFalse(sql.contains("MENU_SYS_LOGIN_LOG"));
        assertFalse(sql.contains("MENU_SYS_OPERATION_LOG"));
        assertFalse(sql.contains("PERM_SYS_DEPT"));
        assertFalse(sql.contains("PERM_SYS_LOGIN_LOG"));
        assertFalse(sql.contains("PERM_SYS_OPERATION_LOG"));
        assertFalse(sql.contains("sys:medical-order-dict"));
        assertFalse(sql.contains("sys:medical-order-charge"));
        assertFalse(sql.contains("M5 Support"));
        assertFalse(sql.contains("Integration Management"));
        assertFalse(sql.contains("Historical Reports"));
        assertFalse(sql.contains("Quality Overview"));
    }

    @Test
    void shouldSeedBuiltInUsersThroughM6WithSecurityFields() throws Exception {
        String sql = readBaselineSql();

        assertTrue(sql.contains("quality_check_result VARCHAR2(32)"));
        assertTrue(sql.contains("quality_issue_codes VARCHAR2(500)"));
        assertTrue(sql.contains("password_algo VARCHAR2(32)"));
        assertTrue(sql.contains("password_salt VARCHAR2(64)"));
        assertTrue(sql.contains("'USER_M1_ADMIN'"));
        assertTrue(sql.contains("'USER_M4_ORDER_EXECUTE'"));
        assertTrue(sql.contains("'USER_M5_ARCHIVE'"));
        assertTrue(sql.contains("'USER_M5_REAGENT'"));
        assertTrue(sql.contains("'USER_M6_ADMIN'"));
        assertTrue(sql.contains("'USER_M6_ARCHIVE'"));
        assertTrue(sql.contains("'USER_M6_QUALITY'"));
        assertTrue(sql.contains("'SM3', '9f3c5a8d7e1b4c2fa6d8e0b3c5f7a9d1'"));
        assertTrue(sql.contains("'DEPT_PATHOLOGY_ARCHIVE', '病理档案组'"));
        assertTrue(sql.contains("'DEPT_PATHOLOGY_SUPPORT', '病理运营支持组'"));

        assertEquals(34, countMatches(sql, "\\('USER_M[1-6]_[A-Z_]+',"));
        assertEquals(31, countMatches(sql, "\\('UR_M[1-6]_[A-Z_]+',"));
    }

    @Test
    void shouldSeedRoleMenusForM4M5AndM6() throws Exception {
        String sql = readBaselineSql();

        assertTrue(sql.contains("('RM_M4_ORDER_EXEC_MENU', 'ROLE_M4_MEDICAL_ORDER_EXECUTE', 'MENU_M4_MEDICAL_ORDER'"));
        assertTrue(sql.contains("('RM_ARCHIVE_ROOT', 'ROLE_ARCHIVE_MANAGER', 'MENU_M5_SUPPORT'"));
        assertTrue(sql.contains("('RM_REAGENT_EQUIPMENT', 'ROLE_REAGENT_DEVICE_MANAGER', 'MENU_M5_EQUIPMENT'"));
        assertTrue(sql.contains("('RM_ARCHIVE_M6_ROOT', 'ROLE_ARCHIVE_MANAGER', 'MENU_M6_SUPPORT'"));
        assertTrue(sql.contains("('RM_QUALITY_STAT', 'ROLE_QUALITY_MANAGER', 'MENU_M6_STAT'"));
        assertTrue(sql.contains("('RM_ADMIN_M6_INTEGRATION', 'ROLE_PATHOLOGY_ADMIN', 'MENU_M6_INTEGRATION'"));
    }

    @Test
    void shouldAvoidDuplicateTablesAndKeepExpectedBaselineCounts() throws Exception {
        String sql = readBaselineSql();

        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        Set<String> tableNames = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        while (matcher.find()) {
            String tableName = matcher.group(1);
            if (!tableNames.add(tableName)) {
                duplicates.add(tableName);
            }
        }
        assertTrue(duplicates.isEmpty(), "duplicate create tables: " + duplicates);

        assertEquals(4, countMatches(sql, "\\('TOPIC_[A-Z_]+',"));
        assertEquals(5, countMatches(sql, "\\('SCI_[A-Z0-9_]+',"));
        assertEquals(7, countMatches(sql, "\\('NR_[A-Z]+',"));
        assertEquals(19, countMatches(sql, "\\('SID_[A-Z0-9_]+',"));
        assertEquals(4, countMatches(sql, "\\('SRT_[A-Z]+',"));
    }

    private String readBaselineSql() throws IOException {
        for (Path candidate : List.of(
            Path.of("..", "docs", "database", "达梦完整部署基线.sql"),
            Path.of("docs", "database", "达梦完整部署基线.sql")
        )) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.exists(normalized)) {
                return Files.readString(normalized, StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Cannot locate 达梦完整部署基线.sql");
    }

    private int countMatches(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
