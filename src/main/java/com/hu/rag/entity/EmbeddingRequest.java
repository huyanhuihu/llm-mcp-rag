package com.hu.rag.entity;

import lombok.Data;

@Data
public class EmbeddingRequest {
    private String model;

    private String input;
}
