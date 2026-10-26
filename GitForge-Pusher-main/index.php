<!DOCTYPE html>
<html lang="en" data-theme="dark">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>GitHub Project Pusher v2.0</title>
  <link rel="stylesheet" href="assets/css/style.css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500;600&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
  <!-- JSZip CDN -->
  <script src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js"></script>
</head>
<body>
  <div class="app-layout">
    
    <!-- Top Navigation Bar -->
    <header class="navbar">
      <div class="nav-brand">
        <svg class="octicon" viewBox="0 0 16 16" width="26" height="26" fill="currentColor">
          <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.21-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path>
        </svg>
        <span class="nav-title">GitHub Project Pusher</span>
        <span class="badge badge-version">v2.0</span>
      </div>

      <!-- Navigation Tabs -->
      <nav class="nav-tabs">
        <button type="button" class="nav-tab active" data-view="view-dashboard">Dashboard</button>
        <button type="button" class="nav-tab" data-view="view-pusher">Push Project</button>
        <button type="button" class="nav-tab" data-view="view-history">History</button>
        <button type="button" class="nav-tab" data-view="view-pricing">Pricing</button>
        <button type="button" class="nav-tab" data-view="view-settings">Settings</button>
        <button type="button" class="nav-tab hidden" id="tab-admin" data-view="view-admin">Admin</button>
      </nav>

      <!-- Right Header Actions -->
      <div class="nav-actions">
        <button type="button" id="btn-theme-toggle" class="btn-icon" title="Toggle Theme">🌓</button>
        <div id="user-badge-container">
          <button type="button" id="btn-open-auth-modal" class="btn btn-sm btn-secondary">Sign In</button>
        </div>
      </div>
    </header>

    <!-- Global Alert Container -->
    <div id="global-alert" class="alert hidden"></div>

    <!-- MAIN VIEW CONTAINER -->
    <main class="content-wrapper">

      <!-- ================= 1. DASHBOARD VIEW ================= -->
      <section id="view-dashboard" class="app-view">
        <div class="dashboard-hero">
          <div class="hero-left">
            <h2>Welcome, <span id="dash-username">Guest Developer</span></h2>
            <p class="text-muted">Manage your connected GitHub repositories, project quotas, and recent pushes.</p>
          </div>
          <div class="hero-right">
            <span id="dash-plan-badge" class="badge badge-free">FREE PLAN</span>
          </div>
        </div>

        <!-- Metrics Grid -->
        <div class="metrics-grid">
          <div class="metric-card">
            <span class="metric-label">Monthly Pushes</span>
            <div class="metric-val" id="dash-pushes-count">0 / 5</div>
            <div class="progress-track"><div id="dash-usage-bar" class="progress-fill" style="width: 0%;"></div></div>
          </div>
          <div class="metric-card">
            <span class="metric-label">GitHub Status</span>
            <div class="metric-val" id="dash-gh-status">Not Connected</div>
            <span class="metric-sub" id="dash-gh-handle">Token required</span>
          </div>
          <div class="metric-card">
            <span class="metric-label">Files Uploaded</span>
            <div class="metric-val" id="dash-total-files">0</div>
            <span class="metric-sub">This billing cycle</span>
          </div>
          <div class="metric-card">
            <span class="metric-label">Active Plan</span>
            <div class="metric-val" id="dash-plan-title">Community Free</div>
            <a href="javascript:void(0)" class="metric-link" id="link-upgrade-pro">Upgrade to Pro ↗</a>
          </div>
        </div>

        <div class="quick-actions-bar">
          <button type="button" id="btn-quick-push" class="btn btn-primary">🚀 Start New Push</button>
          <button type="button" id="btn-quick-history" class="btn btn-secondary">📜 View Upload History</button>
        </div>
      </section>

      <!-- ================= 2. PUSH PROJECT WORKFLOW ================= -->
      <section id="view-pusher" class="app-view hidden">
        <div class="pusher-grid">
          
          <!-- Column A: GitHub Target Setup -->
          <div class="panel">
            <div class="panel-header">
              <h3>1. GitHub Destination</h3>
              <span id="gh-status-indicator" class="status-pill offline">Offline</span>
            </div>
            <div class="panel-body">
              <div class="form-group">
                <label for="gh-token-input">Personal Access Token</label>
                <div class="input-wrapper">
                  <input type="password" id="gh-token-input" placeholder="ghp_xxxxxxxxxxxxxxxxxxxx">
                  <button type="button" id="btn-toggle-token-eye" class="btn-icon">👁</button>
                </div>
                <div class="button-row-sm" style="margin-top: 6px;">
                  <button type="button" id="btn-connect-gh" class="btn btn-primary btn-sm">Connect</button>
                  <button type="button" id="btn-disconnect-gh" class="btn btn-secondary btn-sm hidden">Disconnect</button>
                </div>
              </div>

              <hr class="divider">

              <div class="form-group">
                <div class="flex-between">
                  <label for="repo-dropdown">Repository</label>
                  <button type="button" id="btn-show-create-repo" class="btn-link">+ Create New Repo</button>
                </div>
                <input type="text" id="repo-search-filter" placeholder="Search repositories..." class="input-sm">
                <select id="repo-dropdown" size="4" class="custom-select"></select>
              </div>

              <!-- New Repo Inline Form -->
              <div id="create-repo-box" class="sub-box hidden">
                <h4>Create New GitHub Repository</h4>
                <input type="text" id="new-repo-name" placeholder="repository-name">
                <input type="text" id="new-repo-desc" placeholder="Description (optional)">
                <label><input type="checkbox" id="new-repo-private"> Private Repository</label>
                <div class="button-row-sm">
                  <button type="button" id="btn-submit-create-repo" class="btn btn-success btn-sm">Create on GitHub</button>
                  <button type="button" id="btn-cancel-create-repo" class="btn btn-secondary btn-sm">Cancel</button>
                </div>
              </div>

              <div class="form-group">
                <div class="flex-between">
                  <label for="branch-dropdown">Branch</label>
                  <button type="button" id="btn-show-create-branch" class="btn-link">+ New Branch</button>
                </div>
                <select id="branch-dropdown" class="custom-select"></select>
                <div id="create-branch-box" class="inline-group hidden" style="margin-top: 6px;">
                  <input type="text" id="new-branch-name" placeholder="feature-v2">
                  <button type="button" id="btn-submit-create-branch" class="btn btn-sm btn-primary">Create</button>
                </div>
              </div>

              <div class="form-group">
                <label for="commit-message-input">Commit Message</label>
                <textarea id="commit-message-input" rows="2">Upload project files via GitHub Project Pusher v2</textarea>
              </div>

              <div class="form-group">
                <label for="target-subfolder">Target Subfolder (Optional)</label>
                <input type="text" id="target-subfolder" placeholder="Leave empty for root, e.g. frontend/">
              </div>
            </div>
          </div>

          <!-- Column B: Project Source & Scanning -->
          <div class="panel">
            <div class="panel-header">
              <h3>2. Project Upload & Smart Engine</h3>
              <span id="staged-count-pill" class="badge">0 Files</span>
            </div>
            <div class="panel-body">
              
              <!-- Dropzone -->
              <div id="app-dropzone" class="dropzone">
                <svg viewBox="0 0 24 24" width="40" height="40" stroke="currentColor" stroke-width="1.5" fill="none">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="17 8 12 3 7 8"></polyline>
                  <line x1="12" y1="3" x2="12" y2="15"></line>
                </svg>
                <p>Select your <strong>.ZIP Archive</strong> or Project Folder</p>
                <div class="file-btn-group">
                  <label class="btn btn-secondary btn-sm file-btn">
                    📦 Select .ZIP 
                    <input type="file" id="picker-zip" accept=".zip,application/zip,application/x-zip,application/x-zip-compressed,application/octet-stream">
                  </label>
                  <label class="btn btn-secondary btn-sm file-btn">
                    📁 Folder 
                    <input type="file" id="picker-folder" webkitdirectory directory multiple>
                  </label>
                  <label class="btn btn-secondary btn-sm file-btn">
                    📄 Files 
                    <input type="file" id="picker-files" multiple>
                  </label>
                </div>
              </div>

              <!-- Project Type Banner -->
              <div id="project-analysis-banner" class="analysis-banner hidden">
                <div class="analysis-icon" id="analysis-icon">📦</div>
                <div class="analysis-info">
                  <strong id="analysis-title">Detected: PHP Web Project</strong>
                  <p id="analysis-desc">Smart .gitignore rules recommended.</p>
                </div>
                <button type="button" id="btn-toggle-gitignore" class="btn btn-sm btn-secondary">Review .gitignore</button>
              </div>

              <!-- Smart Gitignore Drawer -->
              <div id="gitignore-drawer" class="sub-box hidden">
                <label for="gitignore-text"><strong>Smart .gitignore Rules (will filter upload list):</strong></label>
                <textarea id="gitignore-text" rows="4"></textarea>
                <button type="button" id="btn-apply-gitignore" class="btn btn-sm btn-primary">Apply Exclusions</button>
              </div>

              <!-- Security Scanner Warning -->
              <div id="security-alert-box" class="security-warning hidden">
                <div class="sec-icon">⚠️</div>
                <div>
                  <strong>Security Alert: Potential Secrets Found</strong>
                  <p>Sensitive configuration or API tokens detected. Dangerous files have been auto-unchecked below.</p>
                </div>
              </div>

              <!-- Change Detection Summary -->
              <div id="diff-summary-bar" class="diff-summary hidden">
                <span class="diff-tag tag-new" id="diff-count-new">0 New</span>
                <span class="diff-tag tag-mod" id="diff-count-mod">0 Modified</span>
                <span class="diff-tag tag-same" id="diff-count-same">0 Unchanged</span>
                <button type="button" id="btn-run-diff" class="btn-link">Refresh Remote Diff</button>
              </div>

              <!-- File Tree Preview -->
              <div id="file-tree-wrapper" class="file-tree-wrapper hidden">
                <div class="file-tree-controls">
                  <label><input type="checkbox" id="cb-select-all" checked> Select All</label>
                  <button type="button" id="btn-clear-staged" class="btn-link">Clear Files</button>
                </div>
                <div id="file-tree-list" class="file-tree-list"></div>
              </div>

              <!-- Push Execution Box -->
              <div class="push-action-wrapper">
                <button type="button" id="btn-execute-push" class="btn btn-large btn-success" disabled>
                  🚀 Review & Push to GitHub
                </button>
              </div>

              <!-- Real-time Progress Bar & Logs -->
              <div id="push-progress-container" class="progress-box hidden">
                <div class="progress-header">
                  <span id="push-stage-label">Uploading Blobs...</span>
                  <span id="push-percent-label">0%</span>
                </div>
                <div class="progress-track"><div id="push-progress-fill" class="progress-fill" style="width: 0%;"></div></div>
                <div class="progress-footer">
                  <span id="push-count-label">Processed: 0 / 0</span>
                  <button type="button" id="btn-retry-failed" class="btn btn-sm btn-danger hidden">Retry Failed Files</button>
                </div>
                <div id="push-log-stream" class="log-stream"></div>
              </div>

              <!-- Result Notification Card -->
              <div id="push-success-card" class="success-card hidden">
                <div class="success-icon">🎉</div>
                <div>
                  <h3>Pushed to GitHub Successfully!</h3>
                  <p id="success-commit-sha">Commit SHA: </p>
                  <div class="button-row-sm">
                    <a id="btn-open-repo-link" href="#" target="_blank" class="btn btn-sm btn-primary">Open Repository ↗</a>
                    <a id="btn-open-commit-link" href="#" target="_blank" class="btn btn-sm btn-secondary">Open Commit ↗</a>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>
      </section>

      <!-- ================= 3. HISTORY VIEW ================= -->
      <section id="view-history" class="app-view hidden">
        <div class="card">
          <div class="card-header">
            <h2>📜 Upload & Push History</h2>
            <button type="button" id="btn-refresh-history" class="btn btn-sm btn-secondary">Refresh</button>
          </div>
          <div class="card-body">
            <div class="table-responsive">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Repository</th>
                    <th>Branch</th>
                    <th>Files</th>
                    <th>Commit Message</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody id="history-table-body">
                  <tr><td colspan="7" class="text-center text-muted">No pushes recorded yet.</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <!-- ================= 4. PRICING VIEW ================= -->
      <section id="view-pricing" class="app-view hidden">
        <div class="pricing-header">
          <h2>Simple, Developer-Friendly Pricing</h2>
          <p class="text-muted">Push faster with smart code intelligence, automatic diffs, and unlimited atomic commits.</p>
        </div>

        <div class="pricing-grid">
          <div class="pricing-card">
            <div class="tier-header">
              <h3>Community Free</h3>
              <div class="tier-price">₹0 <span>/ month</span></div>
            </div>
            <ul class="tier-features">
              <li>✔ 5 Monthly Project Pushes</li>
              <li>✔ ZIP & Folder Extraction</li>
              <li>✔ Standard Git Database API</li>
              <li>✔ Up to 25MB File Uploads</li>
              <li>✖ Change Detection Engine</li>
              <li>✖ Smart Security Token Scan</li>
              <li>✖ Cloud Upload History</li>
            </ul>
            <button type="button" class="btn btn-secondary btn-large" disabled>Current Active Plan</button>
          </div>

          <div class="pricing-card featured">
            <div class="featured-badge">MOST POPULAR</div>
            <div class="tier-header">
              <h3>Pro Developer</h3>
              <div class="tier-price">₹99 <span>/ month</span></div>
            </div>
            <ul class="tier-features">
              <li>✔ <strong>Unlimited</strong> Project Pushes</li>
              <li>✔ <strong>Smart Security Secret Scanner</strong></li>
              <li>✔ <strong>Live Change Detection (Diff)</strong></li>
              <li>✔ <strong>Intelligent .gitignore Engine</strong></li>
              <li>✔ <strong>Create Repos & Branches</strong> Directly</li>
              <li>✔ <strong>Cloud Upload History & Audit Logs</strong></li>
              <li>✔ Up to 100MB File Support</li>
            </ul>
            <button type="button" id="btn-upgrade-pro" class="btn btn-success btn-large">⚡ Upgrade to Pro (₹99)</button>
          </div>
        </div>
      </section>

      <!-- ================= 5. SETTINGS VIEW ================= -->
      <section id="view-settings" class="app-view hidden">
        <div class="card max-w-600">
          <div class="card-header">
            <h2>⚙️ Preferences & Configuration</h2>
          </div>
          <div class="card-body">
            <form id="settings-form">
              <div class="form-group">
                <label for="pref-branch">Default Git Branch</label>
                <input type="text" id="pref-branch" value="main">
              </div>
              <div class="form-group">
                <label for="pref-commit">Default Commit Message</label>
                <input type="text" id="pref-commit" value="Deploy project via GitHub Project Pusher v2">
              </div>
              <div class="form-group">
                <label><input type="checkbox" id="pref-auto-sec" checked> Auto-uncheck detected secret files (.env, keys)</label>
              </div>
              <button type="submit" class="btn btn-primary">Save Preferences</button>
            </form>
          </div>
        </div>
      </section>

      <!-- ================= 6. ADMIN VIEW ================= -->
      <section id="view-admin" class="app-view hidden">
        <div class="card">
          <div class="card-header">
            <h2>🛡️ System Administrator Dashboard</h2>
            <span class="badge badge-pro">Superadmin</span>
          </div>
          <div class="card-body">
            <div class="metrics-grid">
              <div class="metric-card"><span class="metric-label">Total Users</span><div class="metric-val" id="adm-users">0</div></div>
              <div class="metric-card"><span class="metric-label">Pro Subscribers</span><div class="metric-val" id="adm-pro">0</div></div>
              <div class="metric-card"><span class="metric-label">Total Pushes</span><div class="metric-val" id="adm-pushes">0</div></div>
              <div class="metric-card"><span class="metric-label">Success Rate</span><div class="metric-val" id="adm-success">100%</div></div>
            </div>
            <h4 class="mt-4">Recent Platform Pushes</h4>
            <div class="table-responsive">
              <table class="data-table">
                <thead><tr><th>User</th><th>Repo</th><th>Branch</th><th>Files</th><th>Status</th><th>Time</th></tr></thead>
                <tbody id="adm-pushes-table"></tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

    </main>

    <!-- AUTHENTICATION MODAL -->
    <div id="auth-modal" class="modal-overlay hidden">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="auth-modal-title">Sign In to GitHub Pusher</h3>
          <button type="button" id="btn-close-auth-modal" class="btn-icon">✕</button>
        </div>
        <div class="modal-body">
          <form id="auth-login-form">
            <div class="form-group">
              <label for="auth-login-input">Username or Email</label>
              <input type="text" id="auth-login-input" required autocomplete="username">
            </div>
            <div class="form-group">
              <label for="auth-pass-input">Password</label>
              <input type="password" id="auth-pass-input" required autocomplete="current-password">
            </div>
            <button type="submit" class="btn btn-primary btn-large">Log In</button>
            <p class="text-center mt-2 text-muted">Don't have an account? <a href="javascript:void(0)" id="link-goto-signup">Sign Up</a></p>
          </form>

          <form id="auth-reg-form" class="hidden">
            <div class="form-group">
              <label for="reg-user-input">Username</label>
              <input type="text" id="reg-user-input" required>
            </div>
            <div class="form-group">
              <label for="reg-email-input">Email Address</label>
              <input type="email" id="reg-email-input" required>
            </div>
            <div class="form-group">
              <label for="reg-pass-input">Password (min 6 chars)</label>
              <input type="password" id="reg-pass-input" minlength="6" required>
            </div>
            <button type="submit" class="btn btn-success btn-large">Create Account</button>
            <p class="text-center mt-2 text-muted">Already have an account? <a href="javascript:void(0)" id="link-goto-login">Log In</a></p>
          </form>
        </div>
      </div>
    </div>

  </div>

  <!-- SINGLE MASTER APP SCRIPT -->
  <script src="assets/js/app.js"></script>
</body>
</html>