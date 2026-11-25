# TikSnap

TikSnap is a local-first Android social feed application by ByteSnap Inc. The application uses the Android framework directly and stores user-generated content on-device.

## Android build

The GitHub Actions workflow at `.github/workflows/android-release.yml` builds the release APK using Java 17, Gradle 8.7, Android SDK platform 34, and Android Build Tools 34.0.0.

The workflow stages the existing release signing files from the repository's `signing-PRIVATE/` directory into the Gradle project's ignored `signing/` directory. It also stages the ten original screenshots into `app/src/main/assets/feed/` before compilation. The original screenshot files remain in the repository's top-level `screenshot/` directory.

For a local release build, place the existing `keystore.properties` and `tiksnap-release.jks` in `source/TikSnap/signing/`, ensure the configured `storeFile` resolves to `signing/tiksnap-release.jks`, and copy the ten images from `screenshot/` to `source/TikSnap/app/src/main/assets/feed/`. Then run `gradle assembleRelease` from `source/TikSnap/`.

## Release identity

The release build must continue using the existing `tiksnap-release` alias and the original TikSnap Release Signing certificate. Do not create a replacement signing identity. The workflow verifies the resulting APK signature and certificate subject before uploading the APK artifact.

## Project

- Application ID: `com.bytesnap.tiksnap`
- Version: `1.1.0` (version code 2)
- Minimum SDK: 24
- Target / compile SDK: 34
- Java source compatibility: 8
- Android Gradle Plugin: 8.5.2
