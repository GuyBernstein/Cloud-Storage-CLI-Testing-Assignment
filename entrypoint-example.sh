#!/bin/bash
set -e

# Ensure Python environment variables are set
export CLOUDSDK_PYTHON=python3
export CLOUDSDK_PYTHON_SITEPACKAGES=1

# Print Python and pyOpenSSL version info
echo "Python version:"
python3 --version
echo "Installed pyOpenSSL version:"
pip3 show pyopenssl

# Verify gcloud auth is working and activate the project
echo "Verifying Google Cloud authentication..."
gcloud config set project YourProjectID

# Verify the environment variables are set
echo "Checking environment variables..."
echo "CLOUDSDK_PYTHON_SITEPACKAGES = $CLOUDSDK_PYTHON_SITEPACKAGES"
echo "CLOUDSDK_PYTHON = $CLOUDSDK_PYTHON"

# Announce we're ready to run tests
echo "Authentication verified! Running the tests..."

mvn test