package org.cosmicide.exec.linux

import android.content.Context
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.lang.reflect.Field
import android.os.Process as AndroidProcess

object LinuxProcessRunner {

    private const val RUNTIME_USER = "cosmicide"
    private const val JSTAT_SAMPLE_INTERVAL_MS = 1000

    private val DefaultDnsServers = listOf("1.1.1.1", "8.8.8.8")

    data class Configuration(
        val binary: File,
        val arguments: List<String>,
        val workingDir: File,
        val environmentOverrides: Map<String, String> = emptyMap(),
        val pathEntries: List<File> = emptyList()
    )

    private data class GlibcRuntime(
        val tempDir: File,
        val appDir: File,
        val glibcPath: String,
        val customLinker: String,
        val combinedPreload: String,
        val runtimeUser: String,
        val resolvConf: File,
        val hostsFile: File,
        val nsswitchConf: File,
        val gaiConf: File,
        val passwdFile: File,
        val groupFile: File
    )

    /**
     * Spawns an arbitrary GNU/Linux binary inside the custom glibc environment layer.
     */
    fun start(context: Context, config: Configuration): Process {
        return createProcessBuilder(context, config).start()
    }

    fun execute(
        context: Context,
        config: Configuration,
        onOutputReceived: (String) -> Unit,
        onProcessStarted: (Process) -> Unit
    ) {
        val process = start(context, config)
        onProcessStarted(process)

        process.inputStream.bufferedReader().use { reader ->
            val buffer = CharArray(1024)
            var readCount: Int
            while (reader.read(buffer).also { readCount = it } != -1) {
                onOutputReceived(String(buffer, 0, readCount))
            }
        }

        val exitCode = process.waitFor()
        onOutputReceived("\n--- Process finished with exit code $exitCode ---")
    }

    fun toolchainPathEntries(context: Context, jdkDir: File): List<File> {
        return buildList {
            add(jdkDir.resolve("bin"))
            if (FileUtil.isInitialized) {
                add(FileUtil.dataDir.resolve("kotlinc/bin"))
            }
            add(context.filesDir.resolve("jdtls/bin"))
        }
    }

    fun toolchainEnvironment(context: Context, jdkDir: File): Map<String, String> {
        return buildMap {
            put("JAVA_HOME", jdkDir.absolutePath)
            if (FileUtil.isInitialized) {
                put("KOTLIN_HOME", FileUtil.dataDir.resolve("kotlinc").absolutePath)
            }
            put("JDTLS_HOME", context.filesDir.resolve("jdtls").absolutePath)
        }
    }

    fun parseCommandLine(commandLine: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false

        commandLine.forEach { char ->
            when {
                escaping -> {
                    current.append(char)
                    escaping = false
                }

                char == '\\' -> escaping = true

                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        current.append(char)
                    }
                }

                char == '\'' || char == '"' -> quote = char

                char.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }

