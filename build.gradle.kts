/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0-alpha01" apply false
    id("com.android.library") version "8.5.0-alpha07" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0-Beta5" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.0-Beta5" apply false
    id("dev.rikka.tools.materialthemebuilder") version "1.4.1" apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}
