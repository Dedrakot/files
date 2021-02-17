package org.dedrakot

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.serialization.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dedrakot.api.Bucket
import org.dedrakot.api.BucketFactory
import org.dedrakot.api.Item
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.nio.file.Paths


fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(pathPrefix: String = System.getProperty("files.path"), testing: Boolean = false) {
    val di = filesDI(pathPrefix)
    val jsonMapper = Json {
        prettyPrint = true
        encodeDefaults = false
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
    install(ContentNegotiation) {
        json(jsonMapper)
    }
    install(AutoHeadResponse)

//    if (!testing) {
//        install(HttpsRedirect) {
//            sslPort = 443
//            permanentRedirect = true
//        }
//    }

    install(ShutDownUrl.ApplicationCallFeature) {
        // The URL that will be intercepted (you can also use the application.conf's ktor.deployment.shutdown.url key)
        shutDownUrl = "/shutdown"
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = { 0 } // ApplicationCall.() -> Int
    }

    val bucketFactory: BucketFactory by di.instance()
    val logger = LoggerFactory.getLogger("org.dedrakot.files")

    routing {
        route("/load") {
            routeToItem {
                get {
                    accessBucket(bucketFactory) { bucket, path ->
                        val item = bucket.get(path)
                        if (item.size == 0L) {
                            call.respond(HttpStatusCode.NoContent, "")
                        } else {
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    Paths.get(item.path).fileName.toString()
                                ).toString()
                            )
                            call.respond(ItemContent(item, HttpStatusCode.OK))
                        }
                    }
                }
            }
        }

        route("/item") {
            routeToItem {
                get {
                    accessBucket(bucketFactory) { bucket, path ->
                        val item = bucket.get(path)
                        if (item.size == 0L)
                            call.respond(HttpStatusCode.NoContent, "")
                        else
                            call.respond(ItemContent(item, HttpStatusCode.OK))
                    }
                }
                put {
                    accessBucket(bucketFactory) { bucket, path ->
                        val bytesWritten = bucket.writer(path)
                            .write { call.receiveStream().use { input -> input.copyToSuspend(this) } }
                        call.respond(HttpStatusCode.OK)
                        if (logger.isTraceEnabled) {
                            logger.trace("Bytes written: $bytesWritten for \"$path\"")
                        }
                    }
                }
            }
        }

        route("/list") {
            routeToItem {
                get {
                    accessBucket(bucketFactory) { bucket, path ->
                        val items = bucket.list(path)
                        call.respond(items)
                        if (logger.isTraceEnabled) {
                            logger.trace("Items list: " + jsonMapper.encodeToString(items))
                        }
                    }
                }
            }
        }
    }
}

class ItemContent(
    private val item: Item,
    override val status: HttpStatusCode? = null,
    //?: ContentType("text", "plain").withCharset(StandardCharsets.UTF_8)
    override val contentType: ContentType? = null
) : OutgoingContent.WriteChannelContent() {
    override val contentLength: Long
        get() = item.size

    private val writeBody: suspend OutputStream.() -> Unit = {
        item.open().use {
            it.copyTo(this)
        }
    }

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.toOutputStream().use { it.writeBody() }
    }
}

suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Long {
    return withContext(dispatcher) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}

private const val bucketName = "bucket"
private const val pathInBucket = "path"

fun Route.routeToItem(build: Route.() -> Unit): Route = route("/{${bucketName}}/{${pathInBucket}...}", build)

suspend fun PipelineContext<Unit, ApplicationCall>.accessBucket(
    bucketFactory: BucketFactory,
    build: suspend PipelineContext<Unit, ApplicationCall>.(bucket: Bucket, path: String) -> Unit
) {
    val bucket = call.parameters[bucketName]
    val path = call.parameters.getAll(pathInBucket)?.joinToString("/")
    if (bucket == null || path == null) {
        call.respond(HttpStatusCode.BadRequest)
    } else {
        try {
            build(this, bucketFactory.get(bucket), path)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "")
        }
    }
}