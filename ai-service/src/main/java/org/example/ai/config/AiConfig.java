package org.example.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.Embedding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import java.util.Optional;

@Configuration
public class AiConfig {

    @Bean
    @Primary
    public VectorStore dummyVectorStore() {
        return new VectorStore() {
            @Override
            public void add(List<Document> documents) {}
            @Override
            public Optional<Boolean> delete(List<String> idList) { return Optional.of(true); }
            @Override
            public List<Document> similaritySearch(SearchRequest request) { return Collections.emptyList(); }
            @Override
            public List<Document> similaritySearch(String query) { return Collections.emptyList(); }
        };
    }

    @Bean
    @Primary
    public EmbeddingModel dummyEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public float[] embed(java.lang.String document) {
                return new float[384]; // return dummy array
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return new float[384]; // return dummy array
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = request.getInstructions().stream()
                        .map(text -> new Embedding(new float[384], 0))
                        .collect(Collectors.toList());
                return new EmbeddingResponse(embeddings);
            }
        };
    }
}
