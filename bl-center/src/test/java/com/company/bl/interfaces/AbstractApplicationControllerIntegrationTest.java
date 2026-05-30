package com.company.bl.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractApplicationControllerIntegrationTest extends AuthenticatedWebIntegrationTest {

    protected static final String USER_REGISTER = "USER_M2_REGISTER";
    protected static final String USER_FIXATION = "USER_M2_FIXATION";
    protected static final String USER_TRANSPORT = "USER_M2_TRANSPORT";
    protected static final String USER_RECEIVE = "USER_M2_RECEIVE";
    protected static final String USER_TRACKING = "USER_M2_TRACKING";
    protected static final String USER_NO_PERMISSION = "USER_M2_NO_PERMISSION";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    protected JsonNode responseData(ResultActions resultActions, int expectedStatus) throws Exception {
        String response = resultActions
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).path("data");
    }
}
