/**
 * assets/js/zip-handler.js
 * Mobile-friendly ZIP decompression & Directory Preservation Engine
 */
class ZipHandler {
  static IGNORED_FILES = [
    '.DS_Store',
    'Thumbs.db',
    '__MACOSX/',
    'desktop.ini',
    '.git/'
  ];

  static isIgnored(path) {
    return this.IGNORED_FILES.some(ignored => 
      path === ignored || 
      path.startsWith('__MACOSX/') || 
      path.startsWith('.git/') ||
      path.endsWith('/.DS_Store') || 
      path.endsWith('/Thumbs.db')
    );
  }

  static async extractZip(file, onProgress) {
    if (!window.JSZip) {
      throw new Error('JSZip library is still loading or unavailable. Please check your internet connection.');
    }

    const zip = new JSZip();
    const contents = await zip.loadAsync(file);
    const files = [];
    const entries = Object.keys(contents.files);
    
    const validEntries = entries.filter(path => !contents.files[path].dir && !this.isIgnored(path));
    if (validEntries.length === 0) {
      throw new Error('The ZIP archive is empty or contains only system files.');
    }

    let processed = 0;
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

      processed++;
      if (onProgress) onProgress(processed, validEntries.length);
    }

    return files;
  }

  static async extractFiles(fileList, onProgress) {
    const files = [];
    let processed = 0;

    for (const file of fileList) {
      const relativePath = file.webkitRelativePath || file.name;
      if (this.isIgnored(relativePath)) continue;

      const base64Data = await this.fileToBase64(file);

      files.push({
        path: relativePath.replace(/^\/+/, ''),
        base64: base64Data,
        size: file.size,
        _selected: true
      });

      processed++;
      if (onProgress) onProgress(processed, fileList.length);
    }

    return files;
  }

  static fileToBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result;
        const base64 = result.substring(result.indexOf(',') + 1);
        resolve(base64);
      };
      reader.onerror = error => reject(error);
      reader.readAsDataURL(file);
    });
  }

  static formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}