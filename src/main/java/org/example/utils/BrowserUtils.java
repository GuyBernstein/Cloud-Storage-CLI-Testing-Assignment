package org.example.utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.example.config.TestConfig.logger;

/**
 * Utility class for browser interactions using Playwright.
 * Used for testing signed URLs and checking for phishing warnings.
 */
public class BrowserUtils {
    private final Playwright playwright;
    private final Browser browser;

    /**
     * Creates a new BrowserUtils instance and initializes Playwright.
     */
    public BrowserUtils() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)  // Run in headless mode for CI environments
                .setArgs(Arrays.asList("--disable-web-security",  // Disable CORS and other web security features
                        "--disable-features=IsolateOrigins,site-per-process", // Disable site isolation
                        "--allow-running-insecure-content", // Allow loading insecure content
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--ignore-certificate-errors"
                ))
        );
    }


    /**
     * Extracts metadata from a signed URL response to verify object properties.
     *
     * @param url The signed URL to extract metadata from
     * @return A Map containing the metadata headers
     */
    public Map<String, String> extractSignedUrlMetadata(String url) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(true));

        Map<String, String> metadata = new HashMap<>();

        try {
            // Make a HEAD request to get headers without downloading content
            APIResponse response = context.request().fetch(
                    url,
                    RequestOptions.create().setMethod("HEAD"));

            if (response.ok()) {
                // Extract all headers
                for (String headerName : response.headers().keySet()) {
                    metadata.put(headerName.toLowerCase(), response.headers().get(headerName));
                }
            }

            response.dispose();
        } catch (Exception e) {
            // Log error but return what we have
            metadata.put("error", e.getMessage());
        } finally {
            context.close();
        }

        return metadata;
    }

    /**
     * Navigates to a URL and captures a screenshot of the page
     *
     * @param url The URL to navigate to
     * @param fileName Name to use for the screenshot file
     * @return Path to the saved screenshot file, or null if capture failed
     */
    public String capturePageScreenshot(String url, String fileName) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(true));
        Page page = context.newPage();
        String screenshotPath = null;

        try {
            // Create screenshots directory if it doesn't exist
            File screenshotsDir = new File("screenshots");
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs();
            }

            // Generate the screenshot file path
            screenshotPath = "screenshots/" + fileName + ".png";
            File screenshotFile = new File(screenshotPath);

            // Navigate to the URL
            page.navigate(url, new Page.NavigateOptions().setTimeout(30000));

            // Wait for the page to stabilize
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Take screenshot and save it to the file
            logger.info("Taking screenshot and saving to: " + screenshotPath);
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(screenshotPath))
            );

            // Verify screenshot was created successfully
            if (!screenshotFile.exists() || screenshotFile.length() == 0) {
                logger.severe("Screenshot file was not created or is empty");
                screenshotPath = null;
            }


        } catch (Exception e) {
            logger.severe("Error capturing screenshot for URL");
            throw new RuntimeException("Failed to capture screenshot", e);
        } finally {
            // Clean up resources
            page.close();
            context.close();
        }

        return screenshotPath;
    }

    /**
     * Closes the browser and Playwright resources.
     */
    public void close() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
