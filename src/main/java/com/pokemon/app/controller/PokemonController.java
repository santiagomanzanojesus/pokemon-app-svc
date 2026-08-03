package com.pokemon.app.controller;
import com.pokemon.app.dto.PokemonDTO;
import com.pokemon.app.dto.PokemonDetailDTO;
import com.pokemon.app.service.PokemonService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pokemon")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PokemonController {

    private final PokemonService pokemonService;

    @GetMapping
    public ResponseEntity<Page<PokemonDTO>> getPokemons(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy));

        Page<PokemonDTO> result;
        if (search != null && !search.trim().isEmpty()) {
            result = pokemonService.searchPokemons(search.trim(), pageable);
        } else {
            result = pokemonService.getAllPokemons(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{idOrName}")
    public ResponseEntity<PokemonDetailDTO> getPokemonDetail(@PathVariable String idOrName) {
        PokemonDetailDTO detail = pokemonService.getPokemonDetail(idOrName);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<PokemonDetailDTO> refreshPokemon(@PathVariable Integer id) {
        PokemonDetailDTO detail = pokemonService.refreshPokemonDetail(id);
        return ResponseEntity.ok(detail);
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<PokemonDTO> toggleFavorite(@PathVariable Integer id) {
        PokemonDTO pokemon = pokemonService.toggleFavorite(id);
        return ResponseEntity.ok(pokemon);
    }

    @GetMapping("/favorites")
    public ResponseEntity<Page<PokemonDTO>> getFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        return ResponseEntity.ok(pokemonService.getFavorites(pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long total = pokemonService.getTotalPokemons();
        return ResponseEntity.ok(Map.of(
                "totalPokemons", total,
                "favorites", pokemonService.getFavorites(Pageable.unpaged()).getTotalElements()
        ));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncData() {
        pokemonService.syncPokemonData();
        return ResponseEntity.ok(Map.of(
                "message", "Sync initiated successfully",
                "status", "success"
        ));
    }
}
