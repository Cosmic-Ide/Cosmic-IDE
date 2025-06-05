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
    namespace = "org.cosmicide.completion.java"
    compileSdk = 36

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
    implementation("com.github.Cosmic-Ide.kotlinc-android:kotlinc-android:fce2462f00")
    implementation("com.github.javaparser:javaparser-core:3.26.4")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.9") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation(projects.feature.project)
    implementation(projects.common)
    implementation(projects.util)
}
