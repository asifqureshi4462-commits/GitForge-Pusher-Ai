package com.asifqureshi.gitforge.data.remote.ai;

import androidx.annotation.NonNull;

import com.asifqureshi.gitforge.domain.SecretRedactor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Sends one prompt to the configured provider and returns the provider's text answer. Nothing is invented: if the
 * provider returns an error or an answer without text, an {@link AiException} is thrown.
 *
 * <p>The API key is sent only in the provider's auth header and never logged. Request shapes follow each
 * provider's public chat API; they have not been exercised against live services in this project's tests.
 */
public final class AiClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient http;

    public AiClient(@NonNull OkHttpClient http) {
        this.http = http;
    }

    @NonNull
    public String complete(@NonNull AiProviderType type, @NonNull String baseUrl, @NonNull String model,
                           @NonNull String apiKey, @NonNull String system, @NonNull String user) throws AiException {
        Request request;
        switch (type) {
            case ANTHROPIC:
                request = anthropicRequest(baseUrl, model, apiKey, system, user);
                break;
            case GEMINI:
                request = geminiRequest(baseUrl, model, apiKey, system, user);
                break;
            default:
                request = openAiRequest(baseUrl, model, apiKey, system, user);
                break;
        }
        String body;
        int code;
        try (Response response = http.newCall(request).execute()) {
            code = response.code();
            ResponseBody rb = response.body();
            body = rb != null ? rb.string() : "";
        } catch (IOException e) {
            throw new AiException("Network error while contacting the AI provider. Check your connection and the base URL.");
        }
        if (code < 200 || code >= 300) {
            throw new AiException(describeFailure(code, body));
        }
        String text = parseText(type, body);
        if (text == null || text.trim().isEmpty()) {
            throw new AiException("The provider returned no text. Check the model name.");
        }
        return text;
    }

    // ------------------------------------------------------------ request builders

    private static Request openAiRequest(String baseUrl, String model, String key, String system, String user) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        JsonArray messages = new JsonArray();
        messages.add(message("system", system));
        messages.add(message("user", user));
        root.add("messages", messages);
        return new Request.Builder().url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + key)
                .post(RequestBody.create(root.toString(), JSON)).build();
    }

    private static Request anthropicRequest(String baseUrl, String model, String key, String system, String user) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("max_tokens", 4096);
        root.addProperty("system", system);
        JsonArray messages = new JsonArray();
        messages.add(message("user", user));
        root.add("messages", messages);
        return new Request.Builder().url(baseUrl + "/v1/messages")
                .header("x-api-key", key)
                .header("anthropic-version", "2023-06-01")
                .post(RequestBody.create(root.toString(), JSON)).build();
    }

    private static Request geminiRequest(String baseUrl, String model, String key, String system, String user) {
        String id = model.startsWith("models/") ? model.substring("models/".length()) : model;
        JsonObject root = new JsonObject();
        JsonObject sys = new JsonObject();
        sys.add("parts", textParts(system));
        root.add("systemInstruction", sys);
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", textParts(user));
        JsonArray contents = new JsonArray();
        contents.add(content);
        root.add("contents", contents);
        return new Request.Builder().url(baseUrl + "/v1beta/models/" + id + ":generateContent")
                .header("x-goog-api-key", key)
                .post(RequestBody.create(root.toString(), JSON)).build();
    }

    private static JsonObject message(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private static JsonArray textParts(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        return parts;
    }

    // ------------------------------------------------------------ response parsing

    static String parseText(AiProviderType type, String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            switch (type) {
                case ANTHROPIC: {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement el : root.getAsJsonArray("content")) {
                        JsonObject block = el.getAsJsonObject();
                        if (block.has("text")) {
                            sb.append(block.get("text").getAsString());
                        }
                    }
                    return sb.toString();
                }
                case GEMINI: {
                    JsonObject first = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement el : first.getAsJsonObject("content").getAsJsonArray("parts")) {
                        JsonObject part = el.getAsJsonObject();
                        if (part.has("text")) {
                            sb.append(part.get("text").getAsString());
                        }
                    }
                    return sb.toString();
                }
                default:
                    return root.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String describeFailure(int code, String body) {
        String detail = "";
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonElement err = root.get("error");
            if (err != null && err.isJsonObject() && err.getAsJsonObject().has("message")) {
                detail = err.getAsJsonObject().get("message").getAsString();
            } else if (err != null && err.isJsonPrimitive()) {
                detail = err.getAsString();
            }
        } catch (RuntimeException ignored) {
            // Not JSON: use the status code only.
        }
        detail = SecretRedactor.redact(detail);
        String suffix = detail.isEmpty() ? "" : " Provider says: " + detail;
        if (code == 401 || code == 403) {
            return "The provider rejected the API key (HTTP " + code + ")." + suffix;
        }
        if (code == 404) {
            return "The endpoint or model was not found (HTTP 404). Check the base URL and model name." + suffix;
        }
        if (code == 429) {
            return "The provider's rate limit or quota was reached (HTTP 429)." + suffix;
        }
        if (code >= 500) {
            return "The provider is having problems (HTTP " + code + "). Try again later.";
        }
        return "The provider returned HTTP " + code + "." + suffix;
    }
}
