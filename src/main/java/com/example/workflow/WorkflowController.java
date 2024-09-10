package com.example.workflow;

import io.camunda.zeebe.client.ZeebeClient;

import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    public WorkflowController(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startWorkflow() {
        LOG.info(
            "Starting process `" + ProcessConstants.BPMN_PROCESS_ID + "` with variables: " );
    
        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("Workflow_HTO")
                .latestVersion()
                .send()
                .join();
        return ResponseEntity.ok("Workflow started successfully");
    }

    @PostMapping("/generate-token-and-fetch-tasks")
public ResponseEntity<String> generateTokenAndFetchTasks() {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        // Generate JWT
        HttpPost tokenRequest = new HttpPost("http://localhost:18080/auth/realms/camunda-platform/protocol/openid-connect/token");
        tokenRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        StringEntity tokenParams = new StringEntity("client_id=demo-app&client_secret=NUeI6HT8Ce43uCJClwHfXjv3YYL1Xk30&grant_type=client_credentials");
        tokenRequest.setEntity(tokenParams);

        try (CloseableHttpResponse tokenResponse = httpClient.execute(tokenRequest)) {
            String tokenResponseBody = EntityUtils.toString(tokenResponse.getEntity());
            // Extract the token from the response (assuming JSON response)
            String token = extractTokenFromResponse(tokenResponseBody);

            // Use the token to fetch tasks
            HttpPost taskRequest = new HttpPost("http://localhost:8082/v1/tasks/search");
            taskRequest.setHeader("Authorization", "Bearer " + token);
            try (CloseableHttpResponse taskResponse = httpClient.execute(taskRequest)) {
                String taskResponseBody = EntityUtils.toString(taskResponse.getEntity());
                System.out.println("Task Response Body: " + taskResponseBody);

                // Extract task ID from the response
                String taskId = extractTaskIdFromResponse(taskResponseBody);

                // Assign the task
                HttpPatch assignRequest = new HttpPatch("http://localhost:8082/v1/tasks/" + taskId + "/assign");
                assignRequest.setHeader("Authorization", "Bearer " + token);
                try (CloseableHttpResponse assignResponse = httpClient.execute(assignRequest)) {
                    String assignResponseBody = EntityUtils.toString(assignResponse.getEntity());
                    System.out.println("Assign Response Body: " + assignResponseBody);
                    return ResponseEntity.ok(assignResponseBody);
                }
            }
        }
    } catch (Exception e) {
        LOG.error("Error generating token or fetching tasks", e);
        return ResponseEntity.status(500).body("Error generating token or fetching tasks");
    }
}

private String extractTaskIdFromResponse(String responseBody) {
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        LOG.info("Response JSON: " + jsonNode.toString());
        // Directly access the first element of the array
        if (jsonNode.isArray() && jsonNode.size() > 0) {
            String taskId = jsonNode.get(0).get("id").asText();
            LOG.info("Extracted Task ID: " + taskId);
            return taskId;
        } else {
            LOG.error("No tasks found in the response");
            return null;
        }
    } catch (Exception e) {
        LOG.error("Error extracting task ID from response", e);
        return null;
    }
}

    private String extractTokenFromResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            LOG.error("Error extracting token from response", e);
            return null;
        }
    }
}
   