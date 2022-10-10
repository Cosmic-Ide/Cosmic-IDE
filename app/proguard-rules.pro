-dontobfuscate
-keepattributes LineNumberTable,SourceFile
-allowaccessmodification
-repackageclasses ''

-keep,allowoptimization class javax.** { *; }

# we don't load kotlin-stdlib when running projects for better performance,
# so these will classes be needed at runtime by the programs.
-keep,allowoptimization class kotlin.** { *; }

# for kotlin compiler
-keep,allowoptimization class com.intellij.** { *; }
-keep,allowoptimization class org.jetbrains.kotlin.** { *; }
-keep,allowoptimization class org.xml.sax.** { *; }
-keep,allowoptimization class com.sun.xml.internal.stream.** { *; }
-keep,allowoptimization class jdk.xml.internal.** { *; }

# proguard's inlining breaks gson
-keep,allowoptimization,allowshrinking class org.jf.util.** { *; }

# -keepnames class com.google.googlejavaformat.** { *; }
# -keep,allowshrinking class com.sun.tools.classfile.** { *; }
