package com.pokemon.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PokemonDetailDTO {
    private Integer id;
    private String name;
    private String spriteUrl;
    private String officialArtworkUrl;
    private Integer height;
    private Integer weight;
    private Integer baseExperience;
    private Set<String> types;
    private Set<String> abilities;
    private Map<String, Integer> stats;
    private Boolean favorite;
}
