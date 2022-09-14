plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.LINT)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

dependencies {
    api("org.eclipse.platform:org.eclipse.core.resources:3.17.0")
    api("org.eclipse.platform:org.eclipse.core.runtime:3.26.0")
    api("org.eclipse.platform:org.eclipse.equinox.preferences:3.10.100")
    api("org.eclipse.platform:org.eclipse.core.filesystem:1.9.400")
    api("org.eclipse.platform:org.eclipse.text:3.12.100")

    api(files("libs/org.eclipse.jdt.core-3.30.0.jar"))
}
