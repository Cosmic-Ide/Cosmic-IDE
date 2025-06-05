/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

rootProject.name = "CosmicIDE"

include(":app")
include(":build-tools")
include(":common")
include(":feature:completion:java")
include(":feature:completion:kotlin")
include(":feature:formatter:google-java-format")
include(":feature:formatter:ktfmt")
include(":feature:java-completion")
include(":feature:project")
include(":feature:TreeView")
include(":util")
include(":jgit")
include(":feature:code-navigation")
include(":datadir")
include(":feature:aliuhook")
include(":feature:appwrite")
include(":feature:genai")
