package org.dedrakot

import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilesTest {

    @Disabled
    @Test
    fun concurrentWrite() = withTestApplication({
        module(pathPrefix,true)
    }) {
        runBlocking {
            val p1 = Paths.get("$pathPrefix/test1.txt")
            val p2 = Paths.get("$pathPrefix/test2.txt")
            val bytes1 = Files.readAllBytes(p1)
            val bytes2 = Files.readAllBytes(p2)
            val resultPath = "test/result.txt"
            val c1 = launch {
                with(handleRequest(HttpMethod.Put, "/item/$resultPath") {
                    setBody(bytes1)
                }) {
                    assertEquals(HttpStatusCode.OK, response.status())
                }
            }
            val c2 = launch {
                with(handleRequest(HttpMethod.Put, "/item/$resultPath") {
                    setBody(bytes2)
                }) {
                    assertEquals(HttpStatusCode.OK, response.status())
                }
            }
            c1.join()
            c2.join()
            val result = Files.readAllBytes(Paths.get("$pathPrefix/$resultPath"))
            assertTrue(Arrays.equals(result, bytes1) || Arrays.equals(result,bytes2))
        }
    }

    companion object {
        const val pathPrefix = "../tmp"
    }
}