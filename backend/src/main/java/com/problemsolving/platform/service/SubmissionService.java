package com.problemsolving.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemsolving.platform.api.dto.nation.NationResultDto;
import com.problemsolving.platform.api.dto.submission.SubmissionRequest;
import com.problemsolving.platform.api.dto.submission.SubmissionResponse;
import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.domain.entity.Submission;
import com.problemsolving.platform.domain.entity.User;
import com.problemsolving.platform.domain.enums.SubmissionLanguage;
import com.problemsolving.platform.domain.enums.Verdict;
import com.problemsolving.platform.repository.ProblemRepository;
import com.problemsolving.platform.repository.SubmissionRepository;
import com.problemsolving.platform.repository.UserRepository;
import com.problemsolving.platform.service.nation.NationService;
import com.problemsolving.platform.service.presence.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final NationService nationService;
    private final PresenceService presenceService;

    public SubmissionService(SubmissionRepository submissionRepository,
                             ProblemRepository problemRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper,
                             NationService nationService,
                             PresenceService presenceService) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.nationService = nationService;
        this.presenceService = presenceService;
    }

    public SubmissionResponse submit(String username, SubmissionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));

        if (request.getLanguage() != SubmissionLanguage.JAVA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JAVA submissions are currently supported");
        }

        JudgeResult judgeResult = judgeJavaSubmission(problem, request.getCode());

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setLanguage(request.getLanguage());
        submission.setCode(request.getCode());
        submission.setVerdict(judgeResult.verdict());

        Submission saved = submissionRepository.save(submission);

        // Nation system hook: reward accepted solves. A failure here must never break the submission.
        NationResultDto nationResult = null;
        if (saved.getVerdict() == Verdict.ACCEPTED) {
            try {
                nationResult = nationService.awardForAcceptedSolve(
                        user.getId(), problem.getId(), problem.getDifficulty(), Instant.now());
            } catch (Exception ex) {
                log.warn("Nation award failed for user {} problem {}: {}", user.getId(), problem.getId(), ex.toString());
            }
            if (nationResult != null && "FIRST".equals(nationResult.mode())) {
                try {
                    presenceService.markSolved(user.getId(), Instant.now());   // the presence figure glows for a minute
                } catch (Exception ex) {
                    log.warn("Presence update failed for user {}: {}", user.getId(), ex.toString());
                }
            }
        }

        return toResponse(saved, judgeResult, nationResult);
    }

    public List<SubmissionResponse> mySubmissions(String username) {
        return submissionRepository.findByUserUsernameOrderBySubmittedAtDesc(username)
                .stream()
                .map(submission -> toResponse(submission, null))
                .toList();
    }

    private JudgeResult judgeJavaSubmission(Problem problem, String code) {
        List<SampleCase> testCases = resolveTestCases(problem);

        Path runDir = buildRunDirectory(problem.getId());
        Path javaFile = runDir.resolve("Main.java");

        try {
            Files.createDirectories(runDir);
            Files.writeString(javaFile, code, StandardCharsets.UTF_8);

            Process compileProcess = new ProcessBuilder("javac", "Main.java")
                    .directory(runDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            String compileOutput = readFully(compileProcess.getInputStream());
            boolean compileDone = compileProcess.waitFor(10, TimeUnit.SECONDS);

            if (!compileDone) {
                compileProcess.destroyForcibly();
                return new JudgeResult(Verdict.COMPILATION_ERROR, null, null, null, "Compilation timed out");
            }
            if (compileProcess.exitValue() != 0) {
                String message = compileOutput.isBlank() ? "Compilation failed" : compileOutput;
                return new JudgeResult(Verdict.COMPILATION_ERROR, null, null, null, message);
            }

            for (int index = 0; index < testCases.size(); index++) {
                SampleCase sampleCase = testCases.get(index);

                Process runProcess = new ProcessBuilder("java", "Main")
                        .directory(runDir.toFile())
                        .redirectErrorStream(true)
                        .start();

                String caseInput = sampleCase.input() == null ? "" : sampleCase.input();
                runProcess.getOutputStream().write(caseInput.getBytes(StandardCharsets.UTF_8));
                runProcess.getOutputStream().flush();
                runProcess.getOutputStream().close();

                boolean runDone = runProcess.waitFor(5, TimeUnit.SECONDS);
                if (!runDone) {
                    runProcess.destroyForcibly();
                    return new JudgeResult(
                            Verdict.TIME_LIMIT_EXCEEDED,
                            sampleCase.expectedOutput(),
                            "",
                            caseInput,
                            "Execution timed out on testcase #" + (index + 1)
                    );
                }

                String programOutput = readFully(runProcess.getInputStream());
                if (runProcess.exitValue() != 0) {
                    return new JudgeResult(
                            Verdict.RUNTIME_ERROR,
                            sampleCase.expectedOutput(),
                            programOutput,
                            caseInput,
                            "Runtime error on testcase #" + (index + 1)
                    );
                }

                String normalizedActual = normalizeOutput(programOutput);
                String normalizedExpected = normalizeOutput(sampleCase.expectedOutput());

                if (!normalizedActual.equals(normalizedExpected)) {
                    return new JudgeResult(
                            Verdict.WRONG_ANSWER,
                            sampleCase.expectedOutput(),
                            programOutput,
                            caseInput,
                            "Wrong answer on testcase #" + (index + 1)
                    );
                }
            }

            return new JudgeResult(
                    Verdict.ACCEPTED,
                    null,
                    null,
                    null,
                    "Passed all " + testCases.size() + " testcase(s)"
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Execution failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Execution interrupted");
        }
    }

    private SubmissionResponse toResponse(Submission s, JudgeResult judgeResult) {
        return toResponse(s, judgeResult, null);
    }

    private SubmissionResponse toResponse(Submission s, JudgeResult judgeResult, NationResultDto nationResult) {
        return new SubmissionResponse(
                s.getId(),
                s.getProblem().getTitle(),
                s.getLanguage().name(),
                s.getVerdict().name(),
                s.getSubmittedAt(),
                judgeResult != null ? judgeResult.expectedOutput() : null,
                judgeResult != null ? judgeResult.actualOutput() : null,
                judgeResult != null ? judgeResult.failedTestcaseInput() : null,
                judgeResult != null ? judgeResult.executionMessage() : null,
                nationResult);
    }

    private List<SampleCase> resolveTestCases(Problem problem) {
        List<SampleCase> fromHidden = parseHiddenTestCases(problem.getHiddenTestCases());
        if (!fromHidden.isEmpty()) {
            return fromHidden;
        }

        SampleCase fallback = extractSampleCase(problem.getSampleIo());
        if (fallback != null) {
            return List.of(fallback);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid testcases configured for this problem");
    }

    private List<SampleCase> parseHiddenTestCases(String raw) {
        try {
            List<TestCasePayload> payload = objectMapper.readValue(raw, new TypeReference<List<TestCasePayload>>() {
            });

            if (payload == null) {
                return List.of();
            }

            return payload.stream()
                    .filter(item -> item != null && item.output() != null && !item.output().trim().isEmpty())
                    .map(item -> new SampleCase(item.input() == null ? "" : item.input(), item.output()))
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private SampleCase extractSampleCase(String sampleIo) {
        String raw = sampleIo == null ? "" : sampleIo;

        // Accept common testcase styles like:
        // Input: ... Output: ...
        // Sample Input: ... Sample Output: ...
        // Input - ... Output - ...
        Pattern pattern = Pattern.compile(
                "(?is)(?:sample\\s*)?input\\s*[:\\-]?\\s*(.*?)\\s*(?:sample\\s*)?output\\s*[:\\-]?\\s*(.*)"
        );
        Matcher matcher = pattern.matcher(raw);
        if (!matcher.find()) {
            return null;
        }

        String input = matcher.group(1).trim();
        String output = matcher.group(2).trim();
        if (output.isBlank()) {
            return null;
        }
        return new SampleCase(input, output);
    }

    private String normalizeOutput(String raw) {
        String unix = (raw == null ? "" : raw).replace("\r", "").trim();
        String[] lines = unix.split("\n");
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            normalized.append(lines[i].trim());
            if (i < lines.length - 1) {
                normalized.append("\n");
            }
        }
        return normalized.toString().trim();
    }

    private String readFully(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private Path buildRunDirectory(Long problemId) {
        Path base = Paths.get(System.getProperty("java.io.tmpdir"), "algoarena-submissions");
        String folder = "problem-" + problemId + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
        return base.resolve(folder);
    }

    private record SampleCase(String input, String expectedOutput) {
    }

    private record TestCasePayload(String input, String output) {
    }

    private record JudgeResult(Verdict verdict,
                               String expectedOutput,
                               String actualOutput,
                               String failedTestcaseInput,
                               String executionMessage) {
    }
}
