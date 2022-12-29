plugins {
    id(BuildPlugins.JAVA_LIBRARY)
}

dependencies {
    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT") {
        isChanging = true
    }
    api("org.eclipse.lsp4j:org.eclipse.lsp4j:0.19.0")
    implementation("org.jetbrains:annotations:23.1.0")
}
