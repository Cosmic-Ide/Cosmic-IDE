package dependencies

object Dependencies {
    const val CORE_LIBRARY_DESUGARING = "com.android.tools:desugar_jdk_libs:${BuildDependenciesVersions.CORE_LIBRARY_DESUGARING}"
    const val GUAVA = "com.google.guava:guava:${BuildDependenciesVersions.GUAVA}"
    const val APPCOMPAT = "androidx.appcompat:appcompat:${BuildDependenciesVersions.APPCOMPAT}"
    const val MATERIAL = "com.google.android.material:material:${BuildDependenciesVersions.MATERIAL}"
    const val LEAK_CANARY = "com.squareup.leakcanary:leakcanary-android:${BuildDependenciesVersions.LEAK_CANARY}"
    const val CFR = "org.benf:cfr:${BuildDependenciesVersions.CFR}"
    const val DEXLIB2 = "org.smali:dexlib2:${BuildDependenciesVersions.SMALI}"
    const val BAKSMALI = "org.smali:baksmali:${BuildDependenciesVersions.SMALI}"
    const val JAVAC = "io.github.itsaky:nb-javac-android:${BuildDependenciesVersions.JAVAC}"
}