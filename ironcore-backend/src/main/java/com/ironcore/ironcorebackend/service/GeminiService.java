package com.ironcore.ironcorebackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.base-url}")
    private String baseUrl;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> generateText(String prompt) {
        String url = String.format("%s/%s:generateContent?key=%s",
                baseUrl, model, apiKey);

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        var candidates = (java.util.List<?>) response.get("candidates");
                        var first = (Map<?, ?>) candidates.get(0);
                        var content = (Map<?, ?>) first.get("content");
                        var parts = (java.util.List<?>) content.get("parts");
                        var part0 = (Map<?, ?>) parts.get(0);
                        return part0.get("text").toString();
                    } catch (Exception e) {
                        return "Error parsing Gemini response";
                    }
                });
    }
}
