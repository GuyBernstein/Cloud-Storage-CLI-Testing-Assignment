package org.example.cli;

import org.example.utils.ProcessUtils;

import java.io.IOException;
import java.util.*;

public class GCloudStorageCLI {
    private final ProcessUtils processUtils;
    private final String cliPath;
    private final Map<String, String> environmentVariables;


    /**
     * Constructor for GCloudStorageCLI.
     *
     * @param cliPath              Path to the gcloud executable
     * @param environmentVariables Environment variables to set for CLI executions
     */

    public GCloudStorageCLI(String cliPath, Map<String, String> environmentVariables) {
        this.cliPath = cliPath;
        this.environmentVariables = environmentVariables;
        this.processUtils = new ProcessUtils();
    }

    /**
     * Executes a list command to list objects in a Google Cloud Storage bucket.
     *
     * @param bucketPath Name of the bucket
     * @param objectName     Optional prefix to filter the results
     * @return The process result containing stdout, stderr, and exit code
     * @throws IOException          If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */

    public ProcessUtils.ProcessResult listObjects(String bucketPath, String objectName)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("storage");
        command.add("ls");
        command.add(Objects.requireNonNullElseGet(objectName, () -> "gs://" + bucketPath + "/*"));
        return processUtils.executeCommand(command, environmentVariables, 30);
    }

    /**
     * Executes a sign-url command to generate a signed URL for a Google Cloud Storage object.
     *
     * @param bucketPath Path to the object
     * @param duration   Duration for which the signed URL is valid (e.g., "1h" for 1 hour)
     * @param keyFile    Path to the private key file for signing
     * @return The process result containing stdout, stderr, and exit code
     * @throws IOException          If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */

    public ProcessUtils.ProcessResult signUrl(String bucketPath,
                                              String duration, String keyFile)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("storage");
        command.add("sign-url");

        command.add(bucketPath);
        command.add("--duration=" + duration);
        command.add("--private-key-file=" + keyFile);

        return processUtils.executeCommand(command, environmentVariables, 60);
    }


    /**
     * Executes a copy command to copy objects in Google Cloud Storage.
     *
     * @param sourcePath      Source path (e.g., "gs://bucket/object")
     * @param destinationPath Destination path (e.g., "gs://bucket/object")
     * @return The process result containing stdout, stderr, and exit code
     * @throws IOException          If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */

    public ProcessUtils.ProcessResult copyObjects(String sourcePath, String destinationPath)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("storage");
        command.add("cp");
        command.add(sourcePath);
        command.add(destinationPath);

        return processUtils.executeCommand(command, environmentVariables, 60);
    }


    /**
     * Executes a delete command to delete objects in Google Cloud Storage.
     *
     * @param path      Path to the object to delete (e.g., "gs://bucket/object")
     * @return The process result containing stdout, stderr, and exit code
     * @throws IOException          If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */

    public ProcessUtils.ProcessResult deleteObjects(String path)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("storage");
        command.add("rm");

        command.add(path);

        return processUtils.executeCommand(command, environmentVariables, 30);
    }

    /**
     * Gets the version of the gcloud CLI.
     *
     * @return The process result containing stdout with version info, stderr, and exit code
     * @throws IOException          If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */

    public ProcessUtils.ProcessResult getVersion() throws IOException, InterruptedException {
        List<String> command = Arrays.asList(cliPath, "version");
        return processUtils.executeCommand(command, environmentVariables, 10);
    }

}
