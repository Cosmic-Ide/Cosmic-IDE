/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.cosmicide.build"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }


    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            isDefault = true
        }
        create("prod") {
            dimension = "environment"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation(projects.common)
    implementation(projects.feature.project)
    implementation(projects.util)
    implementation(projects.feature.javaCompletion)
    /* D8 cannot handle scala3 compiler rn (https://issuetracker.google.com/issues/285036373)
    implementation("org.scala-lang:scala3-compiler_3:3.3.1-RC1") {
        exclude(group = "org.jline", module = "jline-terminal")
        exclude(group = "org.jline", module = "jline-terminal-jna")
        exclude(group = "org.jline", module = "jline-reader")
    }
    */
    implementation("com.github.Cosmic-Ide.kotlinc-android:kotlinc:2a0a6a7291")
    implementation("io.github.Rosemoe.sora-editor:editor:0.23.4-3895689-SNAPSHOT")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("com.android.tools:r8:8.2.33")
}
