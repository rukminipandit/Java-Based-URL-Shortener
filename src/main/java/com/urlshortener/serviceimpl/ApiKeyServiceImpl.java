package com.urlshortener.serviceimpl;

import com.urlshortener.model.ApiKey;
import com.urlshortener.repository.ApiKeyRepository;
import com.urlshortener.service.ApiKeyService;
import com.urlshortener.util.ShortCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ApiKeyService.
 * Handles creation, validation, revocation, and listing of API keys.
 */
@Service
@Transactional
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyServiceImpl.class);

    private final ApiKeyRepository apiKeyRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository, ShortCodeGenerator shortCodeGenerator) {
        this.apiKeyRepository = apiKeyRepository;
        this.shortCodeGenerator = shortCodeGenerator;
    }

    @Override
    public ApiKey createApiKey(String label) {
        String keyValue;
        // Ensure uniqueness
        do {
            keyValue = shortCodeGenerator.generateApiKey();
        } while (apiKeyRepository.existsByKeyValue(keyValue));

        ApiKey apiKey = new ApiKey(label, keyValue);
        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("Created API key: {} ({})", saved.getMaskedKey(), label);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiKey> validateApiKey(String keyValue) {
        if (keyValue == null || keyValue.isBlank()) return Optional.empty();
        return apiKeyRepository.findByKeyValueAndActiveTrue(keyValue);
    }

    @Override
    public void revokeApiKey(Long id) {
        apiKeyRepository.revokeKey(id);
        log.info("Revoked API key ID={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> listAllKeys() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidKey(String keyValue) {
        return validateApiKey(keyValue).isPresent();
    }
}
