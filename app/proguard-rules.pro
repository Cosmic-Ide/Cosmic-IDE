-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile
-allowaccessmodification

-keep class javax.** { *; }
# we don't load kotlin-stdlib when running projects for better efficiency,
# so these will classes be needed by the programs when running.
-keep,allowoptimization class kotlin.** { *; }
# for kotlin compiler
-keep class com.intellij.** { *; }
-keep class org.jetbrains.kotlin.** { *; }
-keep class org.xml.sax.** { *; }
-keep class com.sun.xml.internal.stream.** { *; }
-keep class jdk.xml.internal.** { *; }
# proguard's inlining breaks gson
-keep class org.jf.util.BitSetUtils { *; }
# -keepnames class com.google.googlejavaformat.** { *; }
# -keep,allowshrinking class com.sun.tools.classfile.** { *; }
