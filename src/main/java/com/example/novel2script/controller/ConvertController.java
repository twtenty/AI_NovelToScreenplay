package com.example.novel2script.controller;

import com.example.novel2script.model.ConvertRequest;
import com.example.novel2script.model.ConvertResponse;
import com.example.novel2script.service.PromptService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConvertController {
    private final PromptService promptService;

    public ConvertController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping(value = "/convert", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ConvertResponse convert(@RequestBody ConvertRequest request, HttpSession session, HttpServletResponse response) {
        if (session.getAttribute(AuthController.SESSION_USERNAME) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return new ConvertResponse(null, "请先登录后再转换剧本。");
        }

        if (request == null || request.getNovelText() == null || request.getNovelText().trim().isEmpty()) {
            return new ConvertResponse(null, "小说正文不能为空。");
        }

        if (promptService.estimateChapterCount(request.getNovelText()) < 3) {
            return new ConvertResponse(null, "请至少输入 3 个章节。建议使用“第一章 / 第二章 / 第三章”或“Chapter 1 / Chapter 2 / Chapter 3”作为章节标题。");
        }

        try {
            String yaml = promptService.convertNovelToYaml(request.getTitle(), request.getSource(), request.getNovelText());
            return new ConvertResponse(yaml, null);
        } catch (Exception e) {
            return new ConvertResponse(null, "转换失败: " + e.getMessage());
        }
    }
}
