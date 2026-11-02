# Verification ledger (Phase 3)

**Bottom line:** the Gradle build, Android lint, instrumented tests and on-device behaviour were **not run** by the author of
this change (the authoring environment has no Android SDK, Gradle or network). You reported that the Phase 2 APK builds;
the Phase 3 code has not been through a real build. The first GitHub Actions run decides.

## A. Verified from official or primary sources
| Item | Source |
|---|---|
| GitHub web flow supports PKCE but still requires `client_secret` at token exchange; GitHub advises native apps cannot keep it secret | GitHub Docs, "Authorizing OAuth apps" |
| Device flow needs only `client_id`, must be enabled in the OAuth app, and polls `login/oauth/access_token` with `device_code` | Same page |
| Retrofit 3.0.0 exists, uses OkHttp 4.12.0, and has a matching `converter-gson:3.0.0` | Retrofit release notes |
| AGP 9.0.1 requires Gradle 9.1.0+, JDK 17, Build Tools 36.0.0 (unchanged from Phase 2) | Android Developers AGP 9.0 notes |
| AndroidX stable releases recyclerview 1.4.0, swiperefreshlayout 1.2.0, documentfile 1.1.0, appcompat 1.7.1, activity 1.12.2, navigation 2.9.7, core-splashscreen 1.2.0 exist | Android Developers stable-channel page |

## B. Executed here (actual results)
| Check | Result |
|---|---|
| `xmllint --noout` on all 42 XML files | All well-formed |
| Resource cross-check: every `@type/name` in XML and `R.type.name` in Java resolves; every `binding.x` / `b.x` / `d.x` view id exists in its layout | 0 unresolved (library `@style/Widget.Material3.*` references are checked only by the real build) |
| Navigation graph classes exist; menu ids equal nav ids; `MainActivity` top-level set matches the menu | Consistent |
| JDK compiler-API parse of all 109 Java files (`-source 17`) | 0 syntax errors |
| **53 JUnit tests of the pure-Java domain layer, compiled and run on JDK 21 with `-source 17` through a small hand-written JUnit shim** (not through Gradle/real JUnit) | **53 passed, 0 failed** |
| Whole-project `javac` attempt without Android/Retrofit/Room jars | Not meaningful: ~1,800 errors are missing external packages. No error was reported against my own classes, but javac hides follow-on errors, so this proves little |

Domain tests cover: path traversal, ZIP-slip, size and count limits, invalid archives, root-folder stripping, Git blob
hashing against known `git hash-object` values, secret detection (incl. the stateful-regex bug), token redaction,
input validation, exact-SHA diffing, GitHub error mapping (rate limit vs permission, empty repo, non-fast-forward),
pagination header, device-flow decisions, ref-path encoding, and the upload engine against a fake GitHub client
(success, unchanged files, stale branch, cancellation never moving the branch, tree chunking, file modes, conflict,
ambiguous final request). `AccentTest` (5 tests) needs the generated `R` class and runs only under Gradle.
Expected Gradle total: 58 unit tests.

## C. NOT verified - do not assume these work
- **Gradle sync/build, the APK, `testDebugUnitTest` under real JUnit, lint, R8 release build, on-device or emulator runs.**
- All Android classes (fragments, ViewModels, adapters, Keystore code, Room, Retrofit wiring) and all layouts were written
  carefully but never compiled against the real libraries. Expect possible compile or inflation errors on the first build.
- Library style/attribute names used from knowledge, not checked: `Widget.Material3.Button.{OutlinedButton,TextButton,TonalButton}`,
  `Widget.Material3.Chip.Filter`, `Widget.Material3.CardView.Filled`, `colorSurfaceContainer*`, `app:endIconMode`.
- Version compatibility not confirmed this session: `room 2.8.4`, `lifecycle 2.10.0`, `material 1.13.0` with compileSdk 36,
  `junit 4.13.2`. (Newer Room 2.8.5 and Lifecycle 2.11.0 exist, which suggests the pinned ones do, but that is inference.)
- Room's annotation processor under AGP 9 with the Groovy DSL, and `buildConfigField` in the new DSL.
- Every GitHub Git Data API call and the device-flow endpoints were written from API knowledge and **never exercised against
  github.com**. The empty-repository 409 behaviour is documented behaviour that was not re-tested.
- AI provider request/response shapes (OpenAI chat completions, Anthropic messages, Gemini generateContent) are written
  from knowledge and were not exercised against live services or re-checked against current docs.
- Android Keystore AES-GCM behaviour on real devices (key invalidation, OEM quirks).
- Storage Access Framework behaviour with different document providers; very large folder trees.
- Accessibility, TalkBack, landscape and tablet layouts.
