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
    private String fullObjectName1;
    private String fullObjectName2;

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
        String testObjectName2 = "delete-" + UUID.randomUUID().toString().substring(0, 8);

        // Create test objects in the bucket (1KB in size)
        testObjectPath1 = createTestObject(testObjectName1, 1024);
        String testObjectPath2 = createTestObject(testObjectName2, 1024);

        // Store the full object names for verification
        fullObjectName1 = testObjectPrefix + testObjectName1;
        fullObjectName2 = testObjectPrefix + testObjectName2;

        logger.info("Created test objects: " + testObjectPath1 + ", " + testObjectPath2);
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
        ProcessUtils.ProcessResult result = gcloudCLI.deleteObjects(testObjectPath1, false);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "rm command failed: " + result.getStderr());

        // Verify the object is deleted by listing the bucket
        ProcessUtils.ProcessResult listResult = gcloudCLI.listObjects(testBucketName, testObjectPrefix);

        // Verify listing succeeded
        Assert.assertTrue(listResult.isSuccess(), "ls command to verify deletion failed: " + listResult.getStderr());

        // Verify the first object is not in the listing but the second object is
        String output = listResult.getStdout();
        Assert.assertFalse(output.contains(fullObjectName1),
                "ls command output contains the deleted object: " + fullObjectName1);
        Assert.assertTrue(output.contains(fullObjectName2),
                "ls command output does not contain the second test object: " + fullObjectName2);
    }

    /**
     * Tests the rm command with recursive flag for deleting multiple objects.
     * Verifies that the command succeeds and both objects are deleted.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testDeleteMultipleObjectsRecursively() throws IOException, InterruptedException {
        // Define a pattern that matches both test objects
        String deletePattern = "gs://" + testBucketName + "/" + testObjectPrefix + "delete-*";

        // Execute rm command with recursive flag
        ProcessUtils.ProcessResult result = gcloudCLI.deleteObjects(deletePattern, true);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "rm command with recursive flag failed: " + result.getStderr());

        // Verify all objects matching the pattern are deleted by listing the bucket
        ProcessUtils.ProcessResult listResult = gcloudCLI.listObjects(testBucketName, testObjectPrefix + "delete-");

        // check if either:
        // 1. The list failed with "no objects" error (expected after successful deletion)
        // 2. The list succeeded but returned an empty output
        boolean objectsDeleted = (!listResult.isSuccess() &&
                listResult.getStderr().contains("matched no objects")) ||
                (listResult.isSuccess() &&
                        !listResult.getStdout().contains(testObjectPrefix + "delete-"));

        Assert.assertTrue(objectsDeleted,
                "Objects still exist after deletion or unexpected error: " + listResult.getStderr());


        // Verify the objects are not in the listing
        String output = listResult.getStdout();
        Assert.assertFalse(output.contains(testObjectPrefix + "delete-"), "ls command output contains deleted objects: " + output);
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
        ProcessUtils.ProcessResult result = gcloudCLI.deleteObjects(nonExistentPath, false);

        // Verify command failed
        Assert.assertFalse(result.isSuccess(), "rm command with non-existent object should fail");

        // Verify error message contains appropriate text
        String errorOutput = result.getStderr();
        Assert.assertTrue(errorOutput.contains("matched no objects") || errorOutput.contains("No URLs matched"),
                "Error message should indicate that the object doesn't exist: " + errorOutput);
    }

    /**
     * Tests the rm command for deleting an object within a nested directory structure.
     * Verifies that the command succeeds and the object is deleted.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testDeleteNestedObject() throws IOException, InterruptedException {
        // Create an object with a nested path
        String nestedObjectName = "nested/path/delete-" + UUID.randomUUID().toString().substring(0, 8);
        String nestedObjectPath = createTestObject(nestedObjectName, 1024);
        String fullNestedObjectName = testObjectPrefix + nestedObjectName;

        // Execute rm command to delete the nested object
        ProcessUtils.ProcessResult result = gcloudCLI.deleteObjects(nestedObjectPath, false);

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "rm command for nested object failed: " + result.getStderr());

        // Verify the nested object is deleted by listing the nested path
        ProcessUtils.ProcessResult listResult = gcloudCLI.listObjects(
                testBucketName,
                testObjectPrefix + "nested/path/");

        // check if either:
        // 1. The list failed with "no objects" error (expected after successful deletion)
        // 2. The list succeeded but returned an empty output
        boolean objectsDeleted = (!listResult.isSuccess() &&
                listResult.getStderr().contains("matched no objects")) ||
                (listResult.isSuccess() &&
                        !listResult.getStdout().contains(testObjectPrefix + "nested/path/"));

        Assert.assertTrue(objectsDeleted,
                "Objects still exist after deletion or unexpected error: " + listResult.getStderr());

        // Verify the nested object is not in the listing
        String output = listResult.getStdout();
        Assert.assertFalse(output.contains(fullNestedObjectName),
                "ls command output contains the deleted nested object: " + fullNestedObjectName);
    }
}