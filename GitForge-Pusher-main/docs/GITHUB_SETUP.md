# GitHub setup

The app supports two ways to sign in. You can use either, or both.

## Option A - Sign in with GitHub (OAuth device flow)

**Why the device flow and not the browser redirect (web flow) with PKCE?** GitHub's web flow does support PKCE, but
GitHub still requires the OAuth app's **client secret** when the app exchanges the code for a token, and GitHub's own
guidance says a native app would have to ship that secret inside the APK. An APK can be unpacked, so a secret inside it
is not secret. This project never embeds a client secret, so it uses the **device flow**, which needs only the public
client ID and no redirect URL. (A web flow would require your own small backend that holds the secret.)

### One-time setup (about two minutes)
1. On GitHub open **Settings, Developer settings, OAuth Apps, New OAuth App**.
2. Fill in any name and homepage. The "Authorization callback URL" field is mandatory on the form but is **not used**
   by the device flow; enter your repository URL.
3. After creating the app, tick **Enable Device Flow** on its settings page and save.
4. Copy the **Client ID**. Do **not** generate or use a client secret.
5. Give the Client ID to the build (it is public, not a secret):
   - **GitHub Actions:** repository **Settings, Secrets and variables, Actions, Variables tab, New repository variable**,
     name `GITFORGE_GITHUB_CLIENT_ID`, value = the Client ID. Re-run the workflow.
   - **Local build:** `gradle -Pgitforge.githubClientId=YOUR_CLIENT_ID :app:assembleDebug`
     or set the environment variable `GITFORGE_GITHUB_CLIENT_ID`.
6. If your repositories belong to an organisation that restricts OAuth apps, an organisation owner must approve the app
   (otherwise those repositories are not visible).

Without a Client ID the "Continue with GitHub" button is disabled and the screen explains why. Nothing is faked.

### Permissions requested
The sign-in screen lets you choose the **minimum** scope: `public_repo` (public repositories only) or `repo`
(public and private). No other scopes are requested.

## Option B - Personal access token (PAT)

Create the token yourself on github.com and paste it into the app. Never paste a real token anywhere else (including
chats with an assistant).

**Fine-grained token (recommended)**
1. GitHub, **Settings, Developer settings, Personal access tokens, Fine-grained tokens, Generate new token**.
2. Set a short expiry. Under *Repository access* choose **Only select repositories** and pick the ones you will upload to.
3. Under *Repository permissions* set **Contents: Read and write** and **Metadata: Read-only**. Nothing else is needed.

**Classic token**: scope `public_repo` for public repositories, or `repo` for private ones.

Notes: fine-grained tokens only see the repositories you selected. Creating repositories is not offered by this version.

## Troubleshooting
| Message | Meaning |
|---|---|
| "Device flow is not enabled for this OAuth app" | Tick **Enable Device Flow** in the OAuth app settings |
| "GitHub does not recognise this client ID" | The Client ID in the build is wrong or empty |
| "The token does not have permission for this action" | Token lacks Contents: write (or `repo` scope) for that repository |
| "Not found" for a private repo | The token cannot see it (repository access / scope) |
| "GitHub rate limit reached" | Wait until the time shown, then retry |
| "This repository is empty" | Add a first commit (for example a README) on GitHub; the Git Data API cannot write to a repository with no commits |
