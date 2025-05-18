package org.example.commands;

import org.example.BaseTest;
import org.example.utils.ProcessUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Tests for the copy (cp) command in Google Cloud Storage CLI.
 * These tests verify that the cp command correctly copies objects within a bucket.
 */
public class CopyCommandTest extends BaseTest {
    private String testObjectPath;

    /**
     * Setup method that runs before each test method.
     * Creates a test object in the test bucket for testing the copy command.
     *
     * @throws IOException If there is an error creating test objects
     * @throws InterruptedException If the process is interrupted
     */
    @BeforeMethod
    public void setUp() throws IOException, InterruptedException {
        // Create a unique test object name for this test
        String testObjectName = "copy-source-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a test object in the bucket (2KB in size)
        testObjectPath = createTestObject(testObjectName, 2048);

        logger.info("Created test object: " + testObjectPath);
    }

    /**
     * Tests the basic cp command for copying an object within the same bucket.
     * Verifies that the command succeeds and the destination object exists.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testCopyObjectWithinBucket() throws IOException, InterruptedException {
        // Define destination object name
        String destObjectName = testObjectPrefix + "copy-dest-" + UUID.randomUUID().toString().substring(0, 8);
        String destObjectPath = "gs://" + testBucketName + "/" + destObjectName;

        // Execute cp command to copy the object
        ProcessUtils.ProcessResult result = gcloudCLI.copyObjects(testObjectPath, destObjectPath);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "cp command failed: " + result.getStderr());

        // Verify the destination object exists by listing it
        ProcessUtils.ProcessResult listResult = gcloudCLI.listObjects(testBucketName, destObjectPath);

        // Verify listing succeeded
        Assert.assertTrue(listResult.isSuccess(), "ls command to verify copy failed: " + listResult.getStderr());

        // Verify the destination object is in the listing
        String output = listResult.getStdout();
        Assert.assertFalse(output.isEmpty(), "ls command output is empty");
        Assert.assertTrue(output.contains(destObjectName),
                "ls command output does not contain the destination object: " + destObjectName);
    }

    /**
     * Tests the cp command with a non-existent source object.
     * Verifies that the command fails with an appropriate error message.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testCopyNonExistentObject() throws IOException, InterruptedException {
        // Define a non-existent source object path
        String nonExistentPath = "gs://" + testBucketName + "/" + testObjectPrefix + "non-existent-"
                + UUID.randomUUID();

        // Define destination object path
        String destObjectName = testObjectPrefix + "copy-dest-" + UUID.randomUUID().toString().substring(0, 8);
        String destObjectPath = "gs://" + testBucketName + "/" + destObjectName;

        // Execute cp command with non-existent source
        ProcessUtils.ProcessResult result = gcloudCLI.copyObjects(nonExistentPath, destObjectPath);

        // Verify command failed
        Assert.assertFalse(result.isSuccess(), "cp command with non-existent source should fail");

        // Verify error message contains appropriate text
        String errorOutput = result.getStderr();
        Assert.assertTrue(errorOutput.contains("The following URLs matched no objects or files"),
                "Error message should indicate that the source object doesn't exist: " + errorOutput);
    }

}