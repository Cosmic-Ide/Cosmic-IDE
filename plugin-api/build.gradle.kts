/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.Properties

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "org.cosmicide.plugin.api"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(libs.pine.core)
    api(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}

fun getLocalProperty(key: String): String? {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return null
    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties.getProperty(key)?.trim('"')
}

val publishVersion = providers.environmentVariable("PUBLISH_VERSION")
    .orElse(
        providers.gradleProperty("version")
            .flatMap { provider { if (it != "unspecified") it else null } })
    .orElse(provider {
        val baseVersion = libs.versions.ideVersion.get()
        val isSnapshot = providers.environmentVariable("IS_SNAPSHOT").map { it.toBoolean() }
            .orElse(providers.gradleProperty("isSnapshot").map { it.toBoolean() })
            .orElse(provider { true })
            .get()
        if (isSnapshot && !baseVersion.endsWith("-SNAPSHOT")) "$baseVersion-SNAPSHOT" else baseVersion
    })
    .get()

afterEvaluate {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = "sonatypeSnapshots"
                url = uri(
                    providers.environmentVariable("MAVEN_SNAPSHOTS_URL")
                        .orElse("https://central.sonatype.com/repository/maven-snapshots/")
                        .get()
                )
                credentials {
                    username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME")
                        .orElse(provider { getLocalProperty("MAVEN_CENTRAL_USERNAME") })
                        .orNull
                    password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD")
                        .orElse(provider { getLocalProperty("MAVEN_CENTRAL_PASSWORD") })
                        .orNull
                }
            }
        }
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "org.invokevirtual.cosmicide"
                artifactId = "plugin-api"
                version = publishVersion

                pom {
                    name.set("Cosmic IDE Plugin API")
                    description.set("The stable API for building extensions for Cosmic IDE")
                    url.set("https://github.com/Cosmic-Ide/Cosmic-IDE")
                    licenses {
                        license {
                            name.set("The GNU General Public License v3.0")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("Cosmic-Ide")
                            name.set("Cosmic IDE Team")
                            email.set("contact@invokevirtual.org")
                            url.set("https://github.com/Cosmic-Ide")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/Cosmic-Ide/Cosmic-IDE.git")
                        developerConnection.set("scm:git:ssh://github.com/Cosmic-Ide/Cosmic-IDE.git")
                        url.set("https://github.com/Cosmic-Ide/Cosmic-IDE")
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
        val signingKey = providers.environmentVariable("GPG_SIGNING_KEY")
            .orElse(provider { getLocalProperty("GPG_SIGNING_KEY") })
        val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD")
            .orElse(provider { getLocalProperty("GPG_SIGNING_PASSWORD") })

        if (signingKey.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.getOrElse(""))
        }
        isRequired =
            signingKey.isPresent || extra.has("signing.keyId") || extra.has("signing.secretKeyRingFile")
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}
