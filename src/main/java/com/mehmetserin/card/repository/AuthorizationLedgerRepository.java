package com.mehmetserin.card.repository;

import com.mehmetserin.card.model.AuthorizationLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorizationLedgerRepository extends JpaRepository<AuthorizationLedgerEntry, Long> {

    List<AuthorizationLedgerEntry> findByCardIdOrderByTimestampAsc(String cardId);
}
