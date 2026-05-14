package com.company.user.interfaces.controller;

import com.company.user.application.query.GetUserByIdQuery;
import com.company.user.application.service.CreateUserAppService;
import com.company.user.application.service.GetUserAppService;
import com.company.user.interfaces.assembler.UserRepresentationAssembler;
import com.company.user.interfaces.dto.CreateUserRequest;
import com.company.user.interfaces.vo.UserDetailResponse;
import com.company.user.interfaces.vo.UserIdResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CreateUserAppService createUserAppService;
    private final GetUserAppService getUserAppService;
    private final UserRepresentationAssembler userRepresentationAssembler;

    public UserController(CreateUserAppService createUserAppService,
                          GetUserAppService getUserAppService,
                          UserRepresentationAssembler userRepresentationAssembler) {
        this.createUserAppService = createUserAppService;
        this.getUserAppService = getUserAppService;
        this.userRepresentationAssembler = userRepresentationAssembler;
    }

    @PostMapping
    public ResponseEntity<UserIdResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserIdResponse response = userRepresentationAssembler.toIdResponse(
            createUserAppService.create(userRepresentationAssembler.toCommand(request)));
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public UserDetailResponse getById(@PathVariable("id") String id) {
        return userRepresentationAssembler.toDetailResponse(getUserAppService.getById(new GetUserByIdQuery(id)));
    }
}
