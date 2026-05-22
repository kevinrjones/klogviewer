package com.klogviewer.domain.repository

interface LocalFileSystem {
    fun exists(path: String): Boolean
    fun isFile(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun listFiles(path: String, filters: List<String>): List<String>
    fun readLines(path: String, limit: Int): List<String>
    fun getName(path: String): String
}
