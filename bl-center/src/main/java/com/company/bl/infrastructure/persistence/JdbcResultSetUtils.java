package com.company.bl.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

final class JdbcResultSetUtils {

    private JdbcResultSetUtils() {
    }

    static String getNullableString(ResultSet rs, String columnLabel) throws SQLException {
        return hasColumn(rs, columnLabel) ? rs.getString(columnLabel) : null;
    }

    static Integer getNullableInteger(ResultSet rs, String columnLabel) throws SQLException {
        return hasColumn(rs, columnLabel) ? rs.getObject(columnLabel, Integer.class) : null;
    }

    static java.sql.Timestamp getNullableTimestamp(ResultSet rs, String columnLabel) throws SQLException {
        return hasColumn(rs, columnLabel) ? rs.getTimestamp(columnLabel) : null;
    }

    private static boolean hasColumn(ResultSet rs, String columnLabel) throws SQLException {
        int columnCount = rs.getMetaData().getColumnCount();
        for (int index = 1; index <= columnCount; index++) {
            String label = rs.getMetaData().getColumnLabel(index);
            if (columnLabel.equalsIgnoreCase(label)) {
                return true;
            }
            String name = rs.getMetaData().getColumnName(index);
            if (columnLabel.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
