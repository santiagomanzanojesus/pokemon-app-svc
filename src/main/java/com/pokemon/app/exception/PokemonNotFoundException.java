package com.pokemon.app.exception;


public class PokemonNotFoundException extends RuntimeException {
    public PokemonNotFoundException(String message) {
        super(message);
    }

    public PokemonNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}