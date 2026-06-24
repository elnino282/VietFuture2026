package org.example.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalAgriExpertRequest {
    private String userMessage;
    private String cropContext;
}
