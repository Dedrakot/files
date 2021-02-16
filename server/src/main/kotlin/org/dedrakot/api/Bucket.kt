package org.dedrakot.api

import io.ktor.http.*
import java.io.InputStream
import java.io.OutputStream

interface Bucket {
    fun get(path: String): Item
    fun writer(path: String): ItemWriter
    fun list(path: String): List<ItemInfo>
}

interface BucketFactory {
    fun get(bucketName: String): Bucket
}

interface Item {
    val path: String

    /**
     * @return item size in bytes or 0L if item doesn't exists
     */
    val size: Long

    /**
     * Open item if exists, otherwise behavior is undefined
     */
    fun open(): InputStream
}

interface ItemInfo : PathInfo {
    val size: Long?
}

interface PathInfo {
    val name: String
}

interface ItemWriter {
    suspend fun write(updater: suspend OutputStream.() -> Long): Long
}