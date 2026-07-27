package org.cosmicide.tooling

import com.google.gson.Gson
import org.gradle.api.Action
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.CancellationToken
import org.gradle.tooling.ModelBuilder
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.TestLauncher
import org.gradle.tooling.TestSpec
import org.gradle.tooling.TestSpecs
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.test.TestOperationDescriptor
import org.gradle.tooling.model.Launchable
import org.gradle.tooling.model.Task
import org.gradle.tooling.model.TaskSelector
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import org.gradle.tooling.ProgressEvent as LegacyProgressEvent
import org.gradle.tooling.ProgressListener as LegacyProgressListener

private fun adaptLegacyProgressListener(listener: LegacyProgressListener): ProgressListener =
    object : ProgressListener {
        override fun statusChanged(event: ProgressEvent) {
            listener.statusChanged(
                object : LegacyProgressEvent {
                    override fun getDescription(): String = event.displayName
                }
            )
        }
    }

class RemoteModelBuilder<T>(
    private val server: ToolingServer,
    private val modelType: Class<T>
) : ModelBuilder<T> {

    private val state = OperationState()
    private val tasks = mutableListOf<String>()

    override fun forTasks(vararg tasks: String): ModelBuilder<T> =
        apply { this.tasks.addAll(tasks) }

    override fun forTasks(tasks: MutableIterable<String>): ModelBuilder<T> =
        apply { this.tasks.addAll(tasks) }

    override fun withArguments(vararg arguments: String): ModelBuilder<T> =
        apply { state.arguments.addAll(arguments) }

    override fun withArguments(arguments: MutableIterable<String>): ModelBuilder<T> =
        apply { state.arguments.addAll(arguments) }

    override fun addArguments(vararg arguments: String): ModelBuilder<T> =
        apply { state.arguments.addAll(arguments) }

    override fun addArguments(arguments: MutableIterable<String>): ModelBuilder<T> =
        apply { state.arguments.addAll(arguments) }

    override fun setStandardOutput(outputStream: OutputStream): ModelBuilder<T> =
        apply { state.stdout = outputStream }

    override fun setStandardError(outputStream: OutputStream): ModelBuilder<T> =
        apply { state.stderr = outputStream }

    override fun setStandardInput(inputStream: InputStream): ModelBuilder<T> =
        apply { state.stdin = inputStream }

    override fun setColorOutput(colorOutput: Boolean): ModelBuilder<T> =
        apply { state.colorOutput = colorOutput }

    override fun setJavaHome(javaHome: File): ModelBuilder<T> =
        apply { state.javaHome = javaHome }

    override fun setJvmArguments(vararg jvmArguments: String): ModelBuilder<T> =
        apply {
            state.jvmArguments.clear()
            state.jvmArguments.addAll(jvmArguments)
        }

    override fun setJvmArguments(jvmArguments: MutableIterable<String>): ModelBuilder<T> =
        apply {
            state.jvmArguments.clear()
            state.jvmArguments.addAll(jvmArguments)
        }

    override fun addJvmArguments(vararg jvmArguments: String): ModelBuilder<T> =
        apply { state.jvmArguments.addAll(jvmArguments) }

    override fun addJvmArguments(jvmArguments: MutableIterable<String>): ModelBuilder<T> =
        apply { state.jvmArguments.addAll(jvmArguments) }

    override fun setEnvironmentVariables(envVariables: MutableMap<String, String>): ModelBuilder<T> =
        apply { state.environmentVariables.putAll(envVariables) }

    override fun withSystemProperties(systemProperties: MutableMap<String, String>): ModelBuilder<T> =
        apply { state.systemProperties.putAll(systemProperties) }

    override fun withDetailedFailure(): ModelBuilder<T> =
        apply { state.detailedFailure = true }

    override fun withCancellationToken(cancellationToken: CancellationToken): ModelBuilder<T> =
        apply { state.cancellationToken = cancellationToken }

    override fun addProgressListener(listener: ProgressListener): ModelBuilder<T> =
        apply { state.addProgressListener(listener) }

    override fun addProgressListener(
        listener: ProgressListener,
        eventTypes: MutableSet<OperationType>
    ): ModelBuilder<T> = apply { state.addProgressListener(listener, eventTypes) }

    override fun addProgressListener(
        listener: ProgressListener,
        vararg operationTypes: OperationType
    ): ModelBuilder<T> = apply {
        state.addProgressListener(listener, operationTypes.asList())
    }

    override fun addProgressListener(listener: LegacyProgressListener): ModelBuilder<T> =
        apply { state.addProgressListener(adaptLegacyProgressListener(listener)) }

    override fun get(): T {
        val latch = CountDownLatch(1)
        var result: T? = null
        var error: Throwable? = null

        get(
            resultHandler(
                onComplete = {
                    result = it
                    latch.countDown()
                },
                onFailure = {
                    error = it
                    latch.countDown()
                }
            )
        )

        latch.await()
        error?.let { throw it }
        return result!!
    }

    override fun get(handler: ResultHandler<in T>) {
        val opId = newOpId()
        val params = state.toParams().apply {
            addProperty("opId", opId)
            addProperty("modelType", modelType.name)
            if (tasks.isNotEmpty()) add("tasks", Gson().toJsonTree(tasks))
        }

        wireStreams(server, opId, state)
        val finishCancellationWatch = wireCancellation(server, opId, state.cancellationToken)

        server.request("gradle/model", params) { result, error ->
            finishCancellationWatch()
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
}

// ---------------------------------------------------------------------------------
// BuildLauncher  (connection.newBuild())
// ---------------------------------------------------------------------------------

class RemoteBuildLauncher(private val server: ToolingServer) : BuildLauncher {

    private val state = OperationState()
    private val tasks = mutableListOf<String>()

    override fun forTasks(vararg tasks: String): BuildLauncher =
        apply { this.tasks.addAll(tasks) }

    override fun forTasks(vararg tasks: Task): BuildLauncher =
        apply { this.tasks.addAll(tasks.map { it.path }) }

    override fun forTasks(tasks: MutableIterable<Task>): BuildLauncher =
        apply { this.tasks.addAll(tasks.map { it.path }) }

    override fun forLaunchables(vararg launchables: Launchable): BuildLauncher =
        apply { addLaunchables(launchables.asIterable()) }

    override fun forLaunchables(launchables: MutableIterable<Launchable>): BuildLauncher =
        apply { addLaunchables(launchables) }

    private fun addLaunchables(launchables: Iterable<Launchable>) {
        launchables.forEach { launchable ->
            when (launchable) {
                is Task -> tasks.add(launchable.path)
                is TaskSelector -> tasks.add(launchable.name)
                else -> throw UnsupportedOperationException(
                    "Unsupported launchable type: ${launchable.javaClass.name}"
                )
            }
        }
    }

    override fun withArguments(vararg arguments: String): BuildLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun withArguments(arguments: MutableIterable<String>): BuildLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun addArguments(vararg arguments: String): BuildLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun addArguments(arguments: MutableIterable<String>): BuildLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun setStandardOutput(outputStream: OutputStream): BuildLauncher =
        apply { state.stdout = outputStream }

    override fun setStandardError(outputStream: OutputStream): BuildLauncher =
        apply { state.stderr = outputStream }

    override fun setStandardInput(inputStream: InputStream): BuildLauncher =
        apply { state.stdin = inputStream }

    override fun setColorOutput(colorOutput: Boolean): BuildLauncher =
        apply { state.colorOutput = colorOutput }

    override fun setJavaHome(javaHome: File): BuildLauncher =
        apply { state.javaHome = javaHome }

    override fun setJvmArguments(vararg jvmArguments: String): BuildLauncher =
        apply {
            state.jvmArguments.clear()
            state.jvmArguments.addAll(jvmArguments)
        }

    override fun setJvmArguments(jvmArguments: MutableIterable<String>): BuildLauncher =
        apply {
            state.jvmArguments.clear()
            state.jvmArguments.addAll(jvmArguments)
        }

    override fun addJvmArguments(vararg jvmArguments: String): BuildLauncher =
        apply { state.jvmArguments.addAll(jvmArguments) }

    override fun addJvmArguments(jvmArguments: MutableIterable<String>): BuildLauncher =
        apply { state.jvmArguments.addAll(jvmArguments) }

    override fun setEnvironmentVariables(envVariables: MutableMap<String, String>): BuildLauncher =
        apply { state.environmentVariables.putAll(envVariables) }

    override fun withSystemProperties(systemProperties: MutableMap<String, String>): BuildLauncher =
        apply { state.systemProperties.putAll(systemProperties) }

    override fun withDetailedFailure(): BuildLauncher =
        apply { state.detailedFailure = true }

    override fun withCancellationToken(cancellationToken: CancellationToken): BuildLauncher =
        apply { state.cancellationToken = cancellationToken }

    override fun addProgressListener(listener: ProgressListener): BuildLauncher =
        apply { state.addProgressListener(listener) }

    override fun addProgressListener(
        listener: ProgressListener,
        eventTypes: MutableSet<OperationType>
    ): BuildLauncher = apply { state.addProgressListener(listener, eventTypes) }

    override fun addProgressListener(
        listener: ProgressListener,
        vararg operationTypes: OperationType
    ): BuildLauncher = apply {
        state.addProgressListener(listener, operationTypes.asList())
    }

    override fun addProgressListener(listener: LegacyProgressListener): BuildLauncher =
        apply { state.addProgressListener(adaptLegacyProgressListener(listener)) }

    override fun run() {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        run(
            voidResultHandler(
                onComplete = latch::countDown,
                onFailure = {
                    error = it
                    latch.countDown()
                }
            )
        )

        latch.await()
        error?.let { throw it }
    }

    override fun run(handler: ResultHandler<in Void>) {
        val opId = newOpId()
        val params = state.toParams().apply {
            addProperty("opId", opId)
            if (tasks.isNotEmpty()) add("tasks", Gson().toJsonTree(tasks))
        }

        wireStreams(server, opId, state)
        val finishCancellationWatch = wireCancellation(server, opId, state.cancellationToken)

        server.request("gradle/run", params) { _, error ->
            finishCancellationWatch()
            if (error != null) handler.onFailure(wrapAsConnectionException(error))
            else handler.onComplete(null)
        }
    }
}

// ---------------------------------------------------------------------------------
// TestLauncher  (connection.newTestLauncher())
// ---------------------------------------------------------------------------------

class RemoteTestLauncher(private val server: ToolingServer) : TestLauncher {

    private val state = OperationState()
    private val testClasses = mutableListOf<String>()
    private val testMethods = mutableMapOf<String, MutableList<String>>()
    private val tasks = mutableListOf<String>()
    private val descriptors = mutableListOf<TestDescriptorData>()
    private val taskTestClasses = mutableMapOf<String, MutableList<String>>()
    private val taskTestMethods = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
    private val testSpecs = mutableListOf<TestSpecData>()
    private var debugPort: Int? = null

    override fun withTests(vararg descriptors: TestOperationDescriptor): TestLauncher =
        apply { addDescriptors(descriptors.asIterable()) }

    override fun withTests(
        descriptors: MutableIterable<TestOperationDescriptor>
    ): TestLauncher = apply { addDescriptors(descriptors) }

    private fun addDescriptors(descriptors: Iterable<TestOperationDescriptor>) {
        this.descriptors.addAll(
            descriptors.map {
                TestDescriptorData(
                    name = it.name,
                    displayName = it.displayName
                )
            }
        )
    }

    override fun withJvmTestClasses(vararg testClasses: String): TestLauncher =
        apply { this.testClasses.addAll(testClasses) }

    override fun withJvmTestClasses(testClasses: MutableIterable<String>): TestLauncher =
        apply { this.testClasses.addAll(testClasses) }

    override fun withJvmTestMethods(testClass: String, vararg methods: String): TestLauncher =
        apply { testMethods.getOrPut(testClass) { mutableListOf() }.addAll(methods) }

    override fun withJvmTestMethods(
        testClass: String,
        methods: MutableIterable<String>
    ): TestLauncher = apply {
        testMethods.getOrPut(testClass) { mutableListOf() }.addAll(methods)
    }

    override fun withTaskAndTestClasses(
        task: String,
        testClasses: MutableIterable<String>
    ): TestLauncher = apply {
        taskTestClasses.getOrPut(task) { mutableListOf() }.addAll(testClasses)
    }

    override fun withTaskAndTestMethods(
        task: String,
        testClass: String,
        methods: MutableIterable<String>
    ): TestLauncher = apply {
        taskTestMethods
            .getOrPut(task) { mutableMapOf() }
            .getOrPut(testClass) { mutableListOf() }
            .addAll(methods)
    }

    override fun debugTestsOn(port: Int): TestLauncher =
        apply { debugPort = port }

    override fun withTestsFor(testSpec: Action<TestSpecs>): TestLauncher = apply {
        testSpec.execute(RecordingTestSpecs(testSpecs))
    }

    override fun forTasks(vararg tasks: String): TestLauncher =
        apply { this.tasks.addAll(tasks) }

    override fun withArguments(vararg arguments: String): TestLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun withArguments(arguments: MutableIterable<String>): TestLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun addArguments(vararg arguments: String): TestLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun addArguments(arguments: MutableIterable<String>): TestLauncher =
        apply { state.arguments.addAll(arguments) }

    override fun setStandardOutput(outputStream: OutputStream): TestLauncher =
        apply { state.stdout = outputStream }

    override fun setStandardError(outputStream: OutputStream): TestLauncher =
        apply { state.stderr = outputStream }

    override fun setStandardInput(inputStream: InputStream): TestLauncher =
        apply { state.stdin = inputStream }

    override fun setColorOutput(colorOutput: Boolean): TestLauncher =
        apply { state.colorOutput = colorOutput }

    override fun setJavaHome(javaHome: File): TestLauncher =
        apply { state.javaHome = javaHome }

    override fun setJvmArguments(vararg jvmArguments: String): TestLauncher =
        apply {
            state.jvmArguments.clear()
            state.jvmArguments.addAll(jvmArguments)
        }

    override fun setJvmArguments(jvmArguments: MutableIterable<String>): TestLauncher =
        apply {
            state.jvmArguments.clear()
            state.jvmArguments.addAll(jvmArguments)
        }

    override fun addJvmArguments(vararg jvmArguments: String): TestLauncher =
        apply { state.jvmArguments.addAll(jvmArguments) }

    override fun addJvmArguments(jvmArguments: MutableIterable<String>): TestLauncher =
        apply { state.jvmArguments.addAll(jvmArguments) }

    override fun setEnvironmentVariables(envVariables: MutableMap<String, String>): TestLauncher =
        apply { state.environmentVariables.putAll(envVariables) }

    override fun withSystemProperties(systemProperties: MutableMap<String, String>): TestLauncher =
        apply { state.systemProperties.putAll(systemProperties) }

    override fun withDetailedFailure(): TestLauncher =
        apply { state.detailedFailure = true }

    override fun withCancellationToken(cancellationToken: CancellationToken): TestLauncher =
        apply { state.cancellationToken = cancellationToken }

    override fun addProgressListener(listener: ProgressListener): TestLauncher =
        apply { state.addProgressListener(listener) }

    override fun addProgressListener(
        listener: ProgressListener,
        eventTypes: MutableSet<OperationType>
    ): TestLauncher = apply { state.addProgressListener(listener, eventTypes) }

    override fun addProgressListener(
        listener: ProgressListener,
        vararg operationTypes: OperationType
    ): TestLauncher = apply {
        state.addProgressListener(listener, operationTypes.asList())
    }

    override fun addProgressListener(listener: LegacyProgressListener): TestLauncher =
        apply { state.addProgressListener(adaptLegacyProgressListener(listener)) }

    override fun run() {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        run(
            voidResultHandler(
                onComplete = latch::countDown,
                onFailure = {
                    error = it
                    latch.countDown()
                }
            )
        )

        latch.await()
        error?.let { throw it }
    }

    override fun run(handler: ResultHandler<in Void>) {
        val opId = newOpId()
        val params = state.toParams().apply {
            addProperty("opId", opId)

            if (tasks.isNotEmpty()) add("tasks", Gson().toJsonTree(tasks))
            if (testClasses.isNotEmpty()) add("testClasses", Gson().toJsonTree(testClasses))
            if (testMethods.isNotEmpty()) add("testMethods", Gson().toJsonTree(testMethods))
            if (descriptors.isNotEmpty()) add("testDescriptors", Gson().toJsonTree(descriptors))
            if (taskTestClasses.isNotEmpty()) add(
                "taskTestClasses",
                Gson().toJsonTree(taskTestClasses)
            )
            if (taskTestMethods.isNotEmpty()) add(
                "taskTestMethods",
                Gson().toJsonTree(taskTestMethods)
            )
            if (testSpecs.isNotEmpty()) add("testSpecs", Gson().toJsonTree(testSpecs))
            debugPort?.let { addProperty("debugPort", it) }
        }

        wireStreams(server, opId, state)
        val finishCancellationWatch = wireCancellation(server, opId, state.cancellationToken)

        server.request("gradle/test", params) { _, error ->
            finishCancellationWatch()
            if (error != null) handler.onFailure(wrapAsConnectionException(error))
            else handler.onComplete(null)
        }
    }
}

private data class TestDescriptorData(
    val name: String,
    val displayName: String
)

private data class TestSpecData(
    val taskPath: String,
    val packages: MutableList<String> = mutableListOf(),
    val classes: MutableList<String> = mutableListOf(),
    val methods: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val patterns: MutableList<String> = mutableListOf()
)

private class RecordingTestSpecs(
    private val specs: MutableList<TestSpecData>
) : TestSpecs {

    override fun forTaskPath(taskPath: String): TestSpec {
        val data = TestSpecData(taskPath)
        specs.add(data)
        return RecordingTestSpec(data)
    }
}

private class RecordingTestSpec(
    private val data: TestSpecData
) : TestSpec {

    override fun includePackage(pkg: String): TestSpec =
        apply { data.packages.add(pkg) }

    override fun includePackages(packages: MutableCollection<String>): TestSpec =
        apply { data.packages.addAll(packages) }

    override fun includeClass(cls: String): TestSpec =
        apply { data.classes.add(cls) }

    override fun includeClasses(classes: MutableCollection<String>): TestSpec =
        apply { data.classes.addAll(classes) }

    override fun includeMethod(cls: String, method: String): TestSpec =
        apply { data.methods.getOrPut(cls) { mutableListOf() }.add(method) }

    override fun includeMethods(cls: String, methods: MutableCollection<String>): TestSpec =
        apply { data.methods.getOrPut(cls) { mutableListOf() }.addAll(methods) }

    override fun includePattern(pattern: String): TestSpec =
        apply { data.patterns.add(pattern) }

    override fun includePatterns(patterns: MutableCollection<String>): TestSpec =
        apply { data.patterns.addAll(patterns) }
}
