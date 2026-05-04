package com.hu.llm.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * deepseek请求模型
 */
@Data
@Builder
public class DeepSeekRequest {
    private String model;
    private List<Message> messages;
    Boolean stream = false;
    private List<ToolWrapper> tools;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
        private String tool_call_id;
        private String reasoning_content;
        private List<Map<String, Object>> tool_calls;
    }

    @Data
    @Builder
    public static class ToolWrapper {
        private String type = "function";
        private Tool function;
    }

    @Data
    @Builder
    public static class Tool {
        private String description;
        private String name;
        private Object parameters;
    }
}
