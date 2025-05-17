package org.example.commands;

import org.example.BaseTest;
import org.example.utils.BrowserUtils;
import org.example.utils.ProcessUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tests for the sign-url command in Google Cloud Storage CLI.
 * These tests verify that signed URLs are generated correctly and
 * don't trigger phishing warnings when accessed in a browser.
 */
public class SignUrlCommandTest extends BaseTest {

    private String testObjectName;
    private String testObjectPath;


    /**
     * Setup method that runs before each test method.
     * Creates a test object in the test bucket for testing sign-url command.
     *
     * @throws IOException          If there is an error creating test object
     * @throws InterruptedException If the process is interrupted
     */

    @BeforeMethod
    public void setUp() throws IOException, InterruptedException {
        // Create a unique test object name for this test
        testObjectName = "signurl-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a test object in the bucket (1KB in size)
        testObjectPath = createTestObject(testObjectName, 1024);

        logger.info("Created test object: " + testObjectPath);
    }

    /**
     * Tests the sign-url command with default options.
     * Verifies that the command succeeds and returns a valid URL.
     *
     * @throws IOException          If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */

    @Test
    public void testSignUrlDefault() throws IOException, InterruptedException {
        // Execute sign-url command
        ProcessUtils.ProcessResult result = gcloudCLI.signUrl(
                testObjectPath,
                config.getSignedUrlDuration(),
                config.getKeyFilePath());

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "sign-url command failed: " + result.getStderr());

        // Verify output contains a URL
        String output = result.getStdout();
        Assert.assertFalse(output.isEmpty(), "sign-url command output is empty");

        // Extract the URL from the output
        String signedUrl = extractUrlFromOutput(output);
        Assert.assertNotNull(signedUrl, "Failed to extract signed URL from output");

        logger.info("Generated signed URL: " + signedUrl);

        // Verify the URL format
        Assert.assertTrue(signedUrl.startsWith("https://"), "Signed URL does not start with https://");
        Assert.assertTrue(signedUrl.contains(testBucketName), "Signed URL does not contain the bucket name");
        Assert.assertTrue(signedUrl.contains(testObjectName), "Signed URL does not contain the object name");
    }

    /**
     * Tests that the generated signed URL can be successfully accessed.
     * This test focuses only on basic accessibility of the URL without checking
     * for phishing warnings. The test represents the first step in a multi-stage
     * approach to fully validate signed URL functionality.

     * The test:
     * 1. Generates a signed URL for the test object
     * 2. Attempts to access the URL using BrowserUtils
     * 3. Verifies that the URL can be successfully accessed

     * Future enhancements will include checks for phishing warnings and security alerts.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testSignedUrlAccess() throws IOException, InterruptedException {
        // Execute sign-url command
        ProcessUtils.ProcessResult result = gcloudCLI.signUrl(
                testObjectPath,
                config.getSignedUrlDuration(),
                config.getKeyFilePath());

        // Verify command succeeded
        Assert.assertTrue(result.isSuccess(), "sign-url command failed: " + result.getStderr());

        // Extract the URL from the output
        String signedUrl = extractUrlFromOutput(result.getStdout());
        Assert.assertNotNull(signedUrl, "Failed to extract signed URL from output");

        // Navigate to the URL - first step focuses only on basic URL accessibility
        // without checking for phishing warnings or security alerts
        BrowserUtils.NavigationResult navResult = browserUtils.navigateToUrl(signedUrl);

        // Verify navigation succeeded - ensures the URL can be accessed
        Assert.assertTrue(navResult.isSuccess(),
                "Failed to navigate to signed URL: " + navResult.getErrorMessage());
    }

    /**
     * Helper method to extract a URL from the command output.
     *
     * @param output The command output
     * @return The extracted URL, or null if no URL is found
     */

    private String extractUrlFromOutput(String output) {
        // Extract URL using regex
        Pattern pattern = Pattern.compile("signed_url:\\s+(https://\\S+)");
        Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
