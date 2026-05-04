package com.hu.rag.entity;

import lombok.Data;

import java.util.List;

/**
 * 硅基流动响应模型
 */
@Data
public class EmbeddingResponse {
    private List<Embedding> data;

    @Data
    public class Embedding {
        private List<Double> embedding;
    }
}
