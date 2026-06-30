package org.cosmicide.exec.linux

import android.content.Context
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.lang.reflect.Field
import android.os.Process as AndroidProcess

object LinuxProcessRunner {

    data class Configuration(
        val binary: File,
        val arguments: List<String>,
        val workingDir: File,
        val environmentOverrides: Map<String, String> = emptyMap(),
        val pathEntries: List<File> = emptyList()
    )

    /**
     * Spawns an arbitrary GNU/Linux binary inside the custom glibc environment layer.
     */
    fun start(
        context: Context,
        config: Configuration
    ): Process {
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

    fun toolchainPathEntries(
        context: Context,
        jdkDir: File
    ): List<File> {
        return buildList {
            add(jdkDir.resolve("bin"))
            if (FileUtil.isInitialized) {
                add(FileUtil.dataDir.resolve("kotlinc/bin"))
            }
            add(context.filesDir.resolve("jdtls/bin"))
        }
    }

    fun toolchainEnvironment(
        context: Context,
        jdkDir: File
    ): Map<String, String> {
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
        val current = kotlin.text.StringBuilder()
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
            throw kotlin.IllegalArgumentException("Unclosed quote in command")
        }
        if (current.isNotEmpty()) result.add(current.toString())

        return result
    }

    fun resolveExecutable(
        commandName: String,
        workingDir: File,
        pathEntries: List<File>
    ): File {
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
            ?: throw kotlin.IllegalArgumentException("Command not found: $commandName")
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
        val tempDir = context.cacheDir
        val appDir = context.filesDir
        val glibcPath = appDir.resolve("glibc").absolutePath
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        val runtimeUser = "cosmicide"
        val runtimeUid = AndroidProcess.myUid()

        val resolvConf = tempDir.resolve("resolv.conf")
        val nsswitchConf = tempDir.resolve("nsswitch.conf")
        val passwdFile = tempDir.resolve("passwd")
        val groupFile = tempDir.resolve("group")

        if (!resolvConf.exists()) {
            val dns = listOf("8.8.8.8", "1.1.1.1")
            resolvConf.bufferedWriter().use { writer ->
                dns.forEach { ip -> writer.write("nameserver $ip\n") }
            }
        }

        if (!nsswitchConf.exists()) {
            nsswitchConf.writeText("passwd: files\ngroup: files\nhosts: files dns\n")
        }

        if (!passwdFile.exists()) {
            passwdFile.writeText("$runtimeUser:x:$runtimeUid:$runtimeUid:Cosmic IDE:${appDir.absolutePath}:/system/bin/sh\n")
        }

        if (!groupFile.exists()) {
            groupFile.writeText("$runtimeUser:x:$runtimeUid:\n")
        }

        val nssWrapper = appDir.resolve("glibc/libnss_wrapper.so").absolutePath
        val pathRedirect = "$nativeLibDir/libpath_redirect.so"
        val combinedPreload = "$nssWrapper:$pathRedirect"

        val customLinker = "$nativeLibDir/libld_linux.so"

        val command = mutableListOf(
            customLinker,
            "--library-path",
            glibcPath,
            "--preload",
            combinedPreload,
            jdkDir.resolve("bin/jstat").absolutePath,
            "-J-Djava.io.tmpdir=${tempDir.absolutePath}",
            option,
            pid.toString(),
            "1000"
        )

        return ProcessBuilder(command).apply {
            directory(workingDir)
            environment().apply {
                clear()

                put("PATH", "${jdkDir.resolve("bin").absolutePath}:/system/bin")
                put("LD_LIBRARY_PATH", glibcPath)
                put("HOME", appDir.absolutePath)
                put("USER", runtimeUser)
                put("LOGNAME", runtimeUser)

                put("TMPDIR", tempDir.absolutePath)
                put("TMP", tempDir.absolutePath)
                put("TEMP", tempDir.absolutePath)

                put("LD_PRELOAD", combinedPreload)

                put("RES_OPTIONS", "resolv-file=${resolvConf.absolutePath}")
                put("NSS_WEAK_ROUTE_CONFIG", nsswitchConf.absolutePath)
                put("NSS_WRAPPER_PASSWD", passwdFile.absolutePath)
                put("NSS_WRAPPER_GROUP", groupFile.absolutePath)
                put("GLIBC_TUNABLES", "glibc.rtld.optional_dirs=$glibcPath")
            }
            redirectErrorStream(true)
        }.start()
    }

    data class JstatGcSample(
        val edenUsedKb: Long,
        val oldUsedKb: Long,
        val edenCapacityKb: Long,
        val oldCapacityKb: Long
    ) {
        val liveHeapUsedKb: Long = edenUsedKb + oldUsedKb
    }

