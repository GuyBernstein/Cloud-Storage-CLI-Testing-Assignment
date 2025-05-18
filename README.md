# Google Cloud Storage CLI Testing Framework

This repository contains an automated testing framework for the Google Cloud Storage CLI (`gcloud storage`). It focuses on validating the execution of four key commands: `list`, `copy`, `delete`, and `sign-url`, with special attention to ensuring signed URLs are accessible and don't trigger phishing warnings.

## Project Overview

The framework is designed to test Google Cloud Storage CLI commands from an end-user perspective. It includes a modular test infrastructure that allows each test to run independently without affecting other test executions. The project prioritizes speed, scalability, and extensibility to support future test expansions.

### Key Features

- Automated tests for four `gcloud storage` commands
- Specialized tests for validating signed URL functionality and security
- Browser-based verification of signed URLs to detect potential phishing warnings
- Containerized test environment using Docker
- Modular and extensible design pattern
- Configuration management for different environments

## Prerequisites

To run this testing framework, you'll need:

- Java 11 or higher
- Maven
- Docker and Docker Compose
- Google Cloud SDK installed
- A Google Cloud Storage bucket with write permissions
- A service account key file with permissions to generate signed URLs

## Project Structure

```
├── src/
│   ├── main/java/org/example/
│   │   ├── cli/                  # CLI command wrappers
│   │   ├── config/               # Configuration utilities
│   │   └── utils/                # Utility classes
│   └── test/java/org/example/
│       ├── browser/              # Browser-based tests
│       ├── commands/             # CLI command tests
│       └── BaseTest.java         # Common test functionality
├── Dockerfile                    # Container definition
├── docker-compose.yml           # Container orchestration
├── pom.xml                      # Maven dependencies
├── phishing_content.html        # Test file for phishing detection
└── test-config.properties       # Test configuration
```

## Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/CloudStorageCLITestingAssignment.git
cd CloudStorageCLITestingAssignment
```

### 2. Configure Service Account

1. Create a service account with the following permissions:
   - `storage.objects.get`
   - `storage.objects.create`
   - `storage.objects.delete`
   - `storage.objects.list`

2. Create and download a JSON key file for this service account

3. Copy and rename the provided example key file:
   ```bash
   cp src/test/resources/key.json.example src/test/resources/key.json
   ```

4. Replace the contents with your actual service account key

### 3. Configure Test Settings

Edit the `test-config.properties` file to set your bucket name and other test parameters:

```properties
# GCloud CLI configuration
gcloud.path=gcloud

# Test bucket configuration
test.bucket.name=YOUR_BUCKET_NAME
test.object.prefix=test-object-

# Signed URL configuration
key.file.path=src/test/resources/key.json
signed.url.duration=1h
```

## Running the Tests

### Using Docker (Recommended)

1. Build the Docker image:
   ```bash
   docker-compose build
   ```

2. Run the tests:
   ```bash
   docker-compose up
   ```

### Using Maven Directly

1. Install the required dependencies:
   ```bash
   mvn clean install -DskipTests
   ```

2. Run the tests:
   ```bash
   mvn test
   ```

## Test Descriptions

The framework includes the following test classes:

1. **ListCommandTest** - Validates the `gcloud storage ls` command functionality:
   - List objects in a bucket
   - List objects with a specific prefix
   - Handle empty results

2. **CopyCommandTest** - Tests the `gcloud storage cp` command:
   - Copy objects within a bucket
   - Handle non-existent source objects

3. **DeleteCommandTest** - Verifies the `gcloud storage rm` command:
   - Delete single objects
   - Handle non-existent objects

4. **SignUrlCommandTest** - Tests the `gcloud storage sign-url` command:
   - Generate valid signed URLs
   - Validate signed URL headers and metadata
   - Verify access to image content via signed URLs

5. **PhishingUrlTest** - Specialized tests for signed URL security:
   - Test HTML content for potential phishing indicators
   - Validate that legitimate content doesn't trigger phishing warnings
   - Analyze browser responses to detect security warnings

For a more detailed explanation of each test, see the [TESTS.md](TESTS.md) file.

## Configuration Options

The testing framework can be configured through:

1. **Environment Variables**:
   - `GCLOUD_PATH` - Path to the gcloud executable
   - `TEST_BUCKET_NAME` - Name of the test bucket
   - `TEST_OBJECT_PREFIX` - Prefix for test objects
   - `KEY_FILE_PATH` - Path to the service account key file
   - `SIGNED_URL_DURATION` - Duration for signed URLs

2. **Properties File** (`test-config.properties`):
   - Same properties as environment variables but in lowercase with dots

3. **Command Line**:
   - Pass as system properties: `-Dtest.bucket.name=YOUR_BUCKET`

## Troubleshooting

### Common Issues

1. **Authentication Errors**:
   - Ensure your service account key file is correctly formatted and has the necessary permissions
   - Verify the `key.file.path` property points to a valid file

2. **Bucket Access Issues**:
   - Confirm the bucket specified in `test.bucket.name` exists and is accessible
   - Check that your service account has the required permissions on this bucket

3. **Docker Issues**:
   - Ensure Docker and Docker Compose are installed and running
   - Check if the container has access to your Google Cloud credentials

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-test`
3. Commit your changes: `git commit -am 'Add new test for X'`
4. Push to the branch: `git push origin feature/new-test`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

Created for the Google Cloud Testing Team assessment.
