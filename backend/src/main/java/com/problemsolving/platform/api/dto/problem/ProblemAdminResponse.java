package com.problemsolving.platform.api.dto.problem;

import com.problemsolving.platform.domain.enums.Difficulty;

import java.util.List;

public class ProblemAdminResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final Difficulty difficulty;
    private final List<String> tags;
    private final String inputFormat;
    private final String outputFormat;
    private final String constraints;
    private final String hiddenTestCases;

    public ProblemAdminResponse(Long id,
                                String title,
                                String description,
                                Difficulty difficulty,
                                List<String> tags,
                                String inputFormat,
                                String outputFormat,
                                String constraints,
                                String hiddenTestCases) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.tags = tags;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.constraints = constraints;
        this.hiddenTestCases = hiddenTestCases;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getConstraints() {
        return constraints;
    }

    public String getHiddenTestCases() {
        return hiddenTestCases;
    }
}