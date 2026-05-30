package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

abstract class AbstractJdbcSpecimenWorkflowReadSupport {

    private static final String SPECIMEN_VERIFICATION_STATUS_EXPRESSION =
        buildVerificationStatusExpression("sfr", "s");

    protected final NamedParameterJdbcTemplate jdbcTemplate;

    private volatile Boolean specimenContainerColumnsAvailable;
    private volatile Boolean collectionPrinterCodeColumnAvailable;
    private volatile Boolean specimenRemovalColumnsAvailable;

    protected AbstractJdbcSpecimenWorkflowReadSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Specimen> findSpecimenByBarcode(String barcode) {
        List<Specimen> rows = jdbcTemplate.query(
            specimenSelectColumns() + """
                where s.barcode = :barcode
                """,
            Map.of("barcode", barcode),
            this::mapSpecimen);
        return rows.stream().findFirst();
    }

    public List<Specimen> findSpecimensBySpecimenNo(String specimenNo) {
        return jdbcTemplate.query(
            specimenSelectColumns() + """
                where s.specimen_no = :specimenNo
                order by s.registered_at asc, s.created_at asc
                """,
            Map.of("specimenNo", specimenNo),
            this::mapSpecimen);
    }

    public List<Specimen> findSpecimensByApplicationId(String applicationId) {
        return jdbcTemplate.query(
            specimenSelectColumns() + """
                where s.application_id = :applicationId
                order by s.registered_at asc, s.created_at asc
                """,
            Map.of("applicationId", applicationId),
            this::mapSpecimen);
    }

    public Optional<PathologyCase> findPathologyCaseByApplicationId(String applicationId) {
        List<PathologyCase> rows = jdbcTemplate.query("""
            select *
            from pathology_cases
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId), this::mapPathologyCase);
        return rows.stream().findFirst();
    }

    public Optional<TransportOrder> findTransportOrderById(String transportOrderId) {
        List<TransportOrder> rows = jdbcTemplate.query("""
            select *
            from transport_orders
            where id = :id
            """, Map.of("id", transportOrderId), this::mapTransportOrder);
        return rows.stream().findFirst();
    }

    public List<TransportOrderItem> findTransportOrderItems(String transportOrderId) {
        return jdbcTemplate.query("""
            select *
            from transport_order_items
            where transport_order_id = :transportOrderId
            order by verified_at asc, id asc
            """, Map.of("transportOrderId", transportOrderId), this::mapTransportOrderItem);
    }

    public List<String> findTransportOrderSpecimenBarcodes(String transportOrderId) {
        return jdbcTemplate.query("""
            select s.barcode
            from transport_order_items toi
            join specimens s on s.id = toi.specimen_id
            where toi.transport_order_id = :transportOrderId
            order by s.registered_at asc, s.id asc
            """, Map.of("transportOrderId", transportOrderId), (rs, rowNum) -> rs.getString("barcode"));
    }

    public List<TrackingEvent> findTrackingEventsByApplicationId(String applicationId) {
        return jdbcTemplate.query("""
            select *
            from workflow_events
            where application_id = :applicationId
            order by event_time asc, created_at asc
            """, Map.of("applicationId", applicationId), this::mapTrackingEvent);
    }

    public Optional<String> findApplicationIdByBarcode(String barcode) {
        List<String> rows = jdbcTemplate.query("""
            select application_id
            from specimens
            where barcode = :barcode
            """, Map.of("barcode", barcode), (rs, rowNum) -> rs.getString("application_id"));
        return rows.stream().findFirst();
    }

    public boolean existsApplicationByExternalSource(String externalOrderNo, String thirdPartySource) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from applications
            where external_order_no = :externalOrderNo
              and third_party_source = :thirdPartySource
            """, new MapSqlParameterSource()
            .addValue("externalOrderNo", externalOrderNo)
            .addValue("thirdPartySource", thirdPartySource), Long.class);
        return count != null && count > 0;
    }

    public List<Specimen> findSpecimensByLabelPrintBatchNoAndStatus(String labelPrintBatchNo, String labelPrintStatus) {
        return jdbcTemplate.query(
            specimenSelectColumns() + """
                where s.label_print_batch_no = :labelPrintBatchNo
                  and s.label_print_status = :labelPrintStatus
                order by s.registered_at asc, s.id asc
                """,
            new MapSqlParameterSource()
                .addValue("labelPrintBatchNo", labelPrintBatchNo)
                .addValue("labelPrintStatus", labelPrintStatus),
            this::mapSpecimen);
    }

    public List<Specimen> findSpecimensByLabelPrintBatchNoAndStatuses(String labelPrintBatchNo, List<String> labelPrintStatuses) {
        return jdbcTemplate.query(
            specimenSelectColumns() + """
                where s.label_print_batch_no = :labelPrintBatchNo
                  and s.label_print_status in (:labelPrintStatuses)
                order by s.registered_at asc, s.id asc
                """,
            new MapSqlParameterSource()
                .addValue("labelPrintBatchNo", labelPrintBatchNo)
                .addValue("labelPrintStatuses", labelPrintStatuses),
            this::mapSpecimen);
    }

