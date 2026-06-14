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
    namespace = "org.cosmicide.completion.java"
    compileSdk = 37

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
}
dependencies {
    implementation("com.github.PranavPurwar:kotlinc-android:8a8572b26b")
    implementation("com.github.javaparser:javaparser-core:3.28.2")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.9") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("org.jetbrains:annotations:26.1.0")
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation("com.github.PranavPurwar:javac-android:27.23")
    implementation(projects.feature.project)
    implementation(projects.common)
    implementation(projects.util)
}
