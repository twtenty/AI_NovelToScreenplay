package com.example.novel2script.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptServiceTest {
    private final PromptService promptService = new PromptService("", "gpt-4o-mini");

    @Test
    void estimatesChineseChapterCount() {
        String text = """
                第一章 开端
                内容
                第二章 转折
                内容
                第三章 结尾
                内容
                """;

        assertEquals(3, promptService.estimateChapterCount(text));
    }

    @Test
    void extractsChapterTitles() {
        String text = """
                第一章 开端
                内容
                第二章 转折
                内容
                Chapter 3 Ending
                内容
                """;

        assertEquals(3, promptService.extractChapterTitles(text).size());
        assertEquals("第一章 开端", promptService.extractChapterTitles(text).get(0));
        assertEquals("Chapter 3 Ending", promptService.extractChapterTitles(text).get(2));
    }

    @Test
    void returnsDemoYamlWithoutApiKey() throws Exception {
        String yaml = promptService.convertNovelToYaml("测试小说", "单元测试", """
                第一章 开端
                内容
                第二章 转折
                内容
                第三章 结尾
                内容
                """);

        assertTrue(yaml.contains("generation_mode: \"demo_without_api_key\""));
        assertTrue(yaml.contains("测试小说"));
        assertTrue(yaml.contains("chapter: \"第一章 开端\""));
        assertTrue(yaml.contains("scene_id: \"3.1\""));
    }
}
