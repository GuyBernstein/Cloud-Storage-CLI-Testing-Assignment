package org.example.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

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
        );
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
