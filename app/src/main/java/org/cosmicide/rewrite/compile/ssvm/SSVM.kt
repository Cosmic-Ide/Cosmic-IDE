package org.cosmicide.rewrite.compile.ssvm

import android.util.Log
import com.android.tools.r8.ByteDataView
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.DexIndexedConsumer
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.origin.Origin
import dalvik.system.InMemoryDexClassLoader
import dev.xdark.ssvm.VirtualMachine
import dev.xdark.ssvm.asm.Modifier
import dev.xdark.ssvm.classloading.BootClassLoader
import dev.xdark.ssvm.classloading.ClassParseResult
import dev.xdark.ssvm.execution.VMException
import dev.xdark.ssvm.fs.HostFileDescriptorManager
import dev.xdark.ssvm.jit.JitClass
import dev.xdark.ssvm.jit.JitCompiler
import dev.xdark.ssvm.jit.JitInstaller
import dev.xdark.ssvm.jvm.ManagementInterface
import dev.xdark.ssvm.mirror.InstanceJavaClass
import dev.xdark.ssvm.util.ClassUtil
import dev.xdark.ssvm.value.InstanceValue
import dev.xdark.ssvm.value.ObjectValue
import dev.xdark.ssvm.value.Value
import org.objectweb.asm.ClassReader
import org.objectweb.asm.MethodTooLargeException
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.ByteBuffer
import java.util.Objects
import java.util.zip.ZipFile

class SSVM(
    private val rtJar: ZipFile,
) {
    private lateinit var vm: VirtualMachine

    private var initialized: Boolean = false
    val isInitialized: Boolean
        get() = initialized

    fun initVM() {
        vm = object : VirtualMachine() {
            override fun createFileDescriptorManager() = HostFileDescriptorManager()

            override fun createManagementInterface() = object : ManagementInterface {
                override fun getVersion() = "null"

                override fun getStartupTime() = System.currentTimeMillis()

                override fun getInputArguments() = listOf<String>()
            }

            override fun createBootClassLoader() = BootClassLoader {

                try {
                    val entry = rtJar.getEntry("$it.class")
                        ?: return@BootClassLoader null

                    rtJar.getInputStream(entry).use { stream ->
                        val cr = ClassReader(stream)
                        val node = ClassUtil.readNode(cr)

                        return@BootClassLoader ClassParseResult(cr, node)
                    }
                } catch (e: IOException) {
                    System.err.println("[VM] Couldn't load class $it: ${Log.getStackTraceString(e)}")
                }

                return@BootClassLoader null
            }
        }

        vm.properties.apply {
            setProperty("sun.stderr.encoding", "UTF-8")
            setProperty("sun.stdout.encoding", "UTF-8")
            setProperty("sun.jnu.encoding", "UTF-8")
            setProperty("line.separator", "\n")
            setProperty("path.separator", ":")
            setProperty("file.separator", "/")
            setProperty("java.home", "/usr/lib/jvm")
            setProperty("user.home", "/home/mike")
            setProperty("user.dir", "/home/mike")
            setProperty("user.name", "mike")
            setProperty("os.version", "10.0")
            setProperty("os.arch", "amd64")
            setProperty("os.name", "Linux")
        }

        initialized = try {
            vm.bootstrap()

            // enable JIT
            val definer = JitDexClassLoader()
            vm.`interface`.registerMethodEnter { ctx ->
                val jm = ctx.method
                val count = jm.invocationCount

                if (count == 256 && !Modifier.isCompiledMethod(jm.access)) {
                    if (JitCompiler.isCompilable(jm)) {
                        try {
                            println("[JIT] Compiling $jm")

                            val jit = JitCompiler.compile(jm, 3)
                            JitInstaller.install(jm, definer, jit)
                        } catch (ex: MethodTooLargeException) {
                            val node = jm.node
                            node.access = node.access or Modifier.ACC_JIT
                        } catch (ex: Throwable) {
                            throw IllegalStateException("Could not install JIT class for $jm", ex)
                        }
                    }
                }
            }

            true
        } catch (e: Exception) {
            System.err.println("[VM] Couldn't start VM: ${Log.getStackTraceString(e)}")
            false
        }
    }

    private fun getVMSystemClassLoader(): Value {
        val ctx = vm.helper.invokeStatic(
            vm.symbols.java_lang_ClassLoader(),
            "getSystemClassLoader",
            "()Ljava/lang/ClassLoader;",
            arrayOf(),
            arrayOf(),
        )
        return ctx.result
    }

    fun addURL(jarFile: File) {
        val classLoader = getVMSystemClassLoader()
        val helper = vm.helper

        // File fileInstance = new File(jarFile.getAbsolutePath());
        val fileClass = vm.findBootstrapClass("java/io/File", true) as InstanceJavaClass
        val fileInstance = vm.memoryManager.newInstance(fileClass)
        helper.invokeExact(
            fileClass,
            "<init>",
            "(Ljava/lang/String;)V",
            arrayOf(),
            arrayOf(fileInstance, helper.newUtf8(jarFile.absolutePath))
        )

        // URI uri = fileInstance.toURI();
        val uri = helper.invokeVirtual(
            "toURI",
            "()Ljava/net/URI;",
            arrayOf(),
            arrayOf(fileInstance)
        ).result

        // URL url = uri.toURL();
        val url = helper.invokeVirtual(
            "toURL",
            "()Ljava/net/URL;",
            arrayOf(),
            arrayOf(uri)
        ).result

        // classLoader.addURL(url);
        helper.invokeVirtual(
            "addURL",
            "(Ljava/net/URL;)V",
            arrayOf(),
            arrayOf(classLoader, url)
        )
    }

    fun invokeMainMethod(className: String) {
        val helper = vm.helper
        val symbols = vm.symbols

        try {
            val classLoader = getVMSystemClassLoader()

            val klass = helper.findClass(
                classLoader as ObjectValue,
                className,
                true
            ) as InstanceJavaClass

            val m = klass.getMethod(
                "main",
                "([Ljava/lang/String;)V",
            )
            // check if main method is static
            if (java.lang.reflect.Modifier.isStatic(m.access)) {
                val method = klass.getStaticMethod(
                    "main",
                    "([Ljava/lang/String;)V",
                )
                helper.invokeStatic(
                    klass,
                    method,
                    arrayOf(),
                    arrayOf(helper.emptyArray(symbols.java_lang_String()))
                )
            } else if (java.lang.reflect.Modifier.isPublic(m.access)) {
                val method = klass.getMethod(
                    "main",
                    "([Ljava/lang/String;)V",
                )

                val instance = vm.memoryManager.newInstance(klass)
                helper.invokeExact(
                    klass,
                    method,
                    arrayOf(),
                    arrayOf(instance, helper.emptyArray(symbols.java_lang_String()))
                )
            }


        } catch (e: VMException) {
            helper.invokeVirtual(
                "printStackTrace",
                "()V",
                arrayOf(),
                arrayOf(e.oop)
            )
        }
    }

    fun release() {
        try {
            rtJar.close()
        } catch (e: Throwable) {
        }
    }

    private class JitDexClassLoader : JitInstaller.ClassDefiner {
        override fun define(jitClass: JitClass): Class<*> {
            val code = jitClass.code
            val buffer = transformBytecodeToDex(code)
                ?: throw IllegalStateException("D8 failed to translate")

            val inMemoryDexClassLoader =
                InMemoryDexClassLoader(buffer, jitClass::class.java.classLoader)
            return inMemoryDexClassLoader.loadClass(jitClass.className)
        }

        private fun transformBytecodeToDex(javaBytecode: ByteArray): ByteBuffer? {
            var result: ByteBuffer? = null

            val d8Command = D8Command.builder()
                .setDisableDesugaring(false)
                .setMinApiLevel(26)
                .addClassProgramData(javaBytecode, Origin.unknown())
                .setProgramConsumer(object : DexIndexedConsumer {
                    override fun finished(p0: DiagnosticsHandler?) {}

                    override fun accept(
                        fileIndex: Int,
                        data: ByteDataView,
                        descriptors: MutableSet<String>?,
                        handler: DiagnosticsHandler?
                    ) {
                        result = ByteBuffer.wrap(data.copyByteData())
                    }
                })
                .build()
            D8.run(d8Command)

            return result
        }
    }

    companion object {
        fun throwableToString(throwable: InstanceValue): String? {
            Objects.requireNonNull(throwable, "throwable")
            val javaClass = throwable.javaClass
            val vm = javaClass.vm
            val helper = vm.helper
            try {
                val stringWriter = helper.newInstance(
                    vm.findBootstrapClass("java/io/StringWriter") as InstanceJavaClass,
                    "()V"
                )
                val printWriter = helper.newInstance(
                    vm.findBootstrapClass("java/io/PrintWriter") as InstanceJavaClass,
                    "(Ljava/io/Writer;)V",
                    stringWriter
                )
                helper.invokeVirtual(
                    "printStackTrace",
                    "(Ljava/io/PrintWriter;)V",
                    arrayOfNulls(0),
                    arrayOf<Value>(throwable, printWriter)
                )
                val throwableAsString = helper.invokeVirtual(
                    "toString",
                    "()Ljava/lang/String;",
                    arrayOfNulls(0),
                    arrayOf<Value>(stringWriter)
                ).result
                return helper.readUtf8(throwableAsString)
            } catch (ignored: VMException) {
            }
            val writer = StringWriter()
            helper.toJavaException(throwable).printStackTrace(PrintWriter(writer))
            return writer.toString()
        }
    }

}