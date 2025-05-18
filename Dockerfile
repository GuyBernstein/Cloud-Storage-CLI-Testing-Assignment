FROM openjdk:11

# Create the target directories
RUN mkdir -p /usr/src /opt/conf

# Install Python 3 and pip
RUN apt-get update && apt-get install -y python3 python3-pip

# Install the Google Cloud SDK
RUN apt-get update && apt-get install -y curl gnupg lsb-release && \
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg add - && \
    apt-get update && apt-get install -y google-cloud-sdk

# Install pyOpenSSL with the specific version
RUN pip3 install pyOpenSSL==23.2.0

# Install additional dependencies for Playwright
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    && apt-get clean

# Install Maven
RUN apt-get update && apt-get install -y maven

# Create a directory for the key file
RUN mkdir -p /src/test/resources

# Set environment variables
ENV GCLOUD_PATH=gcloud
ENV TEST_BUCKET_NAME=example_bucket1sy
ENV TEST_OBJECT_PREFIX=test-object-
ENV SIGNED_URL_DURATION=1h
# Set Python environment variable
ENV CLOUDSDK_PYTHON=python3
ENV CLOUDSDK_PYTHON_SITEPACKAGES=1

# Set the working directory to the project root
WORKDIR /app

# Setup entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Copy the Maven project files
COPY pom.xml .
COPY src/ ./src/
COPY test-config.properties .
COPY phishing_content.html .


# Set entrypoint to our custom script
ENTRYPOINT ["/entrypoint.sh"]