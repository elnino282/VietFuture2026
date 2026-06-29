package org.example.QuanLyMuaVu.module.admin.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminDocumentResponse;
import org.example.QuanLyMuaVu.module.admin.entity.Document;
import org.example.QuanLyMuaVu.module.admin.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DocumentEvent;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDocumentService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DocumentRepository documentRepository;
    private final DomainEventPublisher domainEventPublisher;

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
        domainEventPublisher.publish(new DocumentEvent(saved.getId(), saved.getTitle(), saved.getTopic(), saved.getIsActive(), saved.getUrl(), saved.getDescription(), DocumentEvent.Action.CREATED));
        return mapToResponse(saved);
    }

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
        domainEventPublisher.publish(new DocumentEvent(saved.getId(), saved.getTitle(), saved.getTopic(), saved.getIsActive(), saved.getUrl(), saved.getDescription(), DocumentEvent.Action.UPDATED));
        return mapToResponse(saved);
    }

    public void softDeleteDocument(Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        doc.setIsActive(false);
        Document saved = documentRepository.save(doc);
        domainEventPublisher.publish(new DocumentEvent(saved.getId(), saved.getTitle(), saved.getTopic(), saved.getIsActive(), saved.getUrl(), saved.getDescription(), DocumentEvent.Action.DELETED));
    }

    public void hardDeleteDocument(Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        documentRepository.delete(doc);
        domainEventPublisher.publish(new DocumentEvent(doc.getId(), doc.getTitle(), doc.getTopic(), doc.getIsActive(), doc.getUrl(), doc.getDescription(), DocumentEvent.Action.DELETED));
    }

    private AdminDocumentResponse mapToResponse(Document doc) {
        return AdminDocumentResponse.builder()
                .id(doc.getId() != null ? doc.getId().longValue() : null)
                .title(doc.getTitle())
                .description(doc.getDescription())
                .documentUrl(doc.getUrl())
                .documentType(doc.getTopic() != null ? doc.getTopic() : "OTHER")
                .status(Boolean.TRUE.equals(doc.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().format(DTF) : null)
                .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().format(DTF) : null)
                .createdBy(doc.getCreatedBy())
                .build();
    }
}
