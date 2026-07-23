package com.mehmetserin.card.repository;

import com.mehmetserin.card.model.AuthorizationIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationIdempotencyRepository extends JpaRepository<AuthorizationIdempotency, String> {
}
