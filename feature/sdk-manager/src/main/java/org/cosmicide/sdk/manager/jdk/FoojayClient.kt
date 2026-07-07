package org.cosmicide.sdk.manager.jdk

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Pragmatic, high-performance SDK client for the Foojay Disco API.
 */
class FoojayClient(private val httpClient: HttpClient = defaultClient) {

    enum class OS(val apiValue: String) {
        LINUX("linux"), MACOS("macos"), WINDOWS("windows");
        companion object { fun resolve(os: String) = when(os.lowercase()) { "macos", "mac", "osx" -> MACOS; "windows", "win" -> WINDOWS; else -> LINUX } }
    }

    enum class Arch(val apiValue: String) {
        X64("x64"), AARCH64("aarch64");
        companion object { fun resolve(arch: String) = when(arch.lowercase()) { "aarch64", "arm64" -> AARCH64; else -> X64 } }
    }

    enum class LibCType(val apiValue: String) {
        GLIBC("glibc"), MUSL("musl"), LIBC("libc");

        companion object {
            fun resolve(libc: String) = when (libc.lowercase()) {
                "musl" -> MUSL; "libc" -> LIBC; else -> GLIBC
            }
        }
    }

    data class Distribution(val name: String, val apiParam: String, val versions: List<String>)
    data class Artifact(val vendor: String, val exactVersion: String, val binaryUrl: String, val checksum: String, val filename: String)
    data class LatestVersions(val vendor: String, val latestGa: String?, val latestEa: String?)

    /**
     * Fetches all active vendor distributions, skipping unmaintained tracks.
     */
    suspend fun fetchMaintainedDistributions(): Result<List<Distribution>> = withContext(Dispatchers.IO) {
        runCatching {
            val response: DistrosResponse = httpClient.get("$BASE_URL/distributions").body()
            response.result
                .filter { it.maintained && it.buildOfOpenJDK }
                .map { Distribution(it.name, it.apiParameter, it.versions) }
        }
    }

    /**
     * Instantly grabs the exact tracking strings for both stable (GA) and early access (EA) builds.
     */
    suspend fun fetchLatestVersions(vendorParam: String, includeEa: Boolean = true): Result<LatestVersions> = withContext(Dispatchers.IO) {
        runCatching {
            val response: LatestVersionsResponse = httpClient.get("$BASE_URL/distributions/versions/latest") {
                parameter("distribution", vendorParam)
                parameter("include_ea", includeEa)
            }.body()

            val rawLatest = response.result.firstOrNull()
                ?: throw NoSuchElementException("No version metadata found for distribution: $vendorParam")

            LatestVersions(
                vendor = rawLatest.apiString,
                latestGa = rawLatest.latestGa,
                latestEa = rawLatest.latestEa
            )
        }
    }

    /**
     * Resolves the latest available artifact matching the technical requirements.
     */
    suspend fun resolveLatestArtifact(
        vendorParam: String,
        version: String,
        os: OS,
        arch: Arch,
        libCType: LibCType = LibCType.GLIBC
    ): Result<Artifact> = withContext(Dispatchers.IO) {
        runCatching {
            val packagesResponse: PackagesResponse = httpClient.get("$BASE_URL/packages/jdks") {
                parameter("distribution", vendorParam)
                parameter("version", version)
                parameter("operating_system", os.apiValue)
                parameter("architecture", arch.apiValue)
                parameter("latest", "available")
                parameter("lib_c_type", libCType.apiValue)
            }.body()

            val pkg = packagesResponse.result.firstOrNull()
                ?: throw NoSuchElementException("No package found matching: $vendorParam v$version ($os/$arch)")

            // Step 2: Query the detailed info uri directly to retrieve the checksum block payload
            val infoResponse: InfoResponse = httpClient.get(pkg.links.infoUri).body()
            val infoResult = infoResponse.result.firstOrNull()
                ?: throw NoSuchElementException("Failed to retrieve deep package download details from ${pkg.links.infoUri}")

            Artifact(
                vendor = pkg.distribution,
                exactVersion = pkg.javaVersion,
                binaryUrl = infoResult.directDownloadUri,
                checksum = infoResult.checksum,
                filename = infoResult.filename ?: pkg.filename ?: infoResult.directDownloadUri.substringAfterLast("/").substringBefore("?")
            )
        }
    }

    /**
     * Direct stream download with native Ktor progress reporting.
     */
    suspend fun downloadArtifactWithProgress(
        artifact: Artifact,
        destinationFile: File,
        onProgressUpdate: (progress: Int) -> Unit
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (destinationFile.exists()) destinationFile.delete()
            destinationFile.parentFile?.mkdirs()

            httpClient.prepareGet(artifact.binaryUrl) {
                timeout {
                    requestTimeoutMillis = 300_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 60_000
                }

                onDownload { bytesSentTotal, contentLength ->
                    val percentage = if (contentLength != null && contentLength > 0) {
                        ((bytesSentTotal * 100) / contentLength).toInt()
                    } else {
                        -1
                    }
                    onProgressUpdate(percentage)
                }
            }.execute { response ->
                if (response.status.value !in 200..299) return@execute false

                val fileChannel = destinationFile.writeChannel()
                try {
                    response.bodyAsChannel().copyTo(fileChannel)
                } finally {
                    fileChannel.flushAndClose()
                }
                true
            }
        }
    }

    @Serializable
    private data class DistrosResponse(@SerialName("result") val result: List<RawDistro>)

    @Serializable
    private data class RawDistro(
        val name: String,
        @SerialName("api_parameter") val apiParameter: String,
        val maintained: Boolean,
        @SerialName("build_of_openjdk") val buildOfOpenJDK: Boolean,
        @SerialName("build_of_graalvm") val buildOfGraalVM: Boolean,
        val versions: List<String> = emptyList()
    )

    @Serializable
    private data class LatestVersionsResponse(@SerialName("result") val result: List<RawLatestVersions>)

    @Serializable
    private data class RawLatestVersions(
        @SerialName("api_string") val apiString: String,
        @SerialName("latest_ga") val latestGa: String? = null,
        @SerialName("latest_ea") val latestEa: String? = null
    )

    @Serializable
    private data class PackagesResponse(@SerialName("result") val result: List<RawPackage>)

    @Serializable
    private data class RawPackage(
        @SerialName("java_version") val javaVersion: String,
        val distribution: String,
        val filename: String? = null,
        val links: RawLinks
    )

    @Serializable
    private data class RawLinks(
        @SerialName("pkg_info_uri") val infoUri: String
    )

    // New Schema payload layer mapping the exact response from pkg_info_uri directly
    @Serializable
    private data class InfoResponse(@SerialName("result") val result: List<RawPackageInfo>)

    @Serializable
    private data class RawPackageInfo(
        val filename: String? = null,
        @SerialName("direct_download_uri") val directDownloadUri: String,
        val checksum: String,
        @SerialName("checksum_type") val checksumType: String
    )

    companion object {
        private const val BASE_URL = "https://api.foojay.io/disco/v3.0"

        private val defaultClient by lazy {
            HttpClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; coerceInputValues = true; allowTrailingComma = true }) }
                install(HttpTimeout) { requestTimeoutMillis = 8000; connectTimeoutMillis = 4000 }
                defaultRequest { header("User-Agent", "Cosmic-IDE") }
            }
        }
    }
}