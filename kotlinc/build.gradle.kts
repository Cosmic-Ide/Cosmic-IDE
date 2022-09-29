plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
}
android {
    namespace = "org.jetbrains.kotlin.android"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
        targetSdk = BuildAndroidConfig.TARGET_SDK_VERSION
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

val intellijVersion = "203.8084.24"

dependencies {
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation(projects.jaxp)

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.7.20-RC")

    // Intellij dependencies required by kotlinc
    api("com.jetbrains.intellij.platform:util-rt:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-class-loader:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-text-matching:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-diagnostic:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:extensions:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-strings:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-collections:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-psi:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.java:java-psi-impl:$intellijVersion") { isTransitive = false }
    implementation("org.fusesource.jansi:jansi:1.16")
    implementation("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil:8.5.2-6")
    implementation("org.jetbrains.intellij.deps:asm-all:9.1")
    implementation("org.jetbrains.intellij.deps:jdom:2.0.6")
    implementation("org.jline:jline:3.3.1")
    implementation("one.util:streamex:0.7.3")

    api("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    api(files("libs/kotlin-compiler-1.7.20-RC.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
}
