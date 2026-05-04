package org.example.QuanLyMuaVu.module.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.dto.response.DocumentResponse;
import org.example.QuanLyMuaVu.module.admin.entity.Document;
import org.example.QuanLyMuaVu.module.admin.entity.DocumentFavorite;
import org.example.QuanLyMuaVu.module.admin.entity.DocumentRecentOpen;
import org.example.QuanLyMuaVu.module.admin.repository.DocumentFavoriteRepository;
import org.example.QuanLyMuaVu.module.admin.repository.DocumentRecentOpenRepository;
import org.example.QuanLyMuaVu.module.admin.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentFavoriteRepository documentFavoriteRepository;
    private final DocumentRecentOpenRepository documentRecentOpenRepository;

    /**
     * List documents for farmer with filters and tab support
     */
    public PageResponse<DocumentResponse> listDocuments(
            String tab,
            String q,
            String crop,
            String stage,
            String topic,
            int page,
            int size,
            Long userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Set<Integer> favoritedIds = documentFavoriteRepository.findDocumentIdsByUserId(userId);

        if ("favorites".equalsIgnoreCase(tab)) {
            return listFavoriteDocuments(userId, pageable, favoritedIds);
        } else if ("recent".equalsIgnoreCase(tab)) {
            return listRecentDocuments(userId, pageable, favoritedIds);
        } else {
            // All documents
            Page<Document> pageData = documentRepository.findAllVisible(q, crop, stage, topic, pageable);
            List<DocumentResponse> items = pageData.getContent().stream()
                    .map(doc -> toResponse(doc, favoritedIds.contains(doc.getId())))
                    .collect(Collectors.toList());
            return PageResponse.of(pageData, items);
        }
    }

    private PageResponse<DocumentResponse> listFavoriteDocuments(Long userId, Pageable pageable,
            Set<Integer> favoritedIds) {
        List<DocumentFavorite> favorites = documentFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Integer> documentIds = favorites.stream()
                .map(DocumentFavorite::getDocumentId)
                .collect(Collectors.toList());

        if (documentIds.isEmpty()) {
            return createEmptyPageResponse(pageable);
        }

        List<Document> visibleDocs = documentRepository.findVisibleByIds(documentIds);
        List<DocumentResponse> items = visibleDocs.stream()
                .map(doc -> toResponse(doc, true)) // all are favorited
                .collect(Collectors.toList());

        // Simple manual pagination for favorites
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());

        if (start >= items.size()) {
            return createEmptyPageResponse(pageable);
        }

        List<DocumentResponse> pagedItems = items.subList(start, end);
        return createPageResponse(pagedItems, pageable, items.size());
    }

    private PageResponse<DocumentResponse> listRecentDocuments(Long userId, Pageable pageable,
            Set<Integer> favoritedIds) {
        // Get recent document IDs (limit to 50 for performance)
        List<Integer> recentDocIds = documentRecentOpenRepository.findRecentDocumentIdsByUserId(
                userId, PageRequest.of(0, 50));

        if (recentDocIds.isEmpty()) {
            return createEmptyPageResponse(pageable);
        }

        List<Document> visibleDocs = documentRepository.findVisibleByIds(recentDocIds);

        // Preserve order from recent opens
        List<DocumentResponse> items = recentDocIds.stream()
                .map(id -> visibleDocs.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null))
                .filter(doc -> doc != null)
                .map(doc -> toResponse(doc, favoritedIds.contains(doc.getId())))
                .collect(Collectors.toList());

        // Simple manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());

        if (start >= items.size()) {
            return createEmptyPageResponse(pageable);
        }

        List<DocumentResponse> pagedItems = items.subList(start, end);
        return createPageResponse(pagedItems, pageable, items.size());
    }

    /**
     * Get single document by ID
     */
    public DocumentResponse getById(Integer id, Long userId) {
        Document doc = documentRepository.findVisibleById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        boolean isFavorited = documentFavoriteRepository.existsByUserIdAndDocumentId(userId, id);
        return toResponse(doc, isFavorited);
    }

    /**
     * Record document open (for Recent tab)
     */
    public void recordOpen(Integer documentId, Long userId) {
        // Verify document exists and is visible
        documentRepository.findVisibleById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Check if already exists, update timestamp
        documentRecentOpenRepository.findByUserIdAndDocumentId(userId, documentId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setOpenedAt(LocalDateTime.now());
                            documentRecentOpenRepository.save(existing);
                        },
                        () -> {
                            DocumentRecentOpen newOpen = DocumentRecentOpen.builder()
                                    .userId(userId)
                                    .documentId(documentId)
                                    .build();
                            documentRecentOpenRepository.save(newOpen);
                        });
    }

    /**
     * Add document to favorites
     */
    public void addFavorite(Integer documentId, Long userId) {
        // Verify document exists and is visible
        documentRepository.findVisibleById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Check if already favorited
        if (documentFavoriteRepository.existsByUserIdAndDocumentId(userId, documentId)) {
            return; // Already favorited, no-op
        }

        DocumentFavorite favorite = DocumentFavorite.builder()
                .userId(userId)
                .documentId(documentId)
                .build();
        documentFavoriteRepository.save(favorite);
    }

    /**
     * Remove document from favorites
     */
    public void removeFavorite(Integer documentId, Long userId) {
        documentFavoriteRepository.deleteByUserIdAndDocumentId(userId, documentId);
    }/**
     * Get all documents (legacy method for backward compatibility)
     */
    public List<DocumentResponse> getAll() {
        return documentRepository.findAll().stream()
                .filter(doc -> Boolean.TRUE.equals(doc.getIsActive()) && Boolean.TRUE.equals(doc.getIsPublic()))
                .map(doc -> toResponse(doc, false))
                .toList();
    }

    // ==================== Helper Methods ====================

    private DocumentResponse toResponse(Document doc, boolean isFavorited) {
        return DocumentResponse.builder()
                .documentId(doc.getId())
                .title(doc.getTitle())
                .url(doc.getUrl())
                .description(doc.getDescription())
                .crop(doc.getCrop())
                .stage(doc.getStage())
                .topic(doc.getTopic())
                .isActive(doc.getIsActive())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .isFavorited(isFavorited)
                .build();
    }

    private PageResponse<DocumentResponse> createEmptyPageResponse(Pageable pageable) {
        PageResponse<DocumentResponse> response = new PageResponse<>();
        response.setItems(List.of());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setTotalElements(0);
        response.setTotalPages(0);
        return response;
    }

    private PageResponse<DocumentResponse> createPageResponse(List<DocumentResponse> items, Pageable pageable,
            int totalElements) {
        PageResponse<DocumentResponse> response = new PageResponse<>();
        response.setItems(items);
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setTotalElements(totalElements);
        response.setTotalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()));
        return response;
    }
}


