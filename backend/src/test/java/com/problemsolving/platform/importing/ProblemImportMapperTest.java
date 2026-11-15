package com.problemsolving.platform.importing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.domain.enums.Difficulty;
import com.problemsolving.platform.importing.config.ProblemImportProperties;
import com.problemsolving.platform.importing.hf.HfProblemRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemImportMapperTest {

    private final ProblemImportProperties properties = new ProblemImportProperties();
    private final ProblemImportMapper mapper = new ProblemImportMapper(properties, new ObjectMapper());

    private static HfProblemRow row(String pid, String level) {
        return new HfProblemRow(
                pid,
                "DEGwer's Doctoral Dissertation",
                "## Problem Description\n\nSolve it fast.",
                "atcoder",
                level == null ? null : List.of(new HfProblemRow.DifficultyRating(level, 2, "luogu")),
                List.of(
                        new HfProblemRow.PublicTestCase("5 4 3\r\n1 2 4 5", "2", "stdin"),
                        new HfProblemRow.PublicTestCase("4 8 2\r\n1 1 1 1", "6", "stdin"),
                        new HfProblemRow.PublicTestCase("5 2 3\r\n4 1", null, "stdin")),
                "2000 s",
                "1024.0 MB",
                List.of(
                        new HfProblemRow.TagGroup(List.of("Greedy"), "luogu"),
                        new HfProblemRow.TagGroup(List.of("Greedy", "Math"), "codeforces")),
                "https://atcoder.jp/contests/x/tasks/y");
    }

    @Test
    void mapsRowIntoPlatformCompatibleProblem() throws Exception {
        Problem problem = mapper.mapToProblem(row("atcoder_1202contest_a", "easy"));

        assertNotNull(problem);
        assertEquals("hf:sigcp/hardtests_problems:atcoder_1202contest_a", problem.getExternalId());
        assertEquals("DEGwer's Doctoral Dissertation", problem.getTitle());
        assertEquals("## Problem Description\n\nSolve it fast.", problem.getDescription());
        assertEquals(Difficulty.EASY, problem.getDifficulty());
        assertEquals("Greedy, Math", problem.getTags());
        assertEquals("Read the input from standard input (stdin).", problem.getInputFormat());
        assertEquals("Write the output to standard output (stdout).", problem.getOutputFormat());
        assertEquals("Time limit: 2000 s\nMemory limit: 1024.0 MB", problem.getConstraints());
        assertEquals("Input:\n5 4 3\r\n1 2 4 5\n\nOutput:\n2", problem.getSampleIo());

        // hiddenTestCases must be the exact JSON shape the judge parses: [{"input","output"}],
        // with test cases lacking an expected output dropped.
        List<Map<String, String>> testCases =
                new ObjectMapper().readValue(problem.getHiddenTestCases(), new TypeReference<>() {
                });
        assertEquals(2, testCases.size());
        assertEquals("5 4 3\r\n1 2 4 5", testCases.get(0).get("input"));
        assertEquals("2", testCases.get(0).get("output"));
    }

    @Test
    void mapsDifficultyCaseInsensitivelyAndDefaultsToMedium() {
        assertEquals(Difficulty.HARD, mapper.mapToProblem(row("p1", "HARD")).getDifficulty());
        assertEquals(Difficulty.MEDIUM, mapper.mapToProblem(row("p2", "Medium")).getDifficulty());
        assertEquals(Difficulty.MEDIUM, mapper.mapToProblem(row("p3", "expert")).getDifficulty());
        assertEquals(Difficulty.MEDIUM, mapper.mapToProblem(row("p4", null)).getDifficulty());
    }

    @Test
    void skipsRowsThatCanNeverBeServed() {
        assertNull(mapper.mapToProblem(null));
        assertNull(mapper.mapToProblem(new HfProblemRow(null, "t", "c", null, null,
                List.of(new HfProblemRow.PublicTestCase("in", "out", "stdin")), null, null, null, null)));
        assertNull(mapper.mapToProblem(new HfProblemRow("pid", " ", "content", null, null,
                List.of(new HfProblemRow.PublicTestCase("in", "out", "stdin")), null, null, null, null)));
        // no usable test case (only one, without expected output)
        assertNull(mapper.mapToProblem(new HfProblemRow("pid", "title", "content", null, null,
                List.of(new HfProblemRow.PublicTestCase("in", null, "stdin")), null, null, null, null)));
        // no test cases at all
        assertNull(mapper.mapToProblem(new HfProblemRow("pid", "title", "content", null, null,
                null, null, null, null, null)));
    }

    @Test
    void truncatesTitleAndTagsToColumnLimits() {
        HfProblemRow longRow = new HfProblemRow(
                "pid",
                "T".repeat(500),
                "description",
                null,
                null,
                List.of(new HfProblemRow.PublicTestCase("in", "out", "stdin")),
                null,
                null,
                List.of(new HfProblemRow.TagGroup(List.of("G".repeat(400), "M".repeat(400)), "s")),
                null);

        Problem problem = mapper.mapToProblem(longRow);
        assertNotNull(problem);
        assertEquals(200, problem.getTitle().length());
        assertTrue(problem.getTags().length() <= 500);
    }

    @Test
    void usesFallbackTagAndConstraintsWhenMissing() {
        Problem problem = mapper.mapToProblem(new HfProblemRow("pid", "title", "content", null, null,
                List.of(new HfProblemRow.PublicTestCase("in", "out", "stdin")),
                null, null, null, null));

        assertNotNull(problem);
        assertEquals("imported", problem.getTags());
        assertEquals("Time and memory limits are not specified for this problem.", problem.getConstraints());
    }
}
