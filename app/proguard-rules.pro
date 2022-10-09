-allowaccessmodification

-keep,allowoptimization class javax.** { *; }
-keep,allowshrinking,allowoptimization class com.intellij.** { *; }
-keep class com.intellij.core.JavaCoreApplicationEnvironment { *; }
-keep class org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment { *; }
-keep class org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment { *; }
-keep,allowshrinking class org.jetbrains.kotlin.** { *; }
-keepnames class org.xml.sax.** { *; }
-keepnames class com.sun.xml.internal.stream.** { *; }
-keepnames class com.google.googlejavaformat.** { *; }
-keepnames class com.sun.tools.classfile.** { *; }

-obfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
