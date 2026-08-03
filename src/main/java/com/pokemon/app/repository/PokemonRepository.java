package com.pokemon.app.repository;

import com.pokemon.app.entity.Pokemon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PokemonRepository extends JpaRepository<Pokemon, Long> {

    /**
     * Find a Pokemon by its numeric ID (from PokeAPI)
     * @param pokemonId The Pokemon ID in PokeAPI
     * @return Optional containing the Pokemon if found
     */
    Optional<Pokemon> findByPokemonId(Integer pokemonId);

    /**
     * Find a Pokemon by its name (case insensitive)
     * @param name The Pokemon name
     * @return Optional containing the Pokemon if found
     */
    Optional<Pokemon> findByNameIgnoreCase(String name);

    /**
     * Search Pokemon by name or ID (flexible search)
     * Allows searching by partial name or numeric ID
     * @param searchTerm The search term (name or ID)
     * @param pageable Pagination configuration
     * @return Page with the results
     */
    @Query("SELECT p FROM Pokemon p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "CAST(p.pokemonId AS string) LIKE CONCAT('%', :searchTerm, '%')")
    Page<Pokemon> searchByNameOrId(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Get all favorite Pokemon
     * @param pageable Pagination configuration
     * @return Page with the favorite Pokemon
     */
    Page<Pokemon> findByFavoriteTrue(Pageable pageable);

}