    public Optional<SpecimenWorkflowRepository.RegistrationSnapshotData> findRegistrationSnapshotByApplicationIdAndBatchNo(
        String applicationId,
        String labelPrintBatchNo
    ) {
        String printerCodeSelect = hasCollectionPrinterCodeColumn()
            ? "printer_code"
            : "cast(null as varchar(64)) as printer_code";
        List<SpecimenWorkflowRepository.RegistrationSnapshotData> rows = jdbcTemplate.query("""
            select
                collection_scene,
                collector_user_id,
                collector_name,
                %s,
                terminal_code,
                remarks
            from specimen_collection_records
            where application_id = :applicationId
              and label_print_batch_no = :labelPrintBatchNo
            order by collected_at desc, id desc
            fetch next 1 rows only
            """.formatted(printerCodeSelect), new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("labelPrintBatchNo", labelPrintBatchNo), this::mapRegistrationSnapshot);
        return rows.stream().findFirst();
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

    private Specimen mapSpecimen(ResultSet rs, int rowNum) throws SQLException {
        return new Specimen(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("case_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            rs.getString("specimen_type"),
            rs.getString("specimen_name_standardized"),
            rs.getString("specimen_site"),
            rs.getString("collection_mode"),
            rs.getObject("specimen_count", Integer.class),
            JdbcResultSetUtils.getNullableString(rs, "container_name"),
            JdbcResultSetUtils.getNullableInteger(rs, "container_count"),
            SpecimenStatus.from(rs.getString("specimen_status")),
            FixationStatus.from(rs.getString("fixation_status")),
            JdbcResultSetUtils.getNullableString(rs, "verification_status"),
            rs.getTimestamp("verification_started_at") == null
                ? null
                : rs.getTimestamp("verification_started_at").toLocalDateTime(),
            rs.getTimestamp("verification_completed_at") == null
                ? null
                : rs.getTimestamp("verification_completed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "specimen_removal_operator_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_removal_operator_name"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "resolved_check_in_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "checked_in_by_name"),
            rs.getInt("qualified_flag") != 0,
            rs.getString("unqualified_reason"),
            rs.getString("latest_receipt_status"),
            rs.getString("latest_quality_check_result"),
            rs.getString("latest_quality_issue_codes"),
            rs.getString("clinical_symptom"),
            rs.getString("applicant_department_id"),
            rs.getString("applicant_department_name"),
            rs.getString("applicant_doctor_user_id"),
            rs.getString("applicant_doctor_name"),
            rs.getDate("submission_date") == null ? null : rs.getDate("submission_date").toLocalDate(),
            rs.getString("label_print_batch_no"),
            rs.getString("label_print_status"),
            rs.getString("registered_by_user_id"),
            rs.getString("registered_by_name"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    private PathologyCase mapPathologyCase(ResultSet rs, int rowNum) throws SQLException {
        return new PathologyCase(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("pathology_no"),
            rs.getString("case_status"),
            rs.getString("source_hospital_id"),
            rs.getString("source_hospital_name"),
            rs.getString("source_department_id"),
            rs.getString("source_department_name"),
            rs.getString("received_by_user_id"),
            rs.getString("received_by_name"),
            rs.getTimestamp("received_at") == null ? null : rs.getTimestamp("received_at").toLocalDateTime());
    }

    private SpecimenWorkflowRepository.RegistrationSnapshotData mapRegistrationSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.RegistrationSnapshotData(
            rs.getString("collection_scene"),
            rs.getString("collector_user_id"),
            rs.getString("collector_name"),
            JdbcResultSetUtils.getNullableString(rs, "printer_code"),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    private TransportOrder mapTransportOrder(ResultSet rs, int rowNum) throws SQLException {
        return new TransportOrder(
            rs.getString("id"),
            rs.getString("transport_order_no"),
            rs.getString("application_id"),
            TransportOrderStatus.from(rs.getString("order_status")),
            rs.getString("handover_user_id"),
            rs.getString("handover_user_name"),
            rs.getString("handover_department_id"),
            rs.getString("handover_department_name"),
            rs.getString("receiver_department_id"),
            rs.getString("receiver_department_name"),
            rs.getString("receiver_user_id"),
            rs.getString("receiver_user_name"),
            rs.getTimestamp("printed_at") == null ? null : rs.getTimestamp("printed_at").toLocalDateTime(),
            rs.getTimestamp("to_be_transported_at") == null ? null : rs.getTimestamp("to_be_transported_at").toLocalDateTime(),
            rs.getTimestamp("handed_over_at") == null ? null : rs.getTimestamp("handed_over_at").toLocalDateTime(),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    private TransportOrderItem mapTransportOrderItem(ResultSet rs, int rowNum) throws SQLException {
        return new TransportOrderItem(
            rs.getString("id"),
            rs.getString("transport_order_id"),
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            TransportItemStatus.from(rs.getString("item_status")),
            rs.getString("verification_result"),
            rs.getString("verified_by_user_id"),
            rs.getString("verified_by_name"),
            rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toLocalDateTime(),
            rs.getString("remarks"));
    }

    private TrackingEvent mapTrackingEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TrackingEvent(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            rs.getString("case_id"),
            rs.getString("transport_order_id"),
            rs.getString("node_code"),
            rs.getString("event_type"),
            rs.getString("event_status"),
            rs.getTimestamp("event_time").toLocalDateTime(),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("source_terminal"),
            rs.getString("event_content"));
    }
}
