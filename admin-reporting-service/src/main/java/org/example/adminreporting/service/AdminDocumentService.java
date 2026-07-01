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

        String sortBy = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;

        if (sort != null) {
            if (sort.equalsIgnoreCase("NEWEST")) {
                sortBy = "createdAt";
                direction = Sort.Direction.DESC;
            } else if (sort.equalsIgnoreCase("MOST_VIEWED")) {
                sortBy = "viewCount";
                direction = Sort.Direction.DESC;
            } else if (sort.equalsIgnoreCase("RECOMMENDED")) {
                sortBy = "isPinned";
                direction = Sort.Direction.DESC;
            } else {
                String[] sortParts = sort.split(",");
                sortBy = sortParts[0];
                direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Document> documentPage = documentRepository.findAll(pageable);
        List<AdminDocumentResponse> items = documentPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.of(documentPage, items);
    }

    public org.example.adminreporting.dto.response.DocumentMetaResponse getDocumentMeta() {
        return org.example.adminreporting.dto.response.DocumentMetaResponse.builder()
                .types(List.of("GUIDE", "TEMPLATE", "ANNOUNCEMENT", "SYSTEM_HELP"))
                .stages(List.of("Planting", "Growing", "Harvest", "Post-Harvest"))
                .topics(List.of("Best Practices", "Pest Management", "Water Management", "Soil Management", "Farm Planning", "Climate Adaptation", "POLICY"))
                .crops(List.of(
                        new org.example.adminreporting.dto.response.DocumentMetaResponse.CropOption(1, "Rice"),
                        new org.example.adminreporting.dto.response.DocumentMetaResponse.CropOption(2, "Coffee"),
                        new org.example.adminreporting.dto.response.DocumentMetaResponse.CropOption(3, "Pepper")
                ))
                .build();
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
                .documentId(Long.valueOf(doc.getDocumentId()))
                .title(doc.getTitle())
                .url(doc.getUrl())
                .description(doc.getDescription())
                .crop(doc.getCrop())
                .stage(doc.getStage())
                .topic(doc.getTopic())
                .documentType(doc.getDocumentType())
                .viewCount(doc.getViewCount())
                .isPinned(doc.getIsPinned())
                .isActive(doc.getIsActive())
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().format(DTF) : null)
                .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().format(DTF) : null)
                .createdBy(doc.getCreatedBy())
                .isFavorited(false)  // Per-user tracking not yet implemented
                .isPublic(doc.getIsPublic())
                .build();
    }
}
