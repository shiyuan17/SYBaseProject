package com.company.bl.application.service;

import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.infrastructure.config.ObservabilityConfiguration;
import com.company.bl.infrastructure.observability.ObservedOperationAspect;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpecimenWorkflowObservabilityContractTest {

    @Test
    void facadeShouldOnlyProduceOneQueryMetricForApplicationTracking() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservabilityConfiguration configuration = new ObservabilityConfiguration("bl-center", "bl-center");
        ObservedOperationAspect aspect = new ObservedOperationAspect(meterRegistry, configuration, true);

        Application application = new Application(
            new ApplicationId("APP-1"),
            "APP-NO-1",
            "PAT-1",
            "Patient",
            "F",
            "32",
            "BIOPSY",
            ApplicationStatus.SUBMITTED,
            ApplicationFormStatus.PENDING,
            "EXT-1",
            "HIS",
            "H-1",
            "Hospital",
            "D-1",
            "Dept",
            "DOC-1",
            "Doctor",
            "Dx",
            "Symptom",
            "Liver",
            LocalDate.now(),
            LocalDate.now(),
            null,
            "remark",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now());
        ApplicationTracking tracking = new ApplicationTracking(application, "SPECIMEN_COLLECTION", false, List.of(), List.of());

        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        SpecimenWorkflowQueryRepository queryRepository = mock(SpecimenWorkflowQueryRepository.class);
        SpecimenWorkflowSupport support = mock(SpecimenWorkflowSupport.class);
        when(support.getApplication("APP-1")).thenReturn(application);
        when(queryRepository.getApplicationTracking("APP-1", application)).thenReturn(tracking);

        SpecimenWorkflowTrackingQuerySupport trackingQuerySupport =
            new SpecimenWorkflowTrackingQuerySupport(applicationRepository, queryRepository, support);
        AspectJProxyFactory supportProxyFactory = new AspectJProxyFactory(trackingQuerySupport);
        supportProxyFactory.addAspect(aspect);
        SpecimenWorkflowTrackingQuerySupport proxiedTrackingQuerySupport = supportProxyFactory.getProxy();

        SpecimenWorkflowQueryService queryService = new SpecimenWorkflowQueryService(
            mock(SpecimenWorkflowPendingQuerySupport.class),
            mock(SpecimenWorkflowApplicationQuerySupport.class),
            proxiedTrackingQuerySupport,
            mock(SpecimenWorkflowRemovalQuerySupport.class));

        SpecimenWorkflowAppService facade = new SpecimenWorkflowAppService(
            null,
            null,
            null,
            null,
            null,
            null,
            queryService);

        ApplicationTracking result = facade.getApplicationTracking("APP-1");

        assertThat(result).isSameAs(tracking);
        assertThat(meterRegistry.get("application_query_total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("application_query_duration").timer().count()).isEqualTo(1L);
    }
}
