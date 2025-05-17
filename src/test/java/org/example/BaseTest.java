package org.example;

import org.example.cli.GCloudStorageCLI;
import org.example.config.TestConfig;
import org.example.utils.BrowserUtils;
import org.example.utils.ProcessUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Base test class for Google Cloud Storage CLI tests.
 * Provides common setup and teardown methods, as well as utility methods for tests.
 */
public class BaseTest {

    protected static final Logger logger = Logger.getLogger(BaseTest.class.getName());

    protected static TestConfig config;
    protected static GCloudStorageCLI gcloudCLI;
    protected static BrowserUtils browserUtils;

    // Test data
    protected String testBucketName;
    protected String testObjectPrefix;
    protected String testRunId;

    /**
     * Setup method that runs once before all tests in the suite.
     * Initializes shared resources like test configuration.
     *
     * @throws IOException If there is an error initializing resources
     */
    @BeforeSuite
    public void suiteSetup() throws IOException {
        // Load test configuration
        config = new TestConfig();

        // Initialize browser utils for URL testing
        browserUtils = new BrowserUtils();

        // Create gcloud CLI wrapper
        gcloudCLI = new GCloudStorageCLI(
                config.getGCloudPath(),
                config.getEnvironmentVariables());

        // Generate a unique run ID for this test run to avoid conflicts in parallel test runs
        testRunId = UUID.randomUUID().toString().substring(0, 8);

        // Log GCloud CLI version
        try {
            ProcessUtils.ProcessResult versionResult = gcloudCLI.getVersion();
            if (versionResult.isSuccess()) {
                logger.info("Using gcloud version: " + versionResult.getStdout().trim());
            } else {
                logger.warning("Failed to get gcloud version: " + versionResult.getStderr());
            }
        } catch (Exception e) {
            logger.warning("Error getting gcloud version: " + e.getMessage());
        }
    }

    /**
     * Setup method that runs before each test class.
     * Initializes test-specific resources and data.
     *
     * @throws IOException If there is an error initializing resources
     */
    @BeforeClass
    public void classSetup() throws IOException {
        // Get test bucket name and object prefix from configuration
        testBucketName = config.getTestBucketName();
        testObjectPrefix = config.getTestObjectPrefix() + testRunId + "-";

        // Ensure the test bucket exists (or skip tests if we don't have permissions)
        ensureTestBucketExists();
    }

    /**
     * Teardown method that runs after each test class.
     * Cleans up test-specific resources and data.
     *
     * @throws IOException          If there is an error cleaning up resources
     * @throws InterruptedException If the cleanup process is interrupted
     */
    @AfterClass
    public void classTeardown() throws IOException, InterruptedException {
        // Clean up test objects created during the test
        cleanupTestObjects();
    }

    /**
     * Teardown method that runs once after all tests in the suite.
     * Cleans up shared resources.
     */
    @AfterSuite
    public void suiteTeardown() {
        // Close browser utils
        if (browserUtils != null) {
            browserUtils.close();
        }
    }

    /**
     * Ensures that the test bucket exists.
     * If it doesn't exist and we have permissions, creates it.
     * If we don't have permissions, logs a warning.
     */
    private void ensureTestBucketExists() {
        try {
            // Check if the bucket exists by listing it
            ProcessUtils.ProcessResult result = gcloudCLI.listObjects(testBucketName, null);

            if (!result.isSuccess()) {
                logger.warning("Test bucket doesn't exist or we don't have permissions: " + result.getStderr());
                logger.warning("Tests may fail if the bucket doesn't exist or we don't have permissions");
            }
        } catch (Exception e) {
            logger.warning("Error checking test bucket: " + e.getMessage());
            logger.warning("Tests may fail if the bucket doesn't exist or we don't have permissions");
        }
    }

    /**
     * Cleans up test objects created during the test.
     *
     * @throws IOException          If there is an error during cleanup
     * @throws InterruptedException If the cleanup process is interrupted
     */
    private void cleanupTestObjects() throws IOException, InterruptedException {
        try {
            // Delete test objects with our test prefix
            String objectPath = "gs://" + testBucketName + "/" + testObjectPrefix + "*";
            logger.info("Cleaning up test objects: " + objectPath);

            ProcessUtils.ProcessResult result = gcloudCLI.deleteObjects(objectPath, true);

            if (!result.isSuccess()) {
                logger.warning("Failed to clean up test objects: " + result.getStderr());
            }
        } catch (Exception e) {
            logger.warning("Error cleaning up test objects: " + e.getMessage());
        }
    }

    /**
     * Creates a test object with random content in the test bucket.
     *
     * @param objectName Name of the object to create (without the prefix)
     * @param sizeInBytes Size of the object in bytes
     * @return Path to the created object in the format "gs://bucket-name/object-name"
     * @throws IOException If there is an error creating the object
     * @throws InterruptedException If the process is interrupted
     */
    protected String createTestObject(String objectName, int sizeInBytes) throws IOException, InterruptedException {
        // Create a temporary file with random content
        Path tempFile = Files.createTempFile("gcs-test-", ".tmp");
        byte[] randomData = new byte[sizeInBytes];
        // Generate random data (simple version)
        for (int i = 0; i < sizeInBytes; i++) {
            randomData[i] = (byte) (Math.random() * 256);
        }
        Files.write(tempFile, randomData);

        // Upload the file to GCS - fixing the path construction
        // We use just the objectName parameter directly without prepending testObjectPrefix again
        String objectPath = "gs://" + testBucketName + "/" + testObjectPrefix + objectName;
        ProcessUtils.ProcessResult result = gcloudCLI.copyObjects(tempFile.toString(), objectPath);

        // Delete the temporary file
        Files.delete(tempFile);

        if (!result.isSuccess()) {
            throw new IOException("Failed to create test object: " + result.getStderr());
        }

        return objectPath;
    }

    /**
     * Creates a simple SVG image file and uploads it to the test bucket.
     *
     * @param objectName Name of the object to create (without the prefix)
     * @return Path to the created object in the format "gs://bucket-name/object-name"
     * @throws IOException If there is an error creating or uploading the file
     * @throws InterruptedException If the process is interrupted
     */
    protected String createAndUploadSvgImage(String objectName) throws IOException, InterruptedException {
        // Create a simple SVG image
        String svgContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">\n" +
                "    <rect width=\"100\" height=\"100\" fill=\"blue\" />\n" +
                "    <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\" />\n" +
                "</svg>";

        // Write the SVG to a temporary file
        Path tempFile = Files.createTempFile("test-image-", ".svg");
        Files.write(tempFile, svgContent.getBytes());

        // Upload the file to GCS
        String objectPath = "gs://" + testBucketName + "/" + testObjectPrefix + objectName;
        ProcessUtils.ProcessResult result = gcloudCLI.copyObjects(tempFile.toString(), objectPath);

        // Delete the temporary file
        Files.delete(tempFile);

        if (!result.isSuccess()) {
            throw new IOException("Failed to upload SVG image: " + result.getStderr());
        }

        return objectPath;
    }
}