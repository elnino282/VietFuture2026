package org.example.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalBuyerExpertRequest {
    private String userMessage;
    private String buyerContext;
}
