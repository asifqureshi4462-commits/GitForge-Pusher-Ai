# GitHub Project Pusher v2.0

Production-grade developer platform to inspect, analyze, scan, detect changes, and atomically push `.zip` archives, folders, and multi-file projects directly to GitHub.

---

## 🚀 Key Features

1. **Atomic Git Database API Engine**: Commits all files in a single Git commit using blobs and trees, eliminating per-file merge conflicts.
2. **Project Stack Analysis**: Detects PHP, Node.js, Python, Android/Gradle, Java/Maven, and Static Web projects.
3. **Smart Security Token Scanner**: Scans filenames (`.env`, `*.pem`, `id_rsa`) and file content patterns (AWS keys, OpenAI keys, GitHub tokens, Stripe secrets) and auto-unchecks them.
4. **Change Detection Diff**: Compares local staged files against GitHub's remote Git Tree and reports New, Modified, and Unchanged files.
5. **Feature Gating & Pro Plan**:
   - **Free Plan**: 5 pushes/month, basic ZIP upload.
   - **Pro Plan (₹99/mo)**: Unlimited pushes, secret scanning, change detection diffs, cloud push history, branch/repo creation.
6. **Zero Permanent Token Storage**: GitHub Personal Access Tokens are held only in client session memory during active operations.

---

## 🛠️ Environment Variables & Setup

### Requirements
- **PHP 8.0+** with `pdo_sqlite` or `pdo_mysql` extensions enabled.

### Quick Start
```bash
git clone https://github.com/your-org/github-project-pusher.git
cd github-project-pusher
php -S 127.0.0.1:8000