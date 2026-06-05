package com.example.novel2script.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper(
            YAMLFactory.builder()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .build()
    );
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("(?im)^\\s*(第[一二三四五六七八九十百千万0-9]+[章节回]|chapter\\s*\\d+|chap\\.\\s*\\d+).*$");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    @Autowired
    public PromptService(
            @Value("${LLM_API_KEY:}") String llmApiKey,
            @Value("${DASHSCOPE_API_KEY:}") String dashScopeApiKey,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${LLM_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${LLM_MODEL:qwen-turbo}") String model
    ) {
        this.apiKey = firstNonBlank(llmApiKey, dashScopeApiKey, apiKey);
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.model = model;
    }

    PromptService(String apiKey, String model) {
        this(apiKey, "", "", "https://dashscope.aliyuncs.com/compatible-mode/v1", model);
    }

    public int estimateChapterCount(String novelText) {
        if (novelText == null || novelText.isBlank()) {
            return 0;
        }
        return (int) CHAPTER_PATTERN.matcher(novelText).results().count();
    }

    public List<String> extractChapterTitles(String novelText) {
        List<String> chapters = new ArrayList<>();
        if (novelText == null || novelText.isBlank()) {
            return chapters;
        }

        Matcher matcher = CHAPTER_PATTERN.matcher(novelText);
        while (matcher.find()) {
            chapters.add(matcher.group().trim());
        }
        return chapters;
    }

    public String convertNovelToYaml(String title, String source, String novelText) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            return buildDemoYaml(title, source, novelText);
        }

        String prompt = buildPrompt(title, source, novelText);
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("model", model);
        payload.put("temperature", 0.3);
        ArrayNode messages = payload.putArray("messages");

        ObjectNode systemMessage = OBJECT_MAPPER.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a professional screenwriter. Convert long-form fiction into valid, editable screenplay YAML only.");
        messages.add(systemMessage);

        ObjectNode userMessage = OBJECT_MAPPER.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("LLM API returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode()) {
            throw new IOException("未能从 OpenAI 响应中读取结果。响应内容：" + response.body());
        }
        return normalizeModelOutput(contentNode.asText());
    }

    private String buildPrompt(String title, String source, String novelText) {
        StringBuilder builder = new StringBuilder();
        builder.append("请将下面不少于 3 个章节的小说文本自动改编为结构化剧本 YAML。\n");
        builder.append("要求：\n");
        builder.append("1. 必须输出 YAML，不要输出 JSON，不要输出 Markdown 代码块，不要额外解释。\n");
        builder.append("2. 顶层字段必须是 title、source、chapters，不能只输出 scenes。\n");
        builder.append("3. 保留原章节结构，每章拆分为多个 scenes。\n");
        builder.append("4. 每个 scene 需要包含 scene_id、title、location、time、description、characters、actions、dialogues、notes。\n");
        builder.append("5. actions 使用 actor/text 字段；dialogues 使用 speaker/line 字段。\n");
        builder.append("6. YAML 必须换行缩进，便于人类阅读。\n");
        builder.append("7. 对小说叙述进行剧本化压缩，不要逐字复制大段原文。\n\n");
        if (title != null && !title.isBlank()) {
            builder.append("小说标题：").append(title.trim()).append("\n");
        }
        if (source != null && !source.isBlank()) {
            builder.append("小说来源：").append(source.trim()).append("\n");
        }
        builder.append("---\n");
        builder.append("小说正文开始：\n");
        builder.append(novelText.trim()).append("\n");
        builder.append("小说正文结束。\n");
        return builder.toString();
    }

    private String normalizeModelOutput(String content) throws IOException {
        String cleaned = stripCodeFence(content).trim();
        if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(cleaned);
            return YAML_MAPPER.writeValueAsString(jsonNode).trim();
        }
        return cleaned;
    }

    private String stripCodeFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        return trimmed
                .replaceFirst("(?s)^```(?:yaml|yml|json)?\\s*", "")
                .replaceFirst("(?s)\\s*```$", "");
    }

    private String buildDemoYaml(String title, String source, String novelText) {
        String safeTitle = title == null || title.isBlank() ? "未命名小说" : title.trim();
        String safeSource = source == null || source.isBlank() ? "用户输入文本" : source.trim();
        List<String> chapters = extractChapterTitles(novelText);
        if (chapters.isEmpty()) {
            chapters = List.of("示例章节");
        }

        StringBuilder yaml = new StringBuilder();
        yaml.append("title: \"").append(escapeYaml(safeTitle)).append("\"\n");
        yaml.append("source: \"").append(escapeYaml(safeSource)).append("\"\n");
        yaml.append("generation_mode: \"demo_without_api_key\"\n");
        yaml.append("chapters:\n");

        for (int i = 0; i < chapters.size(); i++) {
            int chapterNumber = i + 1;
            String chapter = escapeYaml(chapters.get(i));
            yaml.append("  - chapter: \"").append(chapter).append("\"\n");
            yaml.append("    summary: \"").append(chapter).append(" 的剧情摘要将在配置 API Key 后由 AI 根据原文生成；当前演示模式先保留章节结构。\"\n");
            yaml.append("    scenes:\n");
            yaml.append("      - scene_id: \"").append(chapterNumber).append(".1\"\n");
            yaml.append("        title: \"").append(chapter).append(" - 核心场景\"\n");
            yaml.append("        location: \"该章节的主要发生地点\"\n");
            yaml.append("        time: \"该章节的主要时间段\"\n");
            yaml.append("        description: \"演示模式已将章节转为剧本场景骨架。配置 API Key 后，这里会生成更具体的场景概述、冲突推进和氛围说明。\"\n");
            yaml.append("        characters:\n");
            yaml.append("          - \"主要角色\"\n");
            yaml.append("          - \"相关角色\"\n");
            yaml.append("        actions:\n");
            yaml.append("          - actor: \"主要角色\"\n");
            yaml.append("            text: \"围绕本章核心事件展开行动，推动冲突发展。\"\n");
            yaml.append("        dialogues:\n");
            yaml.append("          - speaker: \"主要角色\"\n");
            yaml.append("            line: \"配置 API Key 后，这里会生成贴合人物关系和剧情冲突的对白。\"\n");
            yaml.append("        notes: \"当前为无 API Key 演示输出，可用于检查页面、章节识别和 YAML 结构。\"\n");
        }

        return yaml.toString();
    }

    private String escapeYaml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
