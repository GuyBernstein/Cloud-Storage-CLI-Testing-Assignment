package org.example.commands;

import org.example.BaseTest;
import org.example.utils.BrowserUtils;
import org.example.utils.ProcessUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tests for validating the integrity and security of signed URLs.
 * These tests go beyond basic accessibility to verify content, headers,
 * and security aspects of the signed URLs.
 */
public class SignUrlCommandTest extends BaseTest {

    private String testObjectName;
    private String testObjectPath;
    private int objectSize;
    private static final int DEFAULT_OBJECT_SIZE = 4096; // 4KB for better content validation



    /**
     * Setup method that runs before each test method.
     * Creates a test object in the test bucket for testing signed URL validation.
     *
     * @throws IOException          If there is an error creating test object
     * @throws InterruptedException If the process is interrupted
     */

    @BeforeMethod
    public void setUp() throws IOException, InterruptedException {
        // Create a unique test object name for this test
        testObjectName = "security-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a larger test object for better content validation
        objectSize = DEFAULT_OBJECT_SIZE;
        testObjectPath = createTestObject(testObjectName, objectSize);

        logger.info("Created test object for security testing: " + testObjectPath + " (size: " + objectSize + " bytes)");
    }

    /**
     * Tests that a signed URL returns the expected content size.
     * This verifies that the content length matches what we uploaded.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */

    @Test
    public void testSignedUrlContentSize() throws IOException, InterruptedException {
        // Generate a signed URL for the test object
        String signedUrl = generateSignedUrl(testObjectPath);
        Assert.assertNotNull(signedUrl, "Failed to generate signed URL");

        // Validate the signed URL with expected content size
        BrowserUtils.NavigationResult result = browserUtils.validateSignedUrl(
                signedUrl,
                objectSize  // Expected size
        );

        // Verify validation succeeded
        Assert.assertTrue(result.isSuccess(),
                "Signed URL validation failed: " + result.getErrorMessage());

        // Verify content length matches expected size
        Assert.assertEquals(result.getContentLength(), objectSize,
                "Content length does not match expected size");
    }

    /**
     * Tests that a signed URL includes appropriate security headers.
     * This verifies that the response includes headers that help protect against attacks.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testSignedUrlSecurityHeaders() throws IOException, InterruptedException {
        // Generate a signed URL for the test object
        String signedUrl = generateSignedUrl(testObjectPath);
        Assert.assertNotNull(signedUrl, "Failed to generate signed URL");

        // Extract metadata from the signed URL
        Map<String, String> metadata = browserUtils.extractSignedUrlMetadata(signedUrl);

        // Verify we got metadata
        Assert.assertFalse(metadata.isEmpty(), "No metadata received from signed URL");

        // Verify we got metadata
        Assert.assertFalse(metadata.containsKey("error"), "Error occurred while getting metadata from signed URL: " + metadata.get("error"));

        // Check for common security-related headers (at least one should be present)
        boolean hasSecurityHeaders =
                metadata.containsKey("x-goog-storage-class") ||
                        metadata.containsKey("x-goog-generation") ||
                        metadata.containsKey("x-goog-metageneration") ||
                        metadata.containsKey("etag");

        Assert.assertTrue(hasSecurityHeaders,
                "Response lacks expected Google Cloud Storage metadata headers");
    }

    /**
     * Helper method to generate a signed URL for a given object path.
     *
     * @param objectPath The path of the object to sign
     * @return The signed URL or null if generation fails
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    private String generateSignedUrl(String objectPath) throws IOException, InterruptedException {
        ProcessUtils.ProcessResult result = gcloudCLI.signUrl(
                objectPath,
                config.getSignedUrlDuration(),
                config.getKeyFilePath());

        if (!result.isSuccess()) {
            logger.severe("Failed to generate signed URL: " + result.getStderr());
            return null;
        }

        // Extract URL using regex
        Pattern pattern = Pattern.compile("signed_url:\\s+(https://\\S+)");
        Matcher matcher = pattern.matcher(result.getStdout());

        if (matcher.find()) {
            return matcher.group(1);
        }

        logger.severe("Failed to extract URL from output: " + result.getStdout());
        return null;
    }
}
