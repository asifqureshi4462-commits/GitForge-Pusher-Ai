# GitForge-Pusher

**A mobile-friendly project intended to make working with GitHub project
files easier.**

GitForge-Pusher is a work in progress. The repository contains PHP,
JavaScript, CSS and SQL files intended for a project-upload workflow.
Some features may still need implementation and testing; this README
does not claim that every planned feature is already working.

## Contents

-   [Technology](#technology)
-   [Project structure](#project-structure)
-   [Android: Termux](#android-termux)
-   [Linux](#linux)
-   [macOS](#macos)
-   [Windows](#windows)
-   [Configuration](#configuration)
-   [GitHub token safety](#github-token-safety)
-   [Troubleshooting](#troubleshooting)
-   [Limitations and roadmap](#limitations-and-roadmap)
-   [Contributing](#contributing)
-   [License](#license)

## Technology

-   **PHP:** server-side pages and API endpoints.
-   **JavaScript:** browser interactions and client-side utilities.
-   **HTML:** page structure.
-   **CSS:** styling and responsive layout.
-   **SQL:** database schema.
-   **Git / GitHub REST API:** source control and intended GitHub
    integration.

Exact dependencies and versions should be checked in the current source
code.

## Project structure

``` text
GitForge-Pusher/
├── config/
│   ├── app.php
│   └── database.php
├── database/
│   └── schema.sql
├── src/
│   ├── Database.php
│   ├── Auth.php
│   ├── PlanManager.php
│   └── HistoryManager.php
├── api/
│   ├── auth.php
│   ├── user.php
│   ├── history.php
│   ├── admin.php
│   └── webhook.php
├── assets/
│   ├── css/style.css
│   └── js/
│       ├── github-api.js
│       ├── zip-handler.js
│       ├── project-analyzer.js
│       ├── security-scanner.js
│       ├── change-detector.js
│       └── app.js
├── index.php
├── .gitignore
└── README.md
```

This is the intended layout. If a file is missing from your checkout,
check the current repository rather than assuming the file exists.

## Requirements

-   Git
-   PHP CLI
-   A modern browser
-   A database service only if the current configuration requires one
-   Internet access for cloning and GitHub API functionality

Check installations:

``` bash
git --version
php --version
```

## Android: Termux

These commands are for **Termux**. TermOnePlus or a basic Android shell
is different and may not include `pkg`.

### 1. Install packages

``` bash
pkg update
pkg upgrade -y
pkg install git php unzip -y
```

### 2. Clone the repository

``` bash
git clone https://github.com/asifqureshi4462-commits/GitForge-Pusher.git
```

If Git reports that the repository cannot be found, check the URL and
repository visibility. A private repository requires an account with
access.

### 3. Open the project folder

``` bash
cd GitForge-Pusher
ls -la
```

You should see `index.php` and the project folders if they are present
in the current repository version.

### 4. Start the local PHP server

``` bash
php -S 127.0.0.1:8080
```

Keep Termux open while the server runs. Open Chrome on the same phone
and visit:

``` text
http://127.0.0.1:8080
```

Stop the server by returning to Termux and pressing **Ctrl+C**.

### 5. Update the project later

``` bash
cd GitForge-Pusher
git pull
```

Back up or commit your local edits before pulling changes.

### Optional: access shared phone storage

``` bash
termux-setup-storage
```

Approve Android's permission prompt. This is not needed just to clone
the repository.

## Linux

For Debian/Ubuntu:

``` bash
sudo apt update
sudo apt install git php unzip -y
git clone https://github.com/asifqureshi4462-commits/GitForge-Pusher.git
cd GitForge-Pusher
php -S 127.0.0.1:8080
```

Open `http://127.0.0.1:8080` in a browser. Other Linux distributions may
use a different package manager.

## macOS

If Homebrew is installed:

``` bash
brew update
brew install git php
git clone https://github.com/asifqureshi4462-commits/GitForge-Pusher.git
cd GitForge-Pusher
php -S 127.0.0.1:8080
```

Open `http://127.0.0.1:8080`.

## Windows

Install Git for Windows and PHP first, and make sure both commands are
available in your terminal. Then use PowerShell or Git Bash:

``` bash
git clone https://github.com/asifqureshi4462-commits/GitForge-Pusher.git
cd GitForge-Pusher
php -S 127.0.0.1:8080
```

Open `http://127.0.0.1:8080`. If `php` is not recognized, install PHP or
add its folder to PATH.

## Configuration

Before using database-backed features, inspect:

-   `config/app.php`
-   `config/database.php`
-   `database/schema.sql`

Check which database engine the code expects, configure the connection,
create the database, and import `schema.sql` if required. The schema and
PHP code must match; importing the SQL file alone does not guarantee the
app is ready.

Never commit real database passwords, API keys, tokens, or other
secrets.

## GitHub token safety

A GitHub token can grant access to your repositories. Treat it like a
password.

-   Only create a token if the implemented workflow requires one.
-   Grant the minimum permissions needed.
-   Never publish a token in code, README files, issues, discussions,
    screenshots, videos, or chat messages.
-   Never hard-code a token in browser-side JavaScript; users can
    inspect it.
-   Keep secrets out of Git and revoke a token immediately if exposed.
-   Use HTTPS and secure server-side handling for credentials where
    appropriate.

Official guidance:
https://docs.github.com/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens

## Troubleshooting

### `pkg: command not found`

You are probably not in Termux. `pkg` is Termux's package manager and
does not work in every Android terminal app. Use Termux for the Android
instructions or follow the package manager instructions for your actual
environment.

### `git: command not found`

Install Git. In Termux:

``` bash
pkg install git -y
```

In Debian/Ubuntu:

``` bash
sudo apt install git -y
```

### `php: command not found`

In Termux:

``` bash
pkg install php -y
```

### `Repository not found`

Check the repository URL, spelling, visibility, and your account
permissions. Avoid putting a personal access token directly into a clone
URL because it can be saved in shell history.

### Blank page or PHP error

Make sure you started the server from the directory containing
`index.php`. Check the terminal error and run:

``` bash
php -l index.php
```

Also review the configuration and database setup.

### Database connection error

Confirm that the configured database service is running and that the
credentials, database name, and schema match the source code.

### GitHub API or upload error

Check internet access, repository permissions, branch name, API
response, and token permissions. Never make a token public to solve an
authentication problem.

## Limitations and roadmap

GitForge-Pusher is a work in progress. Depending on the current code,
these items may be incomplete or planned:

-   [ ] ZIP upload and safe extraction
-   [ ] File preview and project summary
-   [ ] Repository and branch selection
-   [ ] Commit messages and upload progress
-   [ ] Safe GitHub credential handling
-   [ ] Secret-file warnings
-   [ ] Retry handling and clear error messages
-   [ ] Upload history
-   [ ] Automated tests and security checks
-   [ ] Mobile and desktop usability improvements

The PHP built-in server is for local development only. Do not expose it
directly to the public internet. Before public deployment, test
authentication, authorization, file validation, upload limits, and
database security; use HTTPS and a properly configured production web
server.

Only upload files you own or have permission to publish. Review files
for secrets before pushing them to GitHub.

## Contributing

Ideas and bug reports are welcome.

1.  Open an issue describing the idea or problem.
2.  For bugs, include reproduction steps but remove tokens and private
    data.
3.  For code changes, use a branch and submit a pull request.
4.  Update the documentation when behavior changes.

## License

A license should be added to the repository before the project is
presented as open source. Until then, do not assume others have
permission to reuse or redistribute the code.

## Repository

https://github.com/asifqureshi4462-commits/GitForge-Pusher

Made by **Asif Qureshi**.
