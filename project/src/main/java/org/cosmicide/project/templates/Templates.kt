package org.cosmicide.project.templates

/**
 * Returns the template for a Java class.
 *
 * @param className The name of the class.
 * @param packageName The name of the package that the class should be placed in.
 * @param body Optional body of the main method of the class.
 * @return The template for a Java class.
 */
fun javaClass(className: String, packageName: String, body: String = ""): String {
    return StringBuilder()
        .appendLine("package $packageName;")
        .appendLine()
        .appendLine("public class $className {")
        .appendLine("    public static void main(String[] args) {")
        .append(body)
        .appendLine("    }")
        .appendLine("}")
        .toString()
}

/**
 * Returns the template for a Java interface.
 *
 * @param interfaceName The name of the interface.
 * @param packageName The name of the package that the interface should be placed in.
 * @return The template for a Java interface.
 */
fun javaInterface(interfaceName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName;")
        .appendLine()
        .appendLine("public interface $interfaceName {")
        .appendLine("}")
        .toString()
}

/**
 * Returns the template for a Java enum.
 *
 * @param enumName The name of the enum.
 * @param packageName The name of the package that the enum should be placed in.
 * @return The template for a Java enum.
 */
fun javaEnum(enumName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName;")
        .appendLine()
        .appendLine("public enum $enumName {")
        .appendLine("}")
        .toString()
}

/**
 * Returns the template for a Kotlin class.
 *
 * @param className The name of the class.
 * @param packageName The name of the package that the class should be placed in.
 * @param body Optional body of the main method of the class.
 * @return The template for a Kotlin class.
 */
fun kotlinClass(className: String, packageName: String, body: String = ""): String {
    return StringBuilder()
        .appendLine("package $packageName")
        .appendLine()
        .appendLine("class $className {")
        .appendLine("    fun main(args: Array<String>) {")
        .append(body)
        .appendLine("    }")
        .appendLine("}")
        .toString()
}

/**
 * Returns the template for a Kotlin interface.
 *
 * @param interfaceName The name of the interface.
 * @param packageName The name of the package that the interface should be placed in.
 * @return The template for a Kotlin interface.
 */
fun kotlinInterface(interfaceName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName")
        .appendLine()
        .appendLine("interface $interfaceName {")
        .appendLine("}")
        .toString()
}

/**
 * Returns the template for a Kotlin object.
 *
 * @param objectName The name of the object.
 * @param packageName The name of the package that the object should be placed in.
 * @return The template for a Kotlin object.
 */
fun kotlinObject(objectName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName")
        .appendLine()
        .appendLine("object $objectName {")
        .appendLine("}")
        .toString()
}

/**
 * Returns the template for a Kotlin enum.
 *
 * @param enumName The name of the enum.
 * @param packageName The name of the package that the enum should be placed in.
 * @return The template for a Kotlin enum.
 */
fun kotlinEnum(enumName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName")
        .appendLine()
        .appendLine("enum class $enumName {")
        .appendLine("}")
        .toString()
}

/**
 * Returns the template for a Kotlin annotation.
 *
 * @param annotationName The name of the annotation.
 * @param packageName The name of the package that the annotation should be placed in.
 * @return The template for a Kotlin annotation.
 */
fun kotlinAnnotation(annotationName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName")
        .appendLine()
        .appendLine("annotation class $annotationName")
        .toString()
}

/**
 * Returns the template for a Kotlin data class.
 *
 * @param dataClassName The name of the data class.
 * @param packageName The name of the package that the data class should be placed in.
 * @return The template for a Kotlin data class.
 */
fun kotlinDataClass(dataClassName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName")
        .appendLine()
        .appendLine("data class $dataClassName(")
        .appendLine(")")
        .toString()
}

/**
 * Returns the template for a Kotlin sealed class.
 *
 * @param sealedClassName The name of the sealed class.
 * @param packageName The name of the package that the sealed class should be placed in.
 * @return The template for a Kotlin sealed class.
 */
fun kotlinSealedClass(sealedClassName: String, packageName: String): String {
    return StringBuilder()
        .appendLine("package $packageName")
        .appendLine()
        .appendLine("sealed class $sealedClassName {")
        .appendLine("}")
        .toString()
}