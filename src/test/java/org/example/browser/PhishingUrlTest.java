package org.example.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.example.BaseTest;
import org.example.utils.BrowserUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.example.commands.SignUrlCommandTest.generateSignedUrl;

/**
 * Tests for detecting phishing warnings when accessing potentially malicious URLs.
 * This class tests how browsers and security features respond to known phishing URLs.
 */
public class PhishingUrlTest extends BaseTest {
    private String testObjectPath1;
    private String testObjectPath2;
    private Page page;
    // the html in the project was from the url that was found in phishtank.com:
    private static final String PHISHING_URL = "https://daftar-gratis.vercel.app/";


    /**
     * Setup method that runs before each test method.
     * Creates a new browser context and page with specific settings to detect security warnings.
     *
     * @throws IOException If there is an error creating browser resources
     */
    @BeforeMethod
    public void setUpTest() throws IOException, InterruptedException {

        // Create a new page
        page = browserUtils.getBrowser().newPage();

        // Log the test setup
        logger.info("Created browser and a page for phishing detection test");

        String testObjectName = "security-" + UUID.randomUUID().toString().substring(0, 8);

        // Create a test object in the bucket
        testObjectPath1 = uploadPhishingContentFile(testObjectName);

        testObjectName = "security-" + UUID.randomUUID().toString().substring(0, 8);

        testObjectPath2 = createAndUploadSvgImage(testObjectName);

        logger.info("Created test object: " + testObjectPath1 + " and " + testObjectPath2);
    }

    /**
     * Teardown method that runs after each test method.
     * Closes browser resources.
     */
    @AfterMethod
    public void tearDownTest() {
        if (page != null) {
            page.close();
        }
        logger.info("Closed browser resources for phishing detection test");
    }

    /**
     * Tests phishing detection using a signed URL generated from a storage html object
     */
    @Test
    public void testHTMLPhishingAnalysisWithSignedUrl() {
        logger.info("Starting phishing analysis test with signed URL");

        try {
            // Generate the signed URL
            String signedUrl = generateSignedUrl(testObjectPath1);

            // Run the phishing content analysis on the signed URL
            analyzePhishingContent(signedUrl, true);
        } catch (IOException | InterruptedException e) {
            logger.severe("Error generating signed URL: " + e.getMessage());
            Assert.fail("Test failed due to exception: " + e.getMessage());
        }
    }

    /**
     * Tests phishing detection using a signed URL generated from a storage image object
     */
    @Test
    public void testImagePhishingAnalysisWithSignedUrl() {
        logger.info("Starting non phishing analysis test with signed URL");

        try {
            // Generate the signed URL
            String signedUrl = generateSignedUrl(testObjectPath2);

            // Run the phishing content analysis on the signed URL
            analyzePhishingContent(signedUrl, false);
        } catch (IOException | InterruptedException e) {
            logger.severe("Error generating signed URL: " + e.getMessage());
            Assert.fail("Test failed due to exception: " + e.getMessage());
        }
    }

    /**
     * Analyzes a URL for phishing indicators by examining the page content
     * Modified version of testPhishingContentAnalysis that accepts the URL as a parameter
     *
     * @param url The URL to analyze for phishing indicators
     * @param isSuspectedPhishing If we suspect it is phishing url
     */
    private void analyzePhishingContent(String url, boolean isSuspectedPhishing) throws IOException, InterruptedException {
        if (isSuspectedPhishing) {
            logger.info("Analyzing content of potentially malicious URL: " + url);
        }
        else
            logger.info("Analyzing content of non potentially malicious URL: " + url);


        try {
            // Navigate to the URL with a timeout
            page.navigate(url, new Page.NavigateOptions().setTimeout(30000));

            // Wait for the page to be fully loaded
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Extract page title and URL
            String pageTitle = page.title();
            String currentUrl = page.url();

            logger.info("Current page URL: " + currentUrl);
            logger.info("Page title: " + pageTitle);

            // Get the full page content
            String pageContent = page.content();

            // Save the content to a file for debugging
            String contentFilePath = "phishing-content-" + System.currentTimeMillis() + ".html";
            Files.writeString(Paths.get(contentFilePath), pageContent);
            logger.info("Saved page content to: " + contentFilePath);

            // Take a screenshot for visual verification
            String screenshotPath = "screenshots/phishing-content-" + System.currentTimeMillis() + ".png";
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)));
            logger.info("Screenshot saved to: " + screenshotPath);

            // ========== PHISHING CONTENT ANALYSIS ==========

            // 1. Check for bank/payment related keywords
            String[] financialKeywords = {"bank", "login", "debit", "card",
                    "password", "BRI" };

            for (String keyword : financialKeywords) {
                boolean containsKeyword = pageContent.toLowerCase().contains(keyword.toLowerCase());
                logger.info("Contains keyword '" + keyword + "': " + containsKeyword);
            }

            // 2. Look for mismatched branding - e.g., claims to be one company but has different URLs
            boolean containsBrandMismatch = false;
            String[] commonBrands = {"BRI", "bank", "rakyat", "indonesia"};
            for (String brand : commonBrands) {
                if (pageContent.toLowerCase().contains(brand.toLowerCase()) &&
                        !currentUrl.toLowerCase().contains(brand.toLowerCase())) {
                    containsBrandMismatch = true;
                    logger.info("Brand mismatch detected: Content mentions '" + brand +
                            "' but URL doesn't contain this brand");
                }
            }

            // 3. Check for suspicious redirects in JavaScript
            boolean hasSuspiciousRedirects = pageContent.contains("window.location") ||
                    pageContent.contains("document.location") ||
                    pageContent.contains("window.navigate");
            logger.info("Contains suspicious redirects: " + hasSuspiciousRedirects);

            // 4. Check for data exfiltration methods
            boolean hasDataExfiltration = pageContent.contains("XMLHttpRequest") ||
                    pageContent.contains("fetch(") ||
                    pageContent.contains("$.ajax") ||
                    pageContent.contains("$.post");
            logger.info("Contains data exfiltration methods: " + hasDataExfiltration);

            // Compute overall phishing likelihood based on indicators
            int phishingIndicatorsCount = 0;
            if (containsBrandMismatch) phishingIndicatorsCount++;
            if (hasSuspiciousRedirects) phishingIndicatorsCount++;
            if (hasDataExfiltration) phishingIndicatorsCount++;

            logger.info("Total phishing indicators found: " + phishingIndicatorsCount);

            if(isSuspectedPhishing) {
                // Assert that we found phishing indicators
                Assert.assertTrue(phishingIndicatorsCount > 0,
                        "Expected to find phishing indicators in a known phishing site");
            }
            else
                Assert.assertEquals(phishingIndicatorsCount, 0, "Expected to find phishing indicators in a known phishing site");

        } catch (Exception e) {
            logger.severe("Error during phishing content analysis: " + e.getMessage());
            throw new RuntimeException("Phishing analysis failed", e);
        }
    }
}