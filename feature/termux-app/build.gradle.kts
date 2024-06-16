import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("com.android.library")
}

android {
    compileSdk = 34
    ndkVersion = "27.0.11902837"

    buildFeatures.buildConfig = true

    defaultConfig {
        namespace = "org.cosmicide.termux"
        minSdk = 26

        buildConfigField(
            "String",
            "TERMUX_PACKAGE_VARIANT",
            "\"" + "apt-android-7" + "\""
        ) // Used by TermuxApplication class

        externalNativeBuild {
            ndkBuild {
                cFlags += listOf(
                    "-std=c11",
                    "-Wall",
                    "-Wextra",
                    "-Werror",
                    "-Os",
                    "-fno-stack-protector",
                    "-Wl,--gc-sections"
                )
            }
        }

        splits {
            abi {
                reset()
                include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false // Reproducible builds
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }

    lintOptions.disable += "ProtectedPermissions"
}

fun downloadBootstrap(arch: String, expectedChecksum: String, version: String) {
    val digest = MessageDigest.getInstance("SHA-256")

    val localUrl = "src/main/cpp/bootstrap-$arch.zip"
    val file = File(projectDir, localUrl)
    if (file.exists()) {
        val buffer = ByteArray(8192)
        val input = FileInputStream(file)
        while (true) {
            val readBytes = input.read(buffer)
            if (readBytes < 0) break
            digest.update(buffer, 0, readBytes)
        }
        var checksum = BigInteger(1, digest.digest()).toString(16)
        while (checksum.length < 64) {
            checksum = "0$checksum"
        }
        if (checksum == expectedChecksum) {
            return
        } else {
            logger.quiet("Deleting old local file with wrong hash: $localUrl: expected: $expectedChecksum, actual: $checksum")
            file.delete()
        }
    }

    val remoteUrl =
        "https://github.com/Cosmic-IDE/terminal-packages/releases/download/bootstrap-$version/bootstrap-$arch.zip"
    logger.quiet("Downloading $remoteUrl...")

    file.parentFile.mkdirs()
    val out = BufferedOutputStream(FileOutputStream(file))

    val connection = URL(remoteUrl).openConnection()
    val digestStream = DigestInputStream(connection.inputStream, digest)
    out.use { outStream ->
        digestStream.use { digestStream ->
            val buffer = ByteArray(8192)
            while (true) {
                val readBytes = digestStream.read(buffer)
                if (readBytes < 0) break
                outStream.write(buffer, 0, readBytes)
            }
        }
    }
    out.close()

    var checksum = BigInteger(1, digest.digest()).toString(16)
    while (checksum.length < 64) {
        checksum = "0$checksum"
    }
    if (checksum != expectedChecksum) {
        file.delete()
        throw GradleException("Wrong checksum for $remoteUrl: expected: $expectedChecksum, actual: $checksum")
    }
}

tasks.named("clean") {
    doLast {
        val tree = fileTree(File(projectDir, "src/main/cpp"))
        tree.include("bootstrap-*.zip")
        tree.forEach { it.delete() }
    }
}

fun downloadBootstraps() {
    val version = "2024.06.10-r1+apt-android-7"
    downloadBootstrap(
        "aarch64",
        "d6f62e4711e8dbde8f10e9408a12c27d38ec89308d659acf9b7f041d75718779",
        version
    )
    downloadBootstrap(
        "arm",
        "8f5f8957ab60d2d2f8e018a4facb58469389c19a3733f352763e616e0bb1c8c6",
        version
    )
    downloadBootstrap(
        "i686",
        "b836e380a0d1200c12e15769852718688e5a302dde81076d2e2148a82beaee90",
        version
    )
    downloadBootstrap(
        "x86_64",
        "b511ee9d16192da3443ec5d4c8dbd66b265997b4ad885b78d75207cac6796603",
        version
    )
}

afterEvaluate {
    downloadBootstraps()
}

dependencies {
    implementation("androidx.annotation:annotation:1.8.0")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.viewpager:viewpager:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.guava:guava:33.0.0-jre")
    val markwonVersion = "4.6.2"
    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:ext-strikethrough:$markwonVersion")
    implementation("io.noties.markwon:linkify:$markwonVersion")
    implementation("io.noties.markwon:recycler:$markwonVersion")

    implementation("com.github.termux.termux-app:terminal-view:062c9771a9")
    implementation(projects.feature.termuxShared)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
