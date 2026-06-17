package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationRegistrationWorkbenchAppService {

    private static final String DEFAULT_COLLECTION_MODE = "SURGERY";
    private static final String DEFAULT_COLLECTION_SCENE = "OPERATING_ROOM";
    private static final String DEFAULT_CONTAINER_NAME = "Specimen Bottle";

    private final ApplicationRepository applicationRepository;
    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;
    private final SpecimenWorkflowQueryRepository specimenWorkflowRepository;
    private final SpecimenWorkflowAppService specimenWorkflowAppService;
    private final SystemUserJdbcRepository systemUserJdbcRepository;

    @Transactional(readOnly = true)
    public WorkbenchRecord lookup(String keyword) {
        return lookup(keyword, "AUTO");
    }

    @Transactional(readOnly = true)
    public WorkbenchRecord lookup(String keyword, String queryType) {
        var applicationRow = workbenchRepository.findApplicationByKeyword(keyword, queryType)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "申请登记工作台记录不存在"));
        return loadByApplicationId(applicationRow.applicationId());
    }

    @Transactional(readOnly = true)
    public WorkbenchRecord getByApplicationId(String applicationId) {
        return loadByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public List<ApplicationRegistrationWorkbenchRepository.OperatingBuildingOption> listOperatingBuildingOptions() {
        return workbenchRepository.listOperatingBuildingOptions();
    }

    @Transactional(readOnly = true)
    public List<ApplicationRegistrationWorkbenchRepository.OperatingRoomOption> listOperatingRoomOptions(String buildingId) {
        return workbenchRepository.listOperatingRoomOptions(trim(buildingId));
    }

    @Transactional(readOnly = true)
    public SpecimenDictionaryResult listSpecimenDictionary(String keyword, String currentUserId) {
        String normalizedKeyword = trim(keyword);
        String currentDepartmentId = resolveCurrentUserDepartmentId(currentUserId);
        List<SpecimenDictionaryEntry> filteredEntries = filterDictionaryEntriesByDepartment(currentDepartmentId);
        boolean departmentFiltered = currentDepartmentId != null && !filteredEntries.isEmpty();
        List<SpecimenDictionaryEntry> effectiveEntries = departmentFiltered
            ? filteredEntries
            : SPECIMEN_DICTIONARY_ENTRIES;
        List<SpecimenDictionaryEntry> searchedEntries = filterDictionaryEntriesByKeyword(effectiveEntries, normalizedKeyword);
        return new SpecimenDictionaryResult(
            buildDictionaryGroups(searchedEntries),
            searchedEntries,
            buildCommonSpecimenOptions(searchedEntries),
            departmentFiltered);
    }

    @Transactional
    public WorkbenchRecord save(String applicationId, SaveWorkbenchCommand command) {
        Application application = loadEditableApplication(applicationId);
        persistPatientInfo(applicationId, new SavePatientInfoCommand(
            command.contagiousSpecimen(),
            command.gynecologyInfo(),
            command.patientInfo(),
            command.surgeryInfo()));

        workbenchRepository.clearPreDownstreamRegistrationData(applicationId);

        specimenWorkflowAppService.registerSpecimens(new SpecimenWorkflowModels.RegisterSpecimensCommand(
            applicationId,
            null,
            DEFAULT_COLLECTION_SCENE,
            trim(command.operatorUserId()),
            trim(command.operatorName()),
            null,
            trim(command.patientInfo().remark()),
            command.specimenItems().stream().map(item -> new SpecimenWorkflowModels.SpecimenRegistrationItem(
                trim(item.specimenName()),
                trim(command.patientInfo().specimenType()),
                trim(item.specimenSite()),
                DEFAULT_COLLECTION_MODE,
                item.quantity(),
                DEFAULT_CONTAINER_NAME,
                1,
                null,
                trim(command.patientInfo().clinicalDiagnosis())))
                .toList()));

        return loadByApplicationId(application.getId().value());
    }

    @Transactional
    public WorkbenchRecord savePatientInfo(String applicationId, SavePatientInfoCommand command) {
        Application application = loadEditableApplication(applicationId);
        persistPatientInfo(applicationId, command);
        return loadByApplicationId(application.getId().value());
    }

    WorkbenchRecord savePatientInfoForTechnicalRegistration(Application application, SavePatientInfoCommand command) {
        persistPatientInfo(application.getId().value(), command);
        return loadByApplicationId(application.getId().value());
    }

    private Application loadEditableApplication(String applicationId) {
        Application application = loadApplication(applicationId);
        if (workbenchRepository.hasStartedDownstreamWorkflow(applicationId)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "申请单已进入下游流程，无法在登记工作台重新填写");
        }
        return application;
    }

    private Application loadApplication(String applicationId) {
        return applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
    }

    private void persistPatientInfo(String applicationId, SavePatientInfoCommand command) {
        Application application = loadApplication(applicationId);
        ContagiousSpecimen contagiousSpecimen = command.contagiousSpecimen();
        GynecologyInfo gynecologyInfo = command.gynecologyInfo();
        PatientInfo patientInfo = command.patientInfo();
        SurgeryInfo surgeryInfo = command.surgeryInfo();
        applicationRepository.update(new Application(
            application.getId(),
            application.getApplicationNo(),
            application.getPatientId(),
            application.getPatientName(),
            application.getPatientGender(),
            application.getPatientAge(),
            application.getApplicationType(),
            application.getStatus(),
            application.getApplicationFormStatus(),
            application.getExternalOrderNo(),
            application.getThirdPartySource(),
            application.getSourceHospitalId(),
            application.getSourceHospitalName(),
            application.getSubmittingDepartmentId(),
            application.getSubmittingDepartmentName(),
            application.getSubmittingDoctorUserId(),
            application.getSubmittingDoctorName(),
            trim(patientInfo.clinicalDiagnosis()),
            application.getClinicalSymptom(),
            application.getSpecimenSite(),
            application.getApplicationDate(),
            application.getSubmissionDate(),
            application.getSpecimenRemovalTime(),
            trim(patientInfo.remark()),
            application.getCreatedAt(),
            LocalDateTime.now()));
        workbenchRepository.upsertExtension(new ApplicationRegistrationWorkbenchRepository.SaveWorkbenchExtensionCommand(
            applicationId,
            trim(patientInfo.inpatientNo()),
            trim(patientInfo.bedNo()),
            trim(patientInfo.wardName()),
            trim(patientInfo.phone()),
            trim(patientInfo.idNo()),
            trim(patientInfo.checkItem()),
            trim(patientInfo.clinicalHistory()),
            trim(patientInfo.imagingResult()),
            trim(patientInfo.endoscopyDiagnosis()),
            trim(patientInfo.deliveryRequirement()),
            trim(patientInfo.specimenType()),
            trim(surgeryInfo.surgeryName()),
            trim(surgeryInfo.clinicalFindings()),
            trim(surgeryInfo.fixativeType()),
            trim(surgeryInfo.fixationPerson()),
            parseDateTime(surgeryInfo.fixationTime()),
            trim(surgeryInfo.buildingId()),
            trim(surgeryInfo.roomId()),
            contagiousSpecimen.isolation(),
            contagiousSpecimen.hiv(),
            contagiousSpecimen.tuberculosis(),
            contagiousSpecimen.hepatitis(),
            contagiousSpecimen.syphilis(),
            gynecologyInfo.menopause(),
            trim(gynecologyInfo.lastMenstrualPeriod()),
            trim(gynecologyInfo.hpvResult()),
            trim(gynecologyInfo.previousCytology()),
            trim(gynecologyInfo.previousTreatment()),
            trim(gynecologyInfo.additionalNotes()),
            gynecologyInfo.specialConditions().abnormalBleeding(),
            gynecologyInfo.specialConditions().birthControl(),
            gynecologyInfo.specialConditions().hormoneReplacement(),
            gynecologyInfo.specialConditions().hysterectomy(),
            gynecologyInfo.specialConditions().iud(),
            gynecologyInfo.specialConditions().lactation(),
            gynecologyInfo.specialConditions().pregnancy(),
            gynecologyInfo.specialConditions().radiotherapy(),
            trim(gynecologyInfo.specialConditions().other())));
    }

    private WorkbenchRecord loadByApplicationId(String applicationId) {
        Application application = applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
        var extension = workbenchRepository.findExtensionByApplicationId(applicationId).orElse(null);
        List<Specimen> specimens = specimenWorkflowRepository.findSpecimensByApplicationId(applicationId);
        LocalDateTime specimenRemovalTime = specimens.stream()
            .map(Specimen::specimenRemovalAt)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);

        return new WorkbenchRecord(
            applicationId,
            new ContagiousSpecimen(
                extension != null && extension.contagiousHepatitis(),
                extension != null && extension.contagiousHiv(),
                extension != null && extension.contagiousIsolation(),
                extension != null && extension.contagiousSyphilis(),
                extension != null && extension.contagiousTuberculosis()),
            new GynecologyInfo(
                extension == null ? null : extension.additionalNotes(),
                extension == null ? null : extension.hpvResult(),
                extension == null ? null : extension.lastMenstrualPeriod(),
                extension != null && extension.gynecologyMenopause(),
                extension == null ? null : extension.previousCytology(),
                extension == null ? null : extension.previousTreatment(),
                new SpecialConditions(
                    extension != null && extension.conditionAbnormalBleeding(),
                    extension != null && extension.conditionBirthControl(),
                    extension != null && extension.conditionHormoneReplacement(),
                    extension != null && extension.conditionHysterectomy(),
                    extension != null && extension.conditionIud(),
                    extension != null && extension.conditionLactation(),
                    extension != null && extension.gynecologyMenopause(),
                    extension == null ? null : extension.otherSpecialCondition(),
                    extension != null && extension.conditionPregnancy(),
                    extension != null && extension.conditionRadiotherapy())),
            new PatientInfo(
                application.getPatientAge(),
                stringify(application.getApplicationDate()),
                application.getApplicationNo(),
                application.getSubmittingDepartmentName(),
                application.getSubmittingDoctorName(),
                extension == null ? null : extension.bedNo(),
                extension == null ? null : extension.checkItem(),
                application.getClinicalDiagnosis(),
                extension == null ? null : extension.clinicalHistory(),
                extension == null ? null : extension.deliveryRequirement(),
                extension == null ? null : extension.endoscopyDiagnosis(),
                false,
                application.getPatientGender(),
                extension == null ? null : extension.idNo(),
                extension == null ? null : extension.imagingResult(),
                extension == null ? null : extension.inpatientNo(),
                application.getPatientName(),
                application.getPatientId() != null && !application.getPatientId().isBlank(),
                extension == null ? null : extension.phone(),
                application.getStatus().name(),
                application.getRemarks(),
                extension == null ? null : extension.specimenType(),
                extension == null ? null : extension.wardName()),
            specimens.stream().map(specimen -> new SpecimenItem(
                specimen.id(),
                specimen.specimenCount(),
                specimen.specimenNameStandardized(),
                specimen.specimenNo(),
                specimen.specimenSite(),
                resolveWorkbenchSpecimenStatus(specimen)))
                .toList(),
            new SurgeryInfo(
                extension == null ? null : extension.buildingId(),
                extension == null ? null : extension.clinicalFindings(),
                extension == null ? null : extension.fixativeType(),
                extension == null ? null : extension.fixationPerson(),
                stringify(extension == null ? null : extension.fixationTime()),
                extension == null ? null : extension.roomId(),
                stringify(specimenRemovalTime),
                extension == null ? null : extension.surgeryName()));
    }

    private String resolveWorkbenchSpecimenStatus(Specimen specimen) {
        if (specimen.specimenStatus() == null) {
            return null;
        }
        if (specimen.specimenStatus() == SpecimenStatus.REGISTERED) {
            return "已登记";
        }
        return specimen.specimenStatus().name();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String resolveCurrentUserDepartmentId(String currentUserId) {
        String normalizedUserId = trim(currentUserId);
        if (normalizedUserId == null) {
            return null;
        }
        SystemJdbcRepository.UserRow user = systemUserJdbcRepository.findUserById(normalizedUserId);
        return user == null ? null : trim(user.departmentId());
    }

    private List<SpecimenDictionaryEntry> filterDictionaryEntriesByDepartment(String departmentId) {
        String normalizedDepartmentId = trim(departmentId);
        if (normalizedDepartmentId == null) {
            return List.of();
        }
        return SPECIMEN_DICTIONARY_ENTRIES.stream()
            .filter(entry -> entry.departmentIds().contains(normalizedDepartmentId))
            .toList();
    }

    private List<SpecimenDictionaryEntry> filterDictionaryEntriesByKeyword(
        List<SpecimenDictionaryEntry> entries,
        String keyword
    ) {
        if (keyword == null) {
            return entries;
        }
        String normalizedKeyword = keyword.toLowerCase();
        return entries.stream()
            .filter(entry -> entry.searchKeywords().stream().anyMatch(item -> item.contains(normalizedKeyword)))
            .toList();
    }

    private List<SpecimenDictionaryGroup> buildDictionaryGroups(List<SpecimenDictionaryEntry> entries) {
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> systems = new LinkedHashMap<>();
        LinkedHashMap<String, String> systemNames = new LinkedHashMap<>();
        LinkedHashMap<String, String> partNames = new LinkedHashMap<>();
        for (SpecimenDictionaryEntry entry : entries) {
            systemNames.putIfAbsent(entry.systemId(), entry.systemName());
            partNames.putIfAbsent(entry.systemId() + "\u0000" + entry.partId(), entry.partName());
            systems
                .computeIfAbsent(entry.systemId(), ignored -> new LinkedHashMap<>())
                .computeIfAbsent(entry.partId(), ignored -> new ArrayList<>())
                .add(entry.specimenName());
        }
        return systems.entrySet().stream()
            .map(system -> new SpecimenDictionaryGroup(
                system.getKey(),
                systemNames.get(system.getKey()),
                system.getValue().entrySet().stream()
                    .map(part -> new SpecimenDictionaryPart(
                        part.getKey(),
                        partNames.get(system.getKey() + "\u0000" + part.getKey()),
                        part.getValue()))
                    .toList()))
            .toList();
    }

    private List<SpecimenDictionaryEntry> buildCommonSpecimenOptions(List<SpecimenDictionaryEntry> entries) {
        LinkedHashMap<String, SpecimenDictionaryEntry> deduped = new LinkedHashMap<>();
        for (SpecimenDictionaryEntry entry : entries) {
            deduped.putIfAbsent(entry.partId() + "\u0000" + entry.specimenName(), entry);
        }
        return deduped.values().stream()
            .limit(8)
            .toList();
    }

    private static List<String> keywords(String... values) {
        LinkedHashMap<String, Boolean> keywords = new LinkedHashMap<>();
        for (String value : values) {
            String normalized = value == null ? null : value.trim().toLowerCase();
            if (normalized != null && !normalized.isBlank()) {
                keywords.put(normalized, Boolean.TRUE);
            }
        }
        return List.copyOf(keywords.keySet());
    }

    private static final List<SpecimenDictionaryEntry> SPECIMEN_DICTIONARY_ENTRIES = List.of(
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P101", "骨髓炎", "右侧胫骨感染病灶",
            keywords("骨、关节及软组织", "骨髓炎", "右侧胫骨感染病灶", "骨髓炎右侧胫骨感染病灶"),
            Set.of("DEPT-ORTHO", "DEPT-EMERGENCY")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P101", "骨髓炎", "右股骨骨髓炎病灶",
            keywords("骨、关节及软组织", "骨髓炎", "右股骨骨髓炎病灶", "骨髓炎右股骨骨髓炎病灶"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P101", "骨髓炎", "右腓骨骨髓炎病灶",
            keywords("骨、关节及软组织", "骨髓炎", "右腓骨骨髓炎病灶", "骨髓炎右腓骨骨髓炎病灶"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P101", "骨髓炎", "左侧胫骨感染病灶",
            keywords("骨、关节及软组织", "骨髓炎", "左侧胫骨感染病灶", "骨髓炎左侧胫骨感染病灶"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P101", "骨髓炎", "左股骨骨髓炎病灶",
            keywords("骨、关节及软组织", "骨髓炎", "左股骨骨髓炎病灶", "骨髓炎左股骨骨髓炎病灶"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P101", "骨髓炎", "左腓骨骨髓炎病灶",
            keywords("骨、关节及软组织", "骨髓炎", "左腓骨骨髓炎病灶", "骨髓炎左腓骨骨髓炎病灶"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P102", "关节", "膝关节滑膜组织",
            keywords("骨、关节及软组织", "关节", "膝关节滑膜组织", "关节膝关节滑膜组织"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P102", "关节", "髋关节滑膜组织",
            keywords("骨、关节及软组织", "关节", "髋关节滑膜组织", "关节髋关节滑膜组织"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS001", "骨、关节及软组织", "P102", "关节", "踝关节病灶组织",
            keywords("骨、关节及软组织", "关节", "踝关节病灶组织", "关节踝关节病灶组织"),
            Set.of("DEPT-ORTHO")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "背部黑毛痣",
            keywords("乳腺及皮肤", "皮肤", "背部黑毛痣", "皮肤背部黑毛痣"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "背部溃疡组织",
            keywords("乳腺及皮肤", "皮肤", "背部溃疡组织", "皮肤背部溃疡组织"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "背部皮肤肿物",
            keywords("乳腺及皮肤", "皮肤", "背部皮肤肿物", "皮肤背部皮肤肿物"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "额部黑毛痣",
            keywords("乳腺及皮肤", "皮肤", "额部黑毛痣", "皮肤额部黑毛痣"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "额部皮肤肿物",
            keywords("乳腺及皮肤", "皮肤", "额部皮肤肿物", "皮肤额部皮肤肿物"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "腹部皮肤活检",
            keywords("乳腺及皮肤", "皮肤", "腹部皮肤活检", "皮肤腹部皮肤活检"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "颈部皮肤肿物",
            keywords("乳腺及皮肤", "皮肤", "颈部皮肤肿物", "皮肤颈部皮肤肿物"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "皮肤溃疡组织",
            keywords("乳腺及皮肤", "皮肤", "皮肤溃疡组织", "皮肤皮肤溃疡组织"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "头部皮肤肿物",
            keywords("乳腺及皮肤", "皮肤", "头部皮肤肿物", "皮肤头部皮肤肿物"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "头皮肿物",
            keywords("乳腺及皮肤", "皮肤", "头皮肿物", "皮肤头皮肿物"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "右面部黑毛痣",
            keywords("乳腺及皮肤", "皮肤", "右面部黑毛痣", "皮肤右面部黑毛痣"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "右前臂皮肤",
            keywords("乳腺及皮肤", "皮肤", "右前臂皮肤", "皮肤右前臂皮肤"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "左手背",
            keywords("乳腺及皮肤", "皮肤", "左手背", "皮肤左手背"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P201", "皮肤", "左前臂黑毛痣",
            keywords("乳腺及皮肤", "皮肤", "左前臂黑毛痣", "皮肤左前臂黑毛痣"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P202", "乳腺", "右乳外上象限结节",
            keywords("乳腺及皮肤", "乳腺", "右乳外上象限结节", "乳腺右乳外上象限结节"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P202", "乳腺", "左乳腺肿物",
            keywords("乳腺及皮肤", "乳腺", "左乳腺肿物", "乳腺左乳腺肿物"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P202", "乳腺", "乳头乳晕区组织",
            keywords("乳腺及皮肤", "乳腺", "乳头乳晕区组织", "乳腺乳头乳晕区组织"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS002", "乳腺及皮肤", "P202", "乳腺", "乳腺钙化灶",
            keywords("乳腺及皮肤", "乳腺", "乳腺钙化灶", "乳腺乳腺钙化灶"),
            Set.of("DEPT-GENERAL")),
        new SpecimenDictionaryEntry("SYS003", "妇科", "P301", "宫颈", "宫颈 3 点位组织",
            keywords("妇科", "宫颈", "宫颈 3 点位组织", "宫颈宫颈 3 点位组织"),
            Set.of("DEPT-GYNE")),
        new SpecimenDictionaryEntry("SYS003", "妇科", "P301", "宫颈", "宫颈 6 点位组织",
            keywords("妇科", "宫颈", "宫颈 6 点位组织", "宫颈宫颈 6 点位组织"),
            Set.of("DEPT-GYNE")),
        new SpecimenDictionaryEntry("SYS003", "妇科", "P301", "宫颈", "宫颈锥切标本",
            keywords("妇科", "宫颈", "宫颈锥切标本", "宫颈宫颈锥切标本"),
            Set.of("DEPT-GYNE")),
        new SpecimenDictionaryEntry("SYS003", "妇科", "P301", "宫颈", "宫颈活检组织",
            keywords("妇科", "宫颈", "宫颈活检组织", "宫颈宫颈活检组织"),
            Set.of("DEPT-GYNE")),
        new SpecimenDictionaryEntry("SYS003", "妇科", "P302", "子宫", "宫腔内容物",
            keywords("妇科", "子宫", "宫腔内容物", "子宫宫腔内容物"),
            Set.of("DEPT-GYNE")),
        new SpecimenDictionaryEntry("SYS003", "妇科", "P302", "子宫", "子宫内膜组织",
            keywords("妇科", "子宫", "子宫内膜组织", "子宫子宫内膜组织"),
            Set.of("DEPT-GYNE")),
        new SpecimenDictionaryEntry("SYS003", "妇科", "P302", "子宫", "子宫肌瘤组织",
            keywords("妇科", "子宫", "子宫肌瘤组织", "子宫子宫肌瘤组织"),
            Set.of("DEPT-GYNE"))
    );

    public record SpecimenDictionaryResult(
        List<SpecimenDictionaryGroup> groups,
        List<SpecimenDictionaryEntry> entryOptions,
        List<SpecimenDictionaryEntry> commonOptions,
        boolean departmentFiltered
    ) {
    }

    public record SpecimenDictionaryGroup(
        String systemId,
        String systemName,
        List<SpecimenDictionaryPart> subParts
    ) {
    }

    public record SpecimenDictionaryPart(
        String partId,
        String partName,
        List<String> specimens
    ) {
    }

    public record SpecimenDictionaryEntry(
        String systemId,
        String systemName,
        String partId,
        String partName,
        String specimenName,
        List<String> searchKeywords,
        Set<String> departmentIds
    ) {
    }

    public record SaveWorkbenchCommand(
        ContagiousSpecimen contagiousSpecimen,
        GynecologyInfo gynecologyInfo,
        PatientInfo patientInfo,
        List<SaveSpecimenItem> specimenItems,
        SurgeryInfo surgeryInfo,
        String operatorUserId,
        String operatorName
    ) {
    }

    public record SavePatientInfoCommand(
        ContagiousSpecimen contagiousSpecimen,
        GynecologyInfo gynecologyInfo,
        PatientInfo patientInfo,
        SurgeryInfo surgeryInfo
    ) {
    }

    public record WorkbenchRecord(
        String applicationId,
        ContagiousSpecimen contagiousSpecimen,
        GynecologyInfo gynecologyInfo,
        PatientInfo patientInfo,
        List<SpecimenItem> specimenItems,
        SurgeryInfo surgeryInfo
    ) {
    }

    public record ContagiousSpecimen(
        boolean hepatitis,
        boolean hiv,
        boolean isolation,
        boolean syphilis,
        boolean tuberculosis
    ) {
    }

    public record SpecialConditions(
        boolean abnormalBleeding,
        boolean birthControl,
        boolean hormoneReplacement,
        boolean hysterectomy,
        boolean iud,
        boolean lactation,
        boolean menopause,
        String other,
        boolean pregnancy,
        boolean radiotherapy
    ) {
    }

    public record GynecologyInfo(
        String additionalNotes,
        String hpvResult,
        String lastMenstrualPeriod,
        boolean menopause,
        String previousCytology,
        String previousTreatment,
        SpecialConditions specialConditions
    ) {
    }

    public record PatientInfo(
        String age,
        String applicationDate,
        String applicationNo,
        String applyDept,
        String applyDoctor,
        String bedNo,
        String checkItem,
        String clinicalDiagnosis,
        String clinicalHistory,
        String deliveryRequirement,
        String endoscopyDiagnosis,
        boolean frozenReminder,
        String gender,
        String idNo,
        String imagingResult,
        String inpatientNo,
        String patientName,
        boolean patientVerified,
        String phone,
        String registrationStatus,
        String remark,
        String specimenType,
        String wardName
    ) {
    }

    public record SpecimenItem(
        String id,
        Integer quantity,
        String specimenName,
        String specimenNo,
        String specimenSite,
        String status
    ) {
    }

    public record SaveSpecimenItem(
        String id,
        Integer quantity,
        String specimenName,
        String specimenSite,
        String status
    ) {
    }

    public record SurgeryInfo(
        String buildingId,
        String clinicalFindings,
        String fixativeType,
        String fixationPerson,
        String fixationTime,
        String roomId,
        String specimenRemovalTime,
        String surgeryName
    ) {
    }
}
