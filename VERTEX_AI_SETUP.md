# Vertex AI Setup Guide

## Issue Fixed
Fixed `java.lang.IllegalArgumentException: Vertex AI project-id must be set!` error by adding proper Google Cloud authentication.

## Required Setup

### 1. Google Cloud Service Account Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to "IAM & Admin" → "Service Accounts"
3. Create a new service account or use existing one
4. Grant necessary permissions:
   - Vertex AI User
   - AI Platform Developer (if needed)
5. Generate a JSON key file
6. Download the key file to your project directory

### 2. Environment Variable Configuration
Update the `GOOGLE_APPLICATION_CREDENTIALS` path in your `.env` file:
```
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/your/service-account-key.json
```

### 3. For Docker Deployment
Ensure the service account key file is accessible in the Docker container by mounting it as a volume or copying it during the build process.

## Current Configuration
- Project ID: `meme-wiki-46bcf`
- Location: `us-central1`
- Embedding Model: `text-embedding-004`
- Gemini Model: `gemini-1.5-pro`

## Troubleshooting
- Ensure the service account has proper permissions for Vertex AI
- Verify the JSON key file path is correct and accessible
- Check if the project ID matches your Google Cloud project