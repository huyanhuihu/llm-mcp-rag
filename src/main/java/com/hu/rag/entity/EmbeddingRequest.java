package com.hu.rag.entity;

import lombok.Data;

/**
 * 硅基流动请求模型
 */
@Data
public class EmbeddingRequest {
    private String model;

    private String input;
}
