package com.company.user.infrastructure.convert;

import com.company.user.domain.enums.UserStatus;
import com.company.user.domain.model.User;
import com.company.user.domain.valueobject.UserEmail;
import com.company.user.domain.valueobject.UserId;
import com.company.user.domain.valueobject.UserName;
import com.company.user.infrastructure.persistence.UserDataObject;
import org.springframework.stereotype.Component;

@Component
public class UserInfrastructureConverter {

    public UserDataObject toDataObject(User user) {
        UserDataObject dataObject = new UserDataObject();
        dataObject.setId(user.getId().value());
        dataObject.setName(user.getName().value());
        dataObject.setEmail(user.getEmail().value());
        dataObject.setStatus(user.getStatus().name());
        dataObject.setCreatedAt(user.getCreatedAt());
        return dataObject;
    }

    public User toDomain(UserDataObject dataObject) {
        return new User(
            new UserId(dataObject.getId()),
            new UserName(dataObject.getName()),
            new UserEmail(dataObject.getEmail()),
            UserStatus.valueOf(dataObject.getStatus()),
            dataObject.getCreatedAt());
    }
}
