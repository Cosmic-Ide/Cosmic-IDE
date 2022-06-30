package com.pranav.project.mode;

public class JavaTemplate {

    public static String getClassTemplate(String packageName, String className, boolean isCreateMainMethod) {
        String header = "";
        if(!JavaTemplate.isEmpty(packageName)) {
            header = "package " + packageName + ";\n" + "\n";
        }
        return header + 
            "import java.util.*;\n\n" +
            "public class " + className + " {\n" +
            (isCreateMainMethod ? "\tpublic static void main(String[] args) {\n" +
                "\t\tSystem.out.println(\"Hello, World!\");\n" +
                "\t}"
                : "\t") +
            "\n" +
            "}\n";
    }

    public static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }
}