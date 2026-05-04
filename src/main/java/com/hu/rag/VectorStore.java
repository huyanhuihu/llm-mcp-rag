package com.hu.rag;

import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟文档嵌入后数据存储与检索
 */
@Data
public class VectorStore {
    private List<VectorStoreItem> vectorStore = new ArrayList<>();

    @Data
    class VectorStoreItem{
        private List<Double> number;
        private String document;
    }

    public void addItem(VectorStoreItem vectorStoreItem) {
        this.vectorStore.add(vectorStoreItem);
    }

    public void addItem(List<Double> number, String document) {
        VectorStoreItem vectorStoreItem = new VectorStoreItem();
        vectorStoreItem.setDocument(document);
        vectorStoreItem.setNumber(number);
        this.vectorStore.add(vectorStoreItem);
    }

    public List<Pair<String, Double>> search(List<Double> queryEmbedding, Integer topK) {
        List<Pair<String, Double>> documentScore = new ArrayList<>();
        for (VectorStoreItem vectorStoreItem : this.vectorStore) {
            Double score = cosineSim(queryEmbedding, vectorStoreItem.getNumber());
            documentScore.add(Pair.of(vectorStoreItem.getDocument(), score));
        }

        documentScore.sort((p1, p2) -> p1.getRight().compareTo(p2.getRight()));
        return documentScore.subList(0, topK);
    }

    public Double cosineSim(List<Double> list1, List<Double> list2) {
        double numerator = 0D;
        double leftDenominator = 0D;
        double rightDenominator = 0D;
        for (int i=0; i<list1.size(); i++) {
            numerator += list1.get(i) * list2.get(i);

            leftDenominator += list1.get(i);
            rightDenominator += list2.get(i);
        }

        return numerator / (Math.sqrt(leftDenominator) * Math.sqrt(rightDenominator));
    }
}
