package com.pokemon.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokemonDetail {
    private Integer id;
    private String name;
    private Integer height;
    private Integer weight;
    @JsonProperty("base_experience")
    private Integer baseExperience;
    private List<Type> types;
    private List<Ability> abilities;
    private List<Stat> stats;
    private Sprites sprites;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Type {
        private TypeDetail type;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TypeDetail {
            private String name;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ability {
        private AbilityDetail ability;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AbilityDetail {
            private String name;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stat {
        private StatDetail stat;
        @JsonProperty("base_stat")
        private Integer baseStat;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StatDetail {
            private String name;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sprites {
        @JsonProperty("front_default")
        private String frontDefault;
        @JsonProperty("other")
        private OtherSprites other;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OtherSprites {
            @JsonProperty("official-artwork")
            private OfficialArtwork officialArtwork;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class OfficialArtwork {
                @JsonProperty("front_default")
                private String frontDefault;
            }
        }
    }
}
