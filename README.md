# TikSnap — Official Public Release

TikSnap is an Android social feed application by ByteSnap Inc.

## Source
The Android application source is in `source/TikSnap`.

## Builds and releases
GitHub Actions builds TikSnap 1.1.0. Successful builds publish a standalone signed APK asset in GitHub Releases: `TikSnap-1.1.0-release.apk`. The APK is not wrapped in a ZIP archive.

The workflow retrieves the existing signing configuration from GitHub Actions repository secrets. Signing files are not stored in this public repository.

## Application
- Package: `com.bytesnap.tiksnap`
- Version: 1.1.0
- Version code: 2
- Minimum Android SDK: 24
- Target Android SDK: 34