    fun parseJstatGcSample(headerLine: String, valueLine: String): JstatGcSample? {
        val headers = headerLine.trim().split(Regex("\\s+"))
        val values = valueLine.trim().split(Regex("\\s+"))
        if (headers.isEmpty() || headers.size > values.size) return null

        val row = headers.zip(values).toMap()
        return JstatGcSample(
            edenUsedKb = row["EU"]?.toDoubleOrNull()?.toLong() ?: return null,
            oldUsedKb = row["OU"]?.toDoubleOrNull()?.toLong() ?: return null,
            edenCapacityKb = row["EC"]?.toDoubleOrNull()?.toLong() ?: 0L,
            oldCapacityKb = row["OC"]?.toDoubleOrNull()?.toLong() ?: 0L
        )
    }

    private fun createProcessBuilder(
        context: Context,
        config: Configuration
    ): ProcessBuilder {
        val tempDir = context.cacheDir
        val appDir = context.filesDir
        val glibcPath = appDir.resolve("glibc").absolutePath

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val customLinker = "$nativeLibDir/libld_linux.so"
        val nssWrapper = appDir.resolve("glibc/libnss_wrapper.so").absolutePath
        val pathRedirect = "$nativeLibDir/libpath_redirect.so"
        val combinedPreload = "$nssWrapper:$pathRedirect"

        val resolvConf = tempDir.resolve("resolv.conf")
        val nsswitchConf = tempDir.resolve("nsswitch.conf")
        val passwdFile = tempDir.resolve("passwd")
        val groupFile = tempDir.resolve("group")
        val runtimeUser = "cosmicide"
        val runtimeUid = AndroidProcess.myUid()
        val dns = listOf("8.8.8.8", "1.1.1.1")

        resolvConf.bufferedWriter().use { writer ->
            dns.forEach { ip -> writer.write("nameserver $ip\n") }
        }

        nsswitchConf.writeText("passwd: files\ngroup: files\nhosts: files dns\n")
        passwdFile.writeText("$runtimeUser:x:$runtimeUid:$runtimeUid:Cosmic IDE:${appDir.absolutePath}:/system/bin/sh\n")
        groupFile.writeText("$runtimeUser:x:$runtimeUid:\n")

        val command = mutableListOf(
            customLinker,
            "--library-path",
            glibcPath
        ).apply {
            add("--preload")
            add(combinedPreload)
            add(config.binary.absolutePath)
            addAll(config.arguments)
        }

        return ProcessBuilder(command).apply {
            environment().apply {
                clear()

                val binaryBinDir = config.binary.parentFile
                val pathEntries = buildList {
                    if (binaryBinDir != null) add(binaryBinDir)
                    addAll(config.pathEntries)
                    add(File("/system/bin"))
                }
                    .distinctBy { it.absolutePath }
                    .joinToString(":") { it.absolutePath }

                put("PATH", pathEntries)
                put("LD_LIBRARY_PATH", glibcPath)
                put("HOME", appDir.absolutePath)
                put("USER", runtimeUser)
                put("LOGNAME", runtimeUser)
                put("LD_PRELOAD", combinedPreload)

                put("RES_OPTIONS", "resolv-file=${resolvConf.absolutePath}")
                put("NSS_WEAK_ROUTE_CONFIG", nsswitchConf.absolutePath)
                put("NSS_WRAPPER_PASSWD", passwdFile.absolutePath)
                put("NSS_WRAPPER_GROUP", groupFile.absolutePath)
                put("GLIBC_TUNABLES", "glibc.rtld.optional_dirs=$glibcPath")

                putAll(config.environmentOverrides)
            }
            directory(config.workingDir)
            redirectErrorStream(true)
        }
    }

    fun getResidentMemoryKb(pid: Int): Long {
        return try {
            val statm = File("/proc/$pid/statm").readText().split(" ")
            if (statm.size < 2) return 0L
            val pages = statm[1].toLong()
            (pages * 4096) / 1024
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
            val field: Field = process.javaClass.getDeclaredField("pid").apply { isAccessible = true }
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
        val children = try {
            File("/proc/$pid/task/$pid/children")
                .readText()
                .trim()
                .split(Regex("\\s+"))
                .mapNotNull { it.toIntOrNull() }
        } catch (_: Exception) {
            emptyList()
        }

        if (children.isNotEmpty()) return children

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
            File("/proc/$pid/status")
                .useLines { lines ->
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
        if (command == "java" || command.endsWith("/bin/java")) return true

        return hasLoadedJvm(pid)
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
}
