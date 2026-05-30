package com.company.bl.application.service;

import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;

abstract class AbstractSpecimenWorkflowQuerySupport {

    protected final ApplicationRepository applicationRepository;
    protected final SpecimenWorkflowQueryRepository specimenWorkflowRepository;
    protected final SpecimenWorkflowSupport specimenWorkflowSupport;

    AbstractSpecimenWorkflowQuerySupport(ApplicationRepository applicationRepository,
                                         SpecimenWorkflowQueryRepository specimenWorkflowRepository,
                                         SpecimenWorkflowSupport specimenWorkflowSupport) {
        this.applicationRepository = applicationRepository;
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
    }
}
