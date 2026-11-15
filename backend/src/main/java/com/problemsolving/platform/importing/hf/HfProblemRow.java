package com.problemsolving.platform.importing.hf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One row of the Hugging Face dataset (sigcp/hardtests_problems), as returned by the
 * datasets-server /rows API. Only the fields the importer needs are mapped; everything
 * else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HfProblemRow(
        String pid,
        @JsonProperty("question_title") String questionTitle,
        @JsonProperty("question_content") String questionContent,
        String platform,
        @JsonProperty("difficulty_ratings") List<DifficultyRating> difficultyRatings,
        @JsonProperty("public_test_cases") List<PublicTestCase> publicTestCases,
        @JsonProperty("time_limit") String timeLimit,
        @JsonProperty("memory_limit") String memoryLimit,
        List<TagGroup> tags,
        String url) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DifficultyRating(String level, long score, String source) {
    }

    /** stdin/stdout style test case; only input+output are used by the platform judge. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PublicTestCase(String input, String output, String testtype) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TagGroup(List<String> content, String source) {
    }
}
