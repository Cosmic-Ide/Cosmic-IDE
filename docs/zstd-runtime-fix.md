# Android zstd runtime extraction fix

The APK previously omitted an Android-compatible zstd JNI library. Runtime extraction failed in ZstdInputStream initialization. Manually supplying the Linux/aarch64 library failed because it requires libpthread.so.0 and libc.so.6.

Use the official com.github.luben:zstd-jni Android AAR at the version selected by the version catalog. Its arm64 JNI library depends on Android libc.so, libm.so and libdl.so. Keep setLongMax(30): the bundled archive is generated with --long=30.

## Validation

A user reported successful extraction and subsequent installation of the Arch environment on realme RMX5010, Android 17, arm64, 4096-byte pages, after manually supplying the AAR library. This validates the native library choice; the rebuilt APK still requires clean-install testing.

Build with `./gradlew :app:assembleProdDebug`. Verify that the APK contains `lib/arm64-v8a/libzstd-jni-1.5.7-16.so`. On device, verify extraction, launch Bash, finish setup.sh, and clone a repository.

## Follow-up

Bootstrap checks currently accept an existing glibc directory, and the installer accepts a nonempty directory. A leftover tmp directory can therefore bypass extraction. Add completion checks for both bootstrap and Arch environments and retain deployment error details. These follow-up changes are outside this minimal dependency fix.

The APK also lacks Linux zstd JAR resources. The stage at which those resources disappear has not been established. The upstream cloud classifier includes Linux/aarch64; it must not be described as excluding that architecture.
