package org.example.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration class for test settings.
 * Loads test configuration from properties files and environment variables.
 */
public class TestConfig {
    public static final Logger logger = Logger.getLogger(TestConfig.class.getName());

    private final Properties properties;
    private final Map<String, String> environmentVariables;

    // Default values - only used if not found in properties or environment
    private static final String DEFAULT_GCLOUD_PATH = "gcloud";
    private static final String DEFAULT_BUCKET_NAME = "example_bucket1sy";
    private static final String DEFAULT_TEST_OBJECT_PREFIX = "test-object-";
    private static final String DEFAULT_KEY_FILE_PATH = "src/test/resources/key.json";
    private static final String DEFAULT_SIGNED_URL_DURATION = "1h";

    /**
     * Initializes test configuration from the default properties file and environment variables.
     *
     * @throws IOException If there is an error loading the properties file
     */
    public TestConfig() throws IOException {
        this("test-config.properties");
    }

    /**
     * Initializes test configuration from the specified properties file and environment variables.
     *
     * @param propertiesFile Path to the properties file
     * @throws IOException If there is an error loading the properties file
     */
    public TestConfig(String propertiesFile) throws IOException {
        properties = new Properties();
        environmentVariables = new HashMap<>();

        // Try to load properties from multiple locations
        boolean propertiesLoaded = loadPropertiesFromLocations(propertiesFile);

        if (!propertiesLoaded) {
            logger.warning("Could not find properties file. Using default values.");
        }

        // Setup environment variables for gcloud
        environmentVariables.put("CLOUDSDK_PYTHON_SITEPACKAGES", "1");
        environmentVariables.put("CLOUDSDK_PYTHON", "python3");


        // Log the configuration that will be used
        logConfiguration();
    }

    /**
     * Attempts to load properties from multiple possible locations.
     *
     * @param propertiesFile Base name of the properties file
     * @return true if properties were loaded from any location, false otherwise
     * @throws IOException If there is an error reading the file
     */
    private boolean loadPropertiesFromLocations(String propertiesFile) throws IOException {
        boolean loaded = false;

        // Location 1: Current directory
        Path currentDirPath = Paths.get(propertiesFile);
        if (Files.exists(currentDirPath)) {
            try (FileInputStream fis = new FileInputStream(currentDirPath.toFile())) {
                properties.load(fis);
                logger.info("Loaded properties from current directory: " + currentDirPath.toAbsolutePath());
                loaded = true;
            }
        }

        // Location 2: User's home directory
        if (!loaded) {
            Path homeDirPath = Paths.get(System.getProperty("user.home"), propertiesFile);
            if (Files.exists(homeDirPath)) {
                try (FileInputStream fis = new FileInputStream(homeDirPath.toFile())) {
                    properties.load(fis);
                    logger.info("Loaded properties from home directory: " + homeDirPath.toAbsolutePath());
                    loaded = true;
                }
            }
        }

        // Location 3: Classpath
        if (!loaded) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
                if (is != null) {
                    properties.load(is);
                    logger.info("Loaded properties from classpath: " + propertiesFile);
                    loaded = true;
                }
            }
        }

        return loaded;
    }

    /**
     * Gets the path to the gcloud executable.
     *
     * @return The path to the gcloud executable
     */
    public String getGCloudPath() {
        return getPropertyOrDefault("gcloud.path", DEFAULT_GCLOUD_PATH);
    }

    /**
     * Gets the name of the test bucket.
     *
     * @return The name of the test bucket
     */
    public String getTestBucketName() {
        return getPropertyOrDefault("test.bucket.name", DEFAULT_BUCKET_NAME);
    }

    /**
     * Gets the prefix for test object names.
     *
     * @return The prefix for test object names
     */
    public String getTestObjectPrefix() {
        return getPropertyOrDefault("test.object.prefix", DEFAULT_TEST_OBJECT_PREFIX);
    }

    /**
     * Gets the path to the key file used for signing URLs.
     *
     * @return The path to the key file
     */
    public String getKeyFilePath() {
        return getPropertyOrDefault("key.file.path", DEFAULT_KEY_FILE_PATH);
    }

    /**
     * Gets the duration for signed URLs.
     *
     * @return The duration for signed URLs (e.g., "1h" for 1 hour)
     */
    public String getSignedUrlDuration() {
        return getPropertyOrDefault("signed.url.duration", DEFAULT_SIGNED_URL_DURATION);
    }

    /**
     * Gets the value of a property from environment variables or properties file,
     * or returns the default value if the property is not set.
     *
     * @param key          The property key
     * @param defaultValue The default value to return if the property is not set
     * @return The property value
     */
    private String getPropertyOrDefault(String key, String defaultValue) {
        // Check environment variables first (uppercase with dots replaced by underscores)
        String envVarKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envVarKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Then check properties file
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Gets the environment variables for gcloud commands.
     *
     * @return Map of environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return new HashMap<>(environmentVariables);
    }

    /**
     * Logs the current configuration values and their sources.
     */
    private void logConfiguration() {
        logger.info("=== Test Configuration ===");
        logConfigValue("gcloud.path", getGCloudPath());
        logConfigValue("test.bucket.name", getTestBucketName());
        logConfigValue("test.object.prefix", getTestObjectPrefix());
        logConfigValue("key.file.path", getKeyFilePath());
        logConfigValue("signed.url.duration", getSignedUrlDuration());
        logger.info("========================");
    }

    /**
     * Logs a configuration value and its source.
     *
     * @param key The configuration key
     * @param value The configuration value
     */
    private void logConfigValue(String key, String value) {
        String envVarKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envVarKey);

        if (envValue != null && !envValue.isEmpty()) {
            logger.info(key + " = " + value + " (from environment variable " + envVarKey + ")");
        } else if (properties.containsKey(key)) {
            logger.info(key + " = " + value + " (from properties file)");
        } else {
            logger.info(key + " = " + value + " (default value)");
        }
    }

    /**
     * Prints all configuration values to the console.
     * Useful for debugging.
     */
    public void printConfiguration() {
        System.out.println("=== Test Configuration ===");
        System.out.println("gcloud.path = " + getGCloudPath());
        System.out.println("test.bucket.name = " + getTestBucketName());
        System.out.println("test.object.prefix = " + getTestObjectPrefix());
        System.out.println("key.file.path = " + getKeyFilePath());
        System.out.println("signed.url.duration = " + getSignedUrlDuration());
        System.out.println("========================");
    }
}