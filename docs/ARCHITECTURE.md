# GitForge Pusher - Android Architecture (Phase 2 state)

## Decisions applied (owner-approved)
1. Accounts, pricing, subscriptions and plans are removed. The Android app has no backend.
2. Only real, verifiable statistics are shown (no placeholder numbers such as "AI Credits").
3. "Make it public" will exist behind an explicit confirmation dialog and a permission check (Phase 7).
4. Create Pull Request is in scope for a later phase (Phase 7).
5. Create Repository and Create Branch are in scope, with permission checks (Phase 5/6).
6. GitHub OAuth (Device Flow, client_id only, **no client secret in the APK**) plus Personal Access Token.
7. Java 17, XML layouts, Material Components. No Kotlin sources, no Jetpack Compose.
8. Original PHP/JS web files are untouched; the Android project sits alongside them.

## Pattern
MVVM + Repository, single Activity, Fragment per screen, Navigation component, ViewBinding, LiveData,
manual DI (`AppContainer`). Retrofit/OkHttp, Room and WorkManager arrive in the phases that need them;
none is declared yet, so nothing unverified is on the build path.

## What exists now
```
com.asifqureshi.gitforge
  GitForgeApp            Application; owns AppContainer; applies saved System/Light/Dark mode
  AppContainer           Manual DI container (SettingsStore only, for now)
  MainActivity           Splash install, edge-to-edge, NavHost + bottom navigation
  data/local/SettingsStore   Theme mode + accent id (no secrets)
  ui/theme/Accent, ThemeManager
  ui/common/PendingFeatureFragment   Honest "not implemented yet - planned for Phase N" screen
res/navigation/nav_graph.xml         5 top-level destinations (Home, Repositories, AI, History, Settings)
```
Every top-level destination currently points at `PendingFeatureFragment`. Replace each destination's
`android:name` with the real fragment as it is built; ids, menu and bottom-nav wiring stay unchanged.

## Roadmap (unchanged from the Phase 1 plan, with decisions applied)
| Phase | Scope |
|---|---|
| 3 | Design system and all 18 screen layouts with real empty/loading/error states |
| 4 | Room, Keystore SecureStore, Retrofit GitHub API + interceptors, repositories, domain engines + JUnit tests |
| 5 | PAT login, OAuth Device Flow, profile, repository browser (paging), branches, create repo/branch, dashboard stats |
| 6 | SAF ZIP/folder intake, safe extraction, ignore rules, analyzer, secret scanner, exact blob-SHA diff, selection/preview |
| 7 | WorkManager upload engine (resumable), progress, history, Pull Request, make-public with confirmation |
| 8 | AI providers, models, chat, quick actions, secret redaction |
| 9 | Biometric lock, auto-lock, clear cache, appearance persistence UI, about |
| 10 | Hardening, R8, lint, accessibility, signed release workflow |
