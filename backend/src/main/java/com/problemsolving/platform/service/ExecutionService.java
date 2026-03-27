package com.problemsolving.platform.service;

import com.problemsolving.platform.api.dto.execution.ExecutionRequest;
import com.problemsolving.platform.api.dto.execution.ExecutionResponse;
import com.problemsolving.platform.domain.entity.Problem;
import com.problemsolving.platform.domain.enums.SubmissionLanguage;
import com.problemsolving.platform.repository.ProblemRepository;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ExecutionService {

    private final ProblemRepository problemRepository;

    public ExecutionService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    public ExecutionResponse executeCustomTest(ExecutionRequest request) {
        if (request.getLanguage() != SubmissionLanguage.JAVA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JAVA execution is currently supported");
        }

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));

        return executeJavaCode(request.getCode(), request.getInput());
    }

    private ExecutionResponse executeJavaCode(String code, String input) {
        Path runDir = buildRunDirectory();
        Path javaFile = runDir.resolve("Main.java");

        try {
            Files.createDirectories(runDir);
            Files.writeString(javaFile, code, StandardCharsets.UTF_8);

            // Compile
            long startTime = System.currentTimeMillis();
            Process compileProcess = new ProcessBuilder("javac", "Main.java")
                    .directory(runDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            
            String compileOutput = readFully(compileProcess.getInputStream());
            boolean compileDone = compileProcess.waitFor(10, TimeUnit.SECONDS);

            if (!compileDone) {
                compileProcess.destroyForcibly();
                return new ExecutionResponse("", "Compilation timed out", System.currentTimeMillis() - startTime);
            }
            
            if (compileProcess.exitValue() != 0) {
                String message = compileOutput.isBlank() ? "Compilation failed" : compileOutput;
                return new ExecutionResponse("", message, System.currentTimeMillis() - startTime);
            }

            // Execute
            Process runProcess = new ProcessBuilder("java", "Main")
                    .directory(runDir.toFile())
                    .redirectErrorStream(true)
                    .start();

            runProcess.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
            runProcess.getOutputStream().flush();
            runProcess.getOutputStream().close();

            boolean runDone = runProcess.waitFor(5, TimeUnit.SECONDS);
            if (!runDone) {
                runProcess.destroyForcibly();
                return new ExecutionResponse("", "Execution timed out", System.currentTimeMillis() - startTime);
            }

            String programOutput = readFully(runProcess.getInputStream());
            long executionTime = System.currentTimeMillis() - startTime;

            if (runProcess.exitValue() != 0) {
                return new ExecutionResponse("", "Runtime error:\n" + programOutput, executionTime);
            }

            return new ExecutionResponse(programOutput, "", executionTime);

        } catch (IOException e) {
            return new ExecutionResponse("", "Execution failed: " + e.getMessage(), 0L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResponse("", "Execution interrupted", 0L);
        } finally {
            // Cleanup
            try {
                Files.walk(runDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }

    private String readFully(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private Path buildRunDirectory() {
        Path base = Paths.get(System.getProperty("java.io.tmpdir"), "algoarena-executions");
        String folder = "exec-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
        return base.resolve(folder);
    }
}
