# Sentinel Android 0.4.0 beta

Native Kotlin/Compose app, Android 11+. See the [root README](../README.md), [release notes](../docs/RELEASE_0.4.0.md) and [validation](../docs/VALIDATION_0.4.0.md) for current features, installation and limitations.

Use Android SDK 35 and JDK 17 or newer. Run `./gradlew testDebugUnitTest lintRelease assembleDebug` (Windows: `gradlew.bat`). The wrapper pins Gradle 8.11.1. Initial dependency downloads require internet. Installed codes work offline; optional account login uses INTERNET permission.

Release builds shrink code/resources. Set SENTINEL_KEYSTORE, SENTINEL_STORE_PASSWORD and SENTINEL_KEY_PASSWORD to sign with key alias sentinel. Signing credentials are excluded. A debug build cannot upgrade the published development-signed beta.

Real vault screens block screenshots and lock on background. Public demo screens allow screenshots and clear on background. Local metadata payload v2 reads legacy v1, but older apps cannot read v2. Portable backups remain v1 and omit organization/drill metadata.
