package com.hu;

import com.google.gson.Gson;
import com.hu.llm.ChatDeepSeekAI;
import com.hu.llm.entity.DeepSeekRequest;
import com.hu.llm.entity.DeepSeekResponse;
import com.hu.llm.entity.SchemaCleaner;
import com.hu.llm.util.LogUtil;
import com.hu.mcp.MCPClient;
import com.hu.rag.EmbeddingRetrieve;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Agent {

    private static final Gson gson = new Gson();

    private List<MCPClient> mcpClientList = new ArrayList<>();

    private ChatDeepSeekAI llm;

    private String model;

    private String systemPrompt;

    private String context;

    public Agent(String model, List<MCPClient> mcpClientList, String systemPrompt, String context) {
        this.model = model;
        this.systemPrompt = StringUtils.isEmpty(systemPrompt) ? "" : systemPrompt;
        this.context = StringUtils.isEmpty(context) ? "" : context;
        this.mcpClientList = mcpClientList;
    }

    public void init() {
        LogUtil.logTitle("INIT LLM AND TOOLS");
        for (MCPClient client : this.mcpClientList) {
            client.init();
        }
        List<McpSchema.Tool> toolList =
            this.mcpClientList.stream().flatMap(mcpClient -> mcpClient.getTools().stream()).toList();

        this.llm = new ChatDeepSeekAI(this.model, this.systemPrompt, this.context, transferToolConfig(toolList));
    }

    private List<DeepSeekRequest.ToolWrapper> transferToolConfig(List<McpSchema.Tool> mcpTools) {
        List<DeepSeekRequest.ToolWrapper> toolWrappers = new ArrayList<>();
        for (McpSchema.Tool tool : mcpTools) {
            DeepSeekRequest.Tool toolInfo = DeepSeekRequest.Tool.builder().name(tool.name())
                .description(tool.description()).parameters(SchemaCleaner.clean(tool.inputSchema())).build();
            DeepSeekRequest.ToolWrapper toolWrapper =
                DeepSeekRequest.ToolWrapper.builder().type("function").function(toolInfo).build();
            toolWrappers.add(toolWrapper);
        }
        return toolWrappers;
    }

    public void close() {
        LogUtil.logTitle("CLOSE MCP CLIENTS");
        for (MCPClient mcpClient : this.mcpClientList) {
            mcpClient.close();
        }
    }

    public String invoke(String prompt) throws UnirestException {
        if (Objects.isNull(this.llm)) {
            throw new RuntimeException("LLM not initialized.");
        }
        Pair<String, List<DeepSeekResponse.ToolWrapper>> response = this.llm.chat(prompt);
        while (true) {
            if (!CollectionUtils.isEmpty(response.getRight())) {
                for (DeepSeekResponse.ToolWrapper toolWrapper : response.getRight()) {
                    MCPClient client = null;
                    for (MCPClient mcpClient : mcpClientList) {
                        List<McpSchema.Tool> tools = mcpClient.getTools();
                        boolean findTool = false;
                        for (McpSchema.Tool tool : tools) {
                            if (tool.name().equals(toolWrapper.getFunction().getName())) {
                                findTool = true;
                                break;
                            }
                        }
                        if (findTool) {
                            client = mcpClient;
                            break;
                        }
                    }
                    if (Objects.nonNull(client)) {
                        String toolName = toolWrapper.getFunction().getName();
                        String arguments = toolWrapper.getFunction().getArguments();
                        LogUtil.logTitle("TOOL USE" + toolName);
                        System.out.println("Calling tool: " + toolName);
                        System.out.println("Calling arguments: " + arguments);
                        McpSchema.CallToolResult callToolResult =
                            client.callTool(toolName, gson.fromJson(arguments, Map.class));
                        System.out.println("Result: " + gson.toJson(callToolResult));
                        this.llm.appendToolResult(toolWrapper.getId(), gson.toJson(callToolResult.content()));
                    } else {
                        this.llm.appendToolResult(toolWrapper.getId(), "Tool not found");
                    }
                }
                response = this.llm.chat("");
                continue;
            }
            this.close();
            return response.getKey();
        }
    }

    public static void main(String[] args) throws UnirestException {
        ArrayList<String> fetchMCPArguments = new ArrayList<>();
        fetchMCPArguments.add("-m");
        fetchMCPArguments.add("mcp_server_fetch");
        MCPClient fetchMCP = new MCPClient("python", fetchMCPArguments);

        ArrayList<String> fileMCPArguments = new ArrayList<>();
        fileMCPArguments.add("/c");
        fileMCPArguments.add("npx");
        fileMCPArguments.add("-y");
        fileMCPArguments.add("@modelcontextprotocol/server-filesystem");
        ApplicationHome applicationHome = new ApplicationHome(Agent.class);
        String path = applicationHome.getDir().getAbsolutePath();
        fileMCPArguments.add(path);
        MCPClient fileMCP = new MCPClient("cmd", fileMCPArguments);

        List<MCPClient> mcpClients = new ArrayList<>();
        mcpClients.add(fetchMCP);
        mcpClients.add(fileMCP);

        String prompt = "根据Kurtis-Weissnat的信息，创作一个她的故事保存到" + path + "/Kurtis-Weissnat.md，要包含她的基本信息和故事";
        String context = retrieveContext(prompt);
        Agent agent = new Agent("deepseek-v4-flash", mcpClients, "", context);
        agent.init();

        // String response = agent.invoke("爬取https://tech.sina.com.cn/news/的内容，并且总结后保存" + path + "的news.md文件中");
//        String response = agent
//            .invoke("爬取https://jsonplaceholder.typicode.com/users的内容，在" + path + "/knowledge 中，每个人创建一个md文件，保存基本信息");
        String response = agent.invoke(prompt);
        System.out.println(response);
    }

    private static String retrieveContext(String prompt) throws UnirestException {
        EmbeddingRetrieve embeddingRetrieve = new EmbeddingRetrieve("Qwen/Qwen3-Embedding-0.6B");
        ApplicationHome applicationHome = new ApplicationHome(Agent.class);
        String path = applicationHome.getDir().getAbsolutePath();
        String knowledgeDir = path + "/knowledge";
        File dir = new File(knowledgeDir);
        for (File file : dir.listFiles()) {
            String fileContent = readFileContent(file.getAbsolutePath());
            embeddingRetrieve.embedDocument(fileContent);
        }
        String context = embeddingRetrieve.retrieve(prompt, 3);
        LogUtil.logTitle("CONTEXT");
        System.out.println(context);
        return context;
    }

    private static String readFileContent(String filePath) {
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) { // 按行读取
                fileContent.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent.toString();
    }
}
