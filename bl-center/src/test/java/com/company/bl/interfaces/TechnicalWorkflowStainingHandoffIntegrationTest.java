package com.company.bl.interfaces;

import com.company.bl.BlCenterApplication;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(classes = BlCenterApplication.class)
class TechnicalWorkflowStainingHandoffIntegrationTest extends AbstractTechnicalWorkflowIntegrationTest {

    @Test
    void shouldCreateSingleDiagnosticTaskWhenStainingTasksCompleteConcurrently() throws Exception {
        TechnicalCaseContext context = prepareTwoStainingTasks("APP-M3-STAIN-CONCURRENT-001", "BC-M3-STAIN-CONCURRENT-001");
        JsonNode stainingTasks = listPendingTasks("STAINING", context.pathologyNo(), USER_M3_STAINING);
        assertThat(stainingTasks.path("items")).hasSize(2);

        List<String> stainingTaskIds = new ArrayList<>();
        List<String> slideIds = new ArrayList<>();
        for (JsonNode stainingTask : stainingTasks.path("items")) {
            String taskId = stainingTask.path("id").asText();
            stainingTaskIds.add(taskId);
            slideIds.add(stainingTask.path("objectId").asText());
            postJson("/api/v1/slide-stainings/start", USER_M3_STAINING, """
                {
                  "taskId": "%s",
                  "terminalCode": "TT-CONCURRENT-START"
                }
                """.formatted(taskId)).andExpect(status().isOk());
        }

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < stainingTaskIds.size(); index++) {
                String taskId = stainingTaskIds.get(index);
                String slideId = slideIds.get(index);
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    postJson("/api/v1/slide-stainings/complete", USER_M3_STAINING, """
                        {
                          "taskId": "%s",
                          "slideId": "%s",
                          "stainingType": "HE",
                          "terminalCode": "TT-CONCURRENT-COMPLETE"
                        }
                        """.formatted(taskId, slideId)).andExpect(status().isOk());
                    return null;
                }));
            }
            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        Long diagnosticTaskCount = namedParameterJdbcTemplate.queryForObject("""
            select count(*)
            from diagnostic_tasks
            where case_id = :caseId
              and task_type = 'PRIMARY'
              and status = 'PENDING'
            """, java.util.Map.of("caseId", context.caseId()), Long.class);
        assertThat(diagnosticTaskCount).isEqualTo(1L);

        String caseStatus = namedParameterJdbcTemplate.queryForObject("""
            select case_status
            from pathology_cases
            where id = :caseId
            """, java.util.Map.of("caseId", context.caseId()), String.class);
        assertThat(caseStatus).isEqualTo("DIAGNOSIS_PENDING");

        JsonNode assignmentTasks = responseBody(mockMvc.perform(authorized(get("/api/v1/diagnostic-tasks/assignment"), USER_M1_ADMIN)
            .param("page", "1")
            .param("size", "20")
            .param("pathologyNo", context.pathologyNo())), 200);
        assertThat(assignmentTasks.path("total").asInt()).isEqualTo(1);
    }

    private TechnicalCaseContext prepareTwoStainingTasks(String applicationNo, String barcode) throws Exception {
        TechnicalCaseContext context = receiveCaseAndGetGrossingTask(applicationNo, barcode);
        postJson("/api/v1/grossings/start", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "terminalCode": "TG-CONCURRENT-01"
            }
            """.formatted(context.grossingTaskId()))
            .andExpect(status().isOk());

        postJson("/api/v1/grossings/complete", USER_M3_GROSSING, """
            {
              "taskId": "%s",
              "caseId": "%s",
              "terminalCode": "TG-CONCURRENT-02",
              "specimens": [
                {
                  "specimenId": "%s",
                  "specimenType": "ROUTINE",
                  "grossDescription": "multi slide concurrent staining",
                  "blocks": [
                    { "blockSite": "A", "blockDescription": "block-a" },
                    { "blockSite": "B", "blockDescription": "block-b" }
                  ]
                }
              ]
            }
            """.formatted(context.grossingTaskId(), context.caseId(), context.specimenId()))
            .andExpect(status().isOk());

        JsonNode dehydrationTasks = listPendingTasks("DEHYDRATION", context.pathologyNo(), USER_M3_DEHYDRATION);
        assertThat(dehydrationTasks.path("items")).hasSize(2);
        for (JsonNode dehydrationTask : dehydrationTasks.path("items")) {
            String taskId = dehydrationTask.path("id").asText();
            postJson("/api/v1/dehydrations/start", USER_M3_DEHYDRATION, """
                {
                  "taskId": "%s"
                }
                """.formatted(taskId)).andExpect(status().isOk());
            postJson("/api/v1/dehydrations/complete", USER_M3_DEHYDRATION, """
                {
                  "taskId": "%s"
                }
                """.formatted(taskId)).andExpect(status().isOk());
        }

        JsonNode embeddingTasks = listPendingTasks("EMBEDDING", context.pathologyNo(), USER_M3_EMBEDDING);
        assertThat(embeddingTasks.path("items")).hasSize(2);
        for (JsonNode embeddingTask : embeddingTasks.path("items")) {
            String taskId = embeddingTask.path("id").asText();
            String samplingBlockId = embeddingTask.path("objectId").asText();
            postJson("/api/v1/embeddings/start", USER_M3_EMBEDDING, """
                {
                  "taskId": "%s"
                }
                """.formatted(taskId)).andExpect(status().isOk());
            postJson("/api/v1/embeddings/complete", USER_M3_EMBEDDING, """
                {
                  "taskId": "%s",
                  "samplingBlockId": "%s",
                  "blockCount": 1
                }
                """.formatted(taskId, samplingBlockId)).andExpect(status().isOk());
        }

        JsonNode slicingTasks = listPendingTasks("SLICING", context.pathologyNo(), USER_M3_SLICING);
        assertThat(slicingTasks.path("items")).hasSize(2);
        for (JsonNode slicingTask : slicingTasks.path("items")) {
            String taskId = slicingTask.path("id").asText();
            String embeddingBoxId = slicingTask.path("objectId").asText();
            postJson("/api/v1/slicings/start", USER_M3_SLICING, """
                {
                  "taskId": "%s"
                }
                """.formatted(taskId)).andExpect(status().isOk());
            printSlides(taskId, embeddingBoxId);
            postJson("/api/v1/slicings/complete", USER_M3_SLICING, """
                {
                  "taskId": "%s",
                  "embeddingBoxId": "%s"
                }
                """.formatted(taskId, embeddingBoxId)).andExpect(status().isOk());
        }
        return context;
    }

}
