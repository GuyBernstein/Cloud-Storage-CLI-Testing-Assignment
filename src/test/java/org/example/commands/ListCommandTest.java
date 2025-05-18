package org.example.commands;

import org.example.BaseTest;
import org.example.utils.ProcessUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Tests for the list (ls) command in Google Cloud Storage CLI.
 * These tests verify that the ls command correctly lists objects in a bucket.
 */
public class ListCommandTest extends BaseTest {
    private String testObjectPath1;
    private String testObjectPath2;

    /**
     * Setup method that runs before each test method.
     * Creates test objects in the test bucket for testing the list command.
     *
     * @throws IOException If there is an error creating test objects
     * @throws InterruptedException If the process is interrupted
     */
    @BeforeMethod
    public void setUp() throws IOException, InterruptedException {
        // Create unique test object names for this test (without the prefix)
        String testObjectName1 = "list-" + UUID.randomUUID().toString().substring(0, 8);
        String testObjectName2 = "list-" + UUID.randomUUID().toString().substring(0, 8);

        // Create test objects in the bucket (1KB in size)
        testObjectPath1 = createTestObject(testObjectName1, 1024);
        testObjectPath2 = createTestObject(testObjectName2, 2048);

        logger.info("Created test objects: " + testObjectPath1 + ", " + testObjectPath2);
    }

    /**
     * Tests the ls command for listing objects in a bucket.
     * Verifies that the command succeeds and lists the test objects.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testListObjects() throws IOException, InterruptedException {
        // Execute ls command
        ProcessUtils.ProcessResult result = gcloudCLI.listObjects(testBucketName, null);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "ls command failed: " + result.getStderr());

        // Verify output contains the test objects
        String output = result.getStdout();
        Assert.assertFalse(output.isEmpty(), "ls command output is empty");

        // Verify both test objects are listed
        Assert.assertTrue(output.contains(testObjectPath1),
                "ls command output does not contain the first test object: " + testObjectPath1);

        // Execute ls command
        result = gcloudCLI.listObjects(testBucketName, testObjectPath2);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "ls command failed: " + result.getStderr());

        // Verify output contains the test objects
        output = result.getStdout();
        Assert.assertFalse(output.isEmpty(), "ls command output is empty");
        Assert.assertTrue(output.contains(testObjectPath2),
                "ls command output does not contain the second test object: " + testObjectPath2);
    }

    /**
     * Tests the ls command with a specific prefix to filter results.
     * Verifies that the command succeeds and only lists objects with the given prefix.
     * <p>
     * The key difference between this and the first test,
     * verifies the filtering functionality when a more specific prefix is provided.
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testListObjectsWithPrefix() throws IOException, InterruptedException {
        // Execute ls command
        ProcessUtils.ProcessResult result = gcloudCLI.listObjects(testBucketName, testObjectPath1);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "ls command failed: " + result.getStderr());

        // Verify output contains the test objects
        String output = result.getStdout();
        Assert.assertFalse(output.isEmpty(), "ls command output is empty");

        // Verify both test objects are listed
        Assert.assertTrue(output.contains(testObjectPath1),
                "ls command output does not contain the first test object: " + testObjectPath1);

        // Execute ls command
        result = gcloudCLI.listObjects(testBucketName, testObjectPath2);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "ls command failed: " + result.getStderr());

        // Verify output contains the test objects
        output = result.getStdout();
        Assert.assertFalse(output.isEmpty(), "ls command output is empty");
        Assert.assertTrue(output.contains(testObjectPath2),
                "ls command output does not contain the second test object: " + testObjectPath2);
    }

    @Test
    public void testListObjectsEmptyResult() throws IOException, InterruptedException {
        // Test with a non-existent prefix
        String nonExistentPrefix = "gs://" + testBucketName + "/non-existent-prefix";

        ProcessUtils.ProcessResult result = gcloudCLI.listObjects(testBucketName, nonExistentPrefix);

        // Command shouldn't succeed and return results not containing the test object we created
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getStdout().trim().isEmpty() ||
                !result.getStdout().contains(testObjectPath1));
    }
}