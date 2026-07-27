package org.cosmicide.sdk.manager.jdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FoojayClientTest {
    @Test
    fun `platform aliases resolve to Disco API values`() {
        assertEquals(FoojayClient.OS.MACOS, FoojayClient.OS.resolve("OSX"))
        assertEquals(FoojayClient.OS.WINDOWS, FoojayClient.OS.resolve("win"))
        assertEquals(FoojayClient.OS.LINUX, FoojayClient.OS.resolve("unknown"))
        assertEquals(FoojayClient.Arch.AARCH64, FoojayClient.Arch.resolve("arm64"))
        assertEquals(FoojayClient.Arch.X64, FoojayClient.Arch.resolve("amd64"))
        assertEquals(FoojayClient.LibCType.MUSL, FoojayClient.LibCType.resolve("musl"))
        assertEquals(FoojayClient.LibCType.GLIBC, FoojayClient.LibCType.resolve("gnu"))
    }

    @Test
    fun `maintained distributions exclude inactive and non OpenJDK builds`() = runBlocking {
        val client = clientResponding(
            """
            {"result":[
              {"name":"Temurin","api_parameter":"temurin","maintained":true,"build_of_openjdk":true,"build_of_graalvm":false,"versions":["21","25"]},
              {"name":"Old","api_parameter":"old","maintained":false,"build_of_openjdk":true,"build_of_graalvm":false},
              {"name":"Graal","api_parameter":"graal","maintained":true,"build_of_openjdk":false,"build_of_graalvm":true}
            ]}
            """.trimIndent()
        )

        val result = FoojayClient(client).fetchMaintainedDistributions().getOrThrow()

        assertEquals(1, result.size)
        assertEquals("Temurin", result.single().name)
        assertEquals(listOf("21", "25"), result.single().versions)
        client.close()
    }

    @Test
    fun `latest version request sends vendor and early access flags`() = runBlocking {
        val engine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/distributions/versions/latest"))
            assertEquals("semeru", request.url.parameters["distribution"])
            assertEquals("false", request.url.parameters["include_ea"])
            jsonResponse(
                """{"result":[{"api_string":"semeru","latest_ga":"21.0.8","latest_ea":null}]}"""
            )
        }
        val httpClient = configuredClient(engine)

        val latest = FoojayClient(httpClient).fetchLatestVersions("semeru", false).getOrThrow()

        assertEquals("semeru", latest.vendor)
        assertEquals("21.0.8", latest.latestGa)
        assertEquals(null, latest.latestEa)
        httpClient.close()
    }

    @Test
    fun `artifact resolution follows info link and maps checksum details`() = runBlocking {
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            when (requests) {
                1 -> {
                    assertTrue(request.url.encodedPath.endsWith("/packages/jdks"))
                    assertEquals("temurin", request.url.parameters["distribution"])
                    assertEquals("21", request.url.parameters["version"])
                    assertEquals("linux", request.url.parameters["operating_system"])
                    assertEquals("aarch64", request.url.parameters["architecture"])
                    assertEquals("glibc", request.url.parameters["lib_c_type"])
                    assertEquals("tar.gz", request.url.parameters["archive_type"])
                    jsonResponse(
                        """{"result":[{"java_version":"21.0.8+9","distribution":"temurin","filename":"fallback.tar.gz","links":{"pkg_info_uri":"https://details.test/package"}}]}"""
                    )
                }

                else -> {
                    assertEquals("details.test", request.url.host)
                    jsonResponse(
                        """{"result":[{"filename":"jdk.tar.gz","direct_download_uri":"https://download.test/jdk.tar.gz","checksum":"abc123","checksum_type":"sha256"}]}"""
                    )
                }
            }
        }
        val httpClient = configuredClient(engine)

        val artifact = FoojayClient(httpClient).resolveLatestArtifact(
            "temurin",
            "21",
            FoojayClient.OS.LINUX,
            FoojayClient.Arch.AARCH64
        ).getOrThrow()

        assertEquals(2, requests)
        assertEquals("temurin", artifact.vendor)
        assertEquals("21.0.8+9", artifact.exactVersion)
        assertEquals("jdk.tar.gz", artifact.filename)
        assertEquals("abc123", artifact.checksum)
        httpClient.close()
    }

    @Test
    fun `empty API results retain useful failure context`() = runBlocking {
        val client = clientResponding("""{"result":[]}""")

        val latest = FoojayClient(client).fetchLatestVersions("missing").exceptionOrNull()
        val artifact = FoojayClient(client).resolveLatestArtifact(
            "missing",
            "99",
            FoojayClient.OS.LINUX,
            FoojayClient.Arch.X64
        ).exceptionOrNull()

        assertTrue(latest is NoSuchElementException)
        assertTrue(latest?.message.orEmpty().contains("missing"))
        assertTrue(artifact is NoSuchElementException)
        assertTrue(artifact?.message.orEmpty().contains("missing v99"))
        client.close()
    }

    @Test
    fun `download writes successful response and preserves progress contract`() = runBlocking {
        val payload = "jdk archive".toByteArray()
        val engine = MockEngine {
            respond(
                content = payload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, payload.size.toString())
            )
        }
        val httpClient = configuredClient(engine)
        val root = Files.createTempDirectory("cosmic-sdk-download").toFile()
        try {
            val destination = root.resolve("nested/jdk.tar.gz")
            val progress = mutableListOf<Int>()

            val downloaded = FoojayClient(httpClient).downloadArtifactWithProgress(
                artifact = FoojayClient.Artifact(
                    "temurin", "21", "https://download.test/jdk", "sum", "jdk.tar.gz"
                ),
                destinationFile = destination,
                onProgressUpdate = progress::add
            ).getOrThrow()

            assertTrue(downloaded)
            assertEquals("jdk archive", destination.readText())
            assertTrue(progress.isEmpty() || progress.last() == 100)
        } finally {
            httpClient.close()
            root.deleteRecursively()
        }
    }

    @Test
    fun `failed download removes stale destination and returns false`() = runBlocking {
        val httpClient = configuredClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        val destination = Files.createTempFile("cosmic-stale-sdk", ".zip").toFile().apply {
            writeText("stale")
        }
        try {
            val downloaded = FoojayClient(httpClient).downloadArtifactWithProgress(
                FoojayClient.Artifact("vendor", "1", "https://download.test/missing", "", "x"),
                destination
            ) {}.getOrThrow()

            assertFalse(downloaded)
            assertFalse(destination.exists())
        } finally {
            httpClient.close()
            destination.delete()
        }
    }

    private fun clientResponding(json: String): HttpClient = configuredClient(
        MockEngine { jsonResponse(json) }
    )

    private fun configuredClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
    }

    private fun MockRequestHandleScope.jsonResponse(json: String) = respond(
        content = json,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}
