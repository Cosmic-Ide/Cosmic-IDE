-optimizations !class/merging/*

# Don't obfuscate view classes

-keep class io.github.rosemoe.sora.widget.CodeEditor
-keep class io.github.rosemoe.sora.widget.SymbolInputView
-keep class org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration
-keep class com.google.gson.** { *; }
