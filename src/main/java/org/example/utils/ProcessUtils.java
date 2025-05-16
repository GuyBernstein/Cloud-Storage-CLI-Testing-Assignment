package org.example.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Utility class for executing processes and handling their output.
 */
public class ProcessUtils {

    /**
     * Class to hold the result of a process execution.
     */
    public static class ProcessResult {
        private final String stdout;
        private final String stderr;
        private final int exitCode;

        public ProcessResult(String stdout, String stderr, int exitCode) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        @Override
        public String toString() {
            return "ProcessResult{" +
                    "exitCode=" + exitCode +
                    ", stdout='" + stdout + '\'' +
                    ", stderr='" + stderr + '\'' +
                    '}';
        }
    }

    /**
     * Executes a command as a separate process.
     *
     * @param command              List of command parts to execute
     * @param environmentVariables Environment variables to set for the process
     * @param timeoutInSeconds     Maximum time to wait for the process to complete
     * @return ProcessResult containing stdout, stderr, and exit code
     * @throws IOException          If there is an error starting or communicating with the process
     * @throws InterruptedException If the process is interrupted
     */

    public ProcessResult executeCommand(List<String> command, Map<String, String> environmentVariables,
                                        long timeoutInSeconds) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // Set environment variables if provided
        if (environmentVariables != null && !environmentVariables.isEmpty()) {
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(environmentVariables);
        }

        // Start the process
        Process process = processBuilder.start();

        // Capture stdout and stderr
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Wait for the process to complete with timeout
        boolean completed = process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Process execution timed out after " + timeoutInSeconds + " seconds");
        }

        // Collect stdout and stderr
        String stdout = stdoutReader.lines().collect(Collectors.joining(System.lineSeparator()));
        String stderr = stderrReader.lines().collect(Collectors.joining(System.lineSeparator()));

        // Get the exit code
        int exitCode = process.exitValue();

        return new ProcessResult(stdout, stderr, exitCode);
    }
}
