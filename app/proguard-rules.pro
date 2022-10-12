-dontobfuscate
-keepattributes LineNumberTable,SourceFile
-allowaccessmodification

-keep class javax.** { *; }

# we don't load kotlin-stdlib when running projects for better performance,
# so these will classes be needed at runtime by the programs.
-keep class kotlin.** { *; }

# for kotlin compiler
-keep class com.intellij.** { *; }
-keep class org.jetbrains.kotlin.** { *; }
-keep class org.xml.sax.** { *; }
-keep class com.sun.xml.internal.stream.** { *; }
-keep class jdk.xml.internal.** { *; }

# proguard's merging breaks gson
-keep class com.google.gson.** { *; }
-keep,allowshrinking class com.sun.tools.classfile.** { *; }
