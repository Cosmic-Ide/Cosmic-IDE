package org.cosmicide.exec.linux

import android.content.Context
import java.io.File
import java.lang.reflect.Field

object LinuxProcessRunner {

    data class Configuration(
        val binary: File,
        val arguments: List<String>,
        val workingDir: File,
        val environmentOverrides: Map<String, String> = emptyMap()
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

    fun startJstatGcSampler(
        context: Context,
        jdkDir: File,
        pid: Int,
        workingDir: File,
        intervalMillis: Long = 1_000L
    ): Process {
        return start(
            context,
            Configuration(
                binary = jdkDir.resolve("bin/jstat"),
                arguments = listOf("-gc", pid.toString(), intervalMillis.toString()),
                workingDir = workingDir
            )
        )
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

        val resolvConf = tempDir.resolve("resolv.conf")
        val dns = listOf("8.8.8.8", "1.1.1.1")

        resolvConf.bufferedWriter().use { writer ->
            dns.forEach { ip -> writer.write("nameserver $ip\n") }
        }

        tempDir.resolve("nsswitch.conf").writeText("hosts: files dns\n")

        // Execution Pipeline: Linker -> Lib Directives -> Targeted Linux Binary -> Args
        val command = mutableListOf(
            customLinker,
            "--library-path", glibcPath,
            config.binary.absolutePath
        ).apply {
            addAll(config.arguments)
        }

        val processBuilder = ProcessBuilder(command).apply {
            environment().apply {
                clear()

                val binaryBinDir = config.binary.parentFile?.absolutePath ?: ""
                put("PATH", "$binaryBinDir:/system/bin")
                put("LD_LIBRARY_PATH", glibcPath)

                // Compatibility routing maps
                put("RES_OPTIONS", "resolv-file=${resolvConf.absolutePath}")
                put("NSS_WEAK_ROUTE_CONFIG", "${tempDir.resolve("nsswitch.conf").absolutePath}")
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

    fun getNativePid(process: Process): Int {
        return try {
            val field: Field = process.javaClass.getDeclaredField("pid").apply { isAccessible = true }
            field.get(process) as Int
        } catch (_: Exception) {
            -1
        }
    }
}
