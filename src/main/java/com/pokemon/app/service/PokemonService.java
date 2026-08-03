package com.pokemon.app.service;

import com.pokemon.app.dto.PokemonDTO;
import com.pokemon.app.dto.PokemonDetailDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PokemonService {
    Page<PokemonDTO> getAllPokemons(Pageable pageable);
    Page<PokemonDTO> searchPokemons(String searchTerm, Pageable pageable);
    PokemonDetailDTO getPokemonDetail(String idOrName);
    PokemonDetailDTO refreshPokemonDetail(Integer pokemonId);
    PokemonDTO toggleFavorite(Integer pokemonId);
    Page<PokemonDTO> getFavorites(Pageable pageable);
    long getTotalPokemons();
    void syncPokemonData();
}