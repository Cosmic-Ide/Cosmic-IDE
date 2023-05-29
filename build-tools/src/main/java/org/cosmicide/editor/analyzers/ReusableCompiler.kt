/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers

import com.intellij.openapi.command.impl.CommandLog
import com.sun.source.util.JavacTask
import com.sun.source.util.TaskEvent
import com.sun.source.util.TaskListener
import com.sun.tools.javac.api.JavacTaskImpl
import com.sun.tools.javac.api.JavacTool
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.api.MultiTaskListener
import com.sun.tools.javac.code.Types
import com.sun.tools.javac.comp.Annotate
import com.sun.tools.javac.comp.Check
import com.sun.tools.javac.comp.CompileStates
import com.sun.tools.javac.comp.Enter
import com.sun.tools.javac.comp.Modules
import com.sun.tools.javac.main.Arguments
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Context.Factory
import com.sun.tools.javac.util.DefinedBy
import com.sun.tools.javac.util.Log
import org.cosmicide.editor.analyzers.services.CancelService
import org.cosmicide.editor.analyzers.services.NBAttr
import org.cosmicide.editor.analyzers.services.NBClassFinder
import org.cosmicide.editor.analyzers.services.NBEnter
import org.cosmicide.editor.analyzers.services.NBJavaCompiler
import org.cosmicide.editor.analyzers.services.NBJavacTrees
import org.cosmicide.editor.analyzers.services.NBMemberEnter
import org.cosmicide.editor.analyzers.services.NBParserFactory
import org.cosmicide.editor.analyzers.services.NBResolve
import org.cosmicide.editor.analyzers.services.NBTreeMaker
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject

/**
 * A pool of reusable JavacTasks. When a task is no valid anymore, it is returned to the pool, and its Context may be
 * reused for future processing in some cases. The reuse is achieved by replacing some components (most notably
 * JavaCompiler and Log) with reusable counterparts, and by cleaning up leftovers from previous compilation.
 *
 *
 * For each combination of options, a separate task/context is created and kept, as most option values are cached
 * inside components themselves.
 *
 *
 * When the compilation redefines sensitive classes (e.g. classes in the the java.* packages), the task/context is
 * not reused.
 *
 *
 * When the task is reused, then packages that were already listed won't be listed again.
 *
 *
 * Care must be taken to only return tasks that won't be used by the original caller.
 *
 *
 * Care must also be taken when custom components are installed, as those are not cleaned when the task/context is
 * reused, and subsequent getTask may return a task based on a context with these custom components.
 *
 *
 * **This is NOT part of any supported API. If you write code that depends on this, you do so at your own risk. This
 * code and its internal interfaces are subject to change or deletion without notice.**
 */
class ReusableCompiler {
    private val currentOptions: MutableList<String> = ArrayList()
    var currentContext: ReusableContext? = null
        private set

    @Volatile
    private var checkedOut = false
    private val cancelService = CancelServiceImpl()

    class CancelServiceImpl : CancelService() {
        private val canceled = AtomicBoolean(false)
        private val running = AtomicBoolean(false)
        fun cancel() {
            canceled.set(true)
        }

        fun isRunning(): Boolean {
            return running.get()
        }

        fun setRunning(value: Boolean) {
            running.set(value)
        }

        override fun isCanceled(): Boolean {
            return false
        }

        override fun onCancel() {
            CommandLog.LOG.info("Compilation task cancelled.x")
            running.set(false)
        }
    }

    /**
     * Creates a new task as if by JavaCompiler and runs the provided worker with it. The
     * task is only valid while the worker is running. The internal structures may be reused from some previous
     * compilation.
     *
     * @param fileManager a file manager; if `null` use the compiler's standard filemanager
     * @param diagnosticListener a diagnostic listener; if `null` use the compiler's default method for reporting
     * diagnostics
     * @param options compiler options, `null` means no options
     * @param classes names of classes to be processed by annotation processing, `null` means no class names
     * @param compilationUnits the compilation units to compile, `null` means no compilation units
     * @return an object representing the compilation
     * @throws RuntimeException if an unrecoverable error occurred in a user supplied component. The [     ][Throwable.getCause] will be the error in user code.
     * @throws IllegalArgumentException if any of the options are invalid, or if any of the given compilation units are
     * of other kind than [source][JavaFileObject.Kind.SOURCE]
     */
    fun getTask(
        fileManager: JavaFileManager?,
        diagnosticListener: DiagnosticListener<in JavaFileObject?>?,
        options: Iterable<String>,
        classes: Iterable<String?>?,
        compilationUnits: Iterable<JavaFileObject?>?
    ): Borrow {
        if (checkedOut) {
            throw IllegalStateException("Task is already checked out")
        }
        checkedOut = true
        val opts = StreamSupport.stream(options.spliterator(), false)
            .collect(Collectors.toList())
        if (opts != currentOptions) {
            val difference: MutableList<String> = ArrayList(currentOptions)
            difference.removeAll(opts)
            CommandLog.LOG.warn("Options changed, creating new compiler \n difference: $difference")
            currentOptions.clear()
            currentOptions.addAll(opts)
            currentContext = ReusableContext(ArrayList(opts), cancelService)
        }
        val task = systemProvider.getTask(
            null, fileManager, diagnosticListener, opts, classes, compilationUnits, currentContext
        ) as JavacTaskImpl
        task.addTaskListener(currentContext)
        cancelService.setRunning(true)
        checkedOut = false
        return Borrow(task)
    }

    inner class Borrow internal constructor(val task: JavacTask) :
        AutoCloseable {
        var closed = false
        override fun close() {
            if (closed) return
            // not returning the context to the pool if task crashes with an exception
            // the task/context may be in a broken state
            currentContext!!.clear()
            try {
                val method = JavacTaskImpl::class.java.getDeclaredMethod("cleanup")
                method.isAccessible = true
                method.invoke(task)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            } finally {
                checkedOut = false
                closed = true
            }
        }
    }

    class ReusableContext(var arguments: List<String>, cancelService: CancelService) : Context(),
        TaskListener {
        init {
            put(Log.logKey, ReusableLog.factory)
            put(JavaCompiler.compilerKey, ReusableJavaCompiler.factory)
            registerServices(this, cancelService)
        }

        fun clear() {
            drop(Arguments.argsKey)
            drop(DiagnosticListener::class.java)
            drop(Log.outKey)
            drop(Log.errKey)
            drop(JavaFileManager::class.java)
            drop(JavacTask::class.java)
            drop(JavacTrees::class.java)
            drop(JavacElements::class.java)
            if (ht[Log.logKey] is ReusableLog) {
                // log already inited - not first round
                (Log.instance(this) as ReusableLog).clear()
                Enter.instance(this).newRound()
                (JavaCompiler.instance(this) as ReusableJavaCompiler).clear()
                Types.instance(this).newRound()
                Check.instance(this).newRound()
                Modules.instance(this).newRound()
                Annotate.instance(this).newRound()
                CompileStates.instance(this).clear()
                MultiTaskListener.instance(this).clear()
            }
        }

        @DefinedBy(DefinedBy.Api.COMPILER_TREE)
        override fun finished(e: TaskEvent) {
            // do nothing
        }

        @DefinedBy(DefinedBy.Api.COMPILER_TREE)
        override fun started(e: TaskEvent) {
            // do nothing
        }

        fun <T> drop(k: Key<T>?) {
            ht.remove(k)
        }

        fun <T> drop(c: Class<T>?) {
            ht.remove(key(c))
        }

        /**
         * Reusable JavaCompiler; exposes a method to clean up the component from leftovers associated with previous
         * compilations.
         */
        internal class ReusableJavaCompiler(context: Context?) : NBJavaCompiler(context) {
            override fun close() {
                // do nothing
            }

            fun clear() {
                newRound()
            }

            override fun checkReusable() {
                // do nothing - it's ok to reuse the compiler
            }

            companion object {
                val factory =
                    Factory<JavaCompiler> { context: Context? -> ReusableJavaCompiler(context) }
            }
        }

        /**
         * Reusable Log; exposes a method to clean up the component from leftovers associated with previous
         * compilations.
         */
        internal class ReusableLog(var context: Context) : Log(
            context
        ) {
            fun clear() {
                recorded.clear()
                sourceMap.clear()
                nerrors = 0
                nwarnings = 0
                // Set a fake listener that will lazily lookup the context for the 'real' listener. Since
                // this field is never updated when a new task is created, we cannot simply reset the field
                // or keep old value. This is a hack to workaround the limitations in the current infrastructure.
                diagListener = object : DiagnosticListener<JavaFileObject> {
                    var cachedListener: DiagnosticListener<JavaFileObject>? = null

                    @DefinedBy(DefinedBy.Api.COMPILER)
                    override fun report(diagnostic: Diagnostic<out JavaFileObject>) {
                        if (cachedListener == null) {
                            cachedListener =
                                context.get(DiagnosticListener::class.java) as DiagnosticListener<JavaFileObject>
                        }
                        cachedListener!!.report(diagnostic)
                    }
                }
            }

            companion object {
                val factory = Factory<Log> { context: Context -> ReusableLog(context) }
            }
        }

        companion object {
            private fun registerServices(context: Context, cancelService: CancelService) {
                NBAttr.preRegister(context)
                NBParserFactory.preRegister(context)
                NBTreeMaker.preRegister(context)
                NBJavacTrees.preRegister(context)
                NBResolve.preRegister(context)
                NBEnter.preRegister(context)
                NBMemberEnter.preRegister(context, false)
                NBClassFinder.preRegister(context)
                context.put(CancelService.cancelServiceKey, cancelService)
            }
        }
    }

    companion object {
        private val systemProvider = JavacTool.create()
    }
}