package org.example.utils;

import com.microsoft.playwright.*;

import java.util.Arrays;

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

        public NavigationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

    }

    /**
     * Navigates to the specified URL and checks if it can be accessed.
     *
     * This implementation focuses on basic URL accessibility by making a simple HTTP request.
     *
     * The method:
     * 1. Creates a new browser context with HTTPS errors ignored
     * 2. Makes an HTTP GET request to the URL
     * 3. Checks if the response status indicates success
     * 4. Returns a NavigationResult object with the result and any error message
     *
     * @param url The URL to navigate to
     * @return A NavigationResult object containing success status and error message if applicable
     */
    public NavigationResult navigateToUrl(String url) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(true));

        Page page = context.newPage();
        boolean navigationSuccess = false;
        String errorMessage = null;

        try {
            // Use Playwright's API for making a fetch request
            APIResponse response = page.context().request().get(url);

            // Check if request was successful
            if (response.ok()) {
                navigationSuccess = true;
            } else {
                errorMessage = "HTTP error: " + response.status() + " " + response.statusText();
            }

            // Always dispose of the response
            response.dispose();

        } catch (TimeoutError te) {
            errorMessage = "Navigation timeout: " + te.getMessage();
        } catch (Exception e) {
            errorMessage = "Request error: " + e.getMessage();
        } finally {
            // Clean up resources
            page.close();
            context.close();
        }

        return new NavigationResult(navigationSuccess, errorMessage);
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
