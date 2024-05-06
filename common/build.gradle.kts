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
    namespace = "org.cosmicide.common"
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
    implementation("androidx.appcompat:appcompat:1.7.0-beta01")
    implementation("androidx.databinding:viewbinding:8.5.0-alpha02")

    "prodImplementation"(projects.feature.appwrite)
    "devImplementation"("com.google.firebase:firebase-analytics-ktx:21.6.1")

    implementation("com.itsaky.androidide.treesitter:android-tree-sitter:4.3.1")

    api("androidx.preference:preference-ktx:1.2.1")

    implementation("com.google.android.material:material:1.12.0-beta01")

    implementation(platform("io.github.Rosemoe.sora-editor:bom:0.23.4-cac2770-SNAPSHOT"))
    api("io.github.Rosemoe.sora-editor:editor")
    api("io.github.Rosemoe.sora-editor:language-treesitter")

    implementation(projects.feature.editorTextmate)
}
