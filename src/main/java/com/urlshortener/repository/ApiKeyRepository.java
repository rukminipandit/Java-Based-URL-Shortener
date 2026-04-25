package com.urlshortener.repository;

import com.urlshortener.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ApiKey entities.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyValueAndActiveTrue(String keyValue);

    boolean existsByKeyValue(String keyValue);

    List<ApiKey> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE ApiKey k SET k.active = false WHERE k.id = :id")
    void revokeKey(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ApiKey k SET k.usageCount = k.usageCount + 1 WHERE k.keyValue = :keyValue")
    void incrementUsage(@Param("keyValue") String keyValue);
}
