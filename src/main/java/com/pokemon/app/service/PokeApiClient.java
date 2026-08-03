package com.pokemon.app.service;

import com.pokemon.app.entity.PokemonDetail;
import com.pokemon.app.entity.PokemonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@Slf4j
public class PokeApiClient {

    private final WebClient webClient;
    private final int maxRetries;

    public PokeApiClient(
            @Value("${pokeapi.base-url}") String baseUrl,
            @Value("${pokeapi.timeout}") int timeout,
            @Value("${pokeapi.max-retries}") int maxRetries) {

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "PokedexApp/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.maxRetries = maxRetries;
    }

    @Cacheable(value = "pokemonList", key = "#page + '-' + #limit")
    public PokemonResponse getPokemonList(int page, int limit) {
        int offset = (page - 1) * limit;

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/pokemon")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build())
                .retrieve()
                .bodyToMono(PokemonResponse.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .doBeforeRetry(retrySignal ->
                                log.warn("Reintentando llamada a PokéAPI - intento: {}", retrySignal.totalRetries() + 1)))
                .block();
    }

    @Cacheable(value = "pokemonDetail")
    public PokemonDetail getPokemonDetail(String idOrName) {
        if(idOrName.equals("pokemon")){
            idOrName = "";
        }
        try {
            String finalIdOrName = idOrName;
            return webClient.get()
                    .uri("/pokemon/{id}", idOrName.toLowerCase())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new RuntimeException("Pokemon not found"));
                        }
                        return response.bodyToMono(String.class)
                                .map(data -> new RuntimeException("Invalid data" + data));
                    })
                    .bodyToMono(PokemonDetail.class)
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Reintentando obtener detalle de {} - intento: {}",
                                            finalIdOrName, retrySignal.totalRetries() + 1)))
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Pokemon not found on the API: {}", idOrName);
            throw new RuntimeException("Pokemon not found: " + idOrName);
        } catch (Exception e) {
            log.error("Error on finding the Pokemon {}", idOrName, e);
            throw new RuntimeException("Error on finding the Pokemon ", e);
        }
    }

    /**
     * Method to lookup the Pokemon by name or ID chached
     * @param query
     * @return
     */
    @Cacheable(value = "pokemonSearch", key = "#query")
    public PokemonDetail searchPokemon(String query) {
        log.debug("Searching the pokemon: {}", query);
        return getPokemonDetail(query);
    }
}