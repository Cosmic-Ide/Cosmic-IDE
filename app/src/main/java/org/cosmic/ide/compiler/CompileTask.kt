package org.cosmic.ide.compiler

import android.content.DialogInterface
import android.content.Intent
import android.os.Looper
import android.util.Log
import org.cosmic.ide.App.Companion.getDefaultPreferences
import org.cosmic.ide.R
import org.cosmic.ide.activity.ConsoleActivity
import org.cosmic.ide.activity.MainActivity
import org.cosmic.ide.android.exception.CompilationFailedException
import org.cosmic.ide.android.task.dex.D8Task
import org.cosmic.ide.android.task.java.JavaCompiler
import org.cosmic.ide.android.task.kotlin.KotlinCompiler
import org.cosmic.ide.util.Constants

class CompileTask(private val activity: MainActivity, private val listener: CompilerListeners) :
    Thread() {
    private var showExecuteDialog = false
    private val compilers: Compilers = Compilers(
        KotlinCompiler(),
        JavaCompiler(getDefaultPreferences()),
        D8Task()
    )
    private val STAGE_KOTLINC: String = activity.getString(R.string.compilation_stage_kotlinc)
    private val STAGE_JAVAC: String = activity.getString(R.string.compilation_stage_javac)
    private val STAGE_D8: String = activity.getString(R.string.compilation_stage_d8)
    private val TAG = "CompileTask"

    override fun run() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        listener.onStart()
        compileKotlin()
        Log.i(TAG, "success till now: ${listener.isSuccessTillNow}")
        if (!listener.isSuccessTillNow) return
        compileJava()
        if (!listener.isSuccessTillNow) return
        compileDex()
        if (!listener.isSuccessTillNow) return
        executeDex()
    }

    fun setExecution(enable: Boolean) {
        showExecuteDialog = enable
    }

    private fun compileKotlin() {
        Log.i(TAG, "Starting kotlin compiler.")
        try {
            listener.onCurrentBuildStageChanged(STAGE_KOTLINC)
            compilers.kotlin.doFullTask(activity.project)
        } catch (e: CompilationFailedException) {
            listener.onFailed(e.localizedMessage)
            return
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "No files", e)
            return
        } catch (e: Throwable) {
            listener.onFailed(Log.getStackTraceString(e))
            return
        }
        Log.i(TAG, "Kotlin compilation successful.")
    }

    private fun compileJava() {
        Log.i(TAG, "Starting java compiler.")
        try {
            listener.onCurrentBuildStageChanged(STAGE_JAVAC)
            compilers.java.doFullTask(activity.project)
        } catch (e: CompilationFailedException) {
            listener.onFailed(e.localizedMessage)
        } catch (e: Throwable) {
            listener.onFailed(Log.getStackTraceString(e))
        }
        Log.i(TAG, "Java compilation successful.")
    }

    private fun compileDex() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_D8)
            compilers.dex.doFullTask(activity.project)
        } catch (e: Exception) {
            listener.onFailed(e.localizedMessage)
        }
    }

    private fun executeDex() {
        try {
            listener.onSuccess()
            val classes = activity.classesFromDex ?: return
            if (showExecuteDialog) {
                // if there is only one class, there is no need to show a dialog
                val intent = Intent(activity, ConsoleActivity::class.java)
                intent.putExtra(
                    Constants.PROJECT_PATH, activity.project.projectDirPath
                )
                if (classes.size == 1) {
                    intent.putExtra("class_to_execute", classes[0])
                    activity.startActivity(intent)
                    return
                }
                activity.listDialog(
                    activity.getString(R.string.select_class_run),
                    classes
                ) { _: DialogInterface?, item: Int ->
                    intent.putExtra("class_to_execute", classes[item])
                    activity.startActivity(intent)
                }
            }
        } catch (e: Throwable) {
            listener.onFailed(Log.getStackTraceString(e))
        }
    }

    interface CompilerListeners {
        fun onStart()
        fun onCurrentBuildStageChanged(stage: String)
        fun onSuccess()
        fun onFailed(errorMessage: String?)
        val isSuccessTillNow: Boolean
    }
}