package com.klogviewer.core.source

import aws.sdk.kotlin.runtime.auth.credentials.ProfileCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import com.klogviewer.domain.model.S3Auth
import com.klogviewer.domain.model.S3Config

class S3ClientProvider {
    suspend fun createClient(config: S3Config): S3Client {
        return S3Client.fromEnvironment {
            region = config.region
            credentialsProvider = when (val auth = config.auth) {
                is S3Auth.DefaultChain -> null // Use default chain
                is S3Auth.Profile -> ProfileCredentialsProvider(profileName = auth.profileName)
                is S3Auth.Explicit -> StaticCredentialsProvider {
                    accessKeyId = auth.accessKey
                    secretAccessKey = auth.secretKey
                }
            }
        }
    }
}
