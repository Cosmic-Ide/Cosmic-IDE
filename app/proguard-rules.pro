-dontobfuscate
-dontwarn

-keep class com.google.googlejavaformat.** { *; }
-keep class com.sun.tools.classfile.** { *; }
# -keep class jdk.internal.** { *; }
-keep class org.osgi.** { *; }
-keep class javax.** { *; }
-keep class com.intellij.** { *; }
-keep class org.jetbrains.kotlin.** { *; }
-keepnames class org.xml.sax.** { *; }
-keepnames class com.sun.xml.internal.stream.** { *; }
