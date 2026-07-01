package org.example.adminreporting.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record DocumentMetaResponse(
        List<String> types,
        List<String> stages,
        List<String> topics,
        List<CropOption> crops
) {
    public record CropOption(Integer id, String name) {}
}
