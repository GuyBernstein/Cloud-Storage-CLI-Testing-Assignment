package org.example.commands;

import org.example.BaseTest;
import org.example.utils.ProcessUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Tests for the delete (rm) command in Google Cloud Storage CLI.
 * These tests verify that the rm command correctly deletes objects in a bucket.
 */
public class DeleteCommandTest extends BaseTest {
    private String testObjectPath1;


    /**
     * Setup method that runs before each test method.
     * Creates test objects in the test bucket for testing the delete command.
     *
     * @throws IOException If there is an error creating test objects
     * @throws InterruptedException If the process is interrupted
     */
    @BeforeMethod
    public void setUp() throws IOException, InterruptedException {
        // Create unique test object names for this test
        String testObjectName1 = "delete-" + UUID.randomUUID().toString().substring(0, 8);

        // Create test objects in the bucket (1KB in size)
        testObjectPath1 = createTestObject(testObjectName1, 1024);

        logger.info("Created test objects: " + testObjectPath1);
    }

    /**
     * Tests the rm command for deleting a single object.
     * Verifies that the command succeeds and the object is deleted.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testDeleteSingleObject() throws IOException, InterruptedException {
        // Execute rm command to delete the first test object
        ProcessUtils.ProcessResult result = gcloudCLI.deleteObjects(testObjectPath1);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "rm command failed: " + result.getStderr());

        // Verify the object is deleted by listing the bucket
        ProcessUtils.ProcessResult listResult = gcloudCLI.listObjects(testBucketName, testObjectPath1);

        // Verify listing succeeded
        Assert.assertFalse(listResult.isSuccess(), "ls command to verify deletion failed: " + listResult.getStderr());
    }

    /**
     * Tests the rm command with a non-existent object.
     * Verifies that the command fails with an appropriate error message.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testDeleteNonExistentObject() throws IOException, InterruptedException {
        // Define a non-existent object path
        String nonExistentPath = "gs://" + testBucketName + "/" + testObjectPrefix + "non-existent-"
                + UUID.randomUUID().toString();

        // Execute rm command with non-existent object
        ProcessUtils.ProcessResult result = gcloudCLI.deleteObjects(nonExistentPath);

        // Verify command failed
        Assert.assertFalse(result.isSuccess(), "rm command with non-existent object should fail");

        // Verify error message contains appropriate text
        String errorOutput = result.getStderr();
        Assert.assertTrue(errorOutput.contains("matched no objects") || errorOutput.contains("No URLs matched"),
                "Error message should indicate that the object doesn't exist: " + errorOutput);
    }

}