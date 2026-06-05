package com.example.novel2script.model;

public class ConvertResponse {
    private String yaml;
    private String error;

    public ConvertResponse() {
    }

    public ConvertResponse(String yaml, String error) {
        this.yaml = yaml;
        this.error = error;
    }

    public String getYaml() {
        return yaml;
    }

    public void setYaml(String yaml) {
        this.yaml = yaml;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
