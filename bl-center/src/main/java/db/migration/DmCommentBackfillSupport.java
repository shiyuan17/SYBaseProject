package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

class DmCommentBackfillSupport {

    private final String owner;

    DmCommentBackfillSupport(String owner) {
        this.owner = owner;
    }

    void backfill(Connection connection, List<DmTableCommentDefinition> tables) throws SQLException {
        for (DmTableCommentDefinition table : tables) {
            if (!tableExists(connection, table.tableName())) {
                continue;
            }
            if (shouldBackfillTableComment(connection, table.tableName())) {
                executeComment(connection, "COMMENT ON TABLE " + table.tableName() + " IS '" + escape(table.comment()) + "'");
            }
            for (DmColumnCommentDefinition column : table.columns()) {
                if (!columnExists(connection, table.tableName(), column.columnName())) {
                    continue;
                }
                if (shouldBackfillColumnComment(connection, table.tableName(), column.columnName())) {
                    executeComment(
                        connection,
                        "COMMENT ON COLUMN " + table.tableName() + "." + column.columnName()
                            + " IS '" + escape(column.comment()) + "'");
                }
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select 1 from all_tables where owner = ? and table_name = ?")) {
            statement.setString(1, owner);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select 1 from all_tab_columns where owner = ? and table_name = ? and column_name = ?")) {
            statement.setString(1, owner);
            statement.setString(2, tableName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean shouldBackfillTableComment(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select comments from all_tab_comments where owner = ? and table_name = ?")) {
            statement.setString(1, owner);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next() || isBlank(resultSet.getString(1));
            }
        }
    }

    private boolean shouldBackfillColumnComment(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select comments from all_col_comments where owner = ? and table_name = ? and column_name = ?")) {
            statement.setString(1, owner);
            statement.setString(2, tableName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next() || isBlank(resultSet.getString(1));
            }
        }
    }

    private void executeComment(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escape(String value) {
        return value.replace("'", "''");
    }
}

record DmTableCommentDefinition(String tableName, String comment, List<DmColumnCommentDefinition> columns) {
}

record DmColumnCommentDefinition(String columnName, String comment) {
}
