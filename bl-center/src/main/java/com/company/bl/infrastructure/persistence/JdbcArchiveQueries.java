package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ArchiveRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

    Optional<ArchiveRepository.StorageRecord> findStorageRecord(String objectType, String objectId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, object_type, object_id, storage_status, storage_location,
                   archive_position_id, cabinet_no, layer_no, slot_no, stored_by_user_id, stored_by_name, stored_at, remarks
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
                       app.patient_name,
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
                        fetch first 1 row only) as borrowed_at
                from specimen_storage_records ssr
                join pathology_cases pc on pc.id = ssr.case_id
                join applications app on app.id = pc.application_id
                where ssr.object_type = 'APPLICATION_FORM'
                union all
                select ssr.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_name,
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
                        fetch first 1 row only) as borrowed_at
                from specimen_storage_records ssr
                join pathology_cases pc on pc.id = ssr.case_id
                join applications app on app.id = pc.application_id
                join embedding_boxes eb on eb.id = ssr.object_id
                where ssr.object_type = 'EMBEDDING_BOX'
                union all
                select ssr.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_name,
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
                        fetch first 1 row only) as borrowed_at
                from specimen_storage_records ssr
                join pathology_cases pc on pc.id = ssr.case_id
                join applications app on app.id = pc.application_id
                join slides s on s.id = ssr.object_id
                where ssr.object_type = 'SLIDE'
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

    List<ArchiveRepository.MaterialLoan> findPendingMaterialLoans(String keyword, String materialType) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toUpperCase() + "%";
        return jdbcTemplate.query(materialLoanSelect() + """
            where ml.loan_status = 'BORROWED'
              and (:materialType is null or ml.material_type = :materialType)
              and (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                   or upper(app.application_no) like :keywordLike
                   or upper(app.patient_name) like :keywordLike
                   or upper(coalesce(eb.embedding_box_no, s.slide_no, app.application_no)) like :keywordLike)
            order by ml.borrowed_at desc, ml.id desc
            """, new MapSqlParameterSource()
            .addValue("materialType", materialType)
            .addValue("keywordLike", like), this::mapMaterialLoan);
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
                   ml.loan_status, ml.borrowed_by_user_id, ml.borrowed_by_name, ml.borrowed_at, ml.borrow_purpose,
                   ml.approved_by_user_id, ml.approved_by_name, ml.returned_by_user_id, ml.returned_by_name, ml.returned_at,
                   ml.remarks,
                   pc.pathology_no,
                   app.application_no,
                   app.patient_name,
                   coalesce(eb.embedding_box_no, s.slide_no, app.application_no) as object_code
            from material_loans ml
            join pathology_cases pc on pc.id = ml.case_id
            join applications app on app.id = pc.application_id
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
                       app.patient_name,
                       'APPLICATION_FORM' as object_type,
                       app.id as object_id,
                       app.application_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       'NONE' as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       null as borrowed_by_name,
                       null as borrowed_at
                from applications app
                join pathology_cases pc on pc.application_id = app.id
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
                       app.patient_name,
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
                        fetch first 1 row only) as borrowed_at
                from embedding_boxes eb
                join pathology_cases pc on pc.id = eb.case_id
                join applications app on app.id = pc.application_id
                left join specimen_storage_records ssr
                  on ssr.object_type = 'EMBEDDING_BOX'
                 and ssr.object_id = eb.id
                where (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                       or upper(app.application_no) like :keywordLike
                       or upper(app.patient_name) like :keywordLike
                       or upper(eb.embedding_box_no) like :keywordLike)
                """;
            case "SLIDE" -> """
                select s.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_name,
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
                        fetch first 1 row only) as borrowed_at
                from slides s
                join pathology_cases pc on pc.id = s.case_id
                join applications app on app.id = pc.application_id
                left join specimen_storage_records ssr
                  on ssr.object_type = 'SLIDE'
                 and ssr.object_id = s.id
                where (:keywordLike is null or upper(pc.pathology_no) like :keywordLike
                       or upper(app.application_no) like :keywordLike
                       or upper(app.patient_name) like :keywordLike
                       or upper(s.slide_no) like :keywordLike)
                """;
            case "SPECIMEN" -> """
                select sp.case_id,
                       pc.pathology_no,
                       app.application_no,
                       app.patient_name,
                       'SPECIMEN' as object_type,
                       sp.id as object_id,
                       sp.specimen_no as object_code,
                       ssr.storage_status as archive_status,
                       ssr.storage_location as archive_location,
                       'NONE' as loan_status,
                       ssr.stored_at as archived_at,
                       ssr.stored_by_name,
                       null as borrowed_by_name,
                       null as borrowed_at
                from specimens sp
                join pathology_cases pc on pc.id = sp.case_id
                join applications app on app.id = pc.application_id
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
            rs.getString("remarks"));
    }

    private ArchiveRepository.ArchiveRecordView mapArchiveRecordView(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.ArchiveRecordView(
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("object_type"),
            rs.getString("object_id"),
            rs.getString("object_code"),
            rs.getString("archive_status"),
            rs.getString("archive_location"),
            rs.getString("loan_status"),
            toLocalDateTime(rs.getTimestamp("archived_at")),
            rs.getString("stored_by_name"),
            rs.getString("borrowed_by_name"),
            toLocalDateTime(rs.getTimestamp("borrowed_at")));
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
            rs.getString("borrow_purpose"),
            rs.getString("approved_by_user_id"),
            rs.getString("approved_by_name"),
            rs.getString("returned_by_user_id"),
            rs.getString("returned_by_name"),
            toLocalDateTime(rs.getTimestamp("returned_at")),
            rs.getString("remarks"),
            rs.getString("object_code"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("patient_name"));
    }

    private ArchiveRepository.ObjectArchiveSummary mapObjectArchiveSummary(ResultSet rs, int rowNum) throws SQLException {
        return new ArchiveRepository.ObjectArchiveSummary(
            rs.getString("object_id"),
            rs.getString("archive_status"),
            rs.getString("archive_location"),
            rs.getString("loan_status"));
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
