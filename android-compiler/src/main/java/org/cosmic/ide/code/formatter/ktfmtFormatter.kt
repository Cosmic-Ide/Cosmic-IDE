package org.cosmic.ide.code.formatter

import com.facebook.ktfmt.cli.Main

class ktfmtFormatter(
    private val path: String
) {
    fun format() {
        val args = listOf(
            "--kotlinlang-style",
            path
        )
        Main(System.`in`, System.out, System.err, args.toTypedArray()).run()
    }
}
