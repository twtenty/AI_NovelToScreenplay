package com.example.novel2script.controller;

import com.example.novel2script.model.ConvertRequest;
import com.example.novel2script.model.ConvertResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.example.novel2script.service.AuthService;
import com.example.novel2script.service.HistoryService;
import com.example.novel2script.service.PromptService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ConvertController {
    private static final int MAX_NOVEL_TEXT_LENGTH = 1000;
    private static final int DAILY_FREE_CONVERT_LIMIT = 3;
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private final AuthService authService;
    private final HistoryService historyService;
    private final PromptService promptService;

    public ConvertController(AuthService authService, HistoryService historyService, PromptService promptService) {
        this.authService = authService;
        this.historyService = historyService;
        this.promptService = promptService;
    }

    @PostMapping(value = "/convert", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ConvertResponse convert(@RequestBody ConvertRequest request, HttpSession session, HttpServletResponse response) {
        String username = (String) session.getAttribute(AuthController.SESSION_USERNAME);
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return new ConvertResponse(null, "请先登录后再转换剧本。");
        }

        if (request == null || request.getNovelText() == null || request.getNovelText().trim().isEmpty()) {
            return new ConvertResponse(null, "小说正文不能为空。");
        }

        boolean vip = authService.isVip(username);
        if (!vip && request.getNovelText().trim().length() > MAX_NOVEL_TEXT_LENGTH) {
            return new ConvertResponse(null, "小说正文最多 1000 字，请删减后再转换。");
        }

        if (!vip && historyService.countToday(username) >= DAILY_FREE_CONVERT_LIMIT) {
            return new ConvertResponse(null, "普通用户每天最多转换 3 次，请开通 VIP 后继续使用。");
        }

        if (promptService.estimateChapterCount(request.getNovelText()) < 3) {
            return new ConvertResponse(null, "请至少输入 3 个章节。建议使用“第一章 / 第二章 / 第三章”或“Chapter 1 / Chapter 2 / Chapter 3”作为章节标题。");
        }

        try {
            String yaml = promptService.convertNovelToYaml(request.getTitle(), request.getSource(), request.getNovelText());
            String warning = validateYaml(yaml);
            historyService.save(username, request, yaml);
            return new ConvertResponse(yaml, null, warning);
        } catch (Exception e) {
            return new ConvertResponse(null, "转换失败: " + e.getMessage());
        }
    }

    @GetMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object history(HttpSession session, HttpServletResponse response) {
        String username = (String) session.getAttribute(AuthController.SESSION_USERNAME);
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return List.of();
        }
        return historyService.listByUsername(username);
    }

    @DeleteMapping(value = "/history/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object deleteHistory(@PathVariable("id") long id, HttpSession session, HttpServletResponse response) {
        String username = (String) session.getAttribute(AuthController.SESSION_USERNAME);
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return new ConvertResponse(null, "请先登录后再删除历史记录。");
        }
        if (!historyService.delete(username, id)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ConvertResponse(null, "历史记录不存在或无权删除。");
        }
        return new ConvertResponse(null, null);
    }

    private String validateYaml(String yaml) {
        try {
            YAML_MAPPER.readTree(yaml);
            return null;
        } catch (Exception e) {
            return "YAML 格式可能有问题，建议检查缩进和字段结构后再使用。";
        }
    }
}
