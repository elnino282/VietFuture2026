package org.example.ai.controller;

import org.example.ai.service.DocumentIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/ai")
public class InternalAiController {

    private final DocumentIngestionService documentIngestionService;

    public InternalAiController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(@RequestParam(defaultValue = "/data/ai_knowledge") String dataDir) {
        String result = documentIngestionService.ingestData(dataDir);
        return ResponseEntity.ok(result);
    }
}
