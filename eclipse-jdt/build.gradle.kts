plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.LINT)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

dependencies {
    api("org.eclipse.platform:org.eclipse.core.resources:3.18.0")
    api("org.eclipse.platform:org.eclipse.core.runtime:3.26.0")
    api("org.eclipse.platform:org.eclipse.core.filesystem:1.9.500")
    api("org.eclipse.platform:org.eclipse.text:3.12.200")

    api(files("libs/org.eclipse.jdt.core-3.31.0.jar"))
}
