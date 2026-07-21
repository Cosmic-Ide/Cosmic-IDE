package org.cosmicide.exec.linux

import android.content.Context
import org.cosmicide.util.FileUtil
import org.cosmicide.util.repairJdkExecutablePermissions
import java.io.File
import java.lang.reflect.Field
import android.os.Process as AndroidProcess

object LinuxProcessRunner {

    private const val RUNTIME_USER = "cosmicide"
    private const val JSTAT_SAMPLE_INTERVAL_MS = 1000
    private const val EXECUTABLE_IDENTITY_ENV = "COSMIC_EXECUTABLE"

    private val DefaultDnsServers = listOf("1.1.1.1", "8.8.8.8")

    data class Configuration(
        val binary: File,
        val arguments: List<String>,
        val workingDir: File,
        val environmentOverrides: Map<String, String> = emptyMap(),
        val pathEntries: List<File> = emptyList(),
        val redirectErrorStream: Boolean = true,
        val setup: Boolean = false,
        val usePty: Boolean = false,
        val loadShellStartupFiles: Boolean = usePty,
        val terminalRows: Int = 24,
        val terminalColumns: Int = 80
    )

    private data class WrappedCommand(
        val command: List<String>,
        val executableIdentity: String
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

    private enum class ExecutableKind {
        ELF, SHELL_SCRIPT, UNSUPPORTED
    }

    /**
     * Spawns an arbitrary GNU/Linux binary inside the custom glibc environment layer.
     */
    fun start(context: Context, config: Configuration): Process {
        return createProcessBuilder(context, config).start()
    }

    /**
     * Spawns a process with PTY support for interactive terminal I/O
     * Uses the exec module native PTY implementation to fork/exec and attach the child process.
     */
    fun startWithPty(context: Context, config: Configuration): PtyProcess {
        if (!config.usePty) {
            throw IllegalArgumentException("Configuration must have usePty=true")
        }

        val runtime = prepareGlibcRuntime(context, config.setup)
        val shellArguments = runtime.applyShellStartupArguments(
            binary = config.binary,
            arguments = config.arguments,
            interactive = true,
            enabled = config.loadShellStartupFiles
        )
        val wrappedCommand = runtime.wrapCommand(config.binary, shellArguments)
        val environment = buildMap {
            putCommonGlibcEnvironment(runtime)
            put("PATH", buildPath(runtime, config.binary, config.pathEntries))
            put("TERM", "xterm-256color")
            putAll(config.environmentOverrides)

            // The initial process is launched through ld-linux directly, before
            // exec_wrap can intercept anything. Preserve the real executable
            // identity for /proc/self/exe and AT_EXECFN virtualization.
            put(EXECUTABLE_IDENTITY_ENV, wrappedCommand.executableIdentity)
        }

        // IMPORTANT: config.workingDir is passed to the native layer for chdir().
        val pty = PtyTerminal.allocateAndSpawn(
            workingDir = config.workingDir,
            executable = File(wrappedCommand.command[0]),
            arguments = wrappedCommand.command.drop(1),
            environment = environment
        )

        pty.setWindowSize(config.terminalRows, config.terminalColumns)
        return PtyProcess.create(pty)
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
        jdkDir: File,
        setup: Boolean = false
    ): List<File> {
        return buildList {
            add(jdkDir.resolve("bin"))
            if (FileUtil.isInitialized) {
                add(FileUtil.dataDir.resolve("kotlinc/bin"))
            }
            add(context.filesDir.resolve("jdtls/bin"))
            add(context.filesDir.resolve("scala/bin"))
            add(runtimeDir(context, setup).resolve("usr/bin"))
        }
    }

    fun toolchainEnvironment(jdkDir: File): Map<String, String> {
        repairJdkExecutablePermissions(jdkDir)
        return buildMap {
            put("JAVA_HOME", jdkDir.absolutePath)
        }
    }

    fun parseCommandLine(commandLine: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false
        var tokenStarted = false

        commandLine.forEach { char ->
            when {
                escaping -> {
                    current.append(char)
                    escaping = false
                    tokenStarted = true
                }

                char == '\\' -> {
                    escaping = true
                    tokenStarted = true
                }

                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        current.append(char)
                    }
                }

                char == '\'' || char == '"' -> {
                    quote = char
                    tokenStarted = true
                }

                char.isWhitespace() -> {
                    if (tokenStarted) {
                        result.add(current.toString())
                        current.clear()
                        tokenStarted = false
                    }
                }

                else -> {
                    current.append(char)
                    tokenStarted = true
                }
            }
        }

