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
import org.cosmicide.rewrite.util.FileUtil
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        FileUtil.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            disableModules()
            loadTextmateTheme()
            extractFiles()
        }
    }

    private fun extractFiles() {
        val indexFile = File(FileUtil.dataDir, "index.json")
        assets.open("index.json").use { input ->
            indexFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun disableModules() {
        JavacConfigProvider.disableModules()
    }

    private fun loadTextmateTheme() {
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                assets
            )
        )
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        val themeRegistry = ThemeRegistry.getInstance()
        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream("textmate/darcula.json"),
                    "darcula.json",
                    null
                ),
                "darcula"
            )
        )
        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance()
                        .tryGetInputStream("textmate/QuietLight.tmTheme"),
                    "QuietLight.tmTheme",
                    null
                ),
                "QuietLight"
            )
        )
        themeRegistry.setTheme("QuietLight")
    }
}