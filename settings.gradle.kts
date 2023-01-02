enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Cosmic-Ide"

include(
    ":app",
    ":android-compiler",
    ":common",
    ":google-java-format",
    ":project",
    ":kotlinc",
    ":jaxp",
    ":kotlin-completion",
    ":lynx",
    ":treeview",
    "dependency-resolver",
    "java-completion",
)
