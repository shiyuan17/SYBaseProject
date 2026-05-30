package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

abstract class AbstractJdbcSpecimenWorkflowReadSupport extends AbstractJdbcSpecimenWorkflowRowMapperSupport {

    protected AbstractJdbcSpecimenWorkflowReadSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
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
}