        if (escaping) current.append('\\')
        if (quote != null) {
            throw IllegalArgumentException("Unclosed quote in command")
        }
        if (tokenStarted) result.add(current.toString())

        return result
    }

    fun resolveExecutable(commandName: String, workingDir: File, pathEntries: List<File>): File {
        val commandFile = File(commandName)

        if (commandFile.isAbsolute) return commandFile

        if (commandName.contains(File.separatorChar)) {
            return workingDir.resolve(commandName).canonicalFile
        }

        return pathEntries.plus(File("/system/bin")).asSequence().map { it.resolve(commandName) }
            .firstOrNull { it.isFile }
            ?: throw IllegalArgumentException("Command not found: $commandName")
    }

    fun startJstatGcSampler(
        context: Context, jdkDir: File, option: String = "-gccause", pid: Int, workingDir: File
    ): Process {
        return startJstatSampler(
            context = context, jdkDir = jdkDir, option = option, pid = pid, workingDir = workingDir
        )
    }

    fun startJstatClassSampler(
        context: Context, jdkDir: File, pid: Int, workingDir: File
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
        context: Context, jdkDir: File, option: String, pid: Int, workingDir: File
    ): Process {
        return start(
            context = context, config = Configuration(
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
        val runtime = prepareGlibcRuntime(context, config.setup)
        val shellArguments = runtime.applyShellStartupArguments(
            binary = config.binary,
            arguments = config.arguments,
            interactive = false,
            enabled = config.loadShellStartupFiles
        )
        val wrappedCommand = runtime.wrapCommand(config.binary, shellArguments)

        return ProcessBuilder(wrappedCommand.command).apply {
            directory(config.workingDir)
            redirectErrorStream(config.redirectErrorStream)

            environment().apply {
                clear()
                putCommonGlibcEnvironment(runtime)
                put("PATH", buildPath(runtime, config.binary, config.pathEntries))

                // Keep caller overrides late so special tool invocations can replace
                // JAVA_HOME, TMPDIR, LD_LIBRARY_PATH, PATH, DNS_TRACE, etc.
                putAll(config.environmentOverrides)

                // This is internal launch metadata, not a caller-facing switch.
                put(EXECUTABLE_IDENTITY_ENV, wrappedCommand.executableIdentity)
            }
        }
    }

    private fun GlibcRuntime.applyShellStartupArguments(
        binary: File,
        arguments: List<String>,
        interactive: Boolean,
        enabled: Boolean
    ): List<String> {
        if (!enabled) return arguments

        val target = resolveInitialGlibcTarget(binary.absoluteFile)
        if (target.name != "bash") return arguments

        val alreadyLogin = arguments.any { argument ->
            argument == "--login" ||
                    argument == "-l" ||
                    (argument.startsWith("-") &&
                            !argument.startsWith("--") &&
                            argument.drop(1).contains('l'))
        }
        val alreadyInteractive = arguments.any { argument ->
            argument == "-i" ||
                    (argument.startsWith("-") &&
                            !argument.startsWith("--") &&
                            argument.drop(1).contains('i'))
        }

        return buildList {
            if (!alreadyLogin) add("--login")
            if (interactive && !alreadyInteractive) add("-i")
            addAll(arguments)
        }
    }

    private fun GlibcRuntime.wrapCommand(
        binary: File,
        arguments: List<String>
    ): WrappedCommand {
        val target = resolveInitialGlibcTarget(binary.absoluteFile)

        return when (target.executableKind()) {
            ExecutableKind.ELF -> linkerCommand(target, arguments)

            ExecutableKind.SHELL_SCRIPT -> {
                val shell = resolveShellForScript(target)
                linkerCommand(shell, listOf(target.absolutePath) + arguments)
            }

            ExecutableKind.UNSUPPORTED -> {
                throw IllegalArgumentException(
                    "Unsupported executable: ${target.absolutePath}. " +
                            "Expected a readable ELF binary or a readable shell script with a sh/bash shebang."
                )
            }
        }
    }

    private fun GlibcRuntime.linkerCommand(
        program: File,
        arguments: List<String>
    ): WrappedCommand {
        val programPath = program.canonicalOrAbsolutePath()

        return WrappedCommand(
            command = buildList {
                add(customLinker)
                add("--argv0")
                add(programPath)
                add("--library-path")
                add(linkLibraryDirs().joinToString(":") { it.absolutePath })
                add("--preload")
                add(combinedPreload)
                add(programPath)
                addAll(arguments)
            },
            executableIdentity = programPath
        )
    }

    private fun GlibcRuntime.resolveInitialGlibcTarget(binary: File): File {
        if (!binary.isAndroidSystemPath()) return binary

        val replacement = glibcBinReplacementFor(binary)
        if (replacement != null) return replacement

        throw IllegalArgumentException(
            "Refusing to launch Android system executable through glibc ld-linux: ${binary.absolutePath}. " + "No readable glibc replacement exists at ${
                File(
                    glibcPath
                ).resolve("bin").resolve(binary.name).absolutePath
            }."
        )
    }

    private fun GlibcRuntime.glibcBinReplacementFor(binary: File): File? {
        val replacement = File(glibcPath).resolve("bin").resolve(binary.name).absoluteFile

        return replacement.takeIf { it.executableKind() != ExecutableKind.UNSUPPORTED }
    }

    private fun GlibcRuntime.gccFrontendDirs(): List<File> {
        val gccRoot = File(glibcPath).resolve("lib/gcc")
        if (!gccRoot.isDirectory) return emptyList()

        return try {
            gccRoot.walkTopDown().maxDepth(4).filter { dir ->
                dir.isDirectory && (dir.resolve("cc1").exists() || dir.resolve("cc1plus")
                    .exists() || dir.resolve("f951").exists() || dir.resolve("lto1").exists())
            }.map { it.absoluteFile }.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildPath(runtime: GlibcRuntime, binary: File, extraEntries: List<File>): String {
        val glibcBin = File(runtime.glibcPath).resolve("bin")

        return buildList {
            // Keep the target directory first for project-local tools like ./gradlew,
            // but do not put /system/bin before the relocated GNU/Linux tools.
            binary.parentFile?.takeUnless { it.isAndroidSystemPath() }?.let(::add)

            addAll(extraEntries)
            add(glibcBin)
            addAll(runtime.gccFrontendDirs())
            add(File("/system/bin"))
        }.distinctBy { it.absolutePath }.joinToString(":") { it.absolutePath }
    }

    private fun GlibcRuntime.defaultShell(): File {
        return firstUsableShell(
            listOf(
                File(glibcPath).resolve("bin/bash"), File(glibcPath).resolve("bin/sh")
            )
        ) ?: File(glibcPath).resolve("bin/bash")
    }

    private fun GlibcRuntime.resolveShellForScript(script: File): File {
        val requestedShell = script.readShebangLine()?.shellNameFromShebang()
        val binDir = File(glibcPath).resolve("bin")

        val candidates = buildList {
            when (requestedShell) {
                "bash" -> {
                    add(binDir.resolve("bash"))
                    add(binDir.resolve("sh"))
                }

                "sh", "dash", "ash", "ksh", "zsh" -> {
                    add(binDir.resolve(requestedShell))
                    add(binDir.resolve("sh"))
                    add(binDir.resolve("bash"))
                }

                else -> {
                    add(binDir.resolve("sh"))
                    add(binDir.resolve("bash"))
                }
            }
        }

        val shPath = binDir.resolve("sh").absolutePath
        val bashPath = binDir.resolve("bash").absolutePath

        return firstUsableShell(candidates) ?: throw IllegalArgumentException(
            "Shell script ${script.absolutePath} needs a glibc shell, " + "but neither $shPath nor $bashPath is usable."
        )
    }

    private fun firstUsableShell(candidates: List<File>): File? {
        return candidates.asSequence().map { it.absoluteFile }.distinctBy { it.absolutePath }
            .firstOrNull { it.isElfFile() }
    }

    private fun File.executableKind(): ExecutableKind {
        return when {
            isElfFile() -> ExecutableKind.ELF
            isSupportedShellScriptFile() -> ExecutableKind.SHELL_SCRIPT
            else -> ExecutableKind.UNSUPPORTED
        }
    }

    private fun File.isElfFile(): Boolean {
        // Do not check executable permission here. The kernel executes customLinker;
        // ld-linux only needs to read/map the target ELF.
        return try {
            inputStream().use { input ->
                val magic = ByteArray(4)
                input.read(magic) == 4 && magic[0] == 0x7F.toByte() && magic[1] == 'E'.code.toByte() && magic[2] == 'L'.code.toByte() && magic[3] == 'F'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun File.isSupportedShellScriptFile(): Boolean {
        // Do not check executable permission here either. We run scripts as:
        // ld-linux ... glibc/bin/sh <script>, so the script only needs to be readable.
        return readShebangLine()?.shellNameFromShebang() != null
    }

    private fun File.readShebangLine(maxBytes: Int = 512): String? {
        return try {
            inputStream().use { input ->
                val buffer = ByteArray(maxBytes)
                val count = input.read(buffer)
                if (count <= 0) return null

                val text = buffer.copyOf(count).toString(Charsets.UTF_8)

                text.lineSequence().firstOrNull()?.takeIf { it.startsWith("#!") }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun String.shellNameFromShebang(): String? {
        if (!startsWith("#!")) return null

        val parts = removePrefix("#!").trim().split(Regex("\\s+")).filter { it.isNotBlank() }

        if (parts.isEmpty()) return null

        val interpreter = File(parts[0]).name
        if (interpreter.isKnownShellName()) return interpreter

        if (interpreter != "env") return null

        var index = 1
        while (index < parts.size) {
            val part = parts[index]

            if (part == "-S") {
                index++
                continue
            }

            if (part.startsWith("-")) {
                index++
                continue
            }

            val command = File(part).name
            return command.takeIf { it.isKnownShellName() }
        }

        return null
    }

    private fun String.isKnownShellName(): Boolean {
        return this == "sh" || this == "bash" || this == "dash" || this == "ash" || this == "ksh" || this == "zsh"
    }

    private fun File.canonicalOrAbsolutePath(): String {
        return try {
            canonicalPath
        } catch (_: Exception) {
            absolutePath
        }
    }

    private fun File.isAndroidSystemPath(): Boolean {
        val path = absolutePath
        return path.startsWith("/system/") || path.startsWith("/apex/") || path.startsWith("/vendor/") || path.startsWith(
            "/odm/"
        ) || path.startsWith("/product/")
    }

    private fun prepareGlibcRuntime(context: Context, setup: Boolean): GlibcRuntime {
        val tempDir = context.cacheDir.apply { mkdirs() }
        val appDir = runtimeDir(context, setup)
        val homeDir = appDir.resolve("home").apply { mkdirs() }
        ensureBashStartupBridge(homeDir)
        val glibcRoot = appDir.resolve("usr")
        val glibcPath = glibcRoot.absolutePath
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
        val nssWrapper = glibcRoot.resolve("lib/libnss_wrapper.so").absolutePath

        return GlibcRuntime(
            tempDir = tempDir,
            appDir = appDir,
            glibcPath = glibcPath,
            customLinker = "$nativeLibDir/libld_linux.so",
            combinedPreload = listOfNotNull(
                pathRedirect,
                if (setup) nssWrapper else null
            ).joinToString(":"),
            runtimeUser = RUNTIME_USER,
            resolvConf = resolvConf,
            hostsFile = hostsFile,
            nsswitchConf = nsswitchConf,
            gaiConf = gaiConf,
            passwdFile = passwdFile,
            groupFile = groupFile
        )
    }

    private fun runtimeDir(context: Context, setup: Boolean): File {
        return context.filesDir.resolve(if (setup) "glibc" else "arch")
    }

    private fun ensureBashStartupBridge(homeDir: File) {
        val bashProfile = homeDir.resolve(".bash_profile")
        val bashLogin = homeDir.resolve(".bash_login")

        // Bash login shells read only the first existing file from:
        // .bash_profile, .bash_login, .profile. Create the conventional bridge
        // only when the user has not already supplied a Bash-specific login file.
        if (bashProfile.exists() || bashLogin.exists()) return

        bashProfile.writeTextIfChanged(
            $$"""
            # Generated by Cosmic IDE.
            # Keep login-shell environment in .profile and interactive setup in .bashrc.
            if [ -r "$HOME/.profile" ]; then
                . "$HOME/.profile"
            fi

            if [ -r "$HOME/.bashrc" ]; then
                . "$HOME/.bashrc"
            fi
            """.trimIndent() + "\n"
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
            })

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
            "$RUNTIME_USER:x:$runtimeUid:$runtimeUid:Cosmic IDE:${appDir.resolve("home").absolutePath}:/system/bin/sh\n"
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

    private fun GlibcRuntime.linkLibraryDirs(): List<File> {
        val glibcRoot = File(glibcPath)

        return buildList {
            add(glibcRoot.resolve("lib"))
            add(glibcRoot)
            addAll(gccFrontendDirs())
        }.filter { it.exists() }
            .distinctBy { it.absolutePath }
    }

    private fun MutableMap<String, String>.putCommonGlibcEnvironment(runtime: GlibcRuntime) {
        put(
            "LD_LIBRARY_PATH",
            runtime.linkLibraryDirs().joinToString(":") { it.absolutePath }
        )
        // --preload handles the initial custom-linker exec. LD_PRELOAD makes
        // Java/Gradle child processes inherit the same compatibility layer.
        put("LD_PRELOAD", runtime.combinedPreload)

        // Only path-bearing value exec_wrap.c cannot infer by itself.
        // Behavior toggles live as compile-time constants at the top of exec_wrap.c.
        put("HOME", runtime.appDir.resolve("home").absolutePath)
        put("USER", runtime.runtimeUser)
        put("LOGNAME", runtime.runtimeUser)
        put("SHELL", runtime.defaultShell().absolutePath)

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
        put("GLIBC_TUNABLES", $$"${GLIBC_TUNABLES:+$GLIBC_TUNABLES:}glibc.pthread.rseq=0")

        put("RES_OPTIONS", "attempts:1 timeout:1")

        put("NSS_WRAPPER_PASSWD", runtime.passwdFile.absolutePath)
        put("NSS_WRAPPER_GROUP", runtime.groupFile.absolutePath)

        // Kept for compatibility with your custom glibc/nss routing layer.
        put("NSS_WEAK_ROUTE_CONFIG", runtime.nsswitchConf.absolutePath)

        // Make paths baked into Termux/gpkg binaries resolve against the app-private
        // files directory. runtime.appDir is the fake Termux files root: <app>/files/glibc.
        put("APP_FILES_DIR", runtime.appDir.absolutePath)
        put("TERMUX_PREFIX", File(runtime.glibcPath).absolutePath)

        val gccFrontendPath = runtime.gccFrontendDirs()
            .joinToString(":") { it.absolutePath }

        if (gccFrontendPath.isNotEmpty()) {
            put("COMPILER_PATH", gccFrontendPath)
        }

        val binDir = File(runtime.glibcPath).resolve("bin")
        put("CC", binDir.resolve("gcc").absolutePath)
        put("CXX", binDir.resolve("g++").absolutePath)
        put("AR", binDir.resolve("ar").absolutePath)
        put("RANLIB", binDir.resolve("ranlib").absolutePath)
        put("STRIP", binDir.resolve("strip").absolutePath)
        put("MAKE", binDir.resolve("make").absolutePath)
        put("CMAKE_C_COMPILER", binDir.resolve("gcc").absolutePath)
        put("CMAKE_CXX_COMPILER", binDir.resolve("g++").absolutePath)
        put("CMAKE_MAKE_PROGRAM", binDir.resolve("make").absolutePath)

        put(
            "LIBRARY_PATH",
            runtime.linkLibraryDirs().joinToString(":") { it.absolutePath }
        )
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
            File("/proc/$pid/task/$pid/children").readText().trim().split(Regex("\\s+"))
                .mapNotNull { it.toIntOrNull() }
        } catch (_: Exception) {
            emptyList()
        }

        if (directChildren.isNotEmpty()) return directChildren

        return try {
            File("/proc").listFiles()?.mapNotNull { it.name.toIntOrNull() }
                ?.filter { childPid -> getParentPid(childPid) == pid }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getParentPid(pid: Int): Int? {
        return try {
            File("/proc/$pid/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("PPid:") }?.substringAfter(':')?.trim()
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
            File("/proc/$pid/cmdline").readText().split('\u0000').filter { it.isNotBlank() }
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
