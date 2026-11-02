# GitForge Pusher - Android Architecture (Phase 3 state)

## Decisions applied (owner-approved)
1. Accounts, pricing, subscriptions and plans are removed. No backend.
2. Only real, verifiable statistics are shown (repository count from GitHub; pushes and success rate from local history).
3. "Make it public" is planned behind an explicit confirmation dialog and permission checks. **Not implemented yet.**
4. Create Pull Request is planned for a later phase. **Not implemented yet.**
5. Create Branch is implemented with a permission check. Create Repository is planned. **Not implemented yet.**
6. GitHub OAuth uses the device flow (no client secret in the APK) plus personal access tokens. See GITHUB_SETUP.md.
7. Java 17, XML layouts, Material Components. No Kotlin sources, no Jetpack Compose.
8. The original PHP/JS web files are untouched; the Android project sits alongside them.

## Pattern
MVVM + Repository, single Activity, one Fragment per screen, Navigation component, ViewBinding, LiveData, manual DI
(`AppContainer`). Retrofit/OkHttp for GitHub, Room for history. WorkManager is deliberately **not** used: an upload is a
user-initiated foreground operation that must stay visible and cancellable (see "Known limitations").

## Layers (package `com.asifqureshi.gitforge`)
```
ui/            Fragments, adapters, ViewModels (auth, home, repos, upload, history, ai, settings, theme, common)
data/
  repository/  AuthRepository, GitHubRepository (facade), HistoryRepository, AiRepository
  remote/github/  GitHubApi (Retrofit), DeviceFlowApi, GitHubClient (the ONLY class that executes GitHub calls),
                  NetworkModule, DeviceFlowSession, dto/
  remote/ai/   AiClient (OkHttp), AiProviderType, AiException
  local/       SecureStore (Keystore AES-GCM), CredentialStore, SettingsStore, db/ (Room: OperationEntity, DAO, database)
  model/       Repo, Branch, UserProfile, Page, Selection, TokenCheck
domain/        Pure Java, no Android imports, unit-tested:
  PathSanitizer, IgnoreRules, GitBlobHasher, SecretScanner, SecretRedactor, Validators, DiffEngine
  project/     StagingArea, ZipImporter, ProjectFile, FileStatus, SkipReason
  github/      GitDataClient (interface), ApiException, ApiErrorMapper, LinkHeader, RefPath, RemoteTree, DeviceFlowLogic
  upload/      UploadEngine (blobs -> trees -> commit -> fast-forward ref update)
  ai/          AiPrompts, AiValidators, ProjectDigest
```

## Upload flow
1. **Intake** (UploadFragment): SAF picker for a ZIP, files or a folder. Content is copied into `cache/staging` through
   `StagingArea`, which enforces path safety and size limits, hashes each file as a Git blob and scans for secrets.
2. **Compare**: head commit and recursive tree of the chosen branch are loaded; `DiffEngine` classifies each file as
   New / Will replace / Unchanged by exact blob SHA. If GitHub truncates the listing, affected files are shown as Unknown.
3. **Preview + confirm**: per-file selection, commit message, and a confirmation dialog stating how many existing files
   will be replaced. The upload never starts without this.
4. **Commit** (`UploadEngine`): re-checks the branch head; uploads blobs (real progress); creates trees in chunks of 500;
   creates one commit; updates the branch with force=false. If the connection drops on that last call, the engine reads the
   branch to learn whether it succeeded before reporting anything.
5. **Result + history**: the outcome (success, no changes, cancelled, failed with stage) is stored in Room.

## Known limitations
- Empty repositories (no commits) are not supported (GitHub's Git Data API rejects them); the app says so.
- Uploads are not resumable and are not run in a foreground service. If Android kills the app mid-upload nothing was
  committed (the branch moves only in the final request), but you start over. Keep the app open.
- Cancelling takes effect after the file currently being uploaded; it is impossible once the final branch update starts.
- Remote files that are absent from your upload are never deleted (add/replace only).
- Files over 10 MB are skipped; each file is read into memory (base64) while uploading.
- Branch-protection rules that require pull requests will reject the final update; upload to a new branch instead.
- GitHub Enterprise Server is not supported (hosts are fixed to github.com / api.github.com).
- History has no "retry": staged files are deleted after use, so a blind repeat could overwrite newer remote changes.
  Start a new upload and compare again instead.

## Remaining roadmap
Create Pull Request; Make-public with confirmation; Create Repository; biometric lock and auto-lock; resumable uploads
with a foreground service; GitHub App / backend-assisted web flow; instrumented (on-device) tests; R8 release verification.
