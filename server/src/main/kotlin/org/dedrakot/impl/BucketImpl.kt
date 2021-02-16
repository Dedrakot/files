package org.dedrakot.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dedrakot.api.*
import java.io.*
import java.nio.file.*
import java.util.*
import kotlin.streams.toList
import kotlinx.serialization.Serializable
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class BucketImpl(pathPrefix: String) : Bucket {
    private val pathPrefix: String = withSlash(pathPrefix)

    private fun fsPath(path: String): String {
        if (path.indexOf("..") != -1)
            throw IllegalArgumentException("Relative paths are forbidden")
        return pathPrefix + path
    }

    override fun get(path: String): Item {
        val f = File(fsPath(path))
        if (Files.isSymbolicLink(f.toPath())) {
            return EmptyItem(f.path)
        }
        return FileItem(f)
    }

    override fun writer(path: String): ItemWriter = ItemWriterImpl(fsPath(path))

    override fun list(path: String): List<ItemInfo> {
        val p = Paths.get(fsPath(path))
        return if (Files.isDirectory(p)) listDir(p) else Collections.emptyList();
    }

    companion object Util {
        fun listDir(p: Path): List<ItemInfo> =
            Files.list(p).map {
                val name = it.fileName.toString()
                if (Files.isDirectory(it))
                    ItemInfoImpl(name)
                else
                    ItemInfoImpl(name, Files.size(it))
            }.toList()
    }
}

class ItemWriterImpl(val path: String) : ItemWriter {
    override suspend fun write(updater: suspend OutputStream.() -> Long) = withContext(Dispatchers.IO) {
        FileOutputStream(path).buffered().use { it.updater() }
    }
}

@Serializable
class PathInfoImpl(override val name: String) : PathInfo

@Serializable
class ItemInfoImpl(override val name: String, override val size: Long? = null) : ItemInfo

class FileItem(private val f: File) : Item {
    override val path: String get() = f.path
    override val size: Long get() = f.length()
    override fun open(): InputStream = FileInputStream(f).buffered()
}

class EmptyItem(override val path: String) : Item {
    override val size: Long get() = 0L
    override fun open(): InputStream {
        throw IllegalStateException("Can't open stream for an empty object")
    }
}

class BucketFactoryImpl(pathPrefix: String) : BucketFactory {
    private val pathPrefix: String = withSlash(pathPrefix)

    override fun get(bucketName: String): Bucket {
        return BucketImpl("$pathPrefix$bucketName/")
    }

}

private fun withSlash(pathPrefix: String) = if (pathPrefix.endsWith('/')) pathPrefix else "$pathPrefix/"