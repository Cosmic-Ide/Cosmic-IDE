-dontwarn

-keep,allowoptimization class javax.** { *; }
-keep,allowshrinking class com.intellij.** { *; }
-keep,allowshrinking class org.jetbrains.kotlin.** { *; }
-keepnames class org.xml.sax.** { *; }
-keepnames class com.sun.xml.internal.stream.** { *; }
-keepnames class com.google.googlejavaformat.** { *; }
-keepnames class com.sun.tools.classfile.** { *; }

-obfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
