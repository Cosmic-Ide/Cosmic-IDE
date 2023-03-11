package org.cosmicide.rewrite

import android.app.Application
import com.itsaky.androidide.config.JavacConfigProvider
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.rewrite.util.FileUtil
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import java.io.FileNotFoundException

class App : Application() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var indexFile: File

    override fun onCreate() {
        super.onCreate()
        FileUtil.init(this)
        indexFile = File(FileUtil.dataDir, "index.json")
        scope.launch {
            disableModules()
            loadTextmateTheme()
            extractFiles()
        }
    }

    private suspend fun extractFiles() {
        withContext(Dispatchers.IO) {
            assets.open("index.json").use { input ->
                indexFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun disableModules() {
        JavacConfigProvider.disableModules()
    }

    private fun loadTextmateTheme() {
        val fileProvider = AssetsFileResolver(assets)
        FileProviderRegistry.getInstance().addFileProvider(fileProvider)
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        val themeRegistry = ThemeRegistry.getInstance()
        themeRegistry.loadTheme(loadTheme("darcula.json", "darcula"))
        themeRegistry.loadTheme(loadTheme("QuietLight.tmTheme", "QuietLight"))
        themeRegistry.setTheme("QuietLight")
    }

    private fun loadTheme(fileName: String, themeName: String): ThemeModel {
        val inputStream = FileProviderRegistry.getInstance().tryGetInputStream("textmate/$fileName")
            ?: throw FileNotFoundException("Theme file not found: $fileName")
        val source = IThemeSource.fromInputStream(inputStream, fileName, null)
        return ThemeModel(source, themeName)
    }
}