package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

abstract class AbstractJdbcSpecimenWorkflowProjectionSupport extends AbstractJdbcSpecimenWorkflowReadSupport {

    private final JdbcSpecimenWorkflowPendingProjectionSupport pendingProjectionSupport;
    private final JdbcSpecimenWorkflowManagementProjectionSupport managementProjectionSupport;
    private final JdbcSpecimenWorkflowRemovalProjectionSupport removalProjectionSupport;

    protected AbstractJdbcSpecimenWorkflowProjectionSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.pendingProjectionSupport = new JdbcSpecimenWorkflowPendingProjectionSupport(jdbcTemplate);
        this.managementProjectionSupport = new JdbcSpecimenWorkflowManagementProjectionSupport(jdbcTemplate);
        this.removalProjectionSupport = new JdbcSpecimenWorkflowRemovalProjectionSupport(jdbcTemplate);
    }

    public SpecimenWorkflowRepository.PagedPendingSpecimens findPendingFixations(SpecimenWorkflowRepository.PendingSpecimenQuery query) {
        return pendingProjectionSupport.findPendingFixations(query);
    }

    public SpecimenWorkflowRepository.PagedPendingSpecimens findPendingReceipts(SpecimenWorkflowRepository.PendingSpecimenQuery query) {
        return pendingProjectionSupport.findPendingReceipts(query);
    }

    public SpecimenWorkflowRepository.PagedPendingTransportOrders findPendingTransportOrders(
        SpecimenWorkflowRepository.PendingTransportOrderQuery query
    ) {
        return pendingProjectionSupport.findPendingTransportOrders(query);
    }

    public SpecimenWorkflowRepository.PagedSpecimenOutbounds findSpecimenOutbounds(
        SpecimenWorkflowRepository.SpecimenOutboundListQuery query
    ) {
        return removalProjectionSupport.findSpecimenOutbounds(query);
    }

    public List<SpecimenWorkflowRepository.DuplicateApplicationRow> findDuplicateApplications(
        SpecimenWorkflowRepository.DuplicateApplicationQuery query
    ) {
        return managementProjectionSupport.findDuplicateApplications(query);
    }

    public SpecimenWorkflowRepository.PagedSpecimenManagementItems findSpecimenManagementItems(
        SpecimenWorkflowRepository.SpecimenManagementListQuery query
    ) {
        return managementProjectionSupport.findSpecimenManagementItems(query);
    }

    public SpecimenWorkflowRepository.PagedSpecimenRemovalItems findSpecimenRemovalItems(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    ) {
        return removalProjectionSupport.findSpecimenRemovalItems(query);
    }

    public List<SpecimenWorkflowRepository.SpecimenRemovalListRow> listSpecimenRemovalExportRows(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    ) {
        return removalProjectionSupport.listSpecimenRemovalExportRows(query);
    }
}
