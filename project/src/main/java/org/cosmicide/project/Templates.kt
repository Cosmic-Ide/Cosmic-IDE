package org.cosmicide.project

object Templates {
    fun javaClass(name: String, packageName: String, body: String = ""): String {
        return """package $packageName;

public class $name {
    public static void main(String[] args) {
        $body
    }
}
"""
    }

    fun javaInterface(name: String, packageName: String): String {
        return """package $packageName;

public interface $name {
    
}
"""
    }

    fun javaEnum(name: String, packageName: String): String {
        return """package $packageName;

public enum $name {
    
}
"""
    }

    fun kotlinClass(name: String, packageName: String, body: String = ""): String {
        return """package $packageName

class $name {
    fun main(args: Array<String>) {
       $body
    }
}
"""
    }

    fun kotlinInterface(name: String, packageName: String): String {
        return """package $packageName

interface $name {
    
}
"""
    }

    fun kotlinObject(name: String, packageName: String): String {
        return """package $packageName

object $name {
    
}
"""
    }

    fun kotlinEnum(name: String, packageName: String): String {
        return """
package $packageName

enum class $name {
    
}
"""
    }

    fun kotlinAnnotation(name: String, packageName: String): String {
        return """package $packageName

annotation class $name
"""
    }

    fun kotlinDataClass(name: String, packageName: String): String {
        return """package $packageName

data class $name(
    
)
"""
    }

    fun kotlinSealedClass(name: String, packageName: String): String {
        return """package $packageName

sealed class $name {
    
}
"""
    }
}
