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
    namespace = "com.tyron.javacompletion"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    annotationProcessor("com.google.auto.value:auto-value:1.11.0")

    implementation("com.github.Cosmic-Ide.kotlinc-android:kotlinc-android:fce2462f00")
    implementation("com.google.auto.value:auto-value-annotations:1.11.0")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.google.code.gson:gson:2.13.1")
    api("com.google.guava:guava:33.4.8-android")

    implementation(projects.util)
}
