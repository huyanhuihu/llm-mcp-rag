package com.hu.llm.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * deepseek响应模型
 */
@Data
@Builder
public class DeepSeekResponse {
    private List<Choice> choices;

    private Usage usage;

    @Data
    @Builder
    public static class Choice {
        private Message message;
    }

    @Data
    @Builder
    public static class Message {
        private String content;
        private String reasoning_content;
        private List<ToolWrapper> tool_calls;
    }

    @Data
    @Builder
    public static class ToolWrapper {
        private String id;
        private String type;
        private Tool function;
    }

    @Data
    @Builder
    public static class Tool {
        private String name;
        private String arguments;
    }

    @Data
    @Builder
    public static class Usage {
        private Integer total_tokens;
    }
}
