# Building GitForge Pusher (Android)

**Status: this project has not been built by the author of this scaffold.** The authoring environment had no
Gradle, Android SDK or network. The first GitHub Actions run is the first real build; please report its result.

## Requirements
- JDK 17
- Android SDK: `platforms;android-36`, `build-tools;36.0.0`
- Gradle **9.1.0** (AGP 9.0.x requires Gradle 9.1.0 or newer)

## CI (recommended)
Workflow: `.github/workflows/android-build.yml`. It installs JDK 17 and Gradle 9.1.0, installs the SDK
components, runs `:app:testDebugUnitTest`, runs `:app:assembleDebug`, and uploads the APK as the artifact
**gitforge-debug-apk** (path `app/build/outputs/apk/debug/*.apk`). A second, non-blocking job runs lint.
Trigger it by pushing to `main`/`master`, opening a pull request, or using "Run workflow".

## Local
```
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk   (application id com.asifqureshi.gitforge.debug)
```
The repository intentionally ships **no `gradle-wrapper.jar`** (a binary that could not be generated offline).
To create a wrapper once: `gradle wrapper --gradle-version 9.1.0`, then commit `gradlew`, `gradlew.bat`
and `gradle/wrapper/*`. If you add a wrapper, the workflow can be switched to `./gradlew`.

## If the first build fails
| Symptom | Likely cause / fix |
|---|---|
| Groovy DSL error mentioning android block | AGP 9 new DSL. Temporary escape hatch: add `android.newDsl=false` to gradle.properties |
| "requires compileSdk 37 or higher" for a library | Pin that library to an older version in `gradle/libs.versions.toml` |
| Unknown style/attr in `styles.xml` | A Material 1.13.0 style name differs from the assumption; paste the error and it will be corrected |
| Licence/SDK errors | The "Install Android SDK components" step failed; check the runner log |
