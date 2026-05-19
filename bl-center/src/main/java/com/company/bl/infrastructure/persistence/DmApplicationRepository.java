package com.company.bl.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.infrastructure.convert.ApplicationInfrastructureConverter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("!test")
public class DmApplicationRepository implements ApplicationRepository {

    private final ApplicationMapper applicationMapper;
    private final ApplicationInfrastructureConverter applicationInfrastructureConverter;

    public DmApplicationRepository(ApplicationMapper applicationMapper,
                                   ApplicationInfrastructureConverter applicationInfrastructureConverter) {
        this.applicationMapper = applicationMapper;
        this.applicationInfrastructureConverter = applicationInfrastructureConverter;
    }

    @Override
    public Application save(Application application) {
        ApplicationDataObject dataObject = applicationInfrastructureConverter.toDataObject(application);
        applicationMapper.insert(dataObject);
        return applicationInfrastructureConverter.toDomain(dataObject);
    }

    @Override
    public Optional<Application> findById(ApplicationId applicationId) {
        return Optional.ofNullable(applicationMapper.selectById(applicationId.value()))
            .map(applicationInfrastructureConverter::toDomain);
    }

    @Override
    public boolean existsByApplicationNo(String applicationNo) {
        LambdaQueryWrapper<ApplicationDataObject> queryWrapper = new LambdaQueryWrapper<ApplicationDataObject>()
            .eq(ApplicationDataObject::getApplicationNo, applicationNo);
        Long count = applicationMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }
}
