package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class JdbcSpecimenWorkflowQueryRepository
    extends AbstractJdbcSpecimenWorkflowProjectionSupport
    implements SpecimenWorkflowQueryRepository {

    private final JdbcSpecimenWorkflowRepository commandRepository;

    public JdbcSpecimenWorkflowQueryRepository(NamedParameterJdbcTemplate jdbcTemplate,
                                              JdbcSpecimenWorkflowRepository commandRepository) {
        super(jdbcTemplate);
        this.commandRepository = commandRepository;
    }

    @Override
    public SpecimenWorkflowRepository.PagedApplications findApplications(SpecimenWorkflowRepository.ApplicationListQuery query) {
        return commandRepository.findApplications(query);
    }

    @Override
    public ApplicationTracking getApplicationTracking(String applicationId, Application application) {
        List<Specimen> specimens = findSpecimensByApplicationId(applicationId);
        List<TrackingEvent> events = findTrackingEventsByApplicationId(applicationId);
        boolean abnormal = specimens.stream().anyMatch(specimen ->
            specimen.specimenStatus() == SpecimenStatus.REJECTED
                || specimen.specimenStatus() == SpecimenStatus.RETURNED
                || specimen.fixationStatus() == FixationStatus.ABNORMAL);
        String currentNode = application.getStatus() == ApplicationStatus.VOIDED
            ? application.getStatus().name()
            : events.isEmpty()
                ? application.getStatus().name()
                : events.get(events.size() - 1).nodeCode();
        return new ApplicationTracking(application, currentNode, abnormal, specimens, events);
    }

    @Override
    public List<SpecimenWorkflowRepository.SpecimenVerificationRecordRow> listSpecimenVerificationRecords(String barcode) {
        return jdbcTemplate.query("""
            select
                records.application_id,
                records.specimen_id,
                records.barcode,
                records.verification_type,
                records.result,
                records.operator_name,
                records.terminal_code,
                records.remarks,
                records.verified_at
            from (
                select
                    we.application_id,
                    we.specimen_id,
                    s.barcode,
                    case
                        when we.node_code = 'VERIFICATION' and we.event_type = 'STARTED' then 'SPECIMEN_VERIFICATION_START'
                        when we.node_code = 'VERIFICATION' and we.event_type = 'COMPLETED' then 'SPECIMEN_VERIFICATION_COMPLETE'
                        when we.node_code = 'CONFIRMATION' and we.event_type = 'COMPLETED' then 'SPECIMEN_CONFIRM'
                        when we.node_code = 'CHECK_IN' and we.event_type = 'CHECKED_IN' then 'SPECIMEN_CHECK_IN'
                        else we.event_type
                    end as verification_type,
                    we.event_status as result,
                    we.operator_name,
                    we.source_terminal as terminal_code,
                    we.event_content as remarks,
                    we.event_time as verified_at
                from workflow_events we
                join specimens s on s.id = we.specimen_id
                where s.barcode = :barcode
                  and we.node_code in ('VERIFICATION', 'CONFIRMATION', 'CHECK_IN')

                union all

                select
                    toi.application_id,
                    toi.specimen_id,
                    s.barcode,
                    'TRANSPORT_HANDOVER_VERIFICATION' as verification_type,
                    coalesce(toi.verification_result, toi.item_status) as result,
                    toi.verified_by_name as operator_name,
                    cast(null as varchar(64)) as terminal_code,
                    toi.remarks,
                    toi.verified_at
                from transport_order_items toi
                join specimens s on s.id = toi.specimen_id
                where s.barcode = :barcode
                  and toi.verified_at is not null
            ) records
            order by records.verified_at desc, records.verification_type desc
            """, Map.of("barcode", barcode), (rs, rowNum) -> new SpecimenWorkflowRepository.SpecimenVerificationRecordRow(
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            rs.getString("barcode"),
            rs.getString("verification_type"),
            rs.getString("result"),
            rs.getString("operator_name"),
            rs.getString("terminal_code"),
            rs.getString("remarks"),
            rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toLocalDateTime()
        ));
    }
}
