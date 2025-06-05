/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.genai;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import com.google.genai.types.HttpOptions;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLContext;

/**
 * Interface for an API client which issues HTTP requests to the GenAI APIs.
 */
abstract class ApiClient {

    private static final String SDK_VERSION = "1.3.0"; // x-version-update:google-genai:released
    private static final Logger logger = Logger.getLogger(ApiClient.class.getName());
    // For Google AI APIs
    final Optional<String> apiKey;
    CloseableHttpClient httpClient;
    HttpOptions httpOptions;

    /**
     * Constructs an ApiClient for Google AI APIs.
     */
    protected ApiClient(Optional<String> apiKey, Optional<HttpOptions> customHttpOptions) {
        checkNotNull(apiKey, "API Key cannot be null");
        checkNotNull(customHttpOptions, "customHttpOptions cannot be null");

        try {
            this.apiKey = Optional.of(apiKey.orElse(getApiKeyFromEnv()));
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(
                    "API key must either be provided or set in the environment variable"
                            + " GOOGLE_API_KEY or GEMINI_API_KEY. If both are set, GOOGLE_API_KEY will be used.",
                    e);
        }

        this.httpOptions = defaultHttpOptions(Optional.empty());

        if (customHttpOptions.isPresent()) {
            this.httpOptions = mergeHttpOptions(customHttpOptions.get());
        }

        this.httpClient = createHttpClient(httpOptions.timeout());
    }

    /**
     * Returns the library version.
     */
    static String libraryVersion() {
        // TODO: Automate revisions to the SDK library version.
        String libraryLabel = String.format("google-genai-sdk/%s", SDK_VERSION);
        String languageLabel = "gl-java/" + System.getProperty("java.version");
        return libraryLabel + " " + languageLabel;
    }

    static HttpOptions defaultHttpOptions(Optional<String> location) {
        ImmutableMap.Builder<String, String> defaultHeaders = ImmutableMap.builder();
        defaultHeaders.put("Content-Type", "application/json");
        defaultHeaders.put("user-agent", libraryVersion());
        defaultHeaders.put("x-goog-api-client", libraryVersion());

        HttpOptions.Builder defaultHttpOptionsBuilder =
                HttpOptions.builder().headers(defaultHeaders.build());

        defaultHttpOptionsBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .apiVersion("v1beta");
        return defaultHttpOptionsBuilder.build();
    }

    private String getApiKeyFromEnv() {
        String googleApiKey = System.getenv("GOOGLE_API_KEY");
        if (googleApiKey != null && googleApiKey.isEmpty()) {
            googleApiKey = null;
        }
        String geminiApiKey = System.getenv("GEMINI_API_KEY");
        if (geminiApiKey != null && geminiApiKey.isEmpty()) {
            geminiApiKey = null;
        }
        if (googleApiKey != null && geminiApiKey != null) {
            logger.warning(
                    "Both GOOGLE_API_KEY and GEMINI_API_KEY are set. Using GOOGLE_API_KEY.");
        }
        if (googleApiKey != null) {
            return googleApiKey;
        }
        return geminiApiKey;
    }

    private CloseableHttpClient createHttpClient(Optional<Integer> timeout) {
        try {
            HttpClientBuilder builder = HttpClients.custom();

            // Configure timeout if provided
            if (timeout.isPresent()) {
                RequestConfig config = RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(timeout.get()))
                        .setResponseTimeout(Timeout.ofMilliseconds(timeout.get()))
                        .build();
                builder.setDefaultRequestConfig(config);
            }

            // Create custom SSL context to avoid Android's outdated SSLConnectionSocketFactory
            SSLContext sslContext = createAndroidCompatibleSSLContext();

            // Build SSL socket factory with custom configuration
            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .build();

            builder.setConnectionManager(
                    org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(sslSocketFactory)
                            .build()
            );

            return builder.build();

        } catch (Exception e) {
            // Fallback to minimal configuration that avoids SSL factory initialization
            return createMinimalHttpClient(timeout);
        }
    }

    /**
     * Creates SSL context that works with Android
     */
    private SSLContext createAndroidCompatibleSSLContext() throws Exception {
        return SSLContextBuilder.create()
                .setProtocol("TLS")
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();
    }

    /**
     * Fallback method that creates minimal HTTP client avoiding SSL factory issues
     */
    private CloseableHttpClient createMinimalHttpClient(Optional<Integer> timeout) {
        try {
            HttpClientBuilder builder = HttpClientBuilder.create();

            // Only set timeout, avoid SSL configuration
            if (timeout.isPresent()) {
                RequestConfig config = RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(timeout.get()))
                        .setResponseTimeout(Timeout.ofMilliseconds(timeout.get()))
                        .build();
                builder.setDefaultRequestConfig(config);
            }

            // Use system default SSL context instead of creating custom one
            builder.setConnectionManager(
                    org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                            .useSystemProperties() // This uses Android's system SSL settings
                            .build()
            );

            return builder.build();

        } catch (Exception e) {
            // Ultimate fallback - just return default client
            return HttpClients.createDefault();
        }
    }

    /**
     * Sends a Http request given the http method, path, and request json string.
     */
    public abstract ApiResponse request(
            String httpMethod, String path, String requestJson, Optional<HttpOptions> httpOptions);

    /**
     * Sends a Http request given the http method, path, and request bytes.
     */
    public abstract ApiResponse request(
            String httpMethod, String path, byte[] requestBytes, Optional<HttpOptions> httpOptions);

    /**
     * Returns the API key for Google AI APIs.
     */
    public @Nullable String apiKey() {
        return apiKey.orElse(null);
    }

    /**
     * Returns the HttpClient for API calls.
     */
    CloseableHttpClient httpClient() {
        return httpClient;
    }

    private Optional<Map<String, String>> getTimeoutHeader(HttpOptions httpOptionsToApply) {
        if (httpOptionsToApply.timeout().isPresent()) {
            int timeoutInSeconds = (int) Math.ceil((double) httpOptionsToApply.timeout().get() / 1000.0);
            // TODO(b/329147724): Document the usage of X-Server-Timeout header.
            return Optional.of(ImmutableMap.of("X-Server-Timeout", Integer.toString(timeoutInSeconds)));
        }
        return Optional.empty();
    }

    /**
     * Merges the http options to the client's http options.
     *
     * @param httpOptionsToApply the http options to apply
     * @return the merged http options
     */
    HttpOptions mergeHttpOptions(HttpOptions httpOptionsToApply) {
        if (httpOptionsToApply == null) {
            return this.httpOptions;
        }
        HttpOptions.Builder mergedHttpOptionsBuilder = this.httpOptions.toBuilder();
        if (httpOptionsToApply.baseUrl().isPresent()) {
            mergedHttpOptionsBuilder.baseUrl(httpOptionsToApply.baseUrl().get());
        }
        if (httpOptionsToApply.apiVersion().isPresent()) {
            mergedHttpOptionsBuilder.apiVersion(httpOptionsToApply.apiVersion().get());
        }
        if (httpOptionsToApply.timeout().isPresent()) {
            mergedHttpOptionsBuilder.timeout(httpOptionsToApply.timeout().get());
        }
        if (httpOptionsToApply.headers().isPresent()) {
            Stream<Map.Entry<String, String>> headersStream =
                    Stream.concat(
                            Stream.concat(
                                    this.httpOptions.headers().orElse(ImmutableMap.of()).entrySet().stream(),
                                    getTimeoutHeader(httpOptionsToApply)
                                            .orElse(ImmutableMap.of())
                                            .entrySet()
                                            .stream()),
                            httpOptionsToApply.headers().orElse(ImmutableMap.of()).entrySet().stream());
            Map<String, String> mergedHeaders =
                    headersStream.collect(
                            toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (val1, val2) -> val2));
            mergedHttpOptionsBuilder.headers(mergedHeaders);
        }
        return mergedHttpOptionsBuilder.build();
    }
}



