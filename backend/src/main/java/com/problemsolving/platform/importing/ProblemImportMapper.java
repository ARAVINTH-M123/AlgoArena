package com.problemsolving.platform.importing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.importing.config.ProblemImportProperties;
import com.problemsolving.platform.importing.hf.HfProblemRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps a Hugging Face dataset row to a {@link Problem} that the existing platform
 * (frontend rendering, judge, admin APIs) can serve unchanged:
 *
 * - title/description   from question_title/question_content
 * - difficulty          from the first difficulty rating (easy/hard, anything else -> MEDIUM)
 * - tags                flattened and comma-joined (the problems.tags format)
 * - constraints         built from the dataset time/memory limits
 * - sampleIo            "Input:\n...\n\nOutput:\n..." (same format the admin UI produces)
 * - hiddenTestCases     JSON array [{"input": "...", "output": "..."}] (same JSON the
 *                       admin UI stores and SubmissionService parses)
 *
 * Rows that can never be served (missing identity/content/test cases) are skipped by
 * returning null.
 */
@Component
public class ProblemImportMapper {

    private static final Logger log = LoggerFactory.getLogger(ProblemImportMapper.class);

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_TAGS_LENGTH = 500;
    private static final int MAX_EXTERNAL_ID_LENGTH = 100;

    private static final String INPUT_FORMAT = "Read the input from standard input (stdin).";
    private static final String OUTPUT_FORMAT = "Write the output to standard output (stdout).";

    /** Serialized shape of one hidden test case, identical to the admin-created problems. */
    public record TestCaseJson(String input, String output) {
    }

    private final ProblemImportProperties properties;
    private final ObjectMapper objectMapper;

    public ProblemImportMapper(ProblemImportProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** @return the mapped problem, or null when the row must be skipped (not importable). */
    public Problem mapToProblem(HfProblemRow row) {
        if (row == null || isBlank(row.pid()) || isBlank(row.questionTitle()) || isBlank(row.questionContent())) {
            log.debug("Skipping dataset row without pid/title/content: {}", row == null ? "null" : row.pid());
            return null;
        }

        List<HfProblemRow.PublicTestCase> usableTestCases = usableTestCases(row);
        if (usableTestCases.isEmpty()) {
            // The platform judge needs at least one test case with an expected output.
            log.debug("Skipping dataset row {} without usable public test cases", row.pid());
            return null;
        }

        Problem problem = new Problem();
        problem.setExternalId(externalId(row.pid()));
        problem.setTitle(truncate(row.questionTitle().trim(), MAX_TITLE_LENGTH));
        problem.setDescription(row.questionContent());
        problem.setDifficulty(resolveDifficulty(row));
        problem.setTags(resolveTags(row));
        problem.setInputFormat(INPUT_FORMAT);
        problem.setOutputFormat(OUTPUT_FORMAT);
        problem.setConstraints(resolveConstraints(row));
        problem.setSampleIo(toSampleIo(usableTestCases.get(0)));
        problem.setHiddenTestCases(toHiddenTestCasesJson(usableTestCases));
        return problem;
    }

    private String externalId(String pid) {
        return truncate("hf:" + properties.getDataset() + ":" + pid.trim(), MAX_EXTERNAL_ID_LENGTH);
    }

    private Difficulty resolveDifficulty(HfProblemRow row) {
        if (row.difficultyRatings() != null) {
            for (HfProblemRow.DifficultyRating rating : row.difficultyRatings()) {
                if (rating == null || isBlank(rating.level())) {
                    continue;
                }
                String level = rating.level().trim();
                if (level.equalsIgnoreCase("easy")) {
                    return Difficulty.EASY;
                }
                if (level.equalsIgnoreCase("hard")) {
                    return Difficulty.HARD;
                }
                if (level.equalsIgnoreCase("medium") || level.equalsIgnoreCase("mid")) {
                    return Difficulty.MEDIUM;
                }
            }
        }
        // Dataset ratings are advisory; anything unmapped stays judgeable as MEDIUM.
        return Difficulty.MEDIUM;
    }

    private String resolveTags(HfProblemRow row) {
        Set<String> tags = new LinkedHashSet<>();
        if (row.tags() != null) {
            for (HfProblemRow.TagGroup group : row.tags()) {
                if (group == null || group.content() == null) {
                    continue;
                }
                for (String tag : group.content()) {
                    if (!isBlank(tag)) {
                        tags.add(tag.trim());
                    }
                }
            }
        }
        String joined = String.join(", ", tags);
        return joined.isEmpty() ? "imported" : truncate(joined, MAX_TAGS_LENGTH);
    }

    private String resolveConstraints(HfProblemRow row) {
        List<String> lines = new ArrayList<>();
        if (!isBlank(row.timeLimit())) {
            lines.add("Time limit: " + row.timeLimit().trim());
        }
        if (!isBlank(row.memoryLimit())) {
            lines.add("Memory limit: " + row.memoryLimit().trim());
        }
        if (lines.isEmpty()) {
            return "Time and memory limits are not specified for this problem.";
        }
        return String.join("\n", lines);
    }

    private List<HfProblemRow.PublicTestCase> usableTestCases(HfProblemRow row) {
        List<HfProblemRow.PublicTestCase> usable = new ArrayList<>();
        if (row.publicTestCases() == null) {
            return usable;
        }
        for (HfProblemRow.PublicTestCase testCase : row.publicTestCases()) {
            if (testCase != null && testCase.input() != null && testCase.output() != null
                    && !testCase.output().isBlank()) {
                usable.add(testCase);
            }
        }
        return usable;
    }

    private String toSampleIo(HfProblemRow.PublicTestCase testCase) {
        return "Input:\n" + testCase.input() + "\n\nOutput:\n" + testCase.output();
    }

    private String toHiddenTestCasesJson(List<HfProblemRow.PublicTestCase> testCases) {
        List<TestCaseJson> payload = testCases.stream()
                .map(testCase -> new TestCaseJson(testCase.input(), testCase.output()))
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            // Should never happen for this simple payload; treat the row as not importable.
            throw new IllegalStateException("Could not serialize hidden test cases", ex);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
