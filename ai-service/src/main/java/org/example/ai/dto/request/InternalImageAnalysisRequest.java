package org.example.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalImageAnalysisRequest {
    private String imageBytesBase64;
    private String mimeType;
}
