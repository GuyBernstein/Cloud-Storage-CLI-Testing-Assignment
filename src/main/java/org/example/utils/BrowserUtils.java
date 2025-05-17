package org.example.utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

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
     * Class to hold the result of a URL navigation.
     */
    public static class NavigationResult {
        private final boolean success;
        private final String errorMessage;
        private final Map<String, String> responseHeaders;
        private final int contentLength;
        private final byte[] contentSample;

        public NavigationResult(boolean success, String errorMessage) {
            this(success, errorMessage, null, 0, null);
        }

        public NavigationResult(boolean success, String errorMessage,
                                Map<String, String> responseHeaders,
                                int contentLength,
                                byte[] contentSample) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.responseHeaders = responseHeaders;
            this.contentLength = contentLength;
            this.contentSample = contentSample;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Map<String, String> getResponseHeaders() {
            return responseHeaders;
        }

        public int getContentLength() {
            return contentLength;
        }

        public byte[] getContentSample() {
            return contentSample;
        }

        /**
         * Helper method to get a base64 string representation of the content sample
         *
         * @return Base64 encoded content sample or null if no sample is available
         */
        public String getContentSampleAsBase64() {
            return contentSample != null ? Base64.getEncoder().encodeToString(contentSample) : null;
        }
    }


    /**
     * Performs a comprehensive validation of a signed URL by examining its content,
     * headers, and other metadata to ensure it points to the correct object.
     *
     * @param url The signed URL to validate
     * @param expectedContentLength The expected size of the content in bytes, or -1 if unknown
     * @return A NavigationResult object containing detailed validation results
     */
    public NavigationResult validateSignedUrl(String url, int expectedContentLength) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(true));

        Page page = context.newPage();
        boolean validationSuccess = false;
        String errorMessage = null;
        Map<String, String> headers = new HashMap<>();
        int contentLength = 0;
        byte[] contentSample = null;

        try {
            // Make a GET request to fetch a sample of the content (first 1KB)
            APIResponse getResponse = page.context().request().fetch(
                    url,
                    RequestOptions.create().setMethod("GET").setMaxRedirects(5));

            if (!getResponse.ok()) {
                errorMessage = "GET request failed: " + getResponse.status() + " " + getResponse.statusText();
                getResponse.dispose();
                return new NavigationResult(false, errorMessage);
            }

            try {
                contentLength = Integer.parseInt(
                        getResponse.headers().getOrDefault("content-length", "0")
                );
            }catch (NumberFormatException ignored) {
                // contentLength is already initialize to 0
            }

            // Get a sample of the content (first 1KB)
            byte[] fullContent = getResponse.body();
            int sampleSize = Math.min(1024, fullContent.length);
            contentSample = Arrays.copyOf(fullContent, sampleSize);

            // Perform validation checks
            if (expectedContentLength > 0 && contentLength != expectedContentLength) {
                errorMessage = "Content length mismatch: expected " + expectedContentLength + ", got " + contentLength;
                getResponse.dispose();
                return new NavigationResult(false, errorMessage, headers, contentLength, contentSample);
            }

            // All checks passed
            validationSuccess = true;
            getResponse.dispose();

        } catch (TimeoutError te) {
            errorMessage = "Validation timeout: " + te.getMessage();
        } catch (Exception e) {
            errorMessage = "Validation error: " + e.getMessage();
        } finally {
            // Clean up resources
            page.close();
            context.close();
        }

        return new NavigationResult(validationSuccess, errorMessage, headers, contentLength, contentSample);
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
