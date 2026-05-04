package com.hu.llm;

import com.google.gson.Gson;
import com.hu.confiig.PropertyConfig;
import com.hu.util.LogUtil;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.hu.llm.entity.DeepSeekRequest;
import com.hu.llm.entity.DeepSeekResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * deepseek模型通信逻辑
 */
public class ChatDeepSeekAI {
    private static final Gson gson = new Gson();

    private String modelName;

    private PropertyConfig propertyConfig;

    private List<DeepSeekRequest.Message> messages = new ArrayList<>();

    private List<DeepSeekRequest.ToolWrapper> tools;

    public ChatDeepSeekAI(String modelName, String systemPrompt, String context,
        List<DeepSeekRequest.ToolWrapper> tools) {
        this.propertyConfig = new PropertyConfig();
        this.propertyConfig.init();

        this.modelName = modelName;
        this.tools = tools;
        if (!StringUtils.isEmpty(systemPrompt)) {
            messages.add(DeepSeekRequest.Message.builder().role("system").content(systemPrompt).build());
        }
        if (!StringUtils.isEmpty(context)) {
            messages.add(DeepSeekRequest.Message.builder().role("user").content(context).build());
        }
    }

    public Pair<String, List<DeepSeekResponse.ToolWrapper>> chat(String prompt) throws UnirestException {
        LogUtil.logTitle("CHAT");
        // 拼接大模型请求参数
        if (!StringUtils.isEmpty(prompt)) {
            messages.add(DeepSeekRequest.Message.builder().role("user").content(prompt).build());
        }

        DeepSeekRequest request = DeepSeekRequest.builder().model(modelName).messages(messages).tools(tools).build();

        // 请求大模型接口
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post(propertyConfig.getDeepSeekUrl()).header("Content-Type", "application/json")
            .header("Accept", "application/json").header("Authorization", "Bearer " + propertyConfig.getDeepSeekKey())
            .body(gson.toJson(request)).asString();
        DeepSeekResponse deepSeekResponse = gson.fromJson(response.getBody(), DeepSeekResponse.class);
        LogUtil.logTitle("RESPONSE");

        StringBuilder content = new StringBuilder();
        StringBuilder reasoning_content = new StringBuilder();
        List<DeepSeekResponse.ToolWrapper> toolCalls = new ArrayList<>();

        // 处理大模型响应信息
        for (DeepSeekResponse.Choice choice : deepSeekResponse.getChoices()) {
            DeepSeekResponse.Message message = choice.getMessage();
            // 处理content
            if (!StringUtils.isEmpty(message.getContent())) {
                content.append(message.getContent());
                System.out.println("content: " + message.getContent());
            }
            if (!StringUtils.isEmpty(message.getReasoning_content())) {
                reasoning_content.append(message.getReasoning_content());
                System.out.println("reasoning_content: " + message.getReasoning_content());
            }
            // 处理toolCalls
            if (!CollectionUtils.isEmpty(message.getTool_calls())) {
                // 收到一个toolCall
                toolCalls.addAll(message.getTool_calls());
            }
        }
        String resultContent = content.toString();
        this.messages.add(DeepSeekRequest.Message.builder().role("assistant").content(resultContent)
            .tool_calls(transferToolCalls(toolCalls)).reasoning_content(reasoning_content.toString()).build());
        return Pair.of(resultContent, toolCalls);
    }

    public List<Map<String, Object>> transferToolCalls(List<DeepSeekResponse.ToolWrapper> toolCalls) {
        List<Map<String, Object>> toolCallRequest = new ArrayList<>();

        for (DeepSeekResponse.ToolWrapper toolWrapper : toolCalls) {
            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("id", toolWrapper.getId());
            toolMap.put("type", toolWrapper.getType());
            DeepSeekResponse.Tool function = toolWrapper.getFunction();
            Map<String, String> functionMap = new HashMap<>();
            functionMap.put("name", function.getName());
            functionMap.put("arguments", function.getArguments());
            toolMap.put("function", functionMap);

            toolCallRequest.add(toolMap);
        }
        return toolCallRequest;
    }

    public void appendToolResult(String toolCalId, String toolOutput) {
        this.messages
            .add(DeepSeekRequest.Message.builder().role("tool").tool_call_id(toolCalId).content(toolOutput).build());
    }

    public static void main(String[] args) throws UnirestException {
        ChatDeepSeekAI deepSeekAI = new ChatDeepSeekAI("deepseek-v4-flash", "", "", new ArrayList<>());
        deepSeekAI.chat("你好");
    }
}
