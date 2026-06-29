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
public class AdminDocumentService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
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

        Page<Document> documentPage = documentRepository.findAll(pageable);
        List<AdminDocumentResponse> items = documentPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.of(documentPage, items);
    }

    @Transactional(readOnly = true)
    public AdminDocumentResponse getDocumentById(Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        return mapToResponse(doc);
    }

    @Transactional
    public AdminDocumentResponse createDocument(
            String title,
            String description,
            String documentUrl,
            String documentType,
            String status) {
        Document doc = Document.builder()
                .title(title)
                .description(description)
                .url(documentUrl)
                .topic(documentType)
                .isActive("ACTIVE".equalsIgnoreCase(status))
                .isPublic(true)
                .build();
        Document saved = documentRepository.save(doc);
        return mapToResponse(saved);
    }

    @Transactional
    public AdminDocumentResponse updateDocument(
            Integer id,
            String title,
            String description,
            String documentUrl,
            String documentType,
            String status) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        doc.setTitle(title);
        doc.setDescription(description);
        doc.setUrl(documentUrl);
        doc.setTopic(documentType);
        doc.setIsActive("ACTIVE".equalsIgnoreCase(status));
        Document saved = documentRepository.save(doc);
        return mapToResponse(saved);
    }

    @Transactional
    public void softDeleteDocument(Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        doc.setIsActive(false);
        documentRepository.save(doc);
    }

    @Transactional
    public void hardDeleteDocument(Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        documentRepository.delete(doc);
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
