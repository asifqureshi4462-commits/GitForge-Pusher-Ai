/**
 * GitHub API Engine v2.0
 * Includes Repository Creation, Branch Creation, Tree Differencing, Atomic Commits
 */
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
    
    if (response.status === 401) {
      throw new Error('GitHub authentication failed: Token is invalid or expired.');
    }
    if (response.status === 403) {
      const rateLimit = response.headers.get('x-ratelimit-remaining');
      if (rateLimit === '0') {
        throw new Error('GitHub API rate limit reached. Please wait or use an authenticated token.');
      }
      throw new Error('Permission denied. Ensure your token has write permissions.');
    }
    if (response.status === 404) {
      throw new Error(`GitHub resource not found (${endpoint}).`);
    }

    if (!response.ok) {
      let msg = `GitHub Error ${response.status}: ${response.statusText}`;
      try {
        const err = await response.json();
        if (err.message) msg = err.message;
      } catch (_) {}
      throw new Error(msg);
    }

    if (response.status === 204) return null;
    return await response.json();
  }

  async getAuthenticatedUser() {
    return await this.request('/user');
  }

  async getUserRepositories(page = 1, perPage = 100) {
    return await this.request(
      `/user/repos?per_page=${perPage}&page=${page}&sort=updated&affiliation=owner,collaborator,organization_member`
    );
  }

  async getBranches(owner, repo) {
    return await this.request(`/repos/${owner}/${repo}/branches?per_page=100`);
  }

  async getRepository(owner, repo) {
    return await this.request(`/repos/${owner}/${repo}`);
  }

  // Create a brand-new repository
  async createRepository(name, description = '', isPrivate = false, autoInit = true) {
    return await this.request('/user/repos', {
      method: 'POST',
      body: JSON.stringify({
        name,
        description,
        private: isPrivate,
        auto_init: autoInit
      })
    });
  }

  // Create a new branch pointing to an existing commit SHA
  async createBranch(owner, repo, newBranchName, baseCommitSha) {
    return await this.request(`/repos/${owner}/${repo}/git/refs`, {
      method: 'POST',
      body: JSON.stringify({
        ref: `refs/heads/${newBranchName}`,
        sha: baseCommitSha
      })
    });
  }

  async getBranchRef(owner, repo, branch) {
    try {
      return await this.request(`/repos/${owner}/${repo}/git/ref/heads/${branch}`);
    } catch (_) {
      return null;
    }
  }

  // Recursive tree fetch for change detection
  async getRecursiveTree(owner, repo, treeSha) {
    try {
      const data = await this.request(`/repos/${owner}/${repo}/git/trees/${treeSha}?recursive=1`);
      return data.tree || [];
    } catch (_) {
      return [];
    }
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
      body: JSON.stringify({
        message,
        tree: treeSha,
        parents: parentCommitShas
      })
    });
  }

  async updateBranchRef(owner, repo, branch, commitSha, force = false) {
    return await this.request(`/repos/${owner}/${repo}/git/refs/heads/${branch}`, {
      method: 'PATCH',
      body: JSON.stringify({ sha: commitSha, force })
    });
  }
}