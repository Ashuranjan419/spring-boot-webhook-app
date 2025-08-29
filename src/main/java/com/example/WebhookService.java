package com.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookService implements CommandLineRunner {

    private final RestTemplate restTemplate;

    public WebhookService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // Step 1: Generate webhook
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "John Doe");
        requestBody.put("regNo", "22BCE8699");
        requestBody.put("email", "john@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA",
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            String webhookUrl = (String) responseBody.get("webhook");
            String accessToken = (String) responseBody.get("accessToken");

            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("Access Token: " + accessToken);

            // Step 2: Determine SQL query based on regNo
            String regNo = "22BCE8699";
            String lastTwoDigits = regNo.substring(regNo.length() - 2);
            int lastDigit = Integer.parseInt(lastTwoDigits.substring(1));
            String sqlQuery;

            if (lastDigit % 2 == 1) {
                // Odd - Question 1
                sqlQuery = "SELECT p.AMOUNT as SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) as NAME, YEAR(CURRENT_DATE) - YEAR(e.DOB) as AGE, d.DEPARTMENT_NAME FROM PAYMENTS p JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID WHERE DAY(p.PAYMENT_TIME) != 1 ORDER BY p.AMOUNT DESC LIMIT 1;";
            } else {
                // Even - Question 2
                sqlQuery = "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, COUNT(e2.EMP_ID) as YOUNGER_EMPLOYEES_COUNT FROM EMPLOYEE e1 LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e1.EMP_ID != e2.EMP_ID AND e2.DOB > e1.DOB JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME ORDER BY e1.EMP_ID DESC;";
            }

            // Step 3: Send solution to webhook
            Map<String, String> solutionBody = new HashMap<>();
            solutionBody.put("finalQuery", sqlQuery);

            HttpHeaders solutionHeaders = new HttpHeaders();
            solutionHeaders.setContentType(MediaType.APPLICATION_JSON);
            solutionHeaders.set("Authorization", accessToken);

            HttpEntity<Map<String, String>> solutionEntity = new HttpEntity<>(solutionBody, solutionHeaders);

            ResponseEntity<String> solutionResponse = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                solutionEntity,
                String.class
            );

            System.out.println("Solution submitted. Response: " + solutionResponse.getBody());
        } else {
            System.out.println("Failed to generate webhook. Status: " + response.getStatusCode());
        }
    }
}