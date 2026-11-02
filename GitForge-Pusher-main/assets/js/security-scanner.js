/**
 * Smart Security Scanner
 * Inspects file names and decoded contents for dangerous secrets & credentials
 */
class SecurityScanner {
  static SENSITIVE_FILENAME_PATTERNS = [
    /^\.env(\..+)?$/i,
    /id_rsa$/i,
    /id_ed25519$/i,
    /\.pem$/i,
    /\.pkcs12$/i,
    /\.pfx$/i,
    /\.keystore$/i,
    /\.jks$/i,
    /service[_-]?account.*\.json$/i,
    /credentials\.json$/i,
    /secret[s]?\.json$/i,
    /wp-config\.php$/i
  ];

  static CONTENT_SECRET_PATTERNS = [
    { name: 'AWS Access Key', regex: /AKIA[0-9A-Z]{16}/g },
    { name: 'GitHub Personal Token', regex: /gh[pousr]_[A-Za-z0-9_]{36,255}/g },
    { name: 'OpenAI API Key', regex: /sk-[A-Za-z0-9-_]{32,}/g },
    { name: 'Stripe Live Secret Key', regex: /sk_live_[0-9a-zA-Z]{24}/g },
    { name: 'Google API Key', regex: /AIza[0-9A-Za-z\\-_]{35}/g },
    { name: 'Private RSA/EC Key', regex: /-----BEGIN (RSA|EC|OPENSSH|PRIVATE) KEY-----/g },
    { name: 'Hardcoded Generic Secret/Password', regex: /(?:password|passwd|secret|api_key|access_token)\s*[:=]\s*["'][^"'\s]{8,}["']/gi }
  ];

  /**
   * Scans a file object { path, base64, size }
   * Returns: { isSensitive: boolean, reasons: string[] }
   */
  static scanFile(file) {
    const reasons = [];
    const fileName = file.path.split('/').pop();

    // 1. Filename check
    for (const pattern of this.SENSITIVE_FILENAME_PATTERNS) {
      if (pattern.test(fileName)) {
        reasons.push(`Sensitive file name match: ${fileName}`);
        break;
      }
    }

    // 2. Decode content check (limit scanning to files < 2MB for speed)
    if (file.size < 2 * 1024 * 1024) {
      try {
        const decoded = atob(file.base64);
        for (const patternObj of this.CONTENT_SECRET_PATTERNS) {
          if (patternObj.regex.test(decoded)) {
            reasons.push(`Contains detected pattern: ${patternObj.name}`);
          }
        }
      } catch (_) {
        // Binary file that cannot be decoded via atob is skipped
      }
    }

    return {
      isSensitive: reasons.length > 0,
      reasons: reasons
    };
  }
}