package db.migration;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DmCommentBackfillSupportTest {

    @Test
    void shouldBackfillOnlyMissingComments() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableCommentQuery = mock(PreparedStatement.class);
        PreparedStatement columnCommentQuery = mock(PreparedStatement.class);
        ResultSet emptyResultSet = mock(ResultSet.class);
        ResultSet existingResultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);

        when(connection.prepareStatement("select comments from all_tab_comments where owner = ? and table_name = ?"))
            .thenReturn(tableCommentQuery);
        when(connection.prepareStatement("select comments from all_col_comments where owner = ? and table_name = ? and column_name = ?"))
            .thenReturn(columnCommentQuery);
        PreparedStatement tableExistsQuery = mock(PreparedStatement.class);
        PreparedStatement columnExistsQuery = mock(PreparedStatement.class);
        when(connection.prepareStatement("select 1 from all_tables where owner = ? and table_name = ?"))
            .thenReturn(tableExistsQuery);
        when(connection.prepareStatement("select 1 from all_tab_columns where owner = ? and table_name = ? and column_name = ?"))
            .thenReturn(columnExistsQuery);
        when(connection.createStatement()).thenReturn(statement);

        ResultSet tableExistsResultSet = mock(ResultSet.class);
        ResultSet columnExistsResultSet = mock(ResultSet.class);
        when(tableExistsQuery.executeQuery()).thenReturn(tableExistsResultSet);
        when(columnExistsQuery.executeQuery()).thenReturn(columnExistsResultSet, columnExistsResultSet);
        when(tableExistsResultSet.next()).thenReturn(true);
        when(columnExistsResultSet.next()).thenReturn(true, true);
        when(tableCommentQuery.executeQuery()).thenReturn(emptyResultSet);
        when(columnCommentQuery.executeQuery()).thenReturn(existingResultSet, emptyResultSet);
        when(emptyResultSet.next()).thenReturn(false);
        when(existingResultSet.next()).thenReturn(true);
        when(existingResultSet.getString(1)).thenReturn("已有备注");

        DmCommentBackfillSupport support = new DmCommentBackfillSupport("SYSDBA");
        support.backfill(
            connection,
            List.of(new DmTableCommentDefinition(
                "TEST_TABLE",
                "测试表",
                List.of(
                    new DmColumnCommentDefinition("EXISTING_COL", "已有列"),
                    new DmColumnCommentDefinition("MISSING_COL", "缺失列")))));

        verify(statement).execute("COMMENT ON TABLE TEST_TABLE IS '测试表'");
        verify(statement, never()).execute("COMMENT ON COLUMN TEST_TABLE.EXISTING_COL IS '已有列'");
        verify(statement).execute("COMMENT ON COLUMN TEST_TABLE.MISSING_COL IS '缺失列'");
    }

    @Test
    void shouldBackfillEnglishPlaceholderComments() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableCommentQuery = mock(PreparedStatement.class);
        PreparedStatement columnCommentQuery = mock(PreparedStatement.class);
        PreparedStatement tableExistsQuery = mock(PreparedStatement.class);
        PreparedStatement columnExistsQuery = mock(PreparedStatement.class);
        ResultSet tableExistsResultSet = mock(ResultSet.class);
        ResultSet columnExistsResultSet = mock(ResultSet.class);
        ResultSet tableCommentResultSet = mock(ResultSet.class);
        ResultSet columnCommentResultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);

        when(connection.prepareStatement("select comments from all_tab_comments where owner = ? and table_name = ?"))
            .thenReturn(tableCommentQuery);
        when(connection.prepareStatement("select comments from all_col_comments where owner = ? and table_name = ? and column_name = ?"))
            .thenReturn(columnCommentQuery);
        when(connection.prepareStatement("select 1 from all_tables where owner = ? and table_name = ?"))
            .thenReturn(tableExistsQuery);
        when(connection.prepareStatement("select 1 from all_tab_columns where owner = ? and table_name = ? and column_name = ?"))
            .thenReturn(columnExistsQuery);
        when(connection.createStatement()).thenReturn(statement);

        when(tableExistsQuery.executeQuery()).thenReturn(tableExistsResultSet);
        when(columnExistsQuery.executeQuery()).thenReturn(columnExistsResultSet);
        when(tableExistsResultSet.next()).thenReturn(true);
        when(columnExistsResultSet.next()).thenReturn(true);
        when(tableCommentQuery.executeQuery()).thenReturn(tableCommentResultSet);
        when(columnCommentQuery.executeQuery()).thenReturn(columnCommentResultSet);
        when(tableCommentResultSet.next()).thenReturn(true);
        when(columnCommentResultSet.next()).thenReturn(true);
        when(tableCommentResultSet.getString(1)).thenReturn("TEST_TABLE");
        when(columnCommentResultSet.getString(1)).thenReturn("COLUMN_CODE");

        DmCommentBackfillSupport support = new DmCommentBackfillSupport("SYSDBA");
        support.backfill(
            connection,
            List.of(new DmTableCommentDefinition(
                "TEST_TABLE",
                "测试表",
                List.of(new DmColumnCommentDefinition("COLUMN_CODE", "字段中文名")))));

        verify(statement).execute("COMMENT ON TABLE TEST_TABLE IS '测试表'");
        verify(statement).execute("COMMENT ON COLUMN TEST_TABLE.COLUMN_CODE IS '字段中文名'");
    }

    @Test
    void shouldKeepExistingChineseComments() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableCommentQuery = mock(PreparedStatement.class);
        PreparedStatement columnCommentQuery = mock(PreparedStatement.class);
        PreparedStatement tableExistsQuery = mock(PreparedStatement.class);
        PreparedStatement columnExistsQuery = mock(PreparedStatement.class);
        ResultSet tableExistsResultSet = mock(ResultSet.class);
        ResultSet columnExistsResultSet = mock(ResultSet.class);
        ResultSet tableCommentResultSet = mock(ResultSet.class);
        ResultSet columnCommentResultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);

        when(connection.prepareStatement("select comments from all_tab_comments where owner = ? and table_name = ?"))
            .thenReturn(tableCommentQuery);
        when(connection.prepareStatement("select comments from all_col_comments where owner = ? and table_name = ? and column_name = ?"))
            .thenReturn(columnCommentQuery);
        when(connection.prepareStatement("select 1 from all_tables where owner = ? and table_name = ?"))
            .thenReturn(tableExistsQuery);
        when(connection.prepareStatement("select 1 from all_tab_columns where owner = ? and table_name = ? and column_name = ?"))
            .thenReturn(columnExistsQuery);
        when(connection.createStatement()).thenReturn(statement);

        when(tableExistsQuery.executeQuery()).thenReturn(tableExistsResultSet);
        when(columnExistsQuery.executeQuery()).thenReturn(columnExistsResultSet);
        when(tableExistsResultSet.next()).thenReturn(true);
        when(columnExistsResultSet.next()).thenReturn(true);
        when(tableCommentQuery.executeQuery()).thenReturn(tableCommentResultSet);
        when(columnCommentQuery.executeQuery()).thenReturn(columnCommentResultSet);
        when(tableCommentResultSet.next()).thenReturn(true);
        when(columnCommentResultSet.next()).thenReturn(true);
        when(tableCommentResultSet.getString(1)).thenReturn("测试表");
        when(columnCommentResultSet.getString(1)).thenReturn("字段中文名");

        DmCommentBackfillSupport support = new DmCommentBackfillSupport("SYSDBA");
        support.backfill(
            connection,
            List.of(new DmTableCommentDefinition(
                "TEST_TABLE",
                "测试表",
                List.of(new DmColumnCommentDefinition("COLUMN_CODE", "字段中文名")))));

        verify(statement, never()).execute(anyString());
    }

    @Test
    void shouldEscapeSingleQuotesInCommentStatements() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableCommentQuery = mock(PreparedStatement.class);
        PreparedStatement tableExistsQuery = mock(PreparedStatement.class);
        ResultSet emptyResultSet = mock(ResultSet.class);
        ResultSet tableExistsResultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);

        when(connection.prepareStatement("select comments from all_tab_comments where owner = ? and table_name = ?"))
            .thenReturn(tableCommentQuery);
        when(connection.prepareStatement("select 1 from all_tables where owner = ? and table_name = ?"))
            .thenReturn(tableExistsQuery);
        when(connection.createStatement()).thenReturn(statement);
        when(tableExistsQuery.executeQuery()).thenReturn(tableExistsResultSet);
        when(tableExistsResultSet.next()).thenReturn(true);
        when(tableCommentQuery.executeQuery()).thenReturn(emptyResultSet);
        when(emptyResultSet.next()).thenReturn(false);

        DmCommentBackfillSupport support = new DmCommentBackfillSupport("SYSDBA");
        support.backfill(
            connection,
            List.of(new DmTableCommentDefinition("QUOTE_TABLE", "带'引号'的表", List.of())));

        verify(statement, times(1)).execute("COMMENT ON TABLE QUOTE_TABLE IS '带''引号''的表'");
    }

    @Test
    void shouldSkipMissingTablesAndColumns() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableExistsQuery = mock(PreparedStatement.class);
        PreparedStatement tableCommentQuery = mock(PreparedStatement.class);
        PreparedStatement columnExistsQuery = mock(PreparedStatement.class);
        ResultSet missingTableResultSet = mock(ResultSet.class);
        ResultSet existingTableResultSet = mock(ResultSet.class);
        ResultSet existingTableCommentResultSet = mock(ResultSet.class);
        ResultSet missingColumnResultSet = mock(ResultSet.class);
        Statement statement = mock(Statement.class);

        when(connection.prepareStatement("select 1 from all_tables where owner = ? and table_name = ?"))
            .thenReturn(tableExistsQuery);
        when(connection.prepareStatement("select comments from all_tab_comments where owner = ? and table_name = ?"))
            .thenReturn(tableCommentQuery);
        when(connection.prepareStatement("select 1 from all_tab_columns where owner = ? and table_name = ? and column_name = ?"))
            .thenReturn(columnExistsQuery);
        when(connection.createStatement()).thenReturn(statement);

        when(tableExistsQuery.executeQuery()).thenReturn(missingTableResultSet, existingTableResultSet);
        when(missingTableResultSet.next()).thenReturn(false);
        when(existingTableResultSet.next()).thenReturn(true);
        when(tableCommentQuery.executeQuery()).thenReturn(existingTableCommentResultSet);
        when(existingTableCommentResultSet.next()).thenReturn(false);
        when(columnExistsQuery.executeQuery()).thenReturn(missingColumnResultSet);
        when(missingColumnResultSet.next()).thenReturn(false);

        DmCommentBackfillSupport support = new DmCommentBackfillSupport("SYSDBA");
        support.backfill(
            connection,
            List.of(
                new DmTableCommentDefinition(
                    "MISSING_TABLE",
                    "缺失表",
                    List.of(new DmColumnCommentDefinition("ANY_COL", "任意列"))),
                new DmTableCommentDefinition(
                    "EXISTING_TABLE",
                    "存在表",
                    List.of(new DmColumnCommentDefinition("MISSING_COLUMN", "缺失字段")))));

        verify(statement).execute("COMMENT ON TABLE EXISTING_TABLE IS '存在表'");
        verify(statement, never()).execute("COMMENT ON TABLE MISSING_TABLE IS '缺失表'");
        verify(statement, never()).execute("COMMENT ON COLUMN EXISTING_TABLE.MISSING_COLUMN IS '缺失字段'");
    }
}
