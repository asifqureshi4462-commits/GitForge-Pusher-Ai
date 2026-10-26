# Phase 2 - Verification ledger

## A. Verified from official or primary sources (this session)
| Item | Evidence |
|---|---|
| AGP 9.0.1 needs Gradle >= 9.1.0, JDK 17, SDK Build Tools 36.0.0; max API 36.1 | Android Developers AGP 9.0.1 release notes |
| AGP 9 uses the new DSL; opt-out `android.newDsl=false`; AndroidX is the default | AGP 9.0 release notes / new-DSL page |
| AGP 9 requires library consumers to use the same or higher compileSdk | AGP 9.0 release notes (basis for pinning older AndroidX) |
| androidx appcompat 1.7.1, activity 1.12.2, navigation 2.9.7, core-splashscreen 1.2.0 exist as stable releases | Android Developers stable-channel page (current to 2026-10-21) |
| Newer stable AndroidX exist (appcompat 1.8.0, fragment 1.9.1, navigation 2.10.2, room 2.8.5, work 2.12.0) | Same page. **Intentionally not used**: released Aug-Oct 2026 and may require a newer compileSdk than AGP 9.0.1 supports (max 36.1); their actual requirement was not checked |
| Material Components 1.13.0 exists (minSdk 21, built with compileSdk 35); 1.14.0 is the latest stable | GitHub release notes (via dependency-update PR) and releases page |
| Action majors exist: checkout v6, setup-java v5, setup-gradle v5, upload-artifact v7 | GitHub release notes / widely used workflow files (secondary sources) |

## B. Executed in this environment (actual results)
| Check | Result |
|---|---|
| `xmllint --noout` on all 26 XML files | All well-formed |
| Script: every `@type/name` in XML and `R.type.name` in Java resolves to a defined resource; internal style parents resolve | 0 unresolved |
| Script: nav-graph ids == bottom-nav menu ids == MainActivity top-level set; nav/manifest classes exist | Consistent |
| JDK compiler API parse of all 8 Java files (`-source 17`) | 0 syntax errors; deliberately broken control file correctly flagged |
| Type-check + run of `Accent`, `SettingsStore`, `ThemeManager`, `AppContainer`, `GitForgeApp` against hand-written stubs | Compiled; 14/14 behaviour checks passed. **Stubs are not the real Android SDK** |

## C. NOT verified (do not assume these work)
- **The Gradle build, the APK, `testDebugUnitTest`, lint, and running on a device or emulator. None were run.**
- `MainActivity`, `PendingFeatureFragment` and all XML were not compiled against the real Android/Material libraries
  (no SDK). Style/attr names that depend on Material 1.13.0 (e.g. `Widget.Material3.BottomNavigationView`,
  `colorSurfaceContainer*`, `boxCornerRadius*`) and core-splashscreen theme attributes are used from knowledge, not checked.
- `junit:junit:4.13.2` (assumed available on Maven Central).
- That `gradle/actions/setup-gradle` installs Gradle via `gradle-version` as written, and that
  `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager` exists on `ubuntu-latest`.
- That Groovy `=` assignment syntax is accepted by AGP 9.0.1's new DSL in `app/build.gradle`.
- That Navigation 2.9.7 / Activity 1.12.2 / Material 1.13.0 do not demand a compileSdk above 36.
- Runtime behaviour: splash, edge-to-edge insets, bottom-nav icon rendering, accent overlays on a device.
- Not yet chosen/verified (deferred to the phase that needs them): Retrofit, OkHttp, Gson, Room, WorkManager,
  RecyclerView, ViewPager2, biometric, Glide, Android Studio / AndroidIDE compatibility with AGP 9.0.
