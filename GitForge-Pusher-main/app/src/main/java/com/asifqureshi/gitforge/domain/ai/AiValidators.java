package com.asifqureshi.gitforge.domain.ai;

import java.net.URI;
import java.net.URISyntaxException;

/** Validation of user-supplied AI provider settings. Returns null when valid, else a user-facing message. */
public final class AiValidators {

    private AiValidators() {
    }

    /** Provider endpoints must be HTTPS, have a host, and carry no credentials, query or fragment. */
    public static String validateBaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "Enter the provider's base URL.";
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            return "This is not a valid URL.";
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return "Only https:// endpoints are allowed.";
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            return "The URL has no host name.";
        }
        if (uri.getUserInfo() != null) {
            return "Do not put credentials inside the URL.";
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return "The base URL must not contain a query or fragment.";
        }
        return null;
    }

    public static String normalizeBaseUrl(String url) {
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    public static String validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return "Enter a model name.";
        }
        String m = model.trim();
        if (m.length() > 120) {
            return "The model name is too long.";
        }
        for (int i = 0; i < m.length(); i++) {
            char c = m.charAt(i);
            if (c <= 0x20 || c >= 0x7f || c == '/' && i == 0 || c == '?' || c == '#') {
                return "The model name contains invalid characters.";
            }
        }
        return null;
    }

    public static String validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "Enter an API key.";
        }
        String k = key.trim();
        if (k.length() < 8 || k.length() > 512) {
            return "This does not look like an API key.";
        }
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            if (c <= 0x20 || c >= 0x7f) {
                return "An API key contains only visible characters and no spaces.";
            }
        }
        return null;
    }
}
