package org.example.adminreporting.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.AdminDocumentResponse;
import org.example.adminreporting.entity.Document;
import org.example.adminreporting.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDocumentService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DocumentRepository documentRepository;

    public PageResponse<AdminDocumentResponse> listDocuments(
            int page,
            int size,
            String q,
            String type,
            String status,
            String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        // Simple listing for read-only admin document overview
        Page<Document> documentPage = documentRepository.findAll(pageable);
        List<AdminDocumentResponse> items = documentPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.of(documentPage, items);
    }

    public AdminDocumentResponse getDocumentById(Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        return mapToResponse(doc);
    }

    private AdminDocumentResponse mapToResponse(Document doc) {
        return AdminDocumentResponse.builder()
                .id(Long.valueOf(doc.getDocumentId()))
                .title(doc.getTitle())
                .description(doc.getDescription())
                .documentUrl(doc.getUrl())
                .documentType(doc.getTopic())
                .status(Boolean.TRUE.equals(doc.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().format(DTF) : null)
                .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().format(DTF) : null)
                .createdBy(doc.getCreatedBy())
                .build();
    }
}
