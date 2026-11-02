# Building GitForge Pusher (Android)

**Status:** Phase 3 has not been built by its author (no Android SDK, Gradle or network in the authoring environment).
Phase 2 was reported by you to build successfully. Run the workflow below and send me the log if anything fails.

## Requirements
JDK 17, Android SDK (`platforms;android-36`, `build-tools;36.0.0`), Gradle 9.1.0 (AGP 9.0.x needs 9.1.0 or newer).

## Build and download the APK with GitHub Actions
1. Push the project to GitHub (the workflow is `.github/workflows/android-build.yml`).
2. Optional but needed for "Continue with GitHub": add the repository **variable** `GITFORGE_GITHUB_CLIENT_ID`
   (Settings, Secrets and variables, Actions, **Variables** tab). See `docs/GITHUB_SETUP.md`. Token sign-in works without it.
3. Open the **Actions** tab, choose **Android Build**, and click **Run workflow** (it also runs on every push and pull request).
4. When the run finishes, open it. The job runs unit tests, then builds the debug APK. A failed step is shown in red with its log.
5. Scroll to **Artifacts** and download **gitforge-debug-apk** (a zip containing `app-debug.apk`). Test reports are in
   **unit-test-reports**. A separate, non-blocking job runs lint and uploads **lint-report**.
6. On the phone, allow installing from your file manager/browser, then open the APK. The debug build installs as
   `com.asifqureshi.gitforge.debug`.

## Local build
```
gradle -Pgitforge.githubClientId=YOUR_CLIENT_ID :app:testDebugUnitTest :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
The repository ships no `gradle-wrapper.jar`; create one once with `gradle wrapper --gradle-version 9.1.0` if you want `./gradlew`.

## If the first build fails
| Symptom | Likely cause / fix |
|---|---|
| Groovy DSL error in `android { }` or `buildConfigField` | AGP 9 new DSL. Temporary escape hatch: add `android.newDsl=false` to `gradle.properties` |
| "requires compileSdk 37" for a library | Pin that library to an older version in `gradle/libs.versions.toml` |
| Unknown style/attribute in a layout or `styles.xml` | A Material 1.13.0 name differs from the assumption; paste the error |
| Room error about schema export | Not expected (`exportSchema = false`); paste the error |
| App starts on the sign-in screen with the OAuth button disabled | No client ID was provided to the build (expected; use a token or set the variable) |
