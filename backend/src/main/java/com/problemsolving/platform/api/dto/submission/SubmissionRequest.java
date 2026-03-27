package com.problemsolving.platform.api.dto.submission;

import com.problemsolving.platform.domain.enums.SubmissionLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmissionRequest {

    @NotNull
    private Long problemId;

    @NotNull
    private SubmissionLanguage language;

    @NotBlank
    private String code;

    private String testCase;

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public SubmissionLanguage getLanguage() {
        return language;
    }

    public void setLanguage(SubmissionLanguage language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }
}
