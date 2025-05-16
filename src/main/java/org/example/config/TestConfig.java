package org.example.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration class for test settings.
 * Loads test configuration from properties files and environment variables.
 */
public class TestConfig {
    private final Properties properties;
    private final Map<String, String> environmentVariables;

    // Default values
    private static final String DEFAULT_GCLOUD_PATH = "gcloud";
    private static final String DEFAULT_BUCKET_NAME = "example_bucket1sy";
    private static final String DEFAULT_TEST_OBJECT_PREFIX = "test-object-";


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

        // Load properties from file if it exists
        Path propsPath = Paths.get(propertiesFile);
        if (Files.exists(propsPath)) {
            try (FileInputStream fis = new FileInputStream(propsPath.toFile())) {
                properties.load(fis);
            }
        }

        // Setup environment variables for gcloud
        environmentVariables.put("CLOUDSDK_CORE_DISABLE_PROMPTS", "1"); // Disable interactive prompts
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
}

