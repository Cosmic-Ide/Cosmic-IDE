/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ui.editor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.util.jdksDir
import java.io.File
import java.io.InputStream

const val JDTLS_PORT = 8763

var jdtLspProcess: Process? = null

suspend fun runJdtlsProcess(
    context: Context,
    project: Project,
    onProcessStarted: (Process) -> Unit
) {
    withContext(Dispatchers.IO) {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val appDir = context.filesDir
        val glibcPath = appDir.resolve("glibc").absolutePath
        val jdkDir = context.jdksDir().resolve("jdk-" + Prefs.currentJDK)
        val javaBinary = jdkDir.resolve("bin/java").absolutePath
        val executableLinker = "$nativeLibDir/libld_linux.so"
        val codeCacheDir = context.codeCacheDir

        val jdtlsDir = File(context.filesDir, "jdtls")

        val launcherJar = findMainEquinoxLauncher(jdtlsDir)
            ?: throw IllegalStateException("Equinox launcher jar not found in jdtls/plugins")

        val sharedConfigDir = jdtlsDir.resolve("config_linux")

        // Separate local, writable configuration area to support cascading
        val localConfigDir = File(context.cacheDir, "jdtls_local_config/${project.name}").apply { mkdirs() }
        val workspaceDir = File(context.cacheDir, "jdtls_workspace/${project.name}").apply { mkdirs() }

        val command = mutableListOf(
            executableLinker, "--library-path", glibcPath,
            javaBinary,
            "-Djdk.xml.maxGeneralEntitySizeLimit=0",
            "-Djdk.xml.totalEntitySizeLimit=0",
            "-Djdk.lang.Process.launchMechanism=FORK",
            "-Xshareclasses:name=jdt_ls_cache,cacheDir=${codeCacheDir.absolutePath}",
            "-Xgcpolicy:gencon",
            "-Xtune:virtualized",

            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Djavax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema=org.apache.xenon.BypassFactory",
            "-Dorg.eclipse.xml.disableSchemaValidation=true",

            "-Dlog.level=WARNING",

            "-Xms256m",
            "-Xmx1G",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=50",
            "-XX:+TieredCompilation",
            "-XX:TieredStopAtLevel=1",
            "-Dorg.eclipse.jdt.ls.lombok.support=false",
            "-Djdk.xml.maxGeneralEntitySizeLimit=0",

            "--add-modules=ALL-SYSTEM",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",

            "-Dosgi.checkConfiguration=false",
            "-Dosgi.sharedConfiguration.area=${sharedConfigDir.absolutePath}",
            "-Dosgi.sharedConfiguration.area.readOnly=true",
            "-Dosgi.configuration.cascaded=true",
            "-jar", launcherJar,
            "-configuration", localConfigDir.absolutePath,
            "-data", workspaceDir.absolutePath
        )

        val processBuilder = ProcessBuilder(command).apply {
            environment().apply {
                clear()
                put("PATH", "$jdkDir:/system/bin")
                put("LD_LIBRARY_PATH", glibcPath)
                put("OPENJ9_JAVA_OPTIONS", "-XX:+UseContainerSupport")

                directory(project.root)
                redirectErrorStream(false)
            }
        }

        try {
            Log.d("CosmicIDE", "--- Launching Cascaded JDTLS Core ---")
            val process = processBuilder.start()
            streamStderrToLogcat(process.errorStream)
            Log.d("CosmicIDE", "JDTLS process started with PID: ${LinuxProcessRunner.getNativePid(process)}")

            // write process logs to logcat without closing the stream (JDTLS keeps it open indefinitely)
            Thread {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        Log.d("JDTLS", line)
                    }
                }
            }

            try {
                val pid = LinuxProcessRunner.getNativePid(process)
                if (pid != -1) {
                    Runtime.getRuntime().exec("renice -n -10 -p $pid")
                }
            } catch (e: Exception) {
                Log.w("CosmicIDE", "Failed to renice JDTLS process", e)
            }

            onProcessStarted(process)
            jdtLspProcess = process
        } catch (e: Exception) {
            Log.e("CosmicIDE", "JDTLS execution crashed", e)
        }
    }
}

suspend fun stopJdtlsProcess() {
    withContext(Dispatchers.IO) {
        jdtLspProcess?.destroy()
        jdtLspProcess = null
    }
}

/**
 * Non-blocking logger that pipes stderr to Android Logcat.
 * It uses a manual loop to ensure the stream is never closed by a 'use' block,
 * as JDTLS manages the lifecycle of its own streams.
 */
private fun streamStderrToLogcat(stderr: InputStream) {
    Thread {
        try {
            val reader = stderr.bufferedReader()
            var line: String? = reader.readLine()
            while (line != null) {
                Log.e("JDTLS-LOG", line)
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.w("JDTLS-LOG", "Stderr logger stopped: ${e.message}")
        }
    }.apply {
        name = "JDTLS-Stderr-Logger"
        isDaemon = true
        start()
    }
}

fun findMainEquinoxLauncher(jdtlsDir: File): String? {
    val pluginsDir = File(jdtlsDir, "plugins")
    if (!pluginsDir.exists() || !pluginsDir.isDirectory) return null

    val launcherJar = pluginsDir.listFiles()?.find { file ->
        val name = file.name
        name.startsWith("org.eclipse.equinox.launcher_") && name.endsWith(".jar")
    }

    return launcherJar?.absolutePath
}
