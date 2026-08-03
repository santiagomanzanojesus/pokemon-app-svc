package com.pokemon.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PokemonDTO {
    private Integer id;
    private String name;
    private String spriteUrl;
    private Set<String> types;
    private Boolean favorite;
}