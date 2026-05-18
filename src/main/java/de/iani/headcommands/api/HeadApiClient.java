package de.iani.headcommands.api;

import de.iani.headcommands.HeadCommandsConfig;
import de.iani.headcommands.model.ApiResponse;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCategory;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class HeadApiClient {
    private final HeadCommandsConfig config;
    private final HeadApiParser parser;
    private final HttpClient client;

    public HeadApiClient(HeadCommandsConfig config) {
        this.config = config;
        this.parser = new HeadApiParser();
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(config.connectTimeoutSeconds())).build();
    }

    public ApiResponse<HeadCategory> fetchCategories() throws HeadApiException {
        String json = send(config.apiBaseUri().resolve("/api/heads/categories").toString() + "?app_uuid=" + enc(config.appUuid()));
        return parser.parseCategories(json);
    }

    public ApiResponse<CachedHead> fetchHeads(int categoryId, int page) throws HeadApiException {
        StringBuilder url = new StringBuilder(config.apiBaseUri().resolve("/api/heads/custom-heads").toString());
        url.append("?app_uuid=").append(enc(config.appUuid()));
        url.append("&category_id=").append(categoryId);
        url.append("&id=true&uuid=true&published_at=true");
        if (page > 1) {
            url.append("&page=").append(page);
        }
        return parser.parseHeads(send(url.toString()), true);
    }

    private String send(String url) throws HeadApiException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
                .header("api-key", config.apiKey())
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new HeadApiException("Minecraft-Heads API returned HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (IOException e) {
            throw new HeadApiException("Could not call Minecraft-Heads API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HeadApiException("Interrupted while calling Minecraft-Heads API.", e);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
