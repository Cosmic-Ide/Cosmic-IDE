package org.cosmicide.tooling

import android.content.Context
import java.io.File

object ToolingServerManager {
    private val lock = Any()
    private var currentServer: ToolingServer? = null

    fun forProject(context: Context, projectDir: File): ToolingServer {
        val appContext = context.applicationContext
        val normalizedProjectDir = projectDir.absoluteFile

        synchronized(lock) {
            currentServer?.let { server ->
                if (server.projectDir.absolutePath == normalizedProjectDir.absolutePath) {
                    return server
                }

                server.stop()
            }

            return ToolingServer.bundled(appContext, normalizedProjectDir).also { server ->
                currentServer = server
            }
        }
    }

    fun stopCurrent() {
        synchronized(lock) {
            currentServer?.stop()
            currentServer = null
        }
    }
}
