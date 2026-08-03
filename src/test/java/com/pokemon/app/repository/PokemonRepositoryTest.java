package com.pokemon.app.repository;

import com.pokemon.app.entity.Pokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PokemonRepositoryTest {

    @Autowired
    private PokemonRepository pokemonRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Pokemon pikachu;
    private Pokemon charizard;
    private Pokemon bulbasaur;

    @BeforeEach
    void setUp() {
        // Create test data
        pikachu = createPokemon(25, "pikachu", Set.of("electric"), false);
        charizard = createPokemon(6, "charizard", Set.of("fire", "flying"), true);
        bulbasaur = createPokemon(1, "bulbasaur", Set.of("grass", "poison"), false);

        entityManager.persist(pikachu);
        entityManager.persist(charizard);
        entityManager.persist(bulbasaur);
        entityManager.flush();
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

    @Test
    void findByPokemonId_ShouldReturnPokemon_WhenExists() {
        Optional<Pokemon> found = pokemonRepository.findByPokemonId(25);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("pikachu");
        assertThat(found.get().getTypes()).contains("electric");
    }

    @Test
    void findByPokemonId_ShouldReturnEmpty_WhenNotExists() {
        Optional<Pokemon> found = pokemonRepository.findByPokemonId(999);

        assertThat(found).isEmpty();
    }

    @Test
    void findByNameIgnoreCase_ShouldReturnPokemon_WhenExists() {
        Optional<Pokemon> found = pokemonRepository.findByNameIgnoreCase("PIKACHU");

        assertThat(found).isPresent();
        assertThat(found.get().getPokemonId()).isEqualTo(25);
    }

    @Test
    void searchByNameOrId_ShouldReturnResults_WhenSearchingByName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pokemon> results = pokemonRepository.searchByNameOrId("pika", pageable);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("pikachu");
    }

    @Test
    void searchByNameOrId_ShouldReturnResults_WhenSearchingById() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pokemon> results = pokemonRepository.searchByNameOrId("25", pageable);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("pikachu");
    }

    @Test
    void searchByNameOrId_ShouldReturnMultipleResults_WhenPartialMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pokemon> results = pokemonRepository.searchByNameOrId("char", pageable);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("charizard");
    }

    @Test
    void findByFavoriteTrue_ShouldReturnFavorites() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pokemon> favorites = pokemonRepository.findByFavoriteTrue(pageable);

        assertThat(favorites.getContent()).hasSize(1);
        assertThat(favorites.getContent().get(0).getName()).isEqualTo("charizard");
    }

}