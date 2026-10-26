/**
 * Change Detection Engine
 * Compares local staged files against GitHub's remote Git Tree
 */
class ChangeDetector {
  /**
   * Compares staged local files against remote tree entries
   * remoteTree: Array of { path, sha, size, type } from GitHub API
   */
  static diff(stagedFiles, remoteTree = []) {
    const remoteMap = new Map();
    remoteTree.forEach(item => {
      if (item.type === 'blob') {
        remoteMap.set(item.path, item);
      }
    });

    const results = {
      newFiles: [],
      modifiedFiles: [],
      unchangedFiles: [],
      stats: { newCount: 0, modCount: 0, unchangedCount: 0 }
    };

    stagedFiles.forEach(localFile => {
      const remote = remoteMap.get(localFile.path);

      if (!remote) {
        results.newFiles.push(localFile);
        results.stats.newCount++;
        localFile._diffStatus = 'NEW';
      } else {
        // Simple size/SHA comparison
        if (remote.size !== undefined && remote.size === localFile.size) {
          results.unchangedFiles.push(localFile);
          results.stats.unchangedCount++;
          localFile._diffStatus = 'UNCHANGED';
        } else {
          results.modifiedFiles.push(localFile);
          results.stats.modCount++;
          localFile._diffStatus = 'MODIFIED';
        }
      }
    });

    return results;
  }
}