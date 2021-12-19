package com.pranav.java.lib_android.java

import com.pranav.ide.dx.command.dexer.Main
import com.pranav.java.lib_android.Task
import java.util.*

class DexTask : Task() {
    var input: String? = null
    override fun doFullTask() {
        val args = listOf(
            "--debug",
            "--verbose",
            "--output=/storage/emulated/0/classesTest.dex",
            "/storage/emulated/0/classesTest.jar"
        )
        Main.clearInternTables()
        val arguments = Main.Arguments()
        val method =
            Main.Arguments::class.java.getDeclaredMethod("parse", Array<String>::class.java)
        method.isAccessible = true
        method.invoke(arguments, args.toTypedArray() as Any)
        Main.run(arguments)
    }

    override fun getTaskName(): String {
        return "Compile Java Task"
    }
}