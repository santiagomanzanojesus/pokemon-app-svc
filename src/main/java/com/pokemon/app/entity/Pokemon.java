package com.pokemon.app.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@Entity
@Table/*(name = "pokemons",
        uniqueConstraints = @UniqueConstraint(columnNames = "pokemon_id"))*/
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pokemon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pokemon_id", nullable = false, unique = true)
    private Integer pokemonId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String spriteUrl;

    @Column(length = 2000)
    private String officialArtworkUrl;

    @Column
    private Integer height;

    @Column
    private Integer weight;

    @Column
    private Integer baseExperience;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pokemon_types",
            joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "type")
    private Set<String> types;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pokemon_abilities",
            joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "ability")
    private Set<String> abilities;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pokemon_stats",
            joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "stat")
    private Set<String> stats;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "favorite")
    private boolean favorite ;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}