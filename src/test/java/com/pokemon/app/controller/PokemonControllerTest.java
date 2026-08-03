package com.pokemon.app.controller;

import com.pokemon.app.dto.PokemonDTO;
import com.pokemon.app.dto.PokemonDetailDTO;
import com.pokemon.app.service.PokemonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PokemonController.class)
class PokemonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PokemonService pokemonService;

    private PokemonDTO pikachuDTO;
    private PokemonDTO charizardDTO;
    private PokemonDetailDTO pikachuDetailDTO;

    @BeforeEach
    void setUp() {
        pikachuDTO = PokemonDTO.builder()
                .id(25)
                .name("pikachu")
                .spriteUrl("http://example.com/pikachu.png")
                .types(Set.of("electric"))
                .favorite(false)
                .build();

        charizardDTO = PokemonDTO.builder()
                .id(6)
                .name("charizard")
                .spriteUrl("http://example.com/charizard.png")
                .types(Set.of("fire", "flying"))
                .favorite(true)
                .build();

        pikachuDetailDTO = PokemonDetailDTO.builder()
                .id(25)
                .name("pikachu")
                .spriteUrl("http://example.com/pikachu.png")
                .officialArtworkUrl("http://example.com/pikachu-artwork.png")
                .height(10)
                .weight(50)
                .baseExperience(100)
                .types(Set.of("electric"))
                .abilities(Set.of("static", "lightning-rod"))
                .stats(Map.of("hp", 35, "attack", 55, "defense", 40))
                .favorite(false)
                .build();
    }

    @Test
    void getPokemons_ShouldReturnPageOfPokemons() throws Exception {
        // Given
        List<PokemonDTO> pokemonList = Arrays.asList(pikachuDTO, charizardDTO);
        Page<PokemonDTO> page = new PageImpl<>(pokemonList, PageRequest.of(0, 20), pokemonList.size());

        when(pokemonService.getAllPokemons(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/pokemon")
                        .param("page", "1")
                        .param("limit", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("pikachu"))
                .andExpect(jsonPath("$.content[1].name").value("charizard"));
    }

    @Test
    void getPokemons_WithSearch_ShouldReturnSearchResults() throws Exception {
        // Given
        String searchTerm = "pika";
        List<PokemonDTO> pokemonList = Collections.singletonList(pikachuDTO);
        Page<PokemonDTO> page = new PageImpl<>(pokemonList, PageRequest.of(0, 20), pokemonList.size());

        when(pokemonService.searchPokemons(eq(searchTerm), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/pokemon")
                        .param("page", "1")
                        .param("limit", "20")
                        .param("search", searchTerm)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("pikachu"));
    }

    @Test
    void getPokemonDetail_ShouldReturnPokemonDetail() throws Exception {
        // Given
        String id = "25";
        when(pokemonService.getPokemonDetail(id)).thenReturn(pikachuDetailDTO);

        // When & Then
        mockMvc.perform(get("/api/pokemon/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"))
                .andExpect(jsonPath("$.types[0]").value("electric"))
                .andExpect(jsonPath("$.abilities", hasSize(2)));
    }

    @Test
    void getPokemonDetail_WhenNotFound_ShouldReturn404() throws Exception {
        // Given
        String id = "999";
        when(pokemonService.getPokemonDetail(id))
                .thenThrow(new RuntimeException("Pokémon no encontrado: " + id));

        // When & Then
        mockMvc.perform(get("/api/pokemon/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void refreshPokemon_ShouldReturnUpdatedPokemon() throws Exception {
        // Given
        Integer id = 25;
        when(pokemonService.refreshPokemonDetail(id)).thenReturn(pikachuDetailDTO);

        // When & Then
        mockMvc.perform(post("/api/pokemon/{id}/refresh", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.name").value("pikachu"));
    }

    @Test
    void toggleFavorite_ShouldReturnUpdatedPokemon() throws Exception {
        // Given
        Integer id = 25;
        PokemonDTO updatedPokemon = PokemonDTO.builder()
                .id(25)
                .name("pikachu")
                .favorite(true)
                .build();

        when(pokemonService.toggleFavorite(id)).thenReturn(updatedPokemon);

        // When & Then
        mockMvc.perform(patch("/api/pokemon/{id}/favorite", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void getFavorites_ShouldReturnFavorites() throws Exception {
        // Given
        List<PokemonDTO> favorites = Collections.singletonList(charizardDTO);
        Page<PokemonDTO> page = new PageImpl<>(favorites, PageRequest.of(0, 20), favorites.size());

        when(pokemonService.getFavorites(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/pokemon/favorites")
                        .param("page", "0")
                        .param("limit", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("charizard"))
                .andExpect(jsonPath("$.content[0].favorite").value(true));
    }

    @Test
    void getStats_ShouldReturnStats() throws Exception {
        // Given
        long totalPokemons = 100;
        long totalFavorites = 5;

        when(pokemonService.getTotalPokemons()).thenReturn(totalPokemons);
        when(pokemonService.getFavorites(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        // When & Then
        mockMvc.perform(get("/api/pokemon/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPokemons").value(totalPokemons));
    }

    @Test
    void syncData_ShouldReturnSuccessMessage() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/pokemon/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sync initiated successfully"))
                .andExpect(jsonPath("$.status").value("success"));
    }
}
