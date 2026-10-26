/**
 * Project Tech Stack Analyzer & Intelligent .gitignore Suggester
 */
class ProjectAnalyzer {
  static detect(fileList) {
    const paths = fileList.map(f => f.path.toLowerCase());

    const result = {
      type: 'Static Website / General Project',
      icon: '🌐',
      badgeClass: 'badge-general',
      suggestedGitignore: [
        '# General / OS',
        '.DS_Store',
        'Thumbs.db',
        '*.log',
        '.env'
      ]
    };

    // Node.js / JavaScript / TypeScript
    if (paths.some(p => p.endsWith('package.json'))) {
      result.type = 'Node.js / JavaScript Project';
      result.icon = '📦';
      result.badgeClass = 'badge-node';
      result.suggestedGitignore = [
        '# Node.js',
        'node_modules/',
        'npm-debug.log*',
        'yarn-debug.log*',
        'yarn-error.log*',
        '.pnpm-debug.log*',
        'dist/',
        'build/',
        '.env',
        '.env.local'
      ];
      return result;
    }

    // PHP / Composer
    if (paths.some(p => p.endsWith('composer.json') || p.endsWith('index.php') || p.endsWith('.php'))) {
      result.type = 'PHP Web Application';
      result.icon = '🐘';
      result.badgeClass = 'badge-php';
      result.suggestedGitignore = [
        '# PHP / Composer',
        '/vendor/',
        '.env',
        '*.log',
        '.phpunit.result.cache'
      ];
      return result;
    }

    // Python / Django / Flask / FastAPI
    if (paths.some(p => p.endsWith('requirements.txt') || p.endsWith('pyproject.toml') || p.endsWith('manage.py') || p.endsWith('.py'))) {
      result.type = 'Python Application';
      result.icon = '🐍';
      result.badgeClass = 'badge-python';
      result.suggestedGitignore = [
        '# Python',
        '__pycache__/',
        '*.py[cod]',
        '*$py.class',
        'venv/',
        'env/',
        '.env',
        '.pytest_cache/'
      ];
      return result;
    }

    // Android / Gradle
    if (paths.some(p => p.endsWith('build.gradle') || p.endsWith('build.gradle.kts') || p.endsWith('settings.gradle'))) {
      result.type = 'Android / Gradle Project';
      result.icon = '🤖';
      result.badgeClass = 'badge-android';
      result.suggestedGitignore = [
        '# Android & Gradle',
        '.gradle/',
        '/build/',
        'local.properties',
        '*.apk',
        '*.aab',
        'captures/',
        '.idea/'
      ];
      return result;
    }

    // Java / Maven
    if (paths.some(p => p.endsWith('pom.xml'))) {
      result.type = 'Java Maven Project';
      result.icon = '☕';
      result.badgeClass = 'badge-java';
      result.suggestedGitignore = [
        '# Java Maven',
        'target/',
        '*.class',
        '*.jar',
        '*.war',
        '.idea/'
      ];
      return result;
    }

    return result;
  }
}