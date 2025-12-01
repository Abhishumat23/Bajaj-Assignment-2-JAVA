package com.example.webhooksolver;

import com.example.webhooksolver.dto.GenerateWebhookResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class WebhookSolverApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookSolverApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) {
        return args -> {
            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, String> request = new HashMap<>();
            request.put("name", "John Doe");
            request.put("regNo", "REG12347");
            request.put("email", "john@example.com");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            GenerateWebhookResponse response;
            try {
                ResponseEntity<GenerateWebhookResponse> resp = restTemplate.postForEntity(generateUrl, entity, GenerateWebhookResponse.class);
                response = resp.getBody();
            } catch (Exception ex) {
                System.err.println("Failed to call generateWebhook: " + ex.getMessage());
                return;
            }

            if (response == null || response.getWebhook() == null || response.getAccessToken() == null) {
                System.err.println("Invalid response from generateWebhook");
                return;
            }

            // Compose final SQL (PostgreSQL-style) that solves the problem
            String finalQuery = "WITH high_earners AS (\n" +
                    "    SELECT DISTINCT e.emp_id, e.first_name, e.last_name, e.dob, e.department\n" +
                    "    FROM employee e\n" +
                    "    JOIN payments p ON e.emp_id = p.emp_id\n" +
                    "    WHERE p.amount > 70000\n" +
                    ")\n" +
                    "SELECT d.department_name,\n" +
                    "       ROUND(AVG(EXTRACT(YEAR FROM AGE(CURRENT_DATE, he.dob)))::numeric, 2) AS average_age,\n" +
                    "       (\n" +
                    "           SELECT STRING_AGG(name, ', ')\n" +
                    "           FROM (\n" +
                    "               SELECT CONCAT(he2.first_name, ' ', he2.last_name) AS name\n" +
                    "               FROM high_earners he2\n" +
                    "               WHERE he2.department = d.department_id\n" +
                    "               ORDER BY he2.emp_id\n" +
                    "               LIMIT 10\n" +
                    "           ) names\n" +
                    "       ) AS employee_list\n" +
                    "FROM department d\n" +
                    "JOIN high_earners he ON d.department_id = he.department\n" +
                    "GROUP BY d.department_id, d.department_name\n" +
                    "ORDER BY d.department_id DESC;";

            // store the SQL to a local file
            try {
                File out = new File("solution.sql");
                try (FileWriter fw = new FileWriter(out)) {
                    fw.write("-- Generated on " + LocalDateTime.now() + "\n");
                    fw.write(finalQuery + "\n");
                }
                System.out.println("Wrote solution.sql to " + out.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Failed to write solution.sql: " + ex.getMessage());
            }

            // Send final SQL to testWebhook endpoint
            String testWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
            HttpHeaders postHeaders = new HttpHeaders();
            postHeaders.setContentType(MediaType.APPLICATION_JSON);
            // Use accessToken as-is in Authorization header as per instructions
            postHeaders.set("Authorization", response.getAccessToken());

            Map<String, String> body = new HashMap<>();
            body.put("finalQuery", finalQuery);

            HttpEntity<Map<String, String>> postEntity = new HttpEntity<>(body, postHeaders);

            try {
                ResponseEntity<String> postResp = restTemplate.postForEntity(testWebhookUrl, postEntity, String.class);
                System.out.println("Posted final query. Response: " + postResp.getStatusCode());
            } catch (Exception ex) {
                System.err.println("Failed to post final query: " + ex.getMessage());
            }
        };
    }
}
