package org.cosmic.ide.project;

public class CodeTemplate {

    public static String getJavaClassTemplate(
            String packageName, String className, boolean isCreateMainMethod, String classType) {

        var simpleJavaClass =
                "public class "
                        + className
                        + " {\n"
                        + (isCreateMainMethod
                                ? "\tpublic static void main(String[] args) {\n"
                                        + "\t\tSystem.out.println(\"Hello, World!\");\n"
                                        + "\t}"
                                : "\t")
                        + "\n"
                        + "}\n";

        var simpleJavaInterface = "interface " + className + " {\n" + "\t" + "\n" + "}\n";

        var simpleJavaAbstract = "public abstract class " + className + " {\n" + "\t" +"\n" + "}\n";

        var simpleJavaEnum = "enum " + className + " {\n" + "\t" + "\n" + "}\n";

        var header = "";

        if (!isEmpty(packageName)) {
            header = "package " + packageName + ";\n" + "\n";
        }

        switch (classType) {
            case "Interface":
                return header + simpleJavaInterface;
            case "Abstract":
                return header + simpleJavaAbstract;
            case "Enum":
                return header + simpleJavaEnum;
            default:
                return header + simpleJavaClass;
        }
    }

    public static String getKotlinClassTemplate(
            String packageName, String className, boolean isCreateMainMethod, String classType) {

        var simpleKotlinClass =
                "class "
                        + className
                        + " {\n\t"
                        + (isCreateMainMethod
                                ? "fun main(args: Array<String>) {\n"
                                        + "\t\tprintln(\"Hello, World!\")\n"
                                        + "\t}"
                                : "\t")
                        + "\n"
                        + "}\n";

        var simpleKotlinDataClass = "data class " + className + "()" + "\n";

        var simpleKotlinObjectDeclaration = "object " + className + " {\n" + "\t" + "\n" + "}\n";

        var simpleKotlinInterface = "interface " + className + " {\n" + "\t" + "\n" + "}\n";

        var simpleKotlinAbstract = "abstract class " + className + " {\n" + "\t" + "\n" + "}\n";

        var simpleKotlinEnum = "enum class " + className + " {\n" + "\t" + "\n" + "}\n";

        var header = "";

        if (!isEmpty(packageName)) {
            header = "package " + packageName + "\n" + "\n";
        }

        switch (classType) {
            case "Data":
                return header + simpleKotlinDataClass;
            case "Object":
                return header + simpleKotlinObjectDeclaration;
            case "Interface":
                return header + simpleKotlinInterface;
            case "Abstract":
                return header + simpleKotlinAbstract;
            case "Enum":
                return header + simpleKotlinEnum;
            default:
                return header + simpleKotlinClass;
        }
    }

    public static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() <= 0;
    }
}
