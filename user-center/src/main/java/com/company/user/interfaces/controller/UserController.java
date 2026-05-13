package com.company.user.interfaces.controller;

import com.company.common.web.filter.TraceIdFilter;
import com.company.common.web.response.ApiResponse;
import com.company.user.application.query.GetUserByIdQuery;
import com.company.user.application.service.CreateUserAppService;
import com.company.user.application.service.GetUserAppService;
import com.company.user.interfaces.assembler.UserRepresentationAssembler;
import com.company.user.interfaces.dto.CreateUserRequest;
import com.company.user.interfaces.vo.UserDetailResponse;
import com.company.user.interfaces.vo.UserIdResponse;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<ApiResponse<UserIdResponse>> create(@Valid @RequestBody CreateUserRequest request,
                                                              HttpServletRequest httpServletRequest) {
        UserIdResponse response = userRepresentationAssembler.toIdResponse(
            createUserAppService.create(userRepresentationAssembler.toCommand(request)));
        return ResponseEntity.status(201)
            .body(ApiResponse.success(response, traceId(httpServletRequest)));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDetailResponse> getById(@PathVariable("id") String id,
                                                   HttpServletRequest httpServletRequest) {
        UserDetailResponse response = userRepresentationAssembler.toDetailResponse(
            getUserAppService.getById(new GetUserByIdQuery(id)));
        return ApiResponse.success(response, traceId(httpServletRequest));
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId.toString();
    }
}
