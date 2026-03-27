package com.problemsolving.platform.api.dto.problem;

import com.problemsolving.platform.domain.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ProblemRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Difficulty difficulty;

    @NotEmpty
    private List<String> tags;

    @NotBlank
    private String inputFormat;

    @NotBlank
    private String outputFormat;

    @NotBlank
    private String constraints;

    @NotBlank
    private String sampleIo;

    @NotBlank
    private String hiddenTestCases;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
    }

    public String getSampleIo() {
        return sampleIo;
    }

    public void setSampleIo(String sampleIo) {
        this.sampleIo = sampleIo;
    }

    public String getHiddenTestCases() {
        return hiddenTestCases;
    }

    public void setHiddenTestCases(String hiddenTestCases) {
        this.hiddenTestCases = hiddenTestCases;
    }
}
