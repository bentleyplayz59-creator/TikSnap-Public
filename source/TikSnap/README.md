# TikSnap Android Source

Android application source for TikSnap by ByteSnap Inc.

The project uses Java, Android SDK 34, and Android Gradle Plugin 8.5.2.

The public repository's GitHub Actions workflow builds and publishes signed release APKs. It stages the existing signing configuration from GitHub Actions repository secrets into the ignored local `signing/` build directory.

The original signing identity is retained for compatible updates. Signing files are not committed to this repository.
