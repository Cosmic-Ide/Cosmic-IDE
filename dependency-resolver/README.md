# DependencyResolver

Provides a simple API to check for, and download artifacts from Maven Central, Google Maven and Jitpack.
It was created as a lightweight alternative to Eclipse aether for Android. But this would work on any OS.

For checking if an artifact exists (in the above mentioned repositories), you can simply do
```kt
import org.cosmic.ide.dependency.resolver.checkArtifact

val groupId = "com.squareup.retrofit2"
val artifactId = "retrofit"
val version = "2.9.0"

val repository = checkArtifact(groupId, artifactId, version)
if (repository != null) {
    println("Artifact exists in ${ repository.getName() }")
} else {
    println("Cannot find artifact.")
}
```

For downloading an artifact, you can do
```kt
val file = File("<path to download artifact>")
repository.downloadArtifact(groupId, artifactId, version, file)
```
