package com.pokemon.app.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokemonResponse {
    private Integer count;
    private String next;
    private String previous;
    private List<Result> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String name;
        private String url;

        public Integer getId() {
            if (url == null) return null;
            String[] parts = url.split("/");
            return Integer.valueOf(parts[parts.length - 1]);
        }
    }
}