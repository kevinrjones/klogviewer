# ADR 035: Implementation of S3 Log Source

## Status
Proposed

## Context
KLogViewer needs to support reading logs from AWS S3 buckets as part of the Sprint 8 Connectivity goals. Unlike SFTP/SSH, S3 does not support a native "tail" mechanism.

## Decision
1. **AWS SDK**: Use the AWS SDK for Kotlin (`aws.sdk.kotlin:s3`) for interacting with S3.
2. **Polling-based Tailing**: Implement tailing by periodically polling the S3 object metadata (ETag or Size/LastModified) and performing range requests to fetch new data.
3. **Authentication**:
    - Support AWS Profiles (via `DefaultChainCredentialsProvider`).
    - Support Explicit Credentials (Access Key / Secret Key).
    - Support Environment Variables (via `DefaultChainCredentialsProvider`).
4. **S3DirectoryLogSource**: Implement a directory-like observer for S3 prefixes to automatically discover and monitor new log objects.
5. **Efficiency**: Use `HeadObject` to check for changes before downloading data to minimize API costs and latency.

## Consequences
- **Positive**: Enables integration with cloud-native logging architectures.
- **Negative**: Polling introduces latency compared to streaming; increased API calls might incur costs (mitigated by HEAD requests and configurable polling intervals).
- **Dependency**: Adds AWS SDK to the project.