                else -> current.append(char)
            }
        }

        if (escaping) current.append('\\')
        if (quote != null) {
            throw IllegalArgumentException("Unclosed quote in command")
        }
        if (current.isNotEmpty()) result.add(current.toString())

        return result
    }

    fun resolveExecutable(commandName: String, workingDir: File, pathEntries: List<File>): File {
        val commandFile = File(commandName)

        if (commandFile.isAbsolute) return commandFile

        if (commandName.contains(File.separatorChar)) {
            return workingDir.resolve(commandName).canonicalFile
        }

        return pathEntries
            .plus(File("/system/bin"))
            .asSequence()
            .map { it.resolve(commandName) }
            .firstOrNull { it.isFile }
            ?: throw IllegalArgumentException("Command not found: $commandName")
    }

    fun startJstatGcSampler(
        context: Context,
        jdkDir: File,
        option: String = "-gccause",
        pid: Int,
        workingDir: File
    ): Process {
        return startJstatSampler(
            context = context,
            jdkDir = jdkDir,
            option = option,
            pid = pid,
            workingDir = workingDir
        )
    }

    fun startJstatClassSampler(
        context: Context,
        jdkDir: File,
        pid: Int,
        workingDir: File
    ): Process {
        return startJstatSampler(
            context = context,
            jdkDir = jdkDir,
            option = "-class",
            pid = pid,
            workingDir = workingDir
        )
    }

    private fun startJstatSampler(
        context: Context,
        jdkDir: File,
        option: String,
        pid: Int,
        workingDir: File
    ): Process {
        return start(
            context = context,
            config = Configuration(
                binary = jdkDir.resolve("bin/jstat"),
                arguments = listOf(
                    "-J-Djava.io.tmpdir=${context.cacheDir.absolutePath}",
                    option,
                    pid.toString(),
                    JSTAT_SAMPLE_INTERVAL_MS.toString()
                ),
                workingDir = workingDir,
                environmentOverrides = mapOf("JAVA_HOME" to jdkDir.absolutePath)
            )
        )
    }

    private fun createProcessBuilder(context: Context, config: Configuration): ProcessBuilder {
        val runtime = prepareGlibcRuntime(context)
        val command = runtime.wrapCommand(config.binary, config.arguments)

        return ProcessBuilder(command).apply {
            directory(config.workingDir)
            redirectErrorStream(true)

            environment().apply {
                clear()
                putCommonGlibcEnvironment(runtime)
                put("PATH", buildPath(config.binary, config.pathEntries))

                // Keep this last so callers can intentionally override JAVA_HOME,
                // TMPDIR, LD_LIBRARY_PATH, PATH, DNS_TRACE, etc. for special tool invocations.
                putAll(config.environmentOverrides)
            }
        }
    }

    private fun GlibcRuntime.wrapCommand(binary: File, arguments: List<String>): List<String> {
        return buildList {
            add(customLinker)
            add("--library-path")
            add(glibcPath)
            add("--preload")
            add(combinedPreload)
            add(binary.absolutePath)
            addAll(arguments)
        }
    }

    private fun buildPath(binary: File, extraEntries: List<File>): String {
        return buildList {
            binary.parentFile?.let(::add)
            addAll(extraEntries)
            add(File("/system/bin"))
        }.distinctBy { it.absolutePath }
            .joinToString(":") { it.absolutePath }
    }

    private fun prepareGlibcRuntime(context: Context): GlibcRuntime {
        val tempDir = context.cacheDir.apply { mkdirs() }
        val appDir = context.filesDir
        val glibcPath = appDir.resolve("glibc").absolutePath
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val runtimeUid = AndroidProcess.myUid()

        val resolvConf = tempDir.resolve("resolv.conf")
        val hostsFile = tempDir.resolve("hosts")
        val nsswitchConf = tempDir.resolve("nsswitch.conf")
        val gaiConf = tempDir.resolve("gai.conf")
        val passwdFile = tempDir.resolve("passwd")
        val groupFile = tempDir.resolve("group")

        writeRuntimeFiles(
            appDir = appDir,
            runtimeUid = runtimeUid,
            resolvConf = resolvConf,
            hostsFile = hostsFile,
            nsswitchConf = nsswitchConf,
            gaiConf = gaiConf,
            passwdFile = passwdFile,
            groupFile = groupFile
        )

        val pathRedirect = "$nativeLibDir/libpath_redirect.so"
        val nssWrapper = appDir.resolve("glibc/libnss_wrapper.so").absolutePath

        return GlibcRuntime(
            tempDir = tempDir,
            appDir = appDir,
            glibcPath = glibcPath,
            customLinker = "$nativeLibDir/libld_linux.so",
            combinedPreload = listOf(pathRedirect, nssWrapper).joinToString(":"),
            runtimeUser = RUNTIME_USER,
            resolvConf = resolvConf,
            hostsFile = hostsFile,
            nsswitchConf = nsswitchConf,
            gaiConf = gaiConf,
            passwdFile = passwdFile,
            groupFile = groupFile
        )
    }

    private fun writeRuntimeFiles(
        appDir: File,
        runtimeUid: Int,
        resolvConf: File,
        hostsFile: File,
        nsswitchConf: File,
        gaiConf: File,
        passwdFile: File,
        groupFile: File
    ) {
        resolvConf.writeTextIfChanged(
            buildString {
                DefaultDnsServers.forEach { ip -> append("nameserver $ip\n") }
                append("options timeout:2 attempts:2\n")
            }
        )

        hostsFile.writeTextIfChanged(
            """
            127.0.0.1 localhost
            ::1 localhost ip6-localhost ip6-loopback
            """.trimIndent() + "\n"
        )

        nsswitchConf.writeTextIfChanged(
            """
            passwd: files
            group: files
            hosts: files dns
            """.trimIndent() + "\n"
        )

        // Empty file is fine. It just prevents glibc from trying literal /etc/gai.conf.
        gaiConf.writeTextIfChanged("")

        passwdFile.writeTextIfChanged(
            "$RUNTIME_USER:x:$runtimeUid:$runtimeUid:Cosmic IDE:${appDir.absolutePath}:/system/bin/sh\n"
        )
        groupFile.writeTextIfChanged("$RUNTIME_USER:x:$runtimeUid:\n")
    }

    private fun File.writeTextIfChanged(content: String) {
        parentFile?.mkdirs()

        val oldContent = try {
            if (isFile) readText() else null
        } catch (_: Exception) {
            null
        }

        if (oldContent != content) {
            writeText(content)
        }
    }

    private fun MutableMap<String, String>.putCommonGlibcEnvironment(runtime: GlibcRuntime) {
        put("LD_LIBRARY_PATH", runtime.glibcPath)

        // --preload handles the initial custom-linker exec. LD_PRELOAD makes
        // Java/Gradle child processes inherit the same compatibility layer.
        put("LD_PRELOAD", runtime.combinedPreload)

        put("HOME", runtime.appDir.absolutePath)
        put("USER", runtime.runtimeUser)
        put("LOGNAME", runtime.runtimeUser)

        // TMPDIR is required by libpath_redirect for /tmp redirection. TMP/TEMP
        // are kept for cross-platform Java/native tooling that probes them.
        put("TMPDIR", runtime.tempDir.absolutePath)
        put("TMP", runtime.tempDir.absolutePath)
        put("TEMP", runtime.tempDir.absolutePath)

        // libpath_redirect/dns_fallback read these directly. The optional /etc
        // suffix redirects are only compatibility support for binaries that open
        // Linux config paths themselves.
        put("RESOLV_CONF_PATH", runtime.resolvConf.absolutePath)
        put("HOSTS_PATH", runtime.hostsFile.absolutePath)
        put("NSSWITCH_CONF_PATH", runtime.nsswitchConf.absolutePath)
        put("GAI_CONF_PATH", runtime.gaiConf.absolutePath)

        put("RES_OPTIONS", "attempts:2 timeout:2")

        put("NSS_WRAPPER_PASSWD", runtime.passwdFile.absolutePath)
        put("NSS_WRAPPER_GROUP", runtime.groupFile.absolutePath)

        // Kept for compatibility with your custom glibc/nss routing layer.
        put("NSS_WEAK_ROUTE_CONFIG", runtime.nsswitchConf.absolutePath)

        put("GLIBC_TUNABLES", "glibc.rtld.optional_dirs=${runtime.glibcPath}")
    }

    fun getResidentMemoryKb(pid: Int): Long {
        return try {
            val fields = File("/proc/$pid/statm").readText().split(' ')
            val residentPages = fields.getOrNull(1)?.toLongOrNull() ?: return 0L
            residentPages * PAGE_SIZE_BYTES / BYTES_PER_KB
        } catch (_: Exception) {
            0L
        }
    }

    fun getJvmPid(process: Process): Int {
        return getJvmPidCandidates(process).firstOrNull() ?: -1
    }

    fun getBestProcessMemoryPid(process: Process): Int {
        return getRuntimePidCandidates(process).firstOrNull() ?: -1
    }

    fun getRuntimePidCandidates(process: Process): List<Int> {
        val launcherPid = getNativePid(process)
        if (launcherPid == -1) return emptyList()

        val descendants = findDescendantPids(launcherPid)
        return buildList {
            addAll(jvmPidCandidates(launcherPid, descendants))
            addAll(descendants)
            add(launcherPid)
        }.distinct()
    }

    fun getJvmPidCandidates(process: Process): List<Int> {
        val launcherPid = getNativePid(process)
        if (launcherPid == -1) return emptyList()

        return jvmPidCandidates(launcherPid, findDescendantPids(launcherPid))
    }

    fun getNativePid(process: Process): Int {
        return try {
            val field: Field = (process as Any).javaClass.getDeclaredField("pid").apply {
                isAccessible = true
            }
            field.get(process) as Int
        } catch (_: Exception) {
            -1
        }
    }

    private fun findDescendantPids(rootPid: Int): List<Int> {
        val result = mutableListOf<Int>()
        val queue = ArrayDeque<Int>()
        val seen = mutableSetOf(rootPid)

        queue.add(rootPid)
        while (queue.isNotEmpty()) {
            val parentPid = queue.removeFirst()
            childPidsOf(parentPid).forEach { childPid ->
                if (seen.add(childPid)) {
                    result.add(childPid)
                    queue.add(childPid)
                }
            }
        }

        return result
    }

    private fun childPidsOf(pid: Int): List<Int> {
        val directChildren = try {
            File("/proc/$pid/task/$pid/children")
                .readText()
                .trim()
                .split(Regex("\\s+"))
                .mapNotNull { it.toIntOrNull() }
        } catch (_: Exception) {
            emptyList()
        }

        if (directChildren.isNotEmpty()) return directChildren

        return try {
            File("/proc")
                .listFiles()
                ?.mapNotNull { it.name.toIntOrNull() }
                ?.filter { childPid -> getParentPid(childPid) == pid }
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getParentPid(pid: Int): Int? {
        return try {
            File("/proc/$pid/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("PPid:") }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.toIntOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun jvmPidCandidates(launcherPid: Int, descendants: List<Int>): List<Int> {
        return buildList {
            addAll(descendants.filter { isJvmProcess(it) })
            if (isJvmProcess(launcherPid)) add(launcherPid)
        }.distinct()
    }

    private fun isJvmProcess(pid: Int): Boolean {
        val commandName = try {
            File("/proc/$pid/comm").readText().trim()
        } catch (_: Exception) {
            ""
        }
        if (commandName == "java") return true

        val executableName = try {
            File("/proc/$pid/exe").canonicalFile.name
        } catch (_: Exception) {
            ""
        }
        if (executableName == "java") return true

        val command = readCommandLine(pid).firstOrNull().orEmpty()
        return command == "java" || command.endsWith("/bin/java") || hasLoadedJvm(pid)
    }

    private fun readCommandLine(pid: Int): List<String> {
        return try {
            File("/proc/$pid/cmdline")
                .readText()
                .split('\u0000')
                .filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun hasLoadedJvm(pid: Int): Boolean {
        return try {
            File("/proc/$pid/maps").useLines { lines ->
                lines.any { line -> line.contains("libjvm.so") }
            }
        } catch (_: Exception) {
            false
        }
    }

    private const val PAGE_SIZE_BYTES = 4096L
    private const val BYTES_PER_KB = 1024L
}
