package com.company.bl.infrastructure.convert;

import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.infrastructure.persistence.ApplicationDataObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationInfrastructureConverterTest {

    private final ApplicationInfrastructureConverter converter = new ApplicationInfrastructureConverter();

    @Test
    void shouldConvertDomainToDataObjectAndBack() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 19, 10, 30, 0);
        Application application = new Application(
            new ApplicationId("app-1"),
            "APP-001",
            "patient-1",
            "ROUTINE",
            ApplicationStatus.SUBMITTED,
            ApplicationFormStatus.UPLOADED,
            "ext-1",
            "HIS",
            "Diagnosis",
            "Symptom",
            "Lung",
            LocalDate.of(2026, 5, 18),
            LocalDate.of(2026, 5, 19),
            "Remark",
            now,
            now);

        ApplicationDataObject dataObject = converter.toDataObject(application);
        Application restored = converter.toDomain(dataObject);

        assertThat(dataObject.getApplicationNo()).isEqualTo("APP-001");
        assertThat(dataObject.getStatus()).isEqualTo("SUBMITTED");
        assertThat(restored.getId().value()).isEqualTo("app-1");
        assertThat(restored.getApplicationFormStatus()).isEqualTo(ApplicationFormStatus.UPLOADED);
        assertThat(restored.getApplicationDate()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(restored.getCreatedAt()).isEqualTo(now);
    }
}
