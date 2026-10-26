/**
 * assets/js/app.js - GitHub Project Pusher Master Engine (All-In-One)
 */

// ==========================================
// 1. GITHUB API ENGINE
// ==========================================
class GitHubAPI {
  constructor(token) {
    this.token = token;
    this.baseUrl = 'https://api.github.com';
  }

  async request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${this.baseUrl}${endpoint}`;
    const headers = {
      'Accept': 'application/vnd.github.v3+json',
      'Authorization': `Bearer ${this.token}`,
      'Content-Type': 'application/json',
      ...options.headers
    };

    const response = await fetch(url, { ...options, headers });
    
    if (response.status === 401) throw new Error('GitHub token is invalid or expired.');
    if (response.status === 403) throw new Error('Permission denied or GitHub API rate limit reached.');
    if (response.status === 404) throw new Error(`Resource not found (${endpoint}).`);

    if (!response.ok) {
      let msg = `GitHub Error ${response.status}`;
      try {
        const err = await response.json();
        if (err.message) msg = err.message;
      } catch (_) {}
      throw new Error(msg);
    }

    if (response.status === 204) return null;
    return await response.json();
  }

  async getAuthenticatedUser() { return await this.request('/user'); }
  async getUserRepositories(page = 1, perPage = 100) {
    return await this.request(`/user/repos?per_page=${perPage}&page=${page}&sort=updated&affiliation=owner,collaborator,organization_member`);
  }
  async getBranches(owner, repo) { return await this.request(`/repos/${owner}/${repo}/branches?per_page=100`); }
  async createRepository(name, description = '', isPrivate = false) {
    return await this.request('/user/repos', {
      method: 'POST',
      body: JSON.stringify({ name, description, private: isPrivate, auto_init: true })
    });
  }
  async createBranch(owner, repo, newBranchName, baseCommitSha) {
    return await this.request(`/repos/${owner}/${repo}/git/refs`, {
      method: 'POST',
      body: JSON.stringify({ ref: `refs/heads/${newBranchName}`, sha: baseCommitSha })
    });
  }
  async getBranchRef(owner, repo, branch) {
    try { return await this.request(`/repos/${owner}/${repo}/git/ref/heads/${branch}`); } catch (_) { return null; }
  }
  async getRecursiveTree(owner, repo, treeSha) {
    try {
      const data = await this.request(`/repos/${owner}/${repo}/git/trees/${treeSha}?recursive=1`);
      return data.tree || [];
    } catch (_) { return []; }
  }
  async createBlob(owner, repo, base64Content) {
    return await this.request(`/repos/${owner}/${repo}/git/blobs`, {
      method: 'POST',
      body: JSON.stringify({ content: base64Content, encoding: 'base64' })
    });
  }
  async createTree(owner, repo, treeItems, baseTreeSha = null) {
    const payload = { tree: treeItems };
    if (baseTreeSha) payload.base_tree = baseTreeSha;
    return await this.request(`/repos/${owner}/${repo}/git/trees`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }
  async createCommit(owner, repo, message, treeSha, parentCommitShas = []) {
    return await this.request(`/repos/${owner}/${repo}/git/commits`, {
      method: 'POST',
      body: JSON.stringify({ message, tree: treeSha, parents: parentCommitShas })
    });
  }
  async updateBranchRef(owner, repo, branch, commitSha, force = false) {
    return await this.request(`/repos/${owner}/${repo}/git/refs/heads/${branch}`, {
      method: 'PATCH',
      body: JSON.stringify({ sha: commitSha, force })
    });
  }
}

// ==========================================
// 2. ZIP & FILE EXTRACTION ENGINE
// ==========================================
class ZipHandler {
  static IGNORED_FILES = ['.DS_Store', 'Thumbs.db', '__MACOSX/', 'desktop.ini', '.git/'];

  static isIgnored(path) {
    return this.IGNORED_FILES.some(ignored => 
      path === ignored || path.startsWith('__MACOSX/') || path.startsWith('.git/') ||
      path.endsWith('/.DS_Store') || path.endsWith('/Thumbs.db')
    );
  }

  static async extractZip(file) {
    if (!window.JSZip) throw new Error('JSZip library is not ready. Check your internet connection.');
    const zip = new window.JSZip();
    const contents = await zip.loadAsync(file);
    const files = [];
    const entries = Object.keys(contents.files);

    const validEntries = entries.filter(path => !contents.files[path].dir && !this.isIgnored(path));
    if (validEntries.length === 0) throw new Error('ZIP archive is empty or contains only ignored system files.');

    for (const relativePath of entries) {
      const zipEntry = contents.files[relativePath];
      if (zipEntry.dir || this.isIgnored(relativePath)) continue;

      const base64Data = await zipEntry.async('base64');
      const uint8 = await zipEntry.async('uint8array');

      files.push({
        path: relativePath.replace(/^\/+/, ''),
        base64: base64Data,
        size: uint8.length,
        _selected: true
      });
    }
    return files;
  }

  static async extractFiles(fileList) {
    const files = [];
    for (let i = 0; i < fileList.length; i++) {
      const file = fileList[i];
      const relativePath = file.webkitRelativePath || file.name;
      if (this.isIgnored(relativePath)) continue;

      const base64Data = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result.substring(reader.result.indexOf(',') + 1));
        reader.onerror = err => reject(err);
        reader.readAsDataURL(file);
      });

      files.push({
        path: relativePath.replace(/^\/+/, ''),
        base64: base64Data,
        size: file.size,
        _selected: true
      });
    }
    return files;
  }

  static formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}

// ==========================================
// 3. PROJECT ANALYZER & SECURITY SCANNER
// ==========================================
class ProjectAnalyzer {
  static detect(fileList) {
    const paths = fileList.map(f => f.path.toLowerCase());
    if (paths.some(p => p.endsWith('package.json'))) {
      return { type: 'Node.js / JS Project', icon: '📦', suggestedGitignore: ['node_modules/', 'dist/', '.env', '.env.local'] };
    }
    if (paths.some(p => p.endsWith('composer.json') || p.endsWith('index.php') || p.endsWith('.php'))) {
      return { type: 'PHP Web Application', icon: '🐘', suggestedGitignore: ['/vendor/', '.env', '*.log'] };
    }
    if (paths.some(p => p.endsWith('requirements.txt') || p.endsWith('pyproject.toml') || p.endsWith('.py'))) {
      return { type: 'Python Project', icon: '🐍', suggestedGitignore: ['__pycache__/', 'venv/', '.env'] };
    }
    if (paths.some(p => p.endsWith('build.gradle') || p.endsWith('settings.gradle'))) {
      return { type: 'Android / Gradle Project', icon: '🤖', suggestedGitignore: ['.gradle/', '/build/', 'local.properties'] };
    }
    return { type: 'Static Web / General Project', icon: '🌐', suggestedGitignore: ['.DS_Store', 'Thumbs.db', '*.log', '.env'] };
  }
}

class SecurityScanner {
  static SENSITIVE_NAMES = [/^\.env(\..+)?$/i, /id_rsa$/i, /\.pem$/i, /\.keystore$/i, /credentials\.json$/i, /secret[s]?\.json$/i];
  static CONTENT_PATTERNS = [
    { name: 'AWS Key', regex: /AKIA[0-9A-Z]{16}/g },
    { name: 'GitHub Token', regex: /gh[pousr]_[A-Za-z0-9_]{36,255}/g },
    { name: 'OpenAI Key', regex: /sk-[A-Za-z0-9-_]{32,}/g },
    { name: 'Stripe Key', regex: /sk_live_[0-9a-zA-Z]{24}/g },
    { name: 'Private Key', regex: /-----BEGIN (RSA|EC|OPENSSH|PRIVATE) KEY-----/g }
  ];

  static scanFile(file) {
    const reasons = [];
    const fileName = file.path.split('/').pop();
    for (const p of this.SENSITIVE_NAMES) {
      if (p.test(fileName)) { reasons.push(`Sensitive file: ${fileName}`); break; }
    }
    if (file.size < 1024 * 1024) {
      try {
        const text = atob(file.base64);
        for (const cp of this.CONTENT_PATTERNS) {
          if (cp.regex.test(text)) reasons.push(`Detected pattern: ${cp.name}`);
        }
      } catch (_) {}
    }
    return { isSensitive: reasons.length > 0, reasons };
  }
}

class ChangeDetector {
  static diff(stagedFiles, remoteTree = []) {
    const remoteMap = new Map();
    remoteTree.forEach(item => { if (item.type === 'blob') remoteMap.set(item.path, item); });
    let newCount = 0, modCount = 0, unchangedCount = 0;
    stagedFiles.forEach(local => {
      const rem = remoteMap.get(local.path);
      if (!rem) { local._diffStatus = 'NEW'; newCount++; }
      else if (rem.size === local.size) { local._diffStatus = 'UNCHANGED'; unchangedCount++; }
      else { local._diffStatus = 'MODIFIED'; modCount++; }
    });
    return { stats: { newCount, modCount, unchangedCount } };
  }
}

// ==========================================
// 4. MAIN APP CONTROLLER & UI WIRING
// ==========================================
(function() {
  let sessionUser = null;
  let github = null;
  let ghUser = null;
  let stagedFiles = [];
  let failedFiles = [];
  let selectedRepo = null;

  // Global View Switcher (Works instantly anywhere)
  window.switchView = function(viewId) {
    const navTabs = document.querySelectorAll('.nav-tab');
    const appViews = document.querySelectorAll('.app-view');
    navTabs.forEach(t => t.classList.toggle('active', t.getAttribute('data-view') === viewId));
    appViews.forEach(v => v.classList.toggle('hidden', v.id !== viewId));
    if (viewId === 'view-history') loadHistory();
    if (viewId === 'view-admin') loadAdminDashboard();
  };

  document.addEventListener('DOMContentLoaded', () => {
    // 1. Bind Navigation Tabs
    document.querySelectorAll('.nav-tab').forEach(tab => {
      tab.addEventListener('click', () => {
        const vid = tab.getAttribute('data-view');
        if (vid) window.switchView(vid);
      });
    });

    // 2. Quick Action Buttons on Dashboard
    const btnQuickPush = document.getElementById('btn-quick-push');
    if (btnQuickPush) btnQuickPush.addEventListener('click', () => window.switchView('view-pusher'));

    const btnQuickHistory = document.getElementById('btn-quick-history');
    if (btnQuickHistory) btnQuickHistory.addEventListener('click', () => window.switchView('view-history'));

    const linkUpgrade = document.getElementById('link-upgrade-pro');
    if (linkUpgrade) linkUpgrade.addEventListener('click', () => window.switchView('view-pricing'));

    // 3. Theme Toggle
    const themeBtn = document.getElementById('btn-theme-toggle');
    if (themeBtn) {
      themeBtn.addEventListener('click', () => {
        const curr = document.documentElement.getAttribute('data-theme');
        const next = curr === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
      });
    }

    // 4. GitHub Connection
    const btnConnect = document.getElementById('btn-connect-gh');
    const btnDisconnect = document.getElementById('btn-disconnect-gh');
    const ghToken = document.getElementById('gh-token-input');
    const ghPill = document.getElementById('gh-status-indicator');

    if (btnConnect) {
      btnConnect.addEventListener('click', async () => {
        const token = ghToken ? ghToken.value.trim() : '';
        if (!token) return showAlert('Please enter a GitHub Token.', 'error');
        btnConnect.disabled = true;
        btnConnect.innerText = 'Connecting...';
        try {
          github = new GitHubAPI(token);
          ghUser = await github.getAuthenticatedUser();
          if (ghPill) { ghPill.className = 'status-pill online'; ghPill.innerText = `@${ghUser.login}`; }
          document.getElementById('dash-gh-status').innerText = 'Connected';
          document.getElementById('dash-gh-handle').innerText = `@${ghUser.login}`;
          btnConnect.classList.add('hidden');
          if (btnDisconnect) btnDisconnect.classList.remove('hidden');
          showAlert(`Connected as @${ghUser.login}`, 'success');
          await loadRepositories();
        } catch (err) {
          showAlert(err.message, 'error');
        } finally {
          btnConnect.disabled = false;
          btnConnect.innerText = 'Connect';
        }
      });
    }

    if (btnDisconnect) {
      btnDisconnect.addEventListener('click', () => {
        github = null; ghUser = null;
        if (ghToken) ghToken.value = '';
        if (ghPill) { ghPill.className = 'status-pill offline'; ghPill.innerText = 'Offline'; }
        document.getElementById('dash-gh-status').innerText = 'Not Connected';
        document.getElementById('dash-gh-handle').innerText = 'Token required';
        if (btnConnect) btnConnect.classList.remove('hidden');
        btnDisconnect.classList.add('hidden');
        document.getElementById('repo-dropdown').innerHTML = '';
        document.getElementById('branch-dropdown').innerHTML = '';
      });
    }

    // 5. Dropzone & File Pickers
    const pZip = document.getElementById('picker-zip');
    const pFolder = document.getElementById('picker-folder');
    const pFiles = document.getElementById('picker-files');
    const dropzone = document.getElementById('app-dropzone');

    if (pZip) pZip.addEventListener('change', (e) => handleFiles(e.target.files));
    if (pFolder) pFolder.addEventListener('change', (e) => handleFiles(e.target.files));
    if (pFiles) pFiles.addEventListener('change', (e) => handleFiles(e.target.files));

    if (dropzone) {
      ['dragover', 'dragenter'].forEach(ev => dropzone.addEventListener(ev, (e) => { e.preventDefault(); dropzone.style.borderColor = 'var(--border-active)'; }));
      ['dragleave', 'drop'].forEach(ev => dropzone.addEventListener(ev, (e) => { e.preventDefault(); dropzone.style.borderColor = 'var(--border-main)'; }));
      dropzone.addEventListener('drop', (e) => { if (e.dataTransfer && e.dataTransfer.files) handleFiles(e.dataTransfer.files); });
    }

    async function handleFiles(fileList) {
      if (!fileList || fileList.length === 0) return;
      try {
        const first = fileList[0];
        const isZip = (first.name || '').toLowerCase().endsWith('.zip') || (first.type || '').includes('zip');
        showAlert('Processing files...', 'success');
        
        if (fileList.length === 1 && isZip) {
          stagedFiles = await ZipHandler.extractZip(first);
        } else {
          stagedFiles = await ZipHandler.extractFiles(fileList);
        }

        // Run Scanner & Intelligence
        const analysis = ProjectAnalyzer.detect(stagedFiles);
        document.getElementById('project-analysis-banner').classList.remove('hidden');
        document.getElementById('analysis-icon').innerText = analysis.icon;
        document.getElementById('analysis-title').innerText = `Detected: ${analysis.type}`;
        document.getElementById('gitignore-text').value = analysis.suggestedGitignore.join('\n');

        let secretsCount = 0;
        stagedFiles.forEach(f => {
          const scan = SecurityScanner.scanFile(f);
          f._isSecret = scan.isSensitive;
          f._selected = !scan.isSensitive;
          if (scan.isSensitive) secretsCount++;
        });

        document.getElementById('security-alert-box').classList.toggle('hidden', secretsCount === 0);
        renderFileTree();
        triggerDiffCheck();
        document.getElementById('btn-execute-push').disabled = false;
        showAlert(`Successfully loaded ${stagedFiles.length} files!`, 'success');
      } catch (err) {
        showAlert(`Error: ${err.message}`, 'error');
      } finally {
        if (pZip) pZip.value = '';
        if (pFolder) pFolder.value = '';
        if (pFiles) pFiles.value = '';
      }
    }

    function renderFileTree() {
      const list = document.getElementById('file-tree-list');
      if (!list) return;
      list.innerHTML = '';
      document.getElementById('staged-count-pill').innerText = `${stagedFiles.length} Files`;
      document.getElementById('file-tree-wrapper').classList.remove('hidden');

      stagedFiles.forEach((file, index) => {
        const row = document.createElement('div');
        row.className = 'tree-row';
        const diffBadge = file._diffStatus ? `<span class="diff-tag tag-${file._diffStatus.toLowerCase()}">${file._diffStatus}</span>` : '';
        const secretBadge = file._isSecret ? `<span class="diff-tag" style="background:rgba(218,54,51,0.2);color:#f85149;">SECRET</span>` : '';

        row.innerHTML = `
          <label style="display:flex; align-items:center; gap:8px; overflow:hidden;">
            <input type="checkbox" data-index="${index}" ${file._selected !== false ? 'checked' : ''}>
            <span style="text-overflow:ellipsis; overflow:hidden; white-space:nowrap;">📄 ${file.path}</span>
          </label>
          <div style="display:flex; align-items:center; gap:8px;">
            ${secretBadge}
            ${diffBadge}
            <span style="color:var(--text-muted); font-size:11px;">${ZipHandler.formatBytes(file.size)}</span>
          </div>
        `;
        list.appendChild(row);
      });

      list.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.addEventListener('change', (e) => {
          const idx = parseInt(e.target.dataset.index, 10);
          stagedFiles[idx]._selected = e.target.checked;
        });
      });
    }

    // 6. Push Action Execution
    const btnPush = document.getElementById('btn-execute-push');
    if (btnPush) {
      btnPush.addEventListener('click', async () => {
        if (!github || !selectedRepo) return showAlert('Connect to GitHub and select a repository.', 'error');
        const filesToPush = stagedFiles.filter(f => f._selected !== false);
        if (filesToPush.length === 0) return showAlert('No files selected.', 'error');

        btnPush.disabled = true;
        const pContainer = document.getElementById('push-progress-container');
        const pLogs = document.getElementById('push-log-stream');
        pContainer.classList.remove('hidden');
        pLogs.innerHTML = '';

        const branch = document.getElementById('branch-dropdown').value || 'main';
        const commitMsg = document.getElementById('commit-message-input').value.trim() || 'Upload project via GitHub Pusher v2';
        const subfolder = document.getElementById('target-subfolder').value.trim().replace(/^\/+|\/+$/g, '');

        try {
          addLog(`Target branch: ${branch}`, 'info');
          const branchRef = await github.getBranchRef(selectedRepo.owner, selectedRepo.name, branch);
          let baseCommitSha = branchRef?.object?.sha || null;
          let baseTreeSha = null;

          if (baseCommitSha) {
            const baseCommit = await github.request(`/repos/${selectedRepo.owner}/${selectedRepo.name}/git/commits/${baseCommitSha}`);
            baseTreeSha = baseCommit.tree.sha;
          }

          const treeItems = [];
          const total = filesToPush.length;
          for (let i = 0; i < total; i++) {
            const f = filesToPush[i];
            const blob = await github.createBlob(selectedRepo.owner, selectedRepo.name, f.base64);
            const path = subfolder ? `${subfolder}/${f.path}` : f.path;
            treeItems.push({ path, mode: '100644', type: 'blob', sha: blob.sha });
            const pct = Math.round(((i + 1) / total) * 100);
            document.getElementById('push-percent-label').innerText = `${pct}%`;
            document.getElementById('push-progress-fill').style.width = `${pct}%`;
            document.getElementById('push-count-label').innerText = `Uploaded: ${i + 1} / ${total}`;
            addLog(`✔ Blob created: ${f.path}`, 'success');
          }

          document.getElementById('push-stage-label').innerText = 'Creating atomic commit...';
          const tree = await github.createTree(selectedRepo.owner, selectedRepo.name, treeItems, baseTreeSha);
          const commit = await github.createCommit(selectedRepo.owner, selectedRepo.name, commitMsg, tree.sha, baseCommitSha ? [baseCommitSha] : []);
          
          if (baseCommitSha) {
            await github.updateBranchRef(selectedRepo.owner, selectedRepo.name, branch, commit.sha);
          } else {
            await github.createBranch(selectedRepo.owner, selectedRepo.name, branch, commit.sha);
          }

          document.getElementById('push-success-card').classList.remove('hidden');
          document.getElementById('success-commit-sha').innerText = `Commit: ${commit.sha.substring(0, 7)}`;
          document.getElementById('btn-open-repo-link').href = `https://github.com/${selectedRepo.fullName}/tree/${branch}`;
          document.getElementById('btn-open-commit-link').href = `https://github.com/${selectedRepo.fullName}/commit/${commit.sha}`;
          showAlert('Project pushed to GitHub successfully!', 'success');
        } catch (err) {
          showAlert(`Push Failed: ${err.message}`, 'error');
        } finally {
          btnPush.disabled = false;
        }
      });
    }

    function addLog(msg, type = 'info') {
      const logs = document.getElementById('push-log-stream');
      if (!logs) return;
      const d = document.createElement('div');
      d.className = `log-item ${type}`;
      d.innerText = `[${new Date().toLocaleTimeString()}] ${msg}`;
      logs.appendChild(d);
      logs.scrollTop = logs.scrollHeight;
    }

    // Helper functions for loading repos and diffs
    async function loadRepositories() {
      const sel = document.getElementById('repo-dropdown');
      sel.innerHTML = '<option>Loading repositories...</option>';
      try {
        const repos = await github.getUserRepositories();
        window._cachedRepos = repos;
        sel.innerHTML = '';
        repos.forEach(r => {
          const opt = document.createElement('option');
          opt.value = r.full_name;
          opt.textContent = `${r.full_name} (${r.private ? 'Private' : 'Public'})`;
          opt.dataset.owner = r.owner.login;
          opt.dataset.name = r.name;
          opt.dataset.branch = r.default_branch;
          sel.appendChild(opt);
        });
        if (repos.length > 0) {
          sel.selectedIndex = 0;
          onRepoChanged();
        }
      } catch (err) { showAlert(err.message, 'error'); }
    }

    document.getElementById('repo-dropdown')?.addEventListener('change', onRepoChanged);

    async function onRepoChanged() {
      const sel = document.getElementById('repo-dropdown');
      const opt = sel.options[sel.selectedIndex];
      if (!opt) return;
      selectedRepo = { fullName: opt.value, owner: opt.dataset.owner, name: opt.dataset.name, defaultBranch: opt.dataset.branch };
      const bSel = document.getElementById('branch-dropdown');
      bSel.innerHTML = '<option>Loading branches...</option>';
      try {
        const branches = await github.getBranches(selectedRepo.owner, selectedRepo.name);
        bSel.innerHTML = '';
        branches.forEach(b => {
          const bOpt = document.createElement('option');
          bOpt.value = b.name;
          bOpt.textContent = b.name === selectedRepo.defaultBranch ? `${b.name} (Default)` : b.name;
          bSel.appendChild(bOpt);
        });
        if (selectedRepo.defaultBranch) bSel.value = selectedRepo.defaultBranch;
      } catch (_) {}
    }

    async function triggerDiffCheck() {
      if (!github || !selectedRepo || stagedFiles.length === 0) return;
      const branch = document.getElementById('branch-dropdown').value;
      try {
        const ref = await github.getBranchRef(selectedRepo.owner, selectedRepo.name, branch);
        if (ref && ref.object) {
          const commit = await github.request(`/repos/${selectedRepo.owner}/${selectedRepo.name}/git/commits/${ref.object.sha}`);
          const rTree = await github.getRecursiveTree(selectedRepo.owner, selectedRepo.name, commit.tree.sha);
          const diff = ChangeDetector.diff(stagedFiles, rTree);
          document.getElementById('diff-summary-bar').classList.remove('hidden');
          document.getElementById('diff-count-new').innerText = `${diff.stats.newCount} New`;
          document.getElementById('diff-count-mod').innerText = `${diff.stats.modCount} Modified`;
          document.getElementById('diff-count-same').innerText = `${diff.stats.unchangedCount} Unchanged`;
          renderFileTree();
        }
      } catch (_) {}
    }

    // Auth Modals & Helpers
    const aModal = document.getElementById('auth-modal');
    document.getElementById('btn-open-auth-modal')?.addEventListener('click', () => aModal.classList.remove('hidden'));
    document.getElementById('btn-close-auth-modal')?.addEventListener('click', () => aModal.classList.add('hidden'));
    document.getElementById('link-goto-signup')?.addEventListener('click', () => {
      document.getElementById('auth-login-form').classList.add('hidden');
      document.getElementById('auth-reg-form').classList.remove('hidden');
      document.getElementById('auth-modal-title').innerText = 'Create Account';
    });
    document.getElementById('link-goto-login')?.addEventListener('click', () => {
      document.getElementById('auth-login-form').classList.remove('hidden');
      document.getElementById('auth-reg-form').classList.add('hidden');
      document.getElementById('auth-modal-title').innerText = 'Sign In';
    });

    // Clear Staged
    document.getElementById('btn-clear-staged')?.addEventListener('click', () => {
      stagedFiles = [];
      document.getElementById('file-tree-wrapper').classList.add('hidden');
      document.getElementById('project-analysis-banner').classList.add('hidden');
      document.getElementById('security-alert-box').classList.add('hidden');
      document.getElementById('diff-summary-bar').classList.add('hidden');
      document.getElementById('btn-execute-push').disabled = true;
      document.getElementById('staged-count-pill').innerText = '0 Files';
    });

    // Toggle Secret Vis
    document.getElementById('btn-toggle-token-eye')?.addEventListener('click', () => {
      const inp = document.getElementById('gh-token-input');
      inp.type = inp.type === 'password' ? 'text' : 'password';
    });
  });

  function showAlert(msg, type = 'error') {
    const el = document.getElementById('global-alert');
    if (!el) return;
    el.className = `alert alert-${type}`;
    el.innerText = msg;
    el.classList.remove('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => el.classList.add('hidden'), 5000);
  }
})();