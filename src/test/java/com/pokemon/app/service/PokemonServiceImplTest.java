package com.pokemon.app.service;
import com.pokemon.app.dto.PokemonDTO;
import com.pokemon.app.dto.PokemonDetailDTO;
import com.pokemon.app.entity.Pokemon;
import com.pokemon.app.entity.PokemonDetail;
import com.pokemon.app.entity.PokemonResponse;
import com.pokemon.app.repository.PokemonRepository;
import com.pokemon.app.service.PokeApiClient;
import com.pokemon.app.service.PokemonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokemonServiceImplTest {

    @Mock
    private PokemonRepository pokemonRepository;

    @Mock
    private PokeApiClient pokeApiClient;

    @InjectMocks
    private PokemonServiceImpl pokemonService;

    private Pokemon pikachu;
    private Pokemon charizard;
    private PokemonDetail pikachuDetail;

    @BeforeEach
    void setUp() {
        pikachu = createPokemon(25, "pikachu", Set.of("electric"), false);
        charizard = createPokemon(6, "charizard", Set.of("fire", "flying"), true);
        pikachuDetail = createPokemonDetail(25, "pikachu", Set.of("electric"));
    }

    private Pokemon createPokemon(Integer id, String name, Set<String> types, Boolean favorite) {
        return Pokemon.builder()
                .pokemonId(id)
                .name(name)
                .types(new HashSet<>(types))
                .favorite(favorite)
                .height(10)
                .weight(50)
                .baseExperience(100)
                .spriteUrl("http://example.com/" + name + ".png")
                .build();
    }

    private PokemonDetail createPokemonDetail(Integer id, String name, Set<String> types) {
        PokemonDetail detail = new PokemonDetail();
        detail.setId(id);
        detail.setName(name);
        detail.setHeight(10);
        detail.setWeight(50);
        detail.setBaseExperience(100);

        // Setup sprites
        PokemonDetail.Sprites sprites = new PokemonDetail.Sprites();
        sprites.setFrontDefault("http://example.com/" + name + ".png");
        detail.setSprites(sprites);

        // Setup types
        List<PokemonDetail.Type> typeList = new ArrayList<>();
        types.forEach(typeName -> {
            PokemonDetail.Type type = new PokemonDetail.Type();
            PokemonDetail.Type.TypeDetail typeDetail = new PokemonDetail.Type.TypeDetail();
            typeDetail.setName(typeName);
            type.setType(typeDetail);
            typeList.add(type);
        });
        detail.setTypes(typeList);

        return detail;
    }

    @Test
    void getAllPokemons_ShouldReturnPageOfPokemons() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<Pokemon> pokemonList = Arrays.asList(pikachu, charizard);
        Page<Pokemon> pokemonPage = new PageImpl<>(pokemonList, pageable, pokemonList.size());

        when(pokemonRepository.findAll(pageable)).thenReturn(pokemonPage);

        // When
        Page<PokemonDTO> result = pokemonService.getAllPokemons(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("pikachu");
        assertThat(result.getContent().get(1).getName()).isEqualTo("charizard");

        verify(pokemonRepository, times(1)).findAll(pageable);
    }

    @Test
    void getAllPokemons_WhenDatabaseEmpty_ShouldSyncData() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Pokemon> emptyPage = Page.empty();

        when(pokemonRepository.findAll(pageable))
                .thenReturn(emptyPage)
                .thenReturn(new PageImpl<>(Collections.singletonList(pikachu), pageable, 1));

        PokemonResponse response = new PokemonResponse();
        PokemonResponse.Result result = new PokemonResponse.Result();
        result.setName("pikachu");
        result.setUrl("https://pokeapi.co/api/v2/pokemon/25/");
        response.setResults(Collections.singletonList(result));

        when(pokeApiClient.getPokemonList(1, 100000)).thenReturn(response);
        when(pokeApiClient.getPokemonDetail("25")).thenReturn(pikachuDetail);
        when(pokemonRepository.save(any(Pokemon.class))).thenReturn(pikachu);

        // When
        Page<PokemonDTO> resultPage = pokemonService.getAllPokemons(pageable);

        // Then
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getContent()).hasSize(1);
        assertThat(resultPage.getContent().get(0).getName()).isEqualTo("pikachu");

        verify(pokemonRepository, times(2)).findAll(pageable);
        verify(pokeApiClient, times(1)).getPokemonList(1, 100000);
    }

    @Test
    void searchPokemons_ShouldReturnSearchResults_WhenSearchTermProvided() {
        // Given
        String searchTerm = "pika";
        Pageable pageable = PageRequest.of(0, 20);
        List<Pokemon> pokemonList = Collections.singletonList(pikachu);
        Page<Pokemon> pokemonPage = new PageImpl<>(pokemonList, pageable, pokemonList.size());

        when(pokemonRepository.searchByNameOrId(eq(searchTerm), eq(pageable)))
                .thenReturn(pokemonPage);

        // When
        Page<PokemonDTO> result = pokemonService.searchPokemons(searchTerm, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("pikachu");

        verify(pokemonRepository, times(1)).searchByNameOrId(searchTerm, pageable);
    }

    @Test
    void searchPokemons_WhenSearchTermIsNumericAndNotFound_ShouldFetchFromAPI() {
        // Given
        String searchTerm = "999";
        Pageable pageable = PageRequest.of(0, 20);
        Page<Pokemon> emptyPage = Page.empty();

        lenient().when(pokemonRepository.searchByNameOrId(eq(searchTerm), eq(pageable)))
                .thenReturn(emptyPage);
        lenient().when(pokeApiClient.getPokemonDetail(eq(searchTerm)))
                .thenReturn(pikachuDetail);
        lenient().when(pokemonRepository.save(any(Pokemon.class))).thenReturn(pikachu);

        // Mock the second search after saving
        Page<Pokemon> nonEmptyPage = new PageImpl<>(Collections.singletonList(pikachu), pageable, 1);
        lenient().when(pokemonRepository.searchByNameOrId(eq(searchTerm), eq(pageable)))
                .thenReturn(emptyPage)
                .thenReturn(nonEmptyPage);

        // When
        Page<PokemonDTO> result = pokemonService.searchPokemons(searchTerm, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("pikachu");

        verify(pokeApiClient, times(1)).getPokemonDetail(searchTerm);
        verify(pokemonRepository, times(2)).searchByNameOrId(searchTerm, pageable);
    }

    @Test
    void searchPokemons_WhenSearchTermEmpty_ShouldReturnAllPokemons() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<Pokemon> pokemonList = Arrays.asList(pikachu, charizard);
        Page<Pokemon> pokemonPage = new PageImpl<>(pokemonList, pageable, pokemonList.size());

        when(pokemonRepository.findAll(pageable)).thenReturn(pokemonPage);

        // When
        Page<PokemonDTO> result = pokemonService.searchPokemons("", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        verify(pokemonRepository, times(1)).findAll(pageable);
        verify(pokemonRepository, never()).searchByNameOrId(anyString(), any(Pageable.class));
    }

    @Test
    void getPokemonDetail_ShouldReturnPokemonDetail_WhenInDatabase() {
        // Given
        String id = "25";
        when(pokemonRepository.findByPokemonId(25)).thenReturn(Optional.of(pikachu));

        // When
        PokemonDetailDTO result = pokemonService.getPokemonDetail(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(25);
        assertThat(result.getName()).isEqualTo("pikachu");

        verify(pokemonRepository, times(1)).findByPokemonId(25);
        verify(pokeApiClient, never()).getPokemonDetail(anyString());
    }

    @Test
    void getPokemonDetail_WhenNotInDatabase_ShouldFetchFromAPI() {
        // Given
        String id = "25";
        when(pokemonRepository.findByPokemonId(25)).thenReturn(Optional.empty());
        when(pokeApiClient.getPokemonDetail(id)).thenReturn(pikachuDetail);
        when(pokemonRepository.save(any(Pokemon.class))).thenReturn(pikachu);

        // When
        PokemonDetailDTO result = pokemonService.getPokemonDetail(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(25);
        assertThat(result.getName()).isEqualTo("pikachu");

        verify(pokeApiClient, times(1)).getPokemonDetail(id);
        verify(pokemonRepository, times(1)).save(any(Pokemon.class));
    }

    @Test
    void getPokemonDetail_WhenNotFound_ShouldThrowException() {
        // Given
        String id = "999";
        when(pokemonRepository.findByPokemonId(999)).thenReturn(Optional.empty());
        when(pokeApiClient.getPokemonDetail(id)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> pokemonService.getPokemonDetail(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pokemon not found");
    }

    @Test
    void getPokemonDetail_ByName_ShouldReturnPokemonDetail_WhenInDatabase() {
        // Given
        String name = "pikachu";
        when(pokemonRepository.findByNameIgnoreCase(name)).thenReturn(Optional.of(pikachu));

        // When
        PokemonDetailDTO result = pokemonService.getPokemonDetail(name);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("pikachu");

        verify(pokemonRepository, times(1)).findByNameIgnoreCase(name);
    }

    @Test
    void toggleFavorite_ShouldToggleFavoriteStatus() {
        // Given
        Integer pokemonId = 25;
        Pokemon pokemon = createPokemon(25, "pikachu", Set.of("electric"), false);

        when(pokemonRepository.findByPokemonId(pokemonId)).thenReturn(Optional.of(pokemon));
        when(pokemonRepository.save(any(Pokemon.class))).thenAnswer(invocation -> {
            Pokemon p = invocation.getArgument(0);
            p.setFavorite(true);
            return p;
        });

        // When
        PokemonDTO result = pokemonService.toggleFavorite(pokemonId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFavorite()).isTrue();

        verify(pokemonRepository, times(1)).findByPokemonId(pokemonId);
        verify(pokemonRepository, times(1)).save(any(Pokemon.class));
    }

    @Test
    void toggleFavorite_WhenPokemonNotFound_ShouldThrowException() {
        // Given
        Integer pokemonId = 999;
        when(pokemonRepository.findByPokemonId(pokemonId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pokemonService.toggleFavorite(pokemonId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pokemon not found");
    }

    @Test
    void getFavorites_ShouldReturnFavoritePokemons() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<Pokemon> favorites = Collections.singletonList(charizard);
        Page<Pokemon> favoritesPage = new PageImpl<>(favorites, pageable, favorites.size());

        when(pokemonRepository.findByFavoriteTrue(Pageable.unpaged()))
                .thenReturn(favoritesPage);

        // When
        Page<PokemonDTO> result = pokemonService.getFavorites(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("charizard");
        assertThat(result.getContent().get(0).getFavorite()).isTrue();

        verify(pokemonRepository, times(1)).findByFavoriteTrue(Pageable.unpaged());
    }

    @Test
    void getTotalPokemons_ShouldReturnCount() {
        // Given
        long expectedCount = 10;
        when(pokemonRepository.count()).thenReturn(expectedCount);

        // When
        long result = pokemonService.getTotalPokemons();

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(pokemonRepository, times(1)).count();
    }

    @Test
    void refreshPokemonDetail_ShouldUpdatePokemonAndReturnDetails() {
        // Given
        Integer pokemonId = 25;
        Pokemon existingPokemon = createPokemon(25, "pikachu", Set.of("electric"), false);
        Pokemon updatedPokemon = createPokemon(25, "pikachu", Set.of("electric", "ground"), false);

        when(pokeApiClient.getPokemonDetail(String.valueOf(pokemonId)))
                .thenReturn(pikachuDetail);
        when(pokemonRepository.findByPokemonId(pokemonId))
                .thenReturn(Optional.of(existingPokemon));
        when(pokemonRepository.save(any(Pokemon.class)))
                .thenReturn(updatedPokemon);

        // When
        PokemonDetailDTO result = pokemonService.refreshPokemonDetail(pokemonId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(pokemonId);

        verify(pokeApiClient, times(1)).getPokemonDetail(String.valueOf(pokemonId));
        verify(pokemonRepository, times(1)).findByPokemonId(pokemonId);
        verify(pokemonRepository, times(1)).save(any(Pokemon.class));
    }
}