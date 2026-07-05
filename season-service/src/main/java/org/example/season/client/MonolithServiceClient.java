package org.example.season.client;

import org.example.season.service.ExternalServiceClient.ValidationResultDto;
import org.example.season.service.ExternalServiceClient.CreateNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "monolith-service", url = "${app.monolith-service-url}", fallback = MonolithServiceClientFallback.class)
public interface MonolithServiceClient {

    @GetMapping("/api/v1/public/expenses/exists-by-season/{seasonId}")
    Boolean existsExpenseBySeasonId(@PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/public/lookup/incidents/{incidentId}/validate-season/{seasonId}")
    ValidationResultDto validateIncidentSeason(@PathVariable("incidentId") Integer incidentId, @PathVariable("seasonId") Integer seasonId);

    @GetMapping("/api/v1/public/lookup/expenses/{expenseId}/validate-season/{seasonId}")
    ValidationResultDto validateExpenseSeason(@PathVariable("expenseId") Integer expenseId, @PathVariable("seasonId") Integer seasonId);

    @PostMapping("/api/v1/public/lookup/notifications")
    void createNotification(@RequestBody CreateNotificationRequest request);
}
