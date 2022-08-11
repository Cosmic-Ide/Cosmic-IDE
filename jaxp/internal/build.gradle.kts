plugins {
    id(BuildPlugins.JAVA_LIBRARY)
}

dependencies {
    implementation(projects.jaxp.xml)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}