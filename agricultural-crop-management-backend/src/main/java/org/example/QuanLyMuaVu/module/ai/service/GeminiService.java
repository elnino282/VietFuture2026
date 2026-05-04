package org.example.QuanLyMuaVu.module.ai.service;


import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String API_VERSION = "v1beta";
    private static final String SYSTEM_PROMPT_RESOURCE = "prompts/system_prompt.txt";
    private static final String SYSTEM_PROMPT = loadSystemPrompt();
    private static final String[] API_KEY_ENV_KEYS = new String[] {
            "APP_AI_API_KEY",
            "GEMINI_API_KEY",
            "GOOGLE_API_KEY"
    };

    private static final String CONNECTION_FALLBACK_MESSAGE =
            "Hi\u1EC7n t\u1EA1i t\u00F4i kh\u00F4ng th\u1EC3 k\u1EBFt n\u1ED1i t\u1EDBi d\u1ECBch v\u1EE5 t\u01B0 v\u1EA5n n\u00F4ng nghi\u1EC7p. Vui l\u00F2ng th\u1EED l\u1EA1i sau.";
    private static final String EMPTY_RESPONSE_FALLBACK_MESSAGE =
            "Hi\u1EC7n t\u1EA1i t\u00F4i kh\u00F4ng th\u1EC3 t\u1EA1o \u0111\u01B0\u1EE3c c\u00E2u tr\u1EA3 l\u1EDDi. Vui l\u00F2ng th\u1EED l\u1EA1i sau ho\u1EB7c \u0111\u1EB7t c\u00E2u h\u1ECFi kh\u00E1c li\u00EAn quan \u0111\u1EBFn n\u00F4ng nghi\u1EC7p.";
    private static final String CROP_CONTEXT_LABEL =
            "Th\u00F4ng tin m\u00F9a v\u1EE5 hi\u1EC7n t\u1EA1i:\n";
    private static final String USER_QUESTION_LABEL =
            "C\u00E2u h\u1ECFi c\u1EE7a n\u00F4ng d\u00E2n: ";

    private final Client client;
    private final String model;
    private final String baseUrl;
    private final boolean apiKeyPresent;
    private final boolean aiEnabled;
    private final String apiKeySource;
    private final Environment environment;

    public GeminiService(AppProperties appProperties, Environment environment) {
        this.environment = environment;
        AppProperties.Ai aiProps = appProperties.getAi();

        Client.Builder builder = new Client.Builder();

        this.baseUrl = resolveBaseUrl(aiProps);
        if (this.baseUrl != null) {
            HttpOptions httpOptions = HttpOptions.builder()
                    .baseUrl(this.baseUrl)
                    .build();
            builder = builder.httpOptions(httpOptions);
        }

        ApiKeyResolution apiKeyResolution = resolveApiKey(aiProps);
        String apiKey = apiKeyResolution.value;
        this.apiKeySource = apiKeyResolution.source;
        this.apiKeyPresent = apiKey != null && !apiKey.isBlank();
        this.aiEnabled = this.apiKeyPresent;
        if (this.apiKeyPresent) {
            builder = builder.apiKey(apiKey);
        }

        this.client = builder.build();
        this.model = resolveModel(aiProps);
    }

    @PostConstruct
    void logConfiguration() {
        if (!apiKeyPresent) {
            if (isDevProfile()) {
                log.warn("Gemini API key missing; AI features are disabled in this profile. Set APP_AI_API_KEY, GEMINI_API_KEY, or GOOGLE_API_KEY.");
            } else {
                throw new IllegalStateException("Gemini API key is required in non-dev profiles. Set APP_AI_API_KEY, GEMINI_API_KEY, or GOOGLE_API_KEY.");
            }
        }

        String effectiveBaseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        log.info("Gemini configuration: baseUrl={}, model={}, apiVersion={}, apiKeyPresent={}, apiKeySource={}",
                effectiveBaseUrl, model, API_VERSION, apiKeyPresent, apiKeySource);
    }

    public String chatAsAgriculturalExpert(String userMessage, String cropContext) {
        Objects.requireNonNull(userMessage, "userMessage must not be null");
        if (userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }

        String requestId = UUID.randomUUID().toString();
        if (!aiEnabled) {
            log.warn("Gemini request skipped because AI is disabled (requestId={}).", requestId);
            return CONNECTION_FALLBACK_MESSAGE;
        }

        String prompt = buildPrompt(userMessage, cropContext);

        try {
            GenerateContentResponse response = client.models.generateContent(model, prompt, null);
            String text = response.text();
            if (text == null || text.isBlank()) {
                log.warn("Gemini response empty (requestId={}).", requestId);
                return fallbackMessage();
            }
            return text;
        } catch (ApiException ex) {
            logApiException(requestId, ex);
            return CONNECTION_FALLBACK_MESSAGE;
        } catch (GenAiIOException ex) {
            logIoException(requestId, ex);
            return CONNECTION_FALLBACK_MESSAGE;
        } catch (Exception ex) {
            logUnexpectedException(requestId, ex);
            return CONNECTION_FALLBACK_MESSAGE;
        }
    }

    private String buildPrompt(String userMessage, String cropContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");
        if (cropContext != null && !cropContext.isBlank()) {
            sb.append(CROP_CONTEXT_LABEL);
            sb.append(cropContext).append("\n\n");
        }
        sb.append(USER_QUESTION_LABEL);
        sb.append(userMessage);
        return sb.toString();
    }

    private String fallbackMessage() {
        return EMPTY_RESPONSE_FALLBACK_MESSAGE;
    }

    private String resolveBaseUrl(AppProperties.Ai aiProps) {
        if (aiProps == null || aiProps.getBaseUrl() == null || aiProps.getBaseUrl().isBlank()) {
            return null;
        }

        String baseUrlValue = aiProps.getBaseUrl().trim();
        if (baseUrlValue.endsWith("/")) {
            baseUrlValue = baseUrlValue.substring(0, baseUrlValue.length() - 1);
        }

        String versionSegment = "/" + API_VERSION;
        if (baseUrlValue.contains(versionSegment + versionSegment)) {
            throw new IllegalStateException("app.ai.base-url must not contain '" + versionSegment + versionSegment + "'. Use " + DEFAULT_BASE_URL + " instead.");
        }

        if (baseUrlValue.endsWith(versionSegment)) {
            log.warn("app.ai.base-url should not include '{}'; removing trailing version segment.", versionSegment);
            baseUrlValue = baseUrlValue.substring(0, baseUrlValue.length() - versionSegment.length());
        }

        if (baseUrlValue.contains(versionSegment + "/")) {
            log.warn("app.ai.base-url contains '{}' segment; removing to avoid double versioning.", versionSegment);
            baseUrlValue = baseUrlValue.replace(versionSegment + "/", "/");
        }

        if (baseUrlValue.contains(versionSegment + versionSegment)) {
            throw new IllegalStateException("app.ai.base-url normalized into an invalid path. Use " + DEFAULT_BASE_URL + " instead.");
        }

        return DEFAULT_BASE_URL.equals(baseUrlValue) ? null : baseUrlValue;
    }

    private ApiKeyResolution resolveApiKey(AppProperties.Ai aiProps) {
        if (aiProps != null && aiProps.getApiKey() != null && !aiProps.getApiKey().isBlank()) {
            return new ApiKeyResolution(aiProps.getApiKey().trim(), "app.ai.api-key");
        }

        String geminiApiKey = environment.getProperty("GEMINI_API_KEY");
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return new ApiKeyResolution(geminiApiKey.trim(), "GEMINI_API_KEY");
        }

        String googleApiKey = environment.getProperty("GOOGLE_API_KEY");
        if (googleApiKey != null && !googleApiKey.isBlank()) {
            return new ApiKeyResolution(googleApiKey.trim(), "GOOGLE_API_KEY");
        }

        if (isDevProfile()) {
            ApiKeyResolution dotenvResolution = resolveApiKeyFromDotenv();
            if (dotenvResolution != null) {
                return dotenvResolution;
            }
        }

        return new ApiKeyResolution(null, "missing");
    }

    private String resolveModel(AppProperties.Ai aiProps) {
        if (aiProps != null && aiProps.getModel() != null && !aiProps.getModel().isBlank()) {
            return aiProps.getModel().trim();
        }
        return DEFAULT_MODEL;
    }

    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev", "test", "local"));
    }

    private void logApiException(String requestId, ApiException ex) {
        int statusCode = ex.code();
        String status = ex.status();
        String errorMessage = ex.message();

        String guidance = switch (statusCode) {
            case 401, 403 -> "Invalid or missing API key.";
            case 404 -> "Endpoint not found; check base URL configuration.";
            case 429 -> "Quota or rate limit exceeded.";
            default -> statusCode >= 500 ? "Gemini service error." : "Unexpected Gemini API error.";
        };

        log.warn("Gemini API error (requestId={}, exceptionType={}, statusCode={}, status={}, message={}, guidance={})",
                requestId, ex.getClass().getSimpleName(), statusCode, status, errorMessage, guidance);
    }

    private void logIoException(String requestId, GenAiIOException ex) {
        Throwable cause = ex.getCause();
        String causeSummary = (cause != null)
                ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                : "n/a";

        log.warn("Gemini IO error (requestId={}, exceptionType={}, cause={})",
                requestId, ex.getClass().getSimpleName(), causeSummary);
    }

    private void logUnexpectedException(String requestId, Exception ex) {
        log.warn("Gemini unexpected error (requestId={}, exceptionType={}, exception={})",
                requestId, ex.getClass().getSimpleName(), ex.toString(), ex);
    }

    private static String loadSystemPrompt() {
        try (InputStream input = GeminiService.class.getClassLoader().getResourceAsStream(SYSTEM_PROMPT_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + SYSTEM_PROMPT_RESOURCE);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load system prompt resource: " + SYSTEM_PROMPT_RESOURCE, ex);
        }
    }

    private ApiKeyResolution resolveApiKeyFromDotenv() {
        for (Path dotenvPath : resolveDotenvPaths()) {
            ApiKeyResolution resolution = parseDotenvForApiKey(dotenvPath);
            if (resolution != null && resolution.value != null && !resolution.value.isBlank()) {
                return resolution;
            }
        }
        return null;
    }

    private Path[] resolveDotenvPaths() {
        String userDir = System.getProperty("user.dir");
        return new Path[] {
                Path.of(userDir, ".env"),
                Path.of(userDir, "agricultural-crop-management-backend", ".env")
        };
    }

    private ApiKeyResolution parseDotenvForApiKey(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }

        Map<String, String> values = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }
                int eqIndex = trimmed.indexOf('=');
                if (eqIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eqIndex).trim();
                String value = trimmed.substring(eqIndex + 1).trim();
                if (value.isEmpty()) {
                    continue;
                }
                values.put(key, stripOptionalQuotes(value));
            }
        } catch (IOException ex) {
            log.warn("Failed to read .env file at {} ({}).", path, ex.toString());
            return null;
        }

        for (String key : API_KEY_ENV_KEYS) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return new ApiKeyResolution(value.trim(), "dotenv:" + path);
            }
        }
        return null;
    }

    private String stripOptionalQuotes(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static final class ApiKeyResolution {
        private final String value;
        private final String source;

        private ApiKeyResolution(String value, String source) {
            this.value = value;
            this.source = source;
        }
    }
}
