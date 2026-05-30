package com.company.bl.application.service;

import com.company.bl.application.query.GetApplicationByIdQuery;
import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.common.web.observability.ObservedOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetApplicationAppService {

    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    @ObservedOperation(
        operation = "get_application",
        successCounter = "application_query_total",
        failureCounter = "application_query_failed_total",
        durationMetric = "application_query_duration")
    public Application getById(GetApplicationByIdQuery query) {
        return applicationRepository.findById(new ApplicationId(query.applicationId()))
            .orElseThrow(() -> new ApplicationDomainException(ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }
}
