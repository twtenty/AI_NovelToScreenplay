package com.example.novel2script.model;

public class ConvertResponse {
    private String yaml;
    private String error;
    private String warning;

    public ConvertResponse() {
    }

    public ConvertResponse(String yaml, String error) {
        this(yaml, error, null);
    }

    public ConvertResponse(String yaml, String error, String warning) {
        this.yaml = yaml;
        this.error = error;
        this.warning = warning;
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

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
