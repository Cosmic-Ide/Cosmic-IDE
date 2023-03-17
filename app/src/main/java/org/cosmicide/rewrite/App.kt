package org.cosmicide.rewrite

import android.app.Application
import android.util.Log
import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.config.JavacConfigProvider
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.rewrite.util.FileUtil
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import java.io.FileNotFoundException

class App : Application() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var indexFile: File

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        FileUtil.init(this)
        indexFile = File(FileUtil.dataDir, INDEX_FILE_NAME)
        scope.launch {
            try {
                disableModules()
                loadTextmateTheme()
                extractFiles()
            } catch (e: Throwable) {
                Log.e("App", "Initialization failed: $e")
            }
        }
    }

    private suspend fun extractFiles() {
        withContext(Dispatchers.IO) {
            assets.open(INDEX_FILE_NAME).use { input ->
                indexFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            assets.open(ANDROID_JAR).use { input ->
                File(FileUtil.classpathDir, ANDROID_JAR).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            assets.open(KOTLIN_STDLIB).use { input ->
                File(FileUtil.classpathDir, KOTLIN_STDLIB).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private suspend fun disableModules() {
        withContext(Dispatchers.IO) {
            JavacConfigProvider.disableModules()
        }
    }

    private suspend fun loadTextmateTheme() {
        withContext(Dispatchers.IO) {
            val fileProvider = AssetsFileResolver(assets)
            FileProviderRegistry.getInstance().addFileProvider(fileProvider)
            GrammarRegistry.getInstance().loadGrammars(LANGUAGES_FILE_PATH)
            val themeRegistry = ThemeRegistry.getInstance()
            themeRegistry.loadTheme(loadTheme(DARCULA_THEME_FILE_NAME, DARCULA_THEME_NAME))
            themeRegistry.loadTheme(loadTheme(QUIET_LIGHT_THEME_FILE_NAME, QUIET_LIGHT_THEME_NAME))
            // TODO: if dark theme is enabled, set the darcula theme
            // TODO: otherwise, set the quiet light theme
            themeRegistry.setTheme(DARCULA_THEME_NAME)
        }
    }

    private fun loadTheme(fileName: String, themeName: String): ThemeModel {
        val inputStream = FileProviderRegistry.getInstance().tryGetInputStream("$TEXTMATE_DIR/$fileName")
            ?: throw FileNotFoundException("Theme file not found: $fileName")
        val source = IThemeSource.fromInputStream(inputStream, fileName, null)
        return ThemeModel(source, themeName)
    }

    companion object {
        private const val INDEX_FILE_NAME = "index.json"
        private const val ANDROID_JAR = "android.jar"
        private const val KOTLIN_STDLIB = "kotlin-stdlib-1.8.0.jar"
        private const val LANGUAGES_FILE_PATH = "textmate/languages.json"
        private const val TEXTMATE_DIR = "textmate"
        private const val DARCULA_THEME_FILE_NAME = "darcula.json"
        private const val DARCULA_THEME_NAME = "darcula"
        private const val QUIET_LIGHT_THEME_FILE_NAME = "QuietLight.tmTheme"
        private const val QUIET_LIGHT_THEME_NAME = "QuietLight"
    }
}