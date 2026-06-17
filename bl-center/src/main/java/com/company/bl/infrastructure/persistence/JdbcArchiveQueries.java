package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ArchiveRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcArchiveQueries {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcArchiveQueries(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<ArchiveRepository.ArchiveCabinet> findArchiveCabinets() {
        return jdbcTemplate.query("""
            select id, cabinet_code, cabinet_name, cabinet_type, layer_count, slot_count_per_layer,
                   capacity, cabinet_status, location_description, remarks
            from archive_cabinets
            order by cabinet_code asc
            """, this::mapArchiveCabinet);
    }

    Optional<ArchiveRepository.ArchiveCabinet> findArchiveCabinetById(String cabinetId) {
        return jdbcTemplate.query("""
            select id, cabinet_code, cabinet_name, cabinet_type, layer_count, slot_count_per_layer,
                   capacity, cabinet_status, location_description, remarks
            from archive_cabinets
            where id = :id
            """, Map.of("id", cabinetId), this::mapArchiveCabinet).stream().findFirst();
    }

    Optional<ArchiveRepository.ArchiveCabinet> findArchiveCabinetByCode(String cabinetCode) {
        return jdbcTemplate.query("""
            select id, cabinet_code, cabinet_name, cabinet_type, layer_count, slot_count_per_layer,
                   capacity, cabinet_status, location_description, remarks
            from archive_cabinets
            where cabinet_code = :cabinetCode
            """, Map.of("cabinetCode", cabinetCode), this::mapArchiveCabinet).stream().findFirst();
    }

    boolean existsArchiveCabinetByCodes(List<String> cabinetCodes) {
        if (cabinetCodes.isEmpty()) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
            select count(*)
            from archive_cabinets
            where cabinet_code in (:cabinetCodes)
            """, Map.of("cabinetCodes", cabinetCodes), Integer.class);
        return count != null && count > 0;
    }

    List<ArchiveRepository.ArchiveCabinetNode> findArchiveCabinetNodes() {
        return jdbcTemplate.query("""
            select acn.id, acn.parent_id, acn.node_code, acn.node_type, acn.cabinet_type, acn.cabinet_id,
                   acn.layer_no, acn.capacity, acn.path_location, acn.remarks,
                   case
                       when acn.node_type = 'AREA' then (
                           select count(*)
                           from archive_positions ap
                           join archive_cabinets ac on ac.id = ap.cabinet_id
                           join archive_cabinet_nodes cabinet_node on cabinet_node.cabinet_id = ac.id
                               and cabinet_node.node_type = 'CABINET'
                           where cabinet_node.parent_id = acn.id
                             and ap.position_status = 'AVAILABLE'
                       )
                       when acn.node_type = 'CABINET' then (
                           select count(*)
                           from archive_positions ap
                           where ap.cabinet_id = acn.cabinet_id
                             and ap.position_status = 'AVAILABLE'
                       )
                       when acn.node_type = 'DRAWER' then (
                           select count(*)
                           from archive_positions ap
                           where ap.cabinet_id = acn.cabinet_id
                             and ap.layer_no = acn.layer_no
                             and ap.position_status = 'AVAILABLE'
                       )
                       else 0
                   end as remaining_capacity
            from archive_cabinet_nodes acn
            order by
                case acn.node_type when 'AREA' then 1 when 'CABINET' then 2 when 'DRAWER' then 3 else 9 end,
                acn.node_code asc,
                acn.layer_no asc nulls first
            """, this::mapArchiveCabinetNode);
    }

    Optional<ArchiveRepository.ArchiveCabinetNode> findArchiveCabinetNodeById(String nodeId) {
        return jdbcTemplate.query("""
            select acn.id, acn.parent_id, acn.node_code, acn.node_type, acn.cabinet_type, acn.cabinet_id,
                   acn.layer_no, acn.capacity, acn.path_location, acn.remarks,
                   case
                       when acn.node_type = 'CABINET' then (
                           select count(*) from archive_positions ap
                           where ap.cabinet_id = acn.cabinet_id and ap.position_status = 'AVAILABLE'
                       )
                       when acn.node_type = 'DRAWER' then (
                           select count(*) from archive_positions ap
                           where ap.cabinet_id = acn.cabinet_id and ap.layer_no = acn.layer_no and ap.position_status = 'AVAILABLE'
                       )
                       else 0
                   end as remaining_capacity
            from archive_cabinet_nodes acn
            where acn.id = :id
            """, Map.of("id", nodeId), this::mapArchiveCabinetNode).stream().findFirst();
    }

    Optional<ArchiveRepository.ArchiveCabinetNode> findArchiveCabinetNodeByCabinetIdAndType(String cabinetId, String nodeType) {
        return jdbcTemplate.query("""
            select acn.id, acn.parent_id, acn.node_code, acn.node_type, acn.cabinet_type, acn.cabinet_id,
                   acn.layer_no, acn.capacity, acn.path_location, acn.remarks,
                   case
                       when acn.node_type = 'CABINET' then (
                           select count(*) from archive_positions ap
                           where ap.cabinet_id = acn.cabinet_id and ap.position_status = 'AVAILABLE'
                       )
                       when acn.node_type = 'DRAWER' then (
                           select count(*) from archive_positions ap
                           where ap.cabinet_id = acn.cabinet_id and ap.layer_no = acn.layer_no and ap.position_status = 'AVAILABLE'
                       )
                       else 0
                   end as remaining_capacity
            from archive_cabinet_nodes acn
            where acn.cabinet_id = :cabinetId
              and acn.node_type = :nodeType
            """, new MapSqlParameterSource()
            .addValue("cabinetId", cabinetId)
            .addValue("nodeType", nodeType), this::mapArchiveCabinetNode).stream().findFirst();
    }

    Optional<ArchiveRepository.ArchiveCabinetNode> findArchiveCabinetNodeByCabinetIdAndLayerNo(String cabinetId, int layerNo) {
        return jdbcTemplate.query("""
            select acn.id, acn.parent_id, acn.node_code, acn.node_type, acn.cabinet_type, acn.cabinet_id,
                   acn.layer_no, acn.capacity, acn.path_location, acn.remarks,
                   (
                       select count(*) from archive_positions ap
                       where ap.cabinet_id = acn.cabinet_id and ap.layer_no = acn.layer_no and ap.position_status = 'AVAILABLE'
                   ) as remaining_capacity
            from archive_cabinet_nodes acn
            where acn.cabinet_id = :cabinetId
              and acn.node_type = 'DRAWER'
              and acn.layer_no = :layerNo
            """, new MapSqlParameterSource()
            .addValue("cabinetId", cabinetId)
            .addValue("layerNo", layerNo), this::mapArchiveCabinetNode).stream().findFirst();
    }

    boolean hasNonEmptyArchivePositions(String cabinetId) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*)
            from archive_positions
            where cabinet_id = :cabinetId
              and (
                  position_status <> 'AVAILABLE'
                  or current_object_type is not null
                  or current_object_id is not null
              )
            """, Map.of("cabinetId", cabinetId), Integer.class);
        return count != null && count > 0;
    }

    boolean hasArchivePositionReferences(String cabinetId) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*)
            from archive_positions ap
            where ap.cabinet_id = :cabinetId
              and (
                  exists (
                      select 1
                      from specimen_storage_records ssr
                      where ssr.archive_position_id = ap.id
                  )
                  or exists (
                      select 1
                      from material_loans ml
                      where ml.archive_position_id = ap.id
                  )
              )
            """, Map.of("cabinetId", cabinetId), Integer.class);
        return count != null && count > 0;
    }

    List<ArchiveRepository.ArchivePosition> findAvailableArchivePositions(String cabinetType, String cabinetId) {
        return jdbcTemplate.query("""
            select ap.id, ap.cabinet_id, ap.position_code, ap.layer_no, ap.slot_no, ap.position_status,
                   ap.current_object_type, ap.current_object_id, ap.remarks
            from archive_positions ap
            join archive_cabinets ac on ac.id = ap.cabinet_id
            where ap.position_status = 'AVAILABLE'
              and ac.cabinet_status = 'ACTIVE'
              and (:cabinetType is null or ac.cabinet_type = :cabinetType)
              and (:cabinetId is null or ap.cabinet_id = :cabinetId)
            order by ac.cabinet_code asc, ap.layer_no asc, ap.slot_no asc
            """, new MapSqlParameterSource()
            .addValue("cabinetType", cabinetType)
            .addValue("cabinetId", cabinetId), this::mapArchivePosition);
    }

    List<ArchiveRepository.ArchivePosition> findArchivePositionsByCabinetId(String cabinetId) {
        return jdbcTemplate.query("""
            select id, cabinet_id, position_code, layer_no, slot_no, position_status,
                   current_object_type, current_object_id, remarks
            from archive_positions
            where cabinet_id = :cabinetId
            order by layer_no asc, slot_no asc
            """, Map.of("cabinetId", cabinetId), this::mapArchivePosition);
    }

    Optional<ArchiveRepository.ArchivePosition> findArchivePositionById(String positionId) {
        return jdbcTemplate.query("""
            select id, cabinet_id, position_code, layer_no, slot_no, position_status,
                   current_object_type, current_object_id, remarks
            from archive_positions
            where id = :id
            """, Map.of("id", positionId), this::mapArchivePosition).stream().findFirst();
    }

    List<ArchiveRepository.ArchivePosition> findAvailableArchivePositionsByCabinetId(String cabinetId, int limit) {
        return jdbcTemplate.query("""
            select id, cabinet_id, position_code, layer_no, slot_no, position_status,
                   current_object_type, current_object_id, remarks
            from archive_positions
            where cabinet_id = :cabinetId
              and position_status = 'AVAILABLE'
              and current_object_type is null
              and current_object_id is null
            order by layer_no asc, slot_no asc
            fetch first :limit rows only
            """, new MapSqlParameterSource()
            .addValue("cabinetId", cabinetId)
            .addValue("limit", limit), this::mapArchivePosition);
    }

    Optional<ArchiveRepository.StorageRecord> findStorageRecord(String objectType, String objectId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, object_type, object_id, storage_status, storage_location,
                   archive_position_id, cabinet_no, layer_no, slot_no, stored_by_user_id, stored_by_name,
                   stored_at, archive_expires_at, archive_reminder_days, remarks
            from specimen_storage_records
            where object_type = :objectType and object_id = :objectId
            """, new MapSqlParameterSource()
            .addValue("objectType", objectType)
            .addValue("objectId", objectId), this::mapStorageRecord).stream().findFirst();
    }

    List<ArchiveRepository.ArchiveRecordView> searchArchiveRecords(ArchiveRepository.SearchArchiveRecordsQuery query) {
        String keyword = query.keyword();
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query("""
            select *
            from (
                select ssr.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       ssr.object_type,
                       ssr.object_id,
                       app.application_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       case when exists (
                           select 1 from material_loans ml
                           where ml.material_type = ssr.object_type
                             and ml.material_id = ssr.object_id
                             and ml.loan_status = 'BORROWED'
                       ) then 'BORROWED' else 'NONE' end as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       (select ml.borrowed_by_name from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_by_name,
                       (select ml.borrowed_at from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_at,
                       null as object_status,
                       null as sampled_by_name,
                       null as sampled_at,
                       null as sliced_by_name,
                       null as sliced_at,
                       null as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from specimen_storage_records ssr
                join pathology_cases pc on pc.id = ssr.case_id
                join applications app on app.id = pc.application_id
                left join application_registration_workbench w on w.application_id = app.id
                where ssr.object_type = 'APPLICATION_FORM'
                union all
                select ssr.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       ssr.object_type,
                       ssr.object_id,
                       eb.embedding_box_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       case when exists (
                           select 1 from material_loans ml
                           where ml.material_type = ssr.object_type
                             and ml.material_id = ssr.object_id
                             and ml.loan_status = 'BORROWED'
                       ) then 'BORROWED' else 'NONE' end as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       (select ml.borrowed_by_name from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_by_name,
                       (select ml.borrowed_at from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_at,
                       eb.storage_status as object_status,
                       sm.sampled_by_name,
                       sm.sampled_at,
                       null as sliced_by_name,
                       null as sliced_at,
                       null as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from specimen_storage_records ssr
                join pathology_cases pc on pc.id = ssr.case_id
                join applications app on app.id = pc.application_id
                left join application_registration_workbench w on w.application_id = app.id
                join embedding_boxes eb on eb.id = ssr.object_id
                left join sampling_blocks sb on sb.id = eb.sampling_block_id
                left join samplings sm on sm.id = sb.sampling_id
                where ssr.object_type = 'EMBEDDING_BOX'
                union all
                select ssr.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       ssr.object_type,
                       ssr.object_id,
                       s.slide_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       case when exists (
                           select 1 from material_loans ml
                           where ml.material_type = ssr.object_type
                             and ml.material_id = ssr.object_id
                             and ml.loan_status = 'BORROWED'
                       ) then 'BORROWED' else 'NONE' end as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       (select ml.borrowed_by_name from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_by_name,
                       (select ml.borrowed_at from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_at,
                       s.slide_status as object_status,
                       null as sampled_by_name,
                       null as sampled_at,
                       slc.sliced_by_name,
                       slc.sliced_at,
                       null as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from specimen_storage_records ssr
                join pathology_cases pc on pc.id = ssr.case_id
                join applications app on app.id = pc.application_id
                left join application_registration_workbench w on w.application_id = app.id
                join slides s on s.id = ssr.object_id
                left join slicings slc on slc.id = s.slicing_id
                where ssr.object_type = 'SLIDE'
                union all
                select ssr.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       ssr.object_type,
                       ssr.object_id,
                       sp.specimen_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       case when exists (
                           select 1 from material_loans ml
                           where ml.material_type = ssr.object_type
                             and ml.material_id = ssr.object_id
                             and ml.loan_status = 'BORROWED'
                       ) then 'BORROWED' else 'NONE' end as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       (select ml.borrowed_by_name from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_by_name,
                       (select ml.borrowed_at from material_loans ml
                        where ml.material_type = ssr.object_type and ml.material_id = ssr.object_id and ml.loan_status = 'BORROWED'
                        fetch first 1 row only) as borrowed_at,
                       sp.specimen_status as object_status,
                       null as sampled_by_name,
                       null as sampled_at,
                       null as sliced_by_name,
                       null as sliced_at,
                       (
                         select sm.sampled_by_name
                         from samplings sm
                         where sm.specimen_id = sp.id
                         order by sm.sampled_at desc nulls last, sm.created_at desc
                         fetch first 1 row only
                       ) as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from specimen_storage_records ssr
                join pathology_cases pc on pc.id = ssr.case_id
                join applications app on app.id = pc.application_id
                left join application_registration_workbench w on w.application_id = app.id
                join specimens sp on sp.id = ssr.object_id
                where ssr.object_type = 'SPECIMEN'
            ) records
            where (:objectType is null or records.object_type = :objectType)
              and (:caseId is null or records.case_id = :caseId)
              and (:keywordLike is null or upper(records.pathology_no) like :keywordLike
                   or upper(records.application_no) like :keywordLike
                   or upper(records.patient_name) like :keywordLike
                   or upper(records.object_code) like :keywordLike)
            order by records.archived_at desc nulls last, records.object_type asc, records.object_code asc
            """, new MapSqlParameterSource()
            .addValue("objectType", query.objectType())
            .addValue("caseId", query.caseId())
            .addValue("keywordLike", like), this::mapArchiveRecordView);
    }

    ArchiveRepository.PagedArchiveObjects findArchiveObjects(ArchiveRepository.SearchArchiveObjectsQuery query) {
        String keyword = query.keyword();
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("keywordLike", like)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        String recordsSql = archiveObjectRecordsSql(query.objectType());
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from (
            """ + recordsSql + """
            ) records
            """, parameters, Long.class);
        List<ArchiveRepository.ArchiveRecordView> items = jdbcTemplate.query("""
            select *
            from (
            """ + recordsSql + """
            ) records
            order by records.archived_at desc nulls last, records.object_code asc
            offset :offset rows fetch next :size rows only
            """, parameters, this::mapArchiveRecordView);
        return new ArchiveRepository.PagedArchiveObjects(items, total == null ? 0 : total);
    }

    Optional<ArchiveRepository.MaterialLoan> findMaterialLoanById(String loanId) {
        return jdbcTemplate.query(materialLoanSelect() + """
            where ml.id = :id
            """, Map.of("id", loanId), this::mapMaterialLoan).stream().findFirst();
    }

    List<ArchiveRepository.MaterialLoan> findMaterialLoans(String keyword, String materialType, String loanStatus) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query(materialLoanSelect() + """
            where ml.loan_status = :loanStatus
              and (:materialType is null or ml.material_type = :materialType)
              and (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                   or upper(app.application_no) like :keywordLike
                   or upper(app.patient_name) like :keywordLike
                   or upper(coalesce(eb.embedding_box_no, s.slide_no, app.application_no)) like :keywordLike)
            order by
                case when :loanStatus = 'RETURNED' then ml.returned_at else ml.borrowed_at end desc nulls last,
                ml.borrowed_at desc,
                ml.id desc
            """, new MapSqlParameterSource()
            .addValue("loanStatus", loanStatus)
            .addValue("materialType", materialType)
            .addValue("keywordLike", like), this::mapMaterialLoan);
    }

    List<ArchiveRepository.MaterialLoan> findPendingMaterialLoans(String keyword, String materialType) {
        return findMaterialLoans(keyword, materialType, "BORROWED");
    }

    Optional<ArchiveRepository.ApplicationArchiveSummary> findApplicationArchiveSummary(String caseId, String applicationId) {
        List<ArchiveRepository.ApplicationArchiveSummary> rows = jdbcTemplate.query("""
            select ssr.storage_status as archive_status,
                   ssr.storage_location as archive_location,
                   (
                     select cma.file_url
                     from case_media_assets cma
                     where cma.object_type = 'APPLICATION_FORM'
                       and cma.object_id = :applicationId
                     order by cma.captured_at desc, cma.created_at desc
                     fetch first 1 row only
                   ) as image_url
            from specimen_storage_records ssr
            where ssr.case_id = :caseId
              and ssr.object_type = 'APPLICATION_FORM'
              and ssr.object_id = :applicationId
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("applicationId", applicationId), (rs, rowNum) -> new ArchiveRepository.ApplicationArchiveSummary(
            rs.getString("archive_status"),
            rs.getString("archive_location"),
            rs.getString("image_url")));
        if (!rows.isEmpty()) {
            return rows.stream().findFirst();
        }
        List<String> imageUrls = jdbcTemplate.query("""
            select file_url
            from case_media_assets
            where object_type = 'APPLICATION_FORM'
              and object_id = :applicationId
            order by captured_at desc, created_at desc
            fetch first 1 row only
            """, Map.of("applicationId", applicationId), (rs, rowNum) -> rs.getString("file_url"));
        return imageUrls.isEmpty()
            ? Optional.empty()
            : Optional.of(new ArchiveRepository.ApplicationArchiveSummary(null, null, imageUrls.get(0)));
    }

    List<ArchiveRepository.ObjectArchiveSummary> findEmbeddingBoxArchiveSummaries(String caseId) {
        return jdbcTemplate.query("""
            select ssr.object_id,
                   ssr.storage_status as archive_status,
                   ssr.storage_location as archive_location,
                   case when exists (
                       select 1 from material_loans ml
                       where ml.material_type = 'EMBEDDING_BOX'
                         and ml.material_id = ssr.object_id
                         and ml.loan_status = 'BORROWED'
                   ) then 'BORROWED' else 'NONE' end as loan_status
            from specimen_storage_records ssr
            where ssr.case_id = :caseId
              and ssr.object_type = 'EMBEDDING_BOX'
            """, Map.of("caseId", caseId), this::mapObjectArchiveSummary);
    }

    List<ArchiveRepository.ObjectArchiveSummary> findSpecimenArchiveSummaries(String caseId) {
        return jdbcTemplate.query("""
            select ssr.object_id,
                   ssr.storage_status as archive_status,
                   ssr.storage_location as archive_location,
                   'NONE' as loan_status
            from specimen_storage_records ssr
            where ssr.case_id = :caseId
              and ssr.object_type = 'SPECIMEN'
            """, Map.of("caseId", caseId), this::mapObjectArchiveSummary);
    }

    List<ArchiveRepository.ObjectArchiveSummary> findSlideArchiveSummaries(String caseId) {
        return jdbcTemplate.query("""
            select ssr.object_id,
                   ssr.storage_status as archive_status,
                   ssr.storage_location as archive_location,
                   case when exists (
                       select 1 from material_loans ml
                       where ml.material_type = 'SLIDE'
                         and ml.material_id = ssr.object_id
                         and ml.loan_status = 'BORROWED'
                   ) then 'BORROWED' else 'NONE' end as loan_status
            from specimen_storage_records ssr
            where ssr.case_id = :caseId
              and ssr.object_type = 'SLIDE'
            """, Map.of("caseId", caseId), this::mapObjectArchiveSummary);
    }

    private String materialLoanSelect() {
        return """
            select ml.id, ml.case_id, ml.specimen_id, ml.material_type, ml.material_id, ml.archive_position_id,
                   ml.loan_status, ml.borrowed_by_user_id, ml.borrowed_by_name, ml.borrowed_at, ml.borrower_phone,
                   ml.borrower_unit, ml.borrow_purpose, ml.deposit_amount, ml.approved_by_user_id, ml.approved_by_name,
                   ml.returned_by_user_id, ml.returned_by_name, ml.returned_at, ml.remarks,
                   pc.pathology_no,
                   app.application_no,
                   app.patient_id,
                   w.id_no as patient_id_display,
                   app.patient_name,
                   app.patient_gender,
                   w.inpatient_no,
                   w.ward_name,
                   coalesce(eb.embedding_box_no, s.slide_no, app.application_no) as object_code
            from material_loans ml
            join pathology_cases pc on pc.id = ml.case_id
            join applications app on app.id = pc.application_id
            left join application_registration_workbench w on w.application_id = app.id
            left join embedding_boxes eb on ml.material_type = 'EMBEDDING_BOX' and eb.id = ml.material_id
            left join slides s on ml.material_type = 'SLIDE' and s.id = ml.material_id
            """;
    }

    private String archiveObjectRecordsSql(String objectType) {
        return switch (objectType) {
            case "APPLICATION_FORM" -> """
                select pc.id as case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       'APPLICATION_FORM' as object_type,
                       app.id as object_id,
                       app.application_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       'NONE' as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       null as borrowed_by_name,
                       null as borrowed_at,
                       null as object_status,
                       null as sampled_by_name,
                       null as sampled_at,
                       null as sliced_by_name,
                       null as sliced_at,
                       null as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from applications app
                join pathology_cases pc on pc.application_id = app.id
                left join application_registration_workbench w on w.application_id = app.id
                left join specimen_storage_records ssr
                  on ssr.object_type = 'APPLICATION_FORM'
                 and ssr.object_id = app.id
                where (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                       or upper(app.application_no) like :keywordLike
                       or upper(app.patient_name) like :keywordLike)
                """;
            case "EMBEDDING_BOX" -> """
                select eb.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       'EMBEDDING_BOX' as object_type,
                       eb.id as object_id,
                       eb.embedding_box_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       case when exists (
                           select 1 from material_loans ml
                           where ml.material_type = 'EMBEDDING_BOX'
                             and ml.material_id = eb.id
                             and ml.loan_status = 'BORROWED'
                       ) then 'BORROWED' else 'NONE' end as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       (select ml.borrowed_by_name from material_loans ml
                        where ml.material_type = 'EMBEDDING_BOX'
                          and ml.material_id = eb.id
                          and ml.loan_status = 'BORROWED'
                        order by ml.borrowed_at desc
                        fetch first 1 row only) as borrowed_by_name,
                       (select ml.borrowed_at from material_loans ml
                        where ml.material_type = 'EMBEDDING_BOX'
                          and ml.material_id = eb.id
                          and ml.loan_status = 'BORROWED'
                        order by ml.borrowed_at desc
                        fetch first 1 row only) as borrowed_at,
                       eb.storage_status as object_status,
                       sm.sampled_by_name,
                       sm.sampled_at,
                       null as sliced_by_name,
                       null as sliced_at,
                       null as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from embedding_boxes eb
                join pathology_cases pc on pc.id = eb.case_id
                join applications app on app.id = pc.application_id
                left join application_registration_workbench w on w.application_id = app.id
                left join specimen_storage_records ssr
                  on ssr.object_type = 'EMBEDDING_BOX'
                 and ssr.object_id = eb.id
                left join sampling_blocks sb on sb.id = eb.sampling_block_id
                left join samplings sm on sm.id = sb.sampling_id
                where (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                       or upper(app.application_no) like :keywordLike
                       or upper(app.patient_name) like :keywordLike
                       or upper(eb.embedding_box_no) like :keywordLike)
                """;
            case "SLIDE" -> """
                select s.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       'SLIDE' as object_type,
                       s.id as object_id,
                       s.slide_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       case when exists (
                           select 1 from material_loans ml
                           where ml.material_type = 'SLIDE'
                             and ml.material_id = s.id
                             and ml.loan_status = 'BORROWED'
                       ) then 'BORROWED' else 'NONE' end as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       (select ml.borrowed_by_name from material_loans ml
                        where ml.material_type = 'SLIDE'
                          and ml.material_id = s.id
                          and ml.loan_status = 'BORROWED'
                        order by ml.borrowed_at desc
                        fetch first 1 row only) as borrowed_by_name,
                       (select ml.borrowed_at from material_loans ml
                        where ml.material_type = 'SLIDE'
                          and ml.material_id = s.id
                          and ml.loan_status = 'BORROWED'
                        order by ml.borrowed_at desc
                        fetch first 1 row only) as borrowed_at,
                       s.slide_status as object_status,
                       null as sampled_by_name,
                       null as sampled_at,
                       slc.sliced_by_name,
                       slc.sliced_at,
                       null as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from slides s
                join pathology_cases pc on pc.id = s.case_id
                join applications app on app.id = pc.application_id
                left join application_registration_workbench w on w.application_id = app.id
                left join specimen_storage_records ssr
                  on ssr.object_type = 'SLIDE'
                 and ssr.object_id = s.id
                left join slicings slc on slc.id = s.slicing_id
                where (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                       or upper(app.application_no) like :keywordLike
                       or upper(app.patient_name) like :keywordLike
                       or upper(s.slide_no) like :keywordLike)
                """;
            case "SPECIMEN" -> """
                select sp.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_id,
                       w.id_no as patient_id_display,
                       app.patient_name,
                       app.patient_gender,
                       w.inpatient_no,
                       w.ward_name,
                       app.submitting_doctor_name as applicant_doctor_name,
                       app.application_date,
                       'SPECIMEN' as object_type,
                       sp.id as object_id,
                       sp.specimen_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       'NONE' as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       null as borrowed_by_name,
                       null as borrowed_at,
                       sp.specimen_status as object_status,
                       null as sampled_by_name,
                       null as sampled_at,
                       null as sliced_by_name,
                       null as sliced_at,
                       (
                         select sm.sampled_by_name
                         from samplings sm
                         where sm.specimen_id = sp.id
                         order by sm.sampled_at desc nulls last, sm.created_at desc
                         fetch first 1 row only
                       ) as content_described_by_name,
                       ssr.archive_expires_at,
                       ssr.archive_reminder_days
                from specimens sp
                join pathology_cases pc on pc.id = sp.case_id
                join applications app on app.id = pc.application_id
                left join application_registration_workbench w on w.application_id = app.id
                left join specimen_storage_records ssr
                  on ssr.object_type = 'SPECIMEN'
                 and ssr.object_id = sp.id
                where (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                       or upper(app.application_no) like :keywordLike
                       or upper(app.patient_name) like :keywordLike
                       or upper(sp.specimen_no) like :keywordLike)
                """;
            default -> throw new IllegalArgumentException("Unsupported archive object type: " + objectType);
        };
    }

    private ArchiveRepository.ArchiveCabinet mapArchiveCabinet(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.ArchiveCabinet(
            rs.getString("id"),
            rs.getString("cabinet_code"),
            rs.getString("cabinet_name"),
            rs.getString("cabinet_type"),
            rs.getInt("layer_count"),
            rs.getInt("slot_count_per_layer"),
            rs.getInt("capacity"),
            rs.getString("cabinet_status"),
            rs.getString("location_description"),
            rs.getString("remarks"));
    }

    private ArchiveRepository.ArchiveCabinetNode mapArchiveCabinetNode(ResultSet rs, int rowNum) throws SQLException {
        int layerNo = rs.getInt("layer_no");
        boolean layerNoWasNull = rs.wasNull();
        return new ArchiveRepository.ArchiveCabinetNode(
            rs.getString("id"),
            rs.getString("parent_id"),
            rs.getString("node_code"),
            rs.getString("node_type"),
            rs.getString("cabinet_type"),
            rs.getString("cabinet_id"),
            layerNoWasNull ? null : layerNo,
            rs.getInt("capacity"),
            rs.getInt("remaining_capacity"),
            rs.getString("path_location"),
            rs.getString("remarks"));
    }

    private ArchiveRepository.ArchivePosition mapArchivePosition(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.ArchivePosition(
            rs.getString("id"),
            rs.getString("cabinet_id"),
            rs.getString("position_code"),
            rs.getInt("layer_no"),
            rs.getInt("slot_no"),
            rs.getString("position_status"),
            rs.getString("current_object_type"),
            rs.getString("current_object_id"),
            rs.getString("remarks"));
    }

    private ArchiveRepository.StorageRecord mapStorageRecord(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.StorageRecord(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("object_type"),
            rs.getString("object_id"),
            rs.getString("storage_status"),
            rs.getString("storage_location"),
            rs.getString("archive_position_id"),
            rs.getString("cabinet_no"),
            rs.getString("layer_no"),
            rs.getString("slot_no"),
            rs.getString("stored_by_user_id"),
            rs.getString("stored_by_name"),
            toLocalDateTime(rs.getTimestamp("stored_at")),
            toLocalDateTime(rs.getTimestamp("archive_expires_at")),
            getNullableInteger(rs, "archive_reminder_days"),
            rs.getString("remarks"));
    }

    private ArchiveRepository.ArchiveRecordView mapArchiveRecordView(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.ArchiveRecordView(
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("patient_id"),
            rs.getString("patient_id_display"),
            rs.getString("patient_name"),
            rs.getString("patient_gender"),
            rs.getString("inpatient_no"),
            rs.getString("ward_name"),
            rs.getString("applicant_doctor_name"),
            toLocalDate(rs.getDate("application_date")),
            rs.getString("object_type"),
            rs.getString("object_id"),
            rs.getString("object_code"),
            rs.getString("archive_status"),
            rs.getString("archive_location"),
            rs.getString("loan_status"),
            toLocalDateTime(rs.getTimestamp("archived_at")),
            rs.getString("stored_by_name"),
            rs.getString("borrowed_by_name"),
            toLocalDateTime(rs.getTimestamp("borrowed_at")),
            rs.getString("object_status"),
            rs.getString("sampled_by_name"),
            toLocalDateTime(rs.getTimestamp("sampled_at")),
            rs.getString("sliced_by_name"),
            toLocalDateTime(rs.getTimestamp("sliced_at")),
            rs.getString("content_described_by_name"),
            toLocalDateTime(rs.getTimestamp("archive_expires_at")),
            getNullableInteger(rs, "archive_reminder_days"));
    }

    private ArchiveRepository.MaterialLoan mapMaterialLoan(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.MaterialLoan(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("material_type"),
            rs.getString("material_id"),
            rs.getString("archive_position_id"),
            rs.getString("loan_status"),
            rs.getString("borrowed_by_user_id"),
            rs.getString("borrowed_by_name"),
            toLocalDateTime(rs.getTimestamp("borrowed_at")),
            rs.getString("borrower_phone"),
            rs.getString("borrower_unit"),
            rs.getString("borrow_purpose"),
            rs.getBigDecimal("deposit_amount"),
            rs.getString("approved_by_user_id"),
            rs.getString("approved_by_name"),
            rs.getString("returned_by_user_id"),
            rs.getString("returned_by_name"),
            toLocalDateTime(rs.getTimestamp("returned_at")),
            rs.getString("remarks"),
            rs.getString("object_code"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("patient_id"),
            rs.getString("patient_id_display"),
            rs.getString("patient_name"),
            rs.getString("patient_gender"),
            rs.getString("inpatient_no"),
            rs.getString("ward_name"));
    }

    private ArchiveRepository.ObjectArchiveSummary mapObjectArchiveSummary(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.ObjectArchiveSummary(
            rs.getString("object_id"),
            rs.getString("archive_status"),
            rs.getString("archive_location"),
            rs.getString("loan_status"));
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }
}
