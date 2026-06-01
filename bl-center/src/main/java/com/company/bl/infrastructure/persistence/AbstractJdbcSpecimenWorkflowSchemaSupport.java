package com.company.bl.infrastructure.persistence;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

abstract class AbstractJdbcSpecimenWorkflowSchemaSupport {

    protected static final String SPECIMEN_VERIFICATION_STATUS_EXPRESSION =
        buildVerificationStatusExpression("sfr", "s");

    protected final NamedParameterJdbcTemplate jdbcTemplate;

    private volatile Boolean specimenContainerColumnsAvailable;
    private volatile Boolean collectionPrinterCodeColumnAvailable;
    private volatile Boolean specimenRemovalColumnsAvailable;
    private volatile Boolean transportOrderOutboundColumnsAvailable;

    protected AbstractJdbcSpecimenWorkflowSchemaSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected boolean hasSpecimenContainerColumns() {
        Boolean cached = specimenContainerColumnsAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            columnExists(connection.getMetaData(), "SPECIMENS", "CONTAINER_NAME")
                && columnExists(connection.getMetaData(), "SPECIMENS", "CONTAINER_COUNT"));
        specimenContainerColumnsAvailable = Boolean.TRUE.equals(resolved);
        return specimenContainerColumnsAvailable;
    }

    protected boolean hasCollectionPrinterCodeColumn() {
        Boolean cached = collectionPrinterCodeColumnAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            columnExists(connection.getMetaData(), "SPECIMEN_COLLECTION_RECORDS", "PRINTER_CODE"));
        collectionPrinterCodeColumnAvailable = Boolean.TRUE.equals(resolved);
        return collectionPrinterCodeColumnAvailable;
    }

    protected boolean hasSpecimenRemovalColumns() {
        Boolean cached = specimenRemovalColumnsAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            columnExists(connection.getMetaData(), "SPECIMENS", "SPECIMEN_REMOVAL_AT")
                && columnExists(connection.getMetaData(), "SPECIMENS", "SPECIMEN_REMOVAL_OPERATOR_USER_ID")
                && columnExists(connection.getMetaData(), "SPECIMENS", "SPECIMEN_REMOVAL_OPERATOR_NAME"));
        specimenRemovalColumnsAvailable = Boolean.TRUE.equals(resolved);
        return specimenRemovalColumnsAvailable;
    }

    protected boolean hasSpecimenConfirmationColumns() {
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            columnExists(connection.getMetaData(), "SPECIMENS", "SPECIMEN_CONFIRMED_AT")
                && columnExists(connection.getMetaData(), "SPECIMENS", "CHECK_IN_STATUS")
                && columnExists(connection.getMetaData(), "SPECIMENS", "CHECKED_IN_AT")
                && columnExists(connection.getMetaData(), "SPECIMENS", "CHECKED_IN_BY_NAME"));
        return Boolean.TRUE.equals(resolved);
    }

    protected boolean hasTransportOrderOutboundColumns() {
        Boolean cached = transportOrderOutboundColumnsAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            columnExists(connection.getMetaData(), "TRANSPORT_ORDERS", "OUTBOUND_USER_ID")
                && columnExists(connection.getMetaData(), "TRANSPORT_ORDERS", "OUTBOUND_USER_NAME"));
        transportOrderOutboundColumnsAvailable = Boolean.TRUE.equals(resolved);
        return transportOrderOutboundColumnsAvailable;
    }

    protected String containerNameSelect(String alias) {
        if (hasSpecimenContainerColumns()) {
            return "                " + alias + ".container_name as container_name,\n";
        }
        return "                cast(null as varchar(200)) as container_name,\n";
    }

    protected String containerCountSelect(String alias) {
        if (hasSpecimenContainerColumns()) {
            return "                " + alias + ".container_count as container_count,\n";
        }
        return "                cast(null as integer) as container_count,\n";
    }

    protected String specimenConfirmedAtSelect(String alias) {
        if (hasSpecimenConfirmationColumns()) {
            return "                " + alias + ".specimen_confirmed_at as specimen_confirmed_at,\n";
        }
        return "                cast(null as timestamp) as specimen_confirmed_at,\n";
    }

    protected String specimenRemovalAtSelect(String alias) {
        if (hasSpecimenRemovalColumns()) {
            return "                " + alias + ".specimen_removal_at as specimen_removal_at,\n";
        }
        return "                cast(null as timestamp) as specimen_removal_at,\n";
    }

    protected String specimenRemovalAtExpression(String alias) {
        if (hasSpecimenRemovalColumns()) {
            return alias + ".specimen_removal_at";
        }
        return "cast(null as timestamp)";
    }

    protected String specimenRemovalOperatorUserIdSelect(String alias) {
        if (hasSpecimenRemovalColumns()) {
            return "                " + alias + ".specimen_removal_operator_user_id as specimen_removal_operator_user_id,\n";
        }
        return "                cast(null as varchar(64)) as specimen_removal_operator_user_id,\n";
    }

    protected String specimenRemovalOperatorNameSelect(String alias) {
        if (hasSpecimenRemovalColumns()) {
            return "                " + alias + ".specimen_removal_operator_name as specimen_removal_operator_name,\n";
        }
        return "                cast(null as varchar(100)) as specimen_removal_operator_name,\n";
    }

    protected String checkInStatusSelect(String alias, String resultAlias) {
        if (hasSpecimenConfirmationColumns()) {
            return "                coalesce(" + alias + ".check_in_status, 'NOT_CHECKED_IN') as " + resultAlias + ",\n";
        }
        return "                cast('NOT_CHECKED_IN' as varchar(32)) as " + resultAlias + ",\n";
    }

    protected String checkedInAtSelect(String alias) {
        if (hasSpecimenConfirmationColumns()) {
            return "                " + alias + ".checked_in_at as checked_in_at,\n";
        }
        return "                cast(null as timestamp) as checked_in_at,\n";
    }

    protected String checkedInByNameSelect(String alias) {
        if (hasSpecimenConfirmationColumns()) {
            return "                " + alias + ".checked_in_by_name as checked_in_by_name,\n";
        }
        return "                cast(null as varchar(100)) as checked_in_by_name,\n";
    }

    protected String outboundUserIdSelect(String alias) {
        if (hasTransportOrderOutboundColumns()) {
            return "                " + alias + ".outbound_user_id as outbound_user_id,\n";
        }
        return "                cast(null as varchar(64)) as outbound_user_id,\n";
    }

    protected String outboundUserNameSelect(String alias) {
        if (hasTransportOrderOutboundColumns()) {
            return "                " + alias + ".outbound_user_name as outbound_user_name,\n";
        }
        return "                cast(null as varchar(100)) as outbound_user_name,\n";
    }

    protected String specimenSelectColumns() {
        return """
            select
                s.*,
                %s as verification_status,
            """.formatted(SPECIMEN_VERIFICATION_STATUS_EXPRESSION)
            + checkInStatusSelect("s", "resolved_check_in_status")
            + """
                sfr.verification_started_at as verification_started_at,
                coalesce(sfr.verification_completed_at, sfr.verified_at) as verification_completed_at,
            """
            + specimenConfirmedAtSelect("s")
            + specimenRemovalAtSelect("s")
            + specimenRemovalOperatorUserIdSelect("s")
            + specimenRemovalOperatorNameSelect("s")
            + checkedInAtSelect("s")
            + checkedInByNameSelect("s")
            + """
                latest_receipt.receipt_status as latest_receipt_status,
                latest_receipt.quality_check_result as latest_quality_check_result,
                latest_receipt.quality_issue_codes as latest_quality_issue_codes
            from specimens s
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            left join (
                select
                    ranked.specimen_id,
                    ranked.receipt_status,
                    ranked.quality_check_result,
                    ranked.quality_issue_codes
                from (
                    select
                        sr.*,
                        row_number() over (
                            partition by sr.specimen_id
                            order by sr.received_at desc, sr.id desc
                        ) as rn
                    from specimen_receipts sr
                ) ranked
                where ranked.rn = 1
            ) latest_receipt on latest_receipt.specimen_id = s.id
            """;
    }

    protected String transportOrderSelectColumns() {
        return """
            select
                t.id,
                t.transport_order_no,
                t.application_id,
                t.order_status,
                t.handover_user_id,
                t.handover_user_name,
                t.handover_department_id,
                t.handover_department_name,
                t.receiver_department_id,
                t.receiver_department_name,
                t.receiver_user_id,
                t.receiver_user_name,
            """
            + outboundUserIdSelect("t")
            + outboundUserNameSelect("t")
            + """
                t.printed_at,
                t.to_be_transported_at,
                t.handed_over_at,
                t.terminal_code,
                t.remarks
            from transport_orders t
            """;
    }

    protected static String buildVerificationStatusExpression(String fixationRecordAlias, String specimenAlias) {
        return """
            case
                when coalesce(%s.verification_completed_at, %s.verified_at) is not null
                    or %s.specimen_status in ('VERIFIED', 'FIXING', 'FIXED', 'CHECKED_IN', 'IN_TRANSIT', 'RECEIVED', 'REJECTED', 'RETURNED')
                    or coalesce(%s.fixation_status, 'PENDING') <> 'PENDING'
                then 'VERIFIED'
                when %s.verification_started_at is not null
                then 'VERIFYING'
                else 'UNVERIFIED'
            end
            """.formatted(
            fixationRecordAlias,
            fixationRecordAlias,
            specimenAlias,
            specimenAlias,
            fixationRecordAlias
        ).trim();
    }

    private boolean columnExists(DatabaseMetaData metadata, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, tableName, columnName)) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                    && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet columns = metadata.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                    && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
