package com.hu.rag.entity;

import lombok.Data;

import java.util.List;

@Data
public class EmbeddingResponse {
    private List<Embedding> data;

    @Data
    public class Embedding {
        private List<Double> embedding;
    }
}
