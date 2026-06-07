package com.example.novel2script.service;

import com.example.novel2script.model.ConvertRequest;
import com.example.novel2script.model.HistoryItem;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HistoryService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;

    public HistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initializeSchema() {
        jdbcTemplate.execute("""
                create table if not exists conversion_history (
                    id bigint primary key auto_increment,
                    username varchar(20) not null,
                    title varchar(120),
                    source varchar(200),
                    novel_text mediumtext not null,
                    yaml mediumtext not null,
                    created_at timestamp default current_timestamp,
                    index idx_history_username_created_at (username, created_at)
                )
                """);
    }

    public void save(String username, ConvertRequest request, String yaml) {
        jdbcTemplate.update(
                "insert into conversion_history (username, title, source, novel_text, yaml) values (?, ?, ?, ?, ?)",
                username,
                normalize(request.getTitle()),
                normalize(request.getSource()),
                request.getNovelText(),
                yaml
        );
    }

    public List<HistoryItem> listByUsername(String username) {
        return jdbcTemplate.query(
                """
                        select id, title, source, novel_text, yaml, created_at
                        from conversion_history
                        where username = ?
                        order by created_at desc, id desc
                        limit 20
                        """,
                (rs, rowNum) -> new HistoryItem(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("source"),
                        rs.getString("novel_text"),
                        rs.getString("yaml"),
                        rs.getTimestamp("created_at").toLocalDateTime().format(FORMATTER)
                ),
                username
        );
    }

    public int countToday(String username) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from conversion_history
                        where username = ?
                          and date(created_at) = current_date()
                        """,
                Integer.class,
                username
        );
        return count == null ? 0 : count;
    }

    public boolean delete(String username, long id) {
        return jdbcTemplate.update(
                "delete from conversion_history where username = ? and id = ?",
                username,
                id
        ) > 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
