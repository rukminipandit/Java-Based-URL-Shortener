package com.urlshortener.service;

import com.urlshortener.model.ApiKey;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing API keys.
 */
public interface ApiKeyService {

    /**
     * Creates a new API key with the given label.
     */
    ApiKey createApiKey(String label);

    /**
     * Validates an API key and returns the entity if valid.
     */
    Optional<ApiKey> validateApiKey(String keyValue);

    /**
     * Revokes an API key by ID (sets active=false).
     */
    void revokeApiKey(Long id);

    /**
     * Lists all API keys.
     */
    List<ApiKey> listAllKeys();

    /**
     * Checks if the given key value is valid and active.
     */
    boolean isValidKey(String keyValue);
}
