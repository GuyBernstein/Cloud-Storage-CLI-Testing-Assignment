package org.example.commands;

import org.example.BaseTest;
import org.example.utils.ProcessUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
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
        String testObjectName = "signed-url-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a larger test object for better content validation
        objectSize = DEFAULT_OBJECT_SIZE;
        testObjectPath = createTestObject(testObjectName, objectSize);

        logger.info("Created test object for signed URL testing: " + testObjectPath + " (size: " + objectSize + " bytes)");
    }

    /**
     * Tests that a signed URL includes appropriate headers.
     * This verifies that the response includes headers that help protect against attacks.
     *
     * @throws IOException If there is an error executing the command
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testSignedUrlHeaders() throws IOException, InterruptedException {
        // Generate a signed URL for the test object
        String signedUrl = generateSignedUrl(testObjectPath);
        Assert.assertNotNull(signedUrl, "Failed to generate signed URL");

        // Extract metadata from the signed URL
        Map<String, String> metadata = browserUtils.extractSignedUrlMetadata(signedUrl);

        // Verify we got metadata
        Assert.assertFalse(metadata.isEmpty(), "No metadata received from signed URL");

        // Verify we got metadata
        Assert.assertFalse(metadata.containsKey("error"), "Error occurred while getting metadata from signed URL: " + metadata.get("error"));

        // Check for common headers (at least one should be present)
        boolean hasSecurityHeaders =
                metadata.containsKey("x-goog-storage-class") ||
                        metadata.containsKey("x-goog-generation") ||
                        metadata.containsKey("x-goog-metageneration") ||
                        metadata.containsKey("etag");

        Assert.assertTrue(hasSecurityHeaders,
                "Response lacks expected Google Cloud Storage metadata headers");

        int contentLength = 0;
        try {
            contentLength = Integer.parseInt(
                    metadata.getOrDefault("content-length", "-1")
            );
        }catch (NumberFormatException ignored) {
            contentLength = -1;
        }
        // Verify content length matches expected size
        Assert.assertEquals(contentLength, objectSize,
                "Content length does not match expected size");

    }

    /**
     * Tests that a signed URL for an image can be accessed and successfully captured as a screenshot.
     *
     * @throws IOException If there is an error executing the command or saving the screenshot
     * @throws InterruptedException If the process is interrupted
     */
    @Test
    public void testSignedUrlImageNavigation() throws IOException, InterruptedException {
        // Create and upload a simple SVG image
        String imageObjectName = "test-image-" + UUID.randomUUID().toString().substring(0, 8) + ".svg";
        String imageObjectPath = createAndUploadSvgImage(imageObjectName);

        logger.info("Created and uploaded SVG image: " + imageObjectPath);

        // Generate a signed URL for the image
        String signedUrl = generateSignedUrl(imageObjectPath);
        Assert.assertNotNull(signedUrl, "Failed to generate signed URL");

        // Navigate to the signed URL and capture a screenshot
        String screenshotPath = browserUtils.capturePageScreenshot(signedUrl, imageObjectName);

        // Verify the screenshot was successfully captured
        Assert.assertNotNull(screenshotPath, "Screenshot path should not be null");

        // Check if the file exists
        File screenshotFile = new File(screenshotPath);
        Assert.assertTrue(screenshotFile.exists(), "Screenshot file does not exist at path: " + screenshotPath);

        // Check if the file has content (not empty)
        Assert.assertTrue(screenshotFile.length() > 0, "Screenshot file is empty");
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
