-allowaccessmodification

-keep,allowshrinking class javax.** { *; }
-keep class com.intellij.** { *; }
-keep class org.jetbrains.kotlin.** { *; }
-keepnames class org.xml.sax.** { *; }
-keep public class * extends javax.xml.stream.XMLInputFactory
-keep public class * extends javax.xml.stream.XMLOutputFactory
-keepnames class com.google.googlejavaformat.** { *; }
-keepnames class com.sun.tools.classfile.** { *; }

-obfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
