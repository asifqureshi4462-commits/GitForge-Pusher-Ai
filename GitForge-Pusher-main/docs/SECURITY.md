# Security model and limits

| Data | Where it lives | Protection |
|---|---|---|
| GitHub token | App-private SharedPreferences, **ciphertext only** | AES-256-GCM key in the Android Keystore; entry name bound as authenticated data |
| AI API key | Same store, separate entry | Same |
| Account login name, theme, AI provider/model/base URL | App-private SharedPreferences (not secret) | Android app sandbox |
| Upload history | Room database | No tokens, keys or file contents; error text is redacted and truncated |
| Staged project files | App cache (`cache/staging`) | Deleted on sign-out, after an upload finishes, or via Settings (Clear cache) |

## What is and is not protected
- The Keystore key cannot be exported on devices with hardware-backed storage, but **code running as this app, a rooted
  device, or malware with sufficient privilege can still ask the Keystore to decrypt**. Encryption at rest protects against
  casual file access and backups, not against a compromised device.
- If Android invalidates the key (for example after a lock-screen reset), the stored secret cannot be decrypted. The entry
  is dropped and you sign in again. There is never a plaintext fallback; if encryption fails, sign-in is cancelled.
- `allowBackup` is off and backup/device-transfer rules exclude everything.
- All traffic is HTTPS (cleartext is disabled in the network security config). Certificate validation is the platform default.
- No request/response logging exists in the app. Tokens are never put in log output, exceptions or history.
- The GitHub token is attached only to requests for `api.github.com`.

## AI keys on a device
Storing a user-supplied AI key on a phone carries risk: anyone who obtains it can spend your quota. Use a key with a spending
limit. For production, send requests through **your own backend proxy** that holds the key, and point the OpenAI-compatible
base URL at that proxy. The app refuses to send text that looks like credentials and shows what will be sent before each request.

## Upload safety
- ZIP entry names are sanitised (no `..`, absolute paths or drive letters); extraction is confined to an app-private folder.
- Limits: 5,000 files, 10 MB per file, 150 MB total, 100 MB per ZIP. Violations are reported, not silently ignored.
- Secret detection is **heuristic** (file names plus common token patterns). It can miss secrets and can flag harmless text.
  Flagged files start unselected; you can still include them after a warning.
- The branch is changed only by the last request, which is fast-forward only (never forced). If the branch moved since
  the preview, GitHub refuses it and nothing is overwritten.
