package com.problemsolving.platform.api.dto.submission;

import com.problemsolving.platform.api.dto.nation.NationAwardSummaryDto;

import java.time.LocalDateTime;

public class SubmissionResponse {

    private final Long id;
    private final String problemTitle;
    private final String language;
    private final String verdict;
    private final LocalDateTime submittedAt;
    private final String expectedOutput;
    private final String actualOutput;
    private final String failedTestcaseInput;
    private final String executionMessage;
    private final NationAwardSummaryDto nationAward;

    public SubmissionResponse(Long id,
                              String problemTitle,
                              String language,
                              String verdict,
                              LocalDateTime submittedAt,
                              String expectedOutput,
                              String actualOutput,
                              String failedTestcaseInput,
                              String executionMessage,
                              NationAwardSummaryDto nationAward) {
        this.id = id;
        this.problemTitle = problemTitle;
        this.language = language;
        this.verdict = verdict;
        this.submittedAt = submittedAt;
        this.expectedOutput = expectedOutput;
        this.actualOutput = actualOutput;
        this.failedTestcaseInput = failedTestcaseInput;
        this.executionMessage = executionMessage;
        this.nationAward = nationAward;
    }

    public Long getId() {
        return id;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public String getLanguage() {
        return language;
    }

    public String getVerdict() {
        return verdict;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public String getFailedTestcaseInput() {
        return failedTestcaseInput;
    }

    public String getExecutionMessage() {
        return executionMessage;
    }

    public NationAwardSummaryDto getNationAward() {
        return nationAward;
    }
}
