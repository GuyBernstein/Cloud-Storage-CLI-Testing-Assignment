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
    private String testObjectName1;
    private String testObjectName2;

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
        testObjectName1 = "list-" + UUID.randomUUID().toString().substring(0, 8);
        testObjectName2 = "list-" + UUID.randomUUID().toString().substring(0, 8);

        // Create test objects in the bucket (1KB in size)
        // The createTestObject method will prepend the testObjectPrefix
        String testObjectPath1 = createTestObject(testObjectName1, 1024);
        String testObjectPath2 = createTestObject(testObjectName2, 2048);

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
        // Execute ls command with our test prefix
        ProcessUtils.ProcessResult result = gcloudCLI.listObjects(testBucketName, testObjectPrefix);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "ls command failed: " + result.getStderr());

        // Verify output contains the test objects
        String output = result.getStdout();
        Assert.assertFalse(output.isEmpty(), "ls command output is empty");

        // The full object names include the prefix + the test object names
        String fullObjectName1 = testObjectPrefix + testObjectName1;
        String fullObjectName2 = testObjectPrefix + testObjectName2;

        // Verify both test objects are listed
        Assert.assertTrue(output.contains(fullObjectName1),
                "ls command output does not contain the first test object: " + fullObjectName1);
        Assert.assertTrue(output.contains(fullObjectName2),
                "ls command output does not contain the second test object: " + fullObjectName2);
    }

    /**
     * Tests the ls command with a specific prefix to filter results.
     * Verifies that the command succeeds and only lists objects with the given prefix.
     *
     * The key difference between this and the first test,
     * verifies the filtering functionality when a more specific prefix is provided.
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testListObjectsWithPrefix() throws IOException, InterruptedException {
        // Create a third object with a different prefix
        String differentPrefix = "different-" + UUID.randomUUID().toString().substring(0, 8);
        createTestObject(differentPrefix, 1024);

        // Execute ls command with the specific prefix
        ProcessUtils.ProcessResult result = gcloudCLI.listObjects(
                testBucketName,
                testObjectPrefix + differentPrefix);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "ls command with prefix failed: " + result.getStderr());

        // Verify output contains only the third test object
        String output = result.getStdout();
        Assert.assertFalse(output.isEmpty(), "ls command output is empty");

        // Verify the correct object is listed
        Assert.assertTrue(output.contains(differentPrefix),
                "ls command output does not contain the third test object: " + differentPrefix);

        // Verify the other objects are not listed
        Assert.assertFalse(output.contains(testObjectName1),
                "ls command output contains the first test object but shouldn't: " + testObjectName1);
        Assert.assertFalse(output.contains(testObjectName2),
                "ls command output contains the second test object but shouldn't: " + testObjectName2);
    }
}