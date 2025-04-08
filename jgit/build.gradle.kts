/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        kotlin.jvmToolchain(17)
    }
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.0.202503040940-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.pgm:6.9.0.202403050737-r") {
        exclude("net.java.dev.jna", "jna-platform")
        exclude("net.java.dev.jna", "jna")
        exclude("commons-logging", "commons-logging")
    }
    implementation("com.github.sya-ri:kgit:1.0.6")
}
