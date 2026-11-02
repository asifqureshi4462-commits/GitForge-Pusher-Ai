package com.asifqureshi.gitforge.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.asifqureshi.gitforge.domain.ai.AiPrompts;
import com.asifqureshi.gitforge.domain.ai.AiValidators;
import com.asifqureshi.gitforge.domain.ai.ProjectDigest;
import com.asifqureshi.gitforge.domain.project.ProjectFile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ProjectDigestTest {

    private static ProjectFile write(File dir, String path, String content) throws Exception {
        File f = new File(dir, path.replace('/', '_'));
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return new ProjectFile(path, f, f.length());
    }

    @Test
    public void includesSelectedTextFilesAndExcludesSecretsAndUnselected() throws Exception {
        File dir = Files.createTempDirectory("digest").toFile();
        ProjectFile readme = write(dir, "README.md", "# My project");
        ProjectFile env = write(dir, ".env", "TOKEN=abc");
        env.setSecretFindings(java.util.Collections.singletonList("Environment file"));
        ProjectFile hidden = write(dir, "notes.txt", "private notes");
        hidden.setSelected(false);
        List<ProjectFile> files = new ArrayList<>();
        files.add(readme);
        files.add(env);
        files.add(hidden);
        String digest = ProjectDigest.build(files);
        assertTrue(digest.contains("- README.md"));
        assertTrue(digest.contains("# My project"));
        assertFalse(digest.contains(".env"));
        assertFalse(digest.contains("notes.txt"));
    }

    @Test
    public void inputIsTruncatedAndSecretsAreDetectedBeforeSending() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < AiPrompts.MAX_INPUT_CHARS + 500; i++) {
            sb.append('x');
        }
        assertTrue(AiPrompts.truncate(sb.toString()).length() == AiPrompts.MAX_INPUT_CHARS);
        assertFalse(AiPrompts.findSecrets("api_key = 'abcdefgh12345678'").isEmpty());
        assertTrue(AiPrompts.findSecrets("plain code").isEmpty());
    }

    @Test
    public void aiSettingsValidation() {
        assertTrue(AiValidators.validateBaseUrl("https://api.openai.com/v1") == null);
        assertFalse(AiValidators.validateBaseUrl("http://api.openai.com/v1") == null);
        assertFalse(AiValidators.validateBaseUrl("https://user:pw@host.example/v1") == null);
        assertFalse(AiValidators.validateBaseUrl("https://host.example/v1?key=1") == null);
        assertFalse(AiValidators.validateBaseUrl("") == null);
        assertTrue(AiValidators.normalizeBaseUrl(" https://host.example/v1/// ").equals("https://host.example/v1"));
        assertTrue(AiValidators.validateModel("gpt-4o-mini") == null);
        assertFalse(AiValidators.validateModel("bad model") == null);
        assertFalse(AiValidators.validateApiKey("short") == null);
        assertTrue(AiValidators.validateApiKey("abcdefghijklmnop") == null);
    }
}
