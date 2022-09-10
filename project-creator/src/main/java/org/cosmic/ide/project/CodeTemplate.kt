package org.cosmic.ide.project

object CodeTemplate {

    @JvmStatic
    fun getJavaClassTemplate(
            packageName: String, className: String, isCreateMainMethod: Boolean): String {
        val header = if (!isEmpty(packageName)) "package $packageName;\n\n" else ""
        return header
                + "import java.util.*;\n\n"
                + "public class $className {\n"
                + (if (isCreateMainMethod)
                        "\tpublic static void main(String[] args) {\n"
                                + "\t\tSystem.out.println(\"Hello, World!\");\n"
                                + "\t}"
                        else "\t")
                + "\n"
                + "}\n"
    }

    @JvmStatic
    fun getKotlinClassTemplate(
            packageName: String, className: String, isCreateMainMethod: Boolean): String {
        val header = if (!isEmpty(packageName)) "package $packageName\n\n" else ""

        return header
                + "import java.util.*\n\n"
                + "class $className {\n\t"
                + (if (isCreateMainMethod)
                        "fun main(args: Array<String>) {\n"
                                + "\t\tprintln(\"Hello, World!\")\n"
                                + "\t}"
                        else "\t")
                + "\n"
                + "}\n"
    }

    fun isEmpty(s:al CharSequence): Boolean {
        return (s == null || s.length() == 0)
    }
}
