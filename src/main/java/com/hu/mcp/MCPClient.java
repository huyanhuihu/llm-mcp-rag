package com.hu.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * mcp工具通信逻辑
 */
public class MCPClient {
    private McpSyncClient mcpSyncClient;

    private List<McpSchema.Tool> tools;

    public MCPClient(String command, List<String> arguments) {
        ServerParameters params = ServerParameters.builder(command).args(arguments).build();
        ObjectMapper objectMapper = new ObjectMapper();
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        StdioClientTransport transport = new StdioClientTransport(params, jsonMapper);
        this.mcpSyncClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10))
            .build();
    }

    public void close() {
        this.mcpSyncClient.closeGracefully();
    }

    public void init() {
        this.mcpSyncClient.initialize();
        connectToServer();
    }

    public List<McpSchema.Tool> getTools() {
        return this.tools;
    }

    public McpSchema.CallToolResult callTool(String name, Map<String, Object> arguments) {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(name, arguments);
        McpSchema.CallToolResult callToolResult = this.mcpSyncClient.callTool(request);
        return callToolResult;
    }

    private void connectToServer() {
        try {
            this.tools = this.mcpSyncClient.listTools().tools();
            System.out.println("Connect to server with tools: "
                    + this.tools.stream().map(McpSchema.Tool::name).collect(Collectors.joining(", ")));
        } catch (Exception e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        ArrayList<String> arguments = new ArrayList<>();
        arguments.add("-m");
        arguments.add("mcp_server_fetch");
        MCPClient fetchMCP = new MCPClient("python", arguments);
        fetchMCP.init();
        List<McpSchema.Tool> tools1 = fetchMCP.getTools();
        System.out.println(tools1);
        fetchMCP.close();

        ArrayList<String> fileMCPArguments = new ArrayList<>();
        fileMCPArguments.add("/c");
        fileMCPArguments.add("npx");
        fileMCPArguments.add("-y");
        fileMCPArguments.add("@modelcontextprotocol/server-filesystem");
        ApplicationHome applicationHome = new ApplicationHome(MCPClient.class);
        String path = applicationHome.getDir().getAbsolutePath();
        fileMCPArguments.add(path);
        MCPClient fileMCP = new MCPClient("cmd", fileMCPArguments);
        fileMCP.init();
        List<McpSchema.Tool> tools2 = fileMCP.getTools();
        System.out.println(tools2);
        fileMCP.close();
    }
}
