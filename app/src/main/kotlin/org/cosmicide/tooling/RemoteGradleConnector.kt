package org.cosmicide.tooling

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildActionExecuter
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ModelBuilder
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.TestLauncher
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.CountDownLatch

class RemoteGradleConnector(private val context: Context) : GradleConnector() {
    private var projectDir: File? = null
    private var gradleUserHome: File? = null
    private var gradleVersion: String? = null
    private var gradleInstallation: File? = null
    private var gradleDistributionUri: URI? = null // not yet wired server-side, see note below

    override fun forProjectDirectory(dir: File): GradleConnector = apply { projectDir = dir }
    override fun useGradleVersion(version: String): GradleConnector =
        apply { gradleVersion = version; gradleInstallation = null }

    override fun useInstallation(home: File): GradleConnector =
        apply { gradleInstallation = home; gradleVersion = null }

    override fun useGradleUserHomeDir(dir: File): GradleConnector = apply { gradleUserHome = dir }
    override fun useBuildDistribution(): GradleConnector =
        apply { gradleVersion = null; gradleInstallation = null; gradleDistributionUri = null }

    // Fixed: must actually return GradleConnector (was missing a return statement / wrong param nullability)
    override fun useDistribution(gradleDistribution: URI): GradleConnector = apply {
        gradleVersion = null
        gradleInstallation = null
        gradleDistributionUri = gradleDistribution
        // TODO: thread gradleDistributionUri through ConnectionKey.fromStartupArgs / Main.java
        // (connector.useDistribution(URI)) the same way gradleVersion/gradleInstallation already are,
        // if you need custom-distribution-URL support.
    }

    override fun connect(): ProjectConnection {
        val dir =
            projectDir ?: throw IllegalStateException("forProjectDirectory(...) was not called")
        val server = ToolingServerManager.forProject(context, dir)

        var startError: Throwable? = null
        val latch = CountDownLatch(1)
        server.start(
            onReady = { latch.countDown() },
            onError = { startError = it; latch.countDown() })
        latch.await()
        startError?.let { throw GradleConnectionException("Failed to start tooling server", it) }
        return RemoteProjectConnection(server)
    }

    override fun disconnect() {
        ToolingServerManager.stopCurrent()
    }
}

class RemoteProjectConnection(private val server: ToolingServer) : ProjectConnection {

    override fun <T> getModel(modelType: Class<T>): T {
        val latch = CountDownLatch(1)
        var result: T? = null
        var error: Throwable? = null
        // Fixed: ResultHandler has 2 abstract methods, can't be a lambda -- build it via resultHandler(...)
        getModel(
            modelType, resultHandler(
            onComplete = { result = it; latch.countDown() },
            onFailure = { error = it; latch.countDown() }
        ))
        latch.await()
        error?.let { throw it }
        return result!!
    }

    override fun <T> getModel(modelType: Class<T>, handler: ResultHandler<in T>) {
        val params = JsonObject().apply { addProperty("modelType", modelType.name) }
        server.request("gradle/model", params) { result, error ->
            if (error != null) {
                handler.onFailure(wrapAsConnectionException(error))
                return@request
            }
            try {
                val modelJson = result?.asJsonObject?.get("model")
                @Suppress("UNCHECKED_CAST")
                handler.onComplete(ModelProxyFactory.wrap(modelJson, modelType) as T)
            } catch (t: Throwable) {
                handler.onFailure(wrapAsConnectionException(t))
            }
        }
    }

    override fun <T> model(modelType: Class<T>): ModelBuilder<T> =
        RemoteModelBuilder(server, modelType)

    override fun newBuild(): BuildLauncher = RemoteBuildLauncher(server)
    override fun newTestLauncher(): TestLauncher = RemoteTestLauncher(server)

    override fun notifyDaemonsAboutChangedPaths(paths: MutableList<Path>) {
        val json =
            JsonObject().apply { add("paths", Gson().toJsonTree(paths.map { it.toString() })) }
        server.request("gradle/notifyChanged", json) { _, _ -> }
    }

    override fun close() {
        server.closeProject { _, _ -> }
    }

    // Both action() overloads: no honest way to ship arbitrary BuildAction bytecode over
    // this wire (see earlier discussion) -- add named, pre-compiled actions server-side instead.
    override fun <T> action(buildAction: BuildAction<T>): BuildActionExecuter<T> =
        throw UnsupportedOperationException(
            "Custom BuildAction can't cross the wire. Add a named action to the server's registry instead."
        )

    override fun action(): BuildActionExecuter.Builder =
        throw UnsupportedOperationException(
            "Custom BuildAction can't cross the wire. Add a named action to the server's registry instead."
        )
}