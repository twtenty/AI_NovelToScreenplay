package com.example.novel2script.model;

public class HistoryItem {
    private long id;
    private String title;
    private String source;
    private String novelText;
    private String yaml;
    private String createdAt;

    public HistoryItem() {
    }

    public HistoryItem(long id, String title, String source, String novelText, String yaml, String createdAt) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.novelText = novelText;
        this.yaml = yaml;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNovelText() {
        return novelText;
    }

    public void setNovelText(String novelText) {
        this.novelText = novelText;
    }

    public String getYaml() {
        return yaml;
    }

    public void setYaml(String yaml) {
        this.yaml = yaml;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
