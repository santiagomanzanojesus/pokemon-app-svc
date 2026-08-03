package com.pokemon.app.service;

import com.pokemon.app.dto.PokemonDTO;
import com.pokemon.app.dto.PokemonDetailDTO;
import com.pokemon.app.entity.Pokemon;
import com.pokemon.app.entity.PokemonDetail;
import com.pokemon.app.entity.PokemonResponse;
import com.pokemon.app.repository.PokemonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PokemonServiceImpl implements PokemonService {

    private final PokemonRepository pokemonRepository;
    private final PokeApiClient pokeApiClient;

    @Override
    @Cacheable(value = "pokemonList", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PokemonDTO> getAllPokemons(Pageable pageable) {
        Page<Pokemon> pokemonPage = pokemonRepository.findAll(pageable);

        if (pokemonPage.isEmpty() && pageable.getPageNumber() == 0) {
            syncPokemonData();
            pokemonPage = pokemonRepository.findAll(pageable);
        }

        return pokemonPage.map(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "pokemonSearch", key = "#searchTerm + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PokemonDTO> searchPokemons(String searchTerm, Pageable pageable) {
        log.debug("Searching this pokemon: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllPokemons(pageable);
        }

        Page<Pokemon> pokemonPage = pokemonRepository.searchByNameOrId(searchTerm.trim(), pageable);

        if (pokemonPage.isEmpty() && searchTerm.trim().matches("\\d+")) {
            try {
                Integer pokemonId = Integer.parseInt(searchTerm.trim());
                Optional<Pokemon> existingPokemon = pokemonRepository.findByPokemonId(pokemonId);
                if (existingPokemon.isEmpty()) {
                    // Not in DB, get from the API and save it locally
                    PokemonDetail detail = pokeApiClient.getPokemonDetail(searchTerm);
                    if (detail != null) {
                        savePokemonDetail(detail);
                        pokemonPage = pokemonRepository.searchByNameOrId(searchTerm.trim(), pageable);
                    }
                }
            } catch (Exception e) {
                log.warn("Pokemon not found for this: {}", searchTerm, e);
            }
        }

        return pokemonPage.map(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "pokemonDetail", key = "#idOrName", unless = "#result == null")
    public PokemonDetailDTO getPokemonDetail(String idOrName) {
        log.debug("Getting details for: {}", idOrName);
        idOrName = String.valueOf(idOrName);

            // We will try to find the pokemon by ID or name on DB firstly
        Optional<Pokemon> pokemonOpt = idOrName.matches("\\d+")
                    ? pokemonRepository.findByPokemonId(Integer.parseInt(idOrName))
                    : pokemonRepository.findByNameIgnoreCase(idOrName);
        Pokemon pokemon = pokemonOpt.orElse(null);

        // If this is not in the current DB it will try to download from the pokemon API
        if (pokemon == null) {
            log.info("Pokemon not found: {}", idOrName);
            PokemonDetail detail = pokeApiClient.getPokemonDetail(idOrName);
            if (detail == null) {
                throw new RuntimeException("Pokemon not found: " + idOrName);
            }
            pokemon = savePokemonDetail(detail);
        }

        return convertToDetailDTO(pokemon);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "pokemonDetail", key = "#pokemonId"),
                    @CacheEvict(value = "pokemonSearch", allEntries = true)
            },
            put = @CachePut(value = "pokemonDetail", key = "#pokemonId")
    )
    @Transactional
    public PokemonDetailDTO refreshPokemonDetail(Integer pokemonId) {
        log.info("Refreshing Pokemon from API: {}", pokemonId);
        PokemonDetail detail = pokeApiClient.getPokemonDetail(String.valueOf(pokemonId));
        if (detail == null) {
            throw new RuntimeException("Pokemon no encontrado: " + pokemonId);
        }
        Pokemon pokemon = savePokemonDetail(detail);
        return convertToDetailDTO(pokemon);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "pokemonDetail", key = "#pokemonId"),
            @CacheEvict(value = "pokemonSearch", allEntries = true),
            @CacheEvict(value = "pokemonList", allEntries = true),
            @CacheEvict(value = "pokemonFavorites", allEntries = true)
    })
    @Transactional
    public PokemonDTO toggleFavorite(Integer pokemonId) {
        log.info("Toggling favorite for Pokemon ID: {}", pokemonId);

        Pokemon pokemon = pokemonRepository.findByPokemonId(pokemonId)
                .orElseThrow(() -> new RuntimeException("Pokemon not found: " + pokemonId));

        pokemon.setFavorite(!pokemon.isFavorite());
        pokemon = pokemonRepository.save(pokemon);

        return convertToDTO(pokemon);
    }

    /**
     * Get favorite Pokemon
     * Cache: pokemonFavorites (key = page + '-' + size)
     */
    @Override
    @Cacheable(value = "pokemonFavorites", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PokemonDTO> getFavorites(Pageable pageable) {
        return pokemonRepository.findByFavoriteTrue(Pageable.unpaged())
                .map(this::convertToDTO);
    }

    @Override
    public long getTotalPokemons() {
        return pokemonRepository.count();
    }

    /**
     * Sync data from PokeAPI (scheduled)
     * Cache: Clear all caches after sync
     */
    @Override
    @CacheEvict(value = {"pokemonList", "pokemonSearch", "pokemonDetail", "pokemonFavorites"}, allEntries = true)
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void syncPokemonData() {
        log.info("Starting data synchronization with PokeAPI");
        try {
            PokemonResponse response = pokeApiClient.getPokemonList(1, 100000);
            List<Integer> apiPokemonIds = response.getResults().stream()
                    .map(PokemonResponse.Result::getId)
                    .filter(Objects::nonNull)
                    .toList();

            Set<Integer> existingIds = pokemonRepository.findAll().stream()
                    .map(Pokemon::getPokemonId)
                    .collect(Collectors.toSet());

            int batchSize = 50;
            List<Integer> idsToFetch = apiPokemonIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .limit(1000)
                    .toList();

            log.info("{} new Pokemon to synchronize", idsToFetch.size());

            for (int i = 0; i < idsToFetch.size(); i += batchSize) {
                int end = Math.min(i + batchSize, idsToFetch.size());
                List<Integer> batch = idsToFetch.subList(i, end);

                batch.parallelStream().forEach(id -> {
                    try {
                        PokemonDetail detail = pokeApiClient.getPokemonDetail(String.valueOf(id));
                        if (detail != null) {
                            savePokemonDetail(detail);
                        }
                    } catch (Exception e) {
                        log.error("Error saving Pokemon ID: {}", id, e);
                    }
                });

                log.info("Synced {} of {} Pokemon", end, idsToFetch.size());
            }

            log.info("Sync completed. Total Pokemon in DB: {}", pokemonRepository.count());
        } catch (Exception e) {
            log.error("Error in data sync", e);
        }
    }

    @Transactional
    protected Pokemon savePokemonDetail(PokemonDetail detail) {
        // Validate if it exists already
        Optional<Pokemon> existingPokemon = pokemonRepository.findByPokemonId(detail.getId());

        Pokemon pokemon = existingPokemon.orElse(new Pokemon());

        pokemon.setPokemonId(detail.getId());
        pokemon.setName(detail.getName());
        pokemon.setHeight(detail.getHeight());
        pokemon.setWeight(detail.getWeight());
        pokemon.setBaseExperience(detail.getBaseExperience());

        // Sprites
        if (detail.getSprites() != null) {
            pokemon.setSpriteUrl(detail.getSprites().getFrontDefault());
            if (detail.getSprites().getOther() != null &&
                    detail.getSprites().getOther().getOfficialArtwork() != null) {
                pokemon.setOfficialArtworkUrl(
                        detail.getSprites().getOther().getOfficialArtwork().getFrontDefault()
                );
            }
        }

        // Types
        if (detail.getTypes() != null) {
            Set<String> types = detail.getTypes().stream()
                    .map(type -> type.getType().getName())
                    .collect(Collectors.toSet());
            pokemon.setTypes(types);
        }

        // Abilities
        if (detail.getAbilities() != null) {
            Set<String> abilities = detail.getAbilities().stream()
                    .map(ability -> ability.getAbility().getName())
                    .collect(Collectors.toSet());
            pokemon.setAbilities(abilities);
        }

        // Stats
        if (detail.getStats() != null) {
            Set<String> stats = detail.getStats().stream()
                    .map(stat -> stat.getStat().getName() + ":" + stat.getBaseStat())
                    .collect(Collectors.toSet());
            pokemon.setStats(stats);
        }

        if (existingPokemon.isPresent() && existingPokemon.get().isFavorite() ) {
            pokemon.setFavorite(existingPokemon.get().isFavorite());
        } else {
            pokemon.setFavorite(false);
        }

        return pokemonRepository.save(pokemon);
    }

    protected PokemonDTO convertToDTO(Pokemon pokemon) {
        return PokemonDTO.builder()
                .id(pokemon.getPokemonId())
                .name(pokemon.getName())
                .spriteUrl(pokemon.getSpriteUrl())
                .types(pokemon.getTypes())
                .favorite(pokemon.isFavorite())
                .build();
    }

    protected PokemonDetailDTO convertToDetailDTO(Pokemon pokemon) {
        Map<String, Integer> stats = new HashMap<>();
        if (pokemon.getStats() != null) {
            pokemon.getStats().forEach(statStr -> {
                String[] parts = statStr.split(":");
                if (parts.length == 2) {
                    stats.put(parts[0], Integer.parseInt(parts[1]));
                }
            });
        }

        return PokemonDetailDTO.builder()
                .id(pokemon.getPokemonId())
                .name(pokemon.getName())
                .spriteUrl(pokemon.getSpriteUrl())
                .officialArtworkUrl(pokemon.getOfficialArtworkUrl())
                .height(pokemon.getHeight())
                .weight(pokemon.getWeight())
                .baseExperience(pokemon.getBaseExperience())
                .types(pokemon.getTypes())
                .abilities(pokemon.getAbilities())
                .stats(stats)
                .favorite(pokemon.isFavorite())
                .build();
    }
}