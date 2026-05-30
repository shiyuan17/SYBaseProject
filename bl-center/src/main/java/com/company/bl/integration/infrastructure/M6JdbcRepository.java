package com.company.bl.integration.infrastructure;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class M6JdbcRepository {

    private final JdbcM6IntegrationTaskStore integrationTaskStore;
    private final JdbcM6BillingStore billingStore;
    private final JdbcM6HistoricalReportStore historicalReportStore;
    private final JdbcM6StatisticsStore statisticsStore;

    public M6JdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.integrationTaskStore = new JdbcM6IntegrationTaskStore(jdbcTemplate);
        this.billingStore = new JdbcM6BillingStore(jdbcTemplate);
        this.historicalReportStore = new JdbcM6HistoricalReportStore(jdbcTemplate);
        this.statisticsStore = new JdbcM6StatisticsStore(jdbcTemplate);
    }

    public void insertIntegrationTask(M6IntegrationTaskRows.CreateIntegrationTaskRow row) {
        integrationTaskStore.insertIntegrationTask(row);
    }

    public void updateIntegrationTask(M6IntegrationTaskRows.IntegrationTaskRow row) {
        integrationTaskStore.updateIntegrationTask(row);
    }

    public M6IntegrationTaskRows.IntegrationTaskRow findIntegrationTaskById(String id) {
        return integrationTaskStore.findIntegrationTaskById(id);
    }

    public M6IntegrationTaskRows.IntegrationTaskRow findLatestIntegrationTask(String businessType, String businessId, String stageCode) {
        return integrationTaskStore.findLatestIntegrationTask(businessType, businessId, stageCode);
    }

    public List<M6IntegrationTaskRows.IntegrationTaskRow> findIntegrationTasks(String taskType,
                                                                               String businessType,
                                                                               String businessId,
                                                                               String taskStatus,
                                                                               String stageCode,
                                                                               String externalSystem,
                                                                               String compensationStatus,
                                                                               String reconciliationStatus) {
        return integrationTaskStore.findIntegrationTasks(
            taskType, businessType, businessId, taskStatus, stageCode, externalSystem, compensationStatus, reconciliationStatus);
    }

    public void insertBillingRecord(M6BillingRows.CreateBillingRecordRow row) {
        billingStore.insertBillingRecord(row);
    }

    public void updateBillingRecord(M6BillingRows.BillingRecordRow row) {
        billingStore.updateBillingRecord(row);
    }

    public M6BillingRows.BillingRecordRow findBillingRecordById(String id) {
        return billingStore.findBillingRecordById(id);
    }

    public List<M6BillingRows.BillingRecordRow> findBillingRecords(String billingStatus,
                                                                   String billingStage,
                                                                   String externalSystem,
                                                                   String caseId,
                                                                   String orderId,
                                                                   LocalDateTime from,
                                                                   LocalDateTime to) {
        return billingStore.findBillingRecords(billingStatus, billingStage, externalSystem, caseId, orderId, from, to);
    }

    public void updateMedicalOrderBillingStatus(String orderId, String billingStatus) {
        billingStore.updateMedicalOrderBillingStatus(orderId, billingStatus);
    }

    public void insertHistoricalImportJob(M6HistoricalReportRows.CreateHistoricalImportJobRow row) {
        historicalReportStore.insertHistoricalImportJob(row);
    }

    public void updateHistoricalImportJob(M6HistoricalReportRows.HistoricalImportJobRow row) {
        historicalReportStore.updateHistoricalImportJob(row);
    }

    public M6HistoricalReportRows.HistoricalImportJobRow findHistoricalImportJobById(String id) {
        return historicalReportStore.findHistoricalImportJobById(id);
    }

    public List<M6HistoricalReportRows.HistoricalImportJobRow> findHistoricalImportJobs(String sourceSystem,
                                                                                        String importStatus,
                                                                                        String patientId,
                                                                                        String pathologyNo,
                                                                                        String applicationNo) {
        return historicalReportStore.findHistoricalImportJobs(sourceSystem, importStatus, patientId, pathologyNo, applicationNo);
    }

    public M6HistoricalReportRows.HistoricalReportRow findHistoricalReportBySourceAndExternalNo(String sourceSystem, String externalReportNo) {
        return historicalReportStore.findHistoricalReportBySourceAndExternalNo(sourceSystem, externalReportNo);
    }

    public void insertHistoricalReport(M6HistoricalReportRows.CreateHistoricalReportRow row) {
        historicalReportStore.insertHistoricalReport(row);
    }

    public void updateHistoricalReport(M6HistoricalReportRows.HistoricalReportRow row) {
        historicalReportStore.updateHistoricalReport(row);
    }

    public void deleteHistoricalReportVersions(String historicalReportId) {
        historicalReportStore.deleteHistoricalReportVersions(historicalReportId);
    }

    public void insertHistoricalReportVersion(M6HistoricalReportRows.CreateHistoricalReportVersionRow row) {
        historicalReportStore.insertHistoricalReportVersion(row);
    }

    public List<M6HistoricalReportRows.HistoricalReportRow> findHistoricalReports(String sourceSystem,
                                                                                  String patientId,
                                                                                  String pathologyNo,
                                                                                  String applicationNo,
                                                                                  String externalReportNo,
                                                                                  LocalDateTime from,
                                                                                  LocalDateTime to) {
        return historicalReportStore.findHistoricalReports(sourceSystem, patientId, pathologyNo, applicationNo, externalReportNo, from, to);
    }

    public List<M6HistoricalReportRows.HistoricalReportVersionRow> findHistoricalReportVersions(String historicalReportId) {
        return historicalReportStore.findHistoricalReportVersions(historicalReportId);
    }

    public List<M6StatisticsRows.StatIndicatorDefinitionRow> findStatIndicatorDefinitions(String category) {
        return statisticsStore.findStatIndicatorDefinitions(category);
    }

    public M6StatisticsRows.StatIndicatorDefinitionRow findStatIndicatorDefinitionByCode(String indicatorCode) {
        return statisticsStore.findStatIndicatorDefinitionByCode(indicatorCode);
    }

    public List<M6StatisticsRows.StatReportTemplateRow> findStatReportTemplates(String templateType) {
        return statisticsStore.findStatReportTemplates(templateType);
    }

    public M6StatisticsRows.StatReportTemplateRow findStatReportTemplateByCode(String templateCode) {
        return statisticsStore.findStatReportTemplateByCode(templateCode);
    }

    public void insertStatExportJob(M6StatisticsRows.CreateStatExportJobRow row) {
        statisticsStore.insertStatExportJob(row);
    }

    public void completeStatExportJob(String id, String exportStatus, String errorMessage, LocalDateTime completedAt) {
        statisticsStore.completeStatExportJob(id, exportStatus, errorMessage, completedAt);
    }

    public long countIntegrationTasksByStatus(String taskStatus) {
        return integrationTaskStore.countIntegrationTasksByStatus(taskStatus);
    }

    public long countIntegrationTasksByCompensationStatus(String compensationStatus) {
        return integrationTaskStore.countIntegrationTasksByCompensationStatus(compensationStatus);
    }

    public long countIntegrationTasksByBusinessAndReconciliationStatus(String businessType, String reconciliationStatus) {
        return integrationTaskStore.countIntegrationTasksByBusinessAndReconciliationStatus(businessType, reconciliationStatus);
    }

    public long countHistoricalImportJobsByStatus(String importStatus) {
        return historicalReportStore.countHistoricalImportJobsByStatus(importStatus);
    }
}
