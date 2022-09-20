package com.tyron.kotlin.completion.core.model

import com.tyron.builder.BuildModule
import org.cosmic.ide.project.KotlinProject
import com.tyron.common.TestUtil
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.kotlin.asJava.classes.FacadeCache
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector.Companion.NONE
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.ASTNode
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.AsyncExecutionService
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.*
import com.intellij.pom.PomModel
import com.intellij.pom.PomModelAspect
import com.intellij.pom.PomTransaction
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.*
import com.intellij.psi.codeStyle.ChangedRangesInfo
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.Indent
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.psi.impl.PsiNameHelperImpl
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME
import org.jetbrains.kotlin.config.LanguageVersion.Companion.LATEST_STABLE
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

fun getEnvironment(module: KotlinProject): KotlinCoreEnvironment {
    return KotlinEnvironment.getEnvironment(module)
}

fun getEnvironment(project: com.intellij.openapi.project.Project): KotlinCoreEnvironment? {
    val javaProject = KotlinEnvironment.getJavaProject(project)
    return javaProject?.let { KotlinEnvironment.getEnvironment(it) }
}

class KotlinEnvironment private constructor(val module: KotlinProject, disposable: Disposable) :
    KotlinCommonEnvironment(disposable) {

    val index by lazy { JvmDependenciesIndexImpl(getRoots().toList()) }

    init {
        configureClasspath(module)

        with(project) {
            registerService(FacadeCache::class.java, FacadeCache(project))
        }
    }

    private fun configureClasspath(kotlinModule: KotlinProject) {
        val androidJar = BuildModule.getAndroidJar()
        if (androidJar.exists()) {
            addToClassPath(androidJar)
        }

        addToClassPath(kotlinModule.getSrcDirPath())

        File(kotlinModule.libraries).walkBottomUp().forEach {
            if (it.extension == "jar") {
                addToClassPath(it)
            }
        }
    }

    companion object {
        private val cachedEnvironment = CachedEnvironment<KotlinProject, KotlinCoreEnvironment>()
        private val environmentCreation = { module: KotlinProject ->
            val environment = KotlinCoreEnvironment.createForProduction(
                Disposer.newDisposable("Project Env ${module.name}"),
                getConfiguration(module),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            environment.addKotlinSourceRoots(listOf(module.kotlinDirectory, module.getSrcDirPath()))

            CoreApplicationEnvironment.registerApplicationExtensionPoint(DocumentWriteAccessGuard.EP_NAME, DocumentWriteAccessGuard::class.java);
            environment.projectEnvironment.registerProjectExtensionPoint(
                ExtensionPointName.create(PsiTreeChangeListener.EP.name),
                PsiTreeChangeListener::class.java
            )

            (environment.projectEnvironment.environment as CoreApplicationEnvironment)
                .registerApplicationService(AsyncExecutionService::class.java, object : AsyncExecutionService() {

                    val executor = Executors.newSingleThreadExecutor()

                    override fun createWriteThreadExecutor(p0: ModalityState): AppUIExecutor {
                        return object : AppUIExecutor {
                            override fun expireWith(p0: Disposable): AppUIExecutor {
                                TODO("Not yet implemented")
                            }

                            override fun submit(p0: Runnable): CancellablePromise<*> {
                                val result = executor.submit(p0)
                                return CancellablePromiseWrapper(result)
                            }

                            override fun later(): AppUIExecutor {
                                TODO("Not yet implemented")
                            }

                        }
                    }

                    override fun <T : Any?> buildNonBlockingReadAction(callable: Callable<T>): NonBlockingReadAction<T> {
                        return NonBlockingReadActionImpl(callable)
                    }

                })
            environment.projectEnvironment.project.picoContainer.unregisterComponent(PsiDocumentManager::class.java.name)

            registerProjectDependentServices(module, environment.project as MockProject)
            environment
        }

        @Suppress("UNCHECKED_CAST")
        private fun registerProjectDependentServices(module: KotlinProject, project: MockProject) {
            project.registerService(PomModel::class.java, object: PomModel {
                val userDataHolder = UserDataHolderBase()
                val treeAspect = TreeAspect()
                override fun <T : Any?> getUserData(p0: Key<T>): T? {
                    return userDataHolder.getUserData(p0)
                }

                override fun <T : Any?> putUserData(p0: Key<T>, p1: T?) {
                    return userDataHolder.putUserData(p0, p1)
                }

                override fun <T : PomModelAspect?> getModelAspect(p0: Class<T>): T {
                    return treeAspect as T
                }

                override fun runTransaction(p0: PomTransaction) {
                    p0.run()
                }

            })
            project.registerService(CodeStyleManager::class.java, object: CodeStyleManager() {
                override fun getProject(): Project {
                    TODO("Not yet implemented")
                }

                override fun reformat(p0: PsiElement): PsiElement {
                    TODO("Not yet implemented")
                }

                override fun reformat(p0: PsiElement, p1: Boolean): PsiElement {
                    TODO("Not yet implemented")
                }

                override fun reformatRange(p0: PsiElement, p1: Int, p2: Int): PsiElement {
                    TODO("Not yet implemented")
                }

                override fun reformatRange(
                    p0: PsiElement,
                    p1: Int,
                    p2: Int,
                    p3: Boolean
                ): PsiElement {
                    TODO("Not yet implemented")
                }

                override fun reformatText(p0: PsiFile, p1: Int, p2: Int) {
                    TODO("Not yet implemented")
                }

                override fun reformatText(p0: PsiFile, p1: MutableCollection<TextRange>) {
                    TODO("Not yet implemented")
                }

                override fun reformatTextWithContext(p0: PsiFile, p1: ChangedRangesInfo) {
                    TODO("Not yet implemented")
                }

                override fun adjustLineIndent(p0: PsiFile, p1: TextRange?) {
                    TODO("Not yet implemented")
                }

                override fun adjustLineIndent(p0: PsiFile, p1: Int): Int {
                    TODO("Not yet implemented")
                }

                override fun adjustLineIndent(p0: Document, p1: Int): Int {
                    TODO("Not yet implemented")
                }

                override fun isLineToBeIndented(p0: PsiFile, p1: Int): Boolean {
                    TODO("Not yet implemented")
                }

                override fun getLineIndent(p0: PsiFile, p1: Int): String? {
                    TODO("Not yet implemented")
                }

                override fun getLineIndent(p0: Document, p1: Int): String? {
                    TODO("Not yet implemented")
                }

                override fun getIndent(p0: String?, p1: FileType?): Indent {
                    TODO("Not yet implemented")
                }

                override fun fillIndent(p0: Indent?, p1: FileType?): String {
                    TODO("Not yet implemented")
                }

                override fun zeroIndent(): Indent {
                    TODO("Not yet implemented")
                }

                override fun reformatNewlyAddedElement(p0: ASTNode, p1: ASTNode) {
                    TODO("Not yet implemented")
                }

                override fun isSequentialProcessingAllowed(): Boolean {
                    TODO("Not yet implemented")
                }

                override fun performActionWithFormatterDisabled(p0: Runnable?) {
                    p0?.run()
                }

                override fun <T : Throwable?> performActionWithFormatterDisabled(p0: ThrowableRunnable<T>?) {
                    p0?.run()
                }

                override fun <T : Any?> performActionWithFormatterDisabled(p0: Computable<T>?): T {
                    return p0!!.compute()
                }

            })
            project.registerService(PsiNameHelper::class.java, PsiNameHelperImpl(project))
            project.registerService(PsiDocumentManager::class.java, object: PsiDocumentManagerBase(project) {
                override fun removeListener(p0: Listener) {
                    TODO("Not yet implemented")
                }

                override fun performLaterWhenAllCommitted(p0: Runnable, p1: ModalityState?) {
                    TODO("Not yet implemented")
                }

            })
        }

        @JvmStatic
        fun getEnvironment(kotlinModule: KotlinProject): KotlinCoreEnvironment =
            cachedEnvironment.getOrCreateEnvironment(kotlinModule, environmentCreation)

        @JvmStatic
        fun removeEnvironment(kotlinModule: KotlinProject) {
            cachedEnvironment.removeEnvironment(kotlinModule)
//            KotlinPsiManager.invalidateCachedProjectSourceFiles()
//            KotlinAnalysisFileCache.resetCache()
//            KotlinAnalysisProjectCache.resetCache(eclipseProject)
        }

        @JvmStatic
        fun removeAllEnvironments() {
            cachedEnvironment.removeAllEnvironments()
//            KotlinPsiManager.invalidateCachedProjectSourceFiles()
//            KotlinAnalysisFileCache.resetCache()
//            KotlinAnalysisProjectCache.resetAllCaches()
        }

        @JvmStatic
        fun getJavaProject(project: com.intellij.openapi.project.Project):
                KotlinProject? = cachedEnvironment.getEclipseResource(project)

    }

}

private fun getConfiguration(module: KotlinProject): CompilerConfiguration {
    val configuration = CompilerConfiguration()
    val map: HashMap<LanguageFeature, LanguageFeature.State> = HashMap()
    for (value in LanguageFeature.values()) {
        map[value] = LanguageFeature.State.ENABLED
    }
    val settings: LanguageVersionSettings = LanguageVersionSettingsImpl(
        LATEST_STABLE,
        ApiVersion.createByLanguageVersion(LATEST_STABLE),
        emptyMap(),
        map
    )

    if (TestUtil.isWindows()) {
        configuration.put(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, """C:\Users\tyron scott\StudioProjects\CodeAssist\kotlin-completion\src\test\resources""")
    }
    configuration.put(MODULE_NAME, module.name)
    configuration.put(LANGUAGE_VERSION_SETTINGS, settings)
    configuration.put(
        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        NONE
    )
    configuration.put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, true)
    configuration.put(JVMConfigurationKeys.NO_JDK, true)

    configuration.addJvmSdkRoots(listOf(BuildModule.getAndroidJar()))

    configuration.addJavaSourceRoot(module.getSrcDirPath())
    configuration.addJvmClasspathRoots(module.getLibDirPath())

    return configuration
}