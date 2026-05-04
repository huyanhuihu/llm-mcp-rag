package com.hu.llm.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SchemaCleaner {

    private static final Gson GSON = new GsonBuilder().create();
    // 定义 DeepSeek 不支持的字段，方便统一管理和修改
    private static final Set<String> UNSUPPORTED_FIELDS = new HashSet<>(Arrays.asList(
        "format", "title", "minLength", "maxLength", 
        "exclusiveMinimum", "exclusiveMaximum", "$ref"
    ));

    /**
     * 清洗 McpSchema 对象，返回一个移除了不兼容字段的 Map。
     *
     * @param schema McpSchema 对象或其子对象
     * @return 清洗后的 Map 对象，可直接用于构建 API 请求
     */
    public static Object clean(Object schema) {
        // 1. 将 McpSchema 对象转换为 JsonElement
        JsonElement jsonElement = GSON.toJsonTree(schema);
        // 2. 递归清洗 JsonElement
        cleanJsonElement(jsonElement);
        // 3. 将清洗后的 JsonElement 转换回 Java 对象（Map/List/基础类型）
        return GSON.fromJson(jsonElement, Object.class);
    }

    // 递归函数，核心逻辑
    private static void cleanJsonElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // 1. 删除当前层级中不支持的字段
            UNSUPPORTED_FIELDS.forEach(obj::remove);
            
            // 2. 特殊处理：如果 required 字段变成了空数组，也可以考虑删掉它（可选，取决于 API 宽容度）
            if (obj.has("required") && obj.get("required").isJsonArray() && obj.getAsJsonArray("required").isEmpty()) {
                obj.remove("required");
            }

            // 3. 递归处理当前对象中所有嵌套的值
            obj.entrySet().forEach(entry -> cleanJsonElement(entry.getValue()));
        } else if (element.isJsonArray()) {
            // 4. 递归处理数组中的每个元素
            element.getAsJsonArray().forEach(SchemaCleaner::cleanJsonElement);
        }
        // 基础类型（字符串、数字等）无需处理
    }
}