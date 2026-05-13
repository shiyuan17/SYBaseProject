package com.company.user.interfaces.assembler;

import com.company.user.application.command.CreateUserCommand;
import com.company.user.domain.model.User;
import com.company.user.domain.valueobject.UserId;
import com.company.user.interfaces.dto.CreateUserRequest;
import com.company.user.interfaces.vo.UserDetailResponse;
import com.company.user.interfaces.vo.UserIdResponse;
import org.springframework.stereotype.Component;

@Component
public class UserRepresentationAssembler {

    public CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(request.getName(), request.getEmail());
    }

    public UserIdResponse toIdResponse(UserId userId) {
        return new UserIdResponse(userId.value());
    }

    public UserDetailResponse toDetailResponse(User user) {
        return new UserDetailResponse(
            user.getId().value(),
            user.getName().value(),
            user.getEmail().value(),
            user.getStatus().name(),
            user.getCreatedAt().toString());
    }
}
