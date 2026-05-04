package com.hu.rag;

import com.google.gson.Gson;
import com.hu.llm.confiig.PropertyConfig;
import com.hu.rag.entity.EmbeddingRequest;
import com.hu.rag.entity.EmbeddingResponse;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class EmbeddingRetrieve {

    private static final Gson gson = new Gson();
    private String embeddingModel;

    private VectorStore vectorStore = new VectorStore();

    private PropertyConfig propertyConfig;

    public EmbeddingRetrieve(String embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.propertyConfig = new PropertyConfig();
        this.propertyConfig.init();
    }

    public List<Double> embedQuery(String query) throws UnirestException {
        return embed(query);
    }

    public List<Double> embedDocument(String document) throws UnirestException {
        List<Double> embedding = embed(document);
        this.vectorStore.addItem(embedding, document);
        return embedding;
    }

    private List<Double> embed(String document) throws UnirestException {
        Unirest.setTimeouts(0, 0);
        EmbeddingRequest embeddingRequest = new EmbeddingRequest();
        embeddingRequest.setModel(this.embeddingModel);
        embeddingRequest.setInput(document);
        HttpResponse<String> response = Unirest.post(propertyConfig.getEmbeddingUrl() + "/embeddings")
            .header("Content-Type", "application/json").header("Accept", "application/json")
            .header("Authorization", "Bearer " + propertyConfig.getEmbeddingKey()).body(gson.toJson(embeddingRequest))
            .asString();
        EmbeddingResponse embeddingResponse = gson.fromJson(response.getBody(), EmbeddingResponse.class);
        return embeddingResponse.getData().get(0).getEmbedding();
    }

    public String retrieve(String query, Integer topK) throws UnirestException {
        List<Double> queryEmbedding = this.embedQuery(query);
        List<Pair<String, Double>> search = vectorStore.search(queryEmbedding, topK);
        return search.stream().map(Pair::getLeft).collect(Collectors.joining("\n"));
    }
}
