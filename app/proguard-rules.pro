# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-adaptclassstrings
-adaptresourcefilecontents
-keepattributes SourceFile,LineNumberTable

-keep class org.cosmicide.util.*
-keep class org.cosmicide.editor.*
-keep class org.cosmicide.plugin.api.*
-keep class org.cosmicide.plugin.runtime.*
-keep class org.cosmicide.plugin.runtime.hook.*
-keep class org.cosmicide.plugin.runtime.loading.*

-keep class com.github.luben.zstd.** { *; }

-keepclassmembers class com.termux.view.TerminalView {
  public void inputCodePoint(int, int, boolean, boolean);
  public boolean handleKeyCode(int, int);
}

-keepclassmembers class com.termux.view.textselection.TextSelectionCursorController {
  public java.lang.String getSelectedText();
  private com.termux.view.TerminalView terminalView;
}

-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.gradle.internal.impldep.com.google.j2objc.annotations.*
-dontwarn org.gradle.internal.impldep.org.jetbrains.annotations.Contract

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile