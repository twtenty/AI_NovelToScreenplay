package com.example.novel2script.service;

import com.example.novel2script.model.ConvertRequest;
import com.example.novel2script.model.HistoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryServiceTest {
    @Test
    void savesConversionHistory() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HistoryService historyService = new HistoryService(jdbcTemplate);
        ConvertRequest request = new ConvertRequest();
        request.setTitle("远方的灯火");
        request.setSource("测试章节");
        request.setNovelText("第一章...\n第二章...\n第三章...");

        historyService.save("writer01", request, "scenes: []");

        verify(jdbcTemplate).update(
                any(String.class),
                eq("writer01"),
                eq("远方的灯火"),
                eq("测试章节"),
                eq(request.getNovelText()),
                eq("scenes: []")
        );
    }

    @Test
    void listsUserHistory() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HistoryService historyService = new HistoryService(jdbcTemplate);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("title")).thenReturn("远方的灯火");
        when(resultSet.getString("source")).thenReturn("测试章节");
        when(resultSet.getString("novel_text")).thenReturn("正文");
        when(resultSet.getString("yaml")).thenReturn("scenes: []");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 6, 7, 12, 0)));
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("writer01")))
                .thenAnswer(invocation -> {
                    RowMapper<HistoryItem> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        List<HistoryItem> items = historyService.listByUsername("writer01");

        assertEquals(1, items.size());
        assertEquals("远方的灯火", items.get(0).getTitle());
        assertEquals("2026-06-07 12:00:00", items.get(0).getCreatedAt());
    }
}
