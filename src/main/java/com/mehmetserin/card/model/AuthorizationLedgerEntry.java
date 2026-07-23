package com.mehmetserin.card.model;

import com.mehmetserin.card.model.CardModels.AuthorizationResultCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class AuthorizationLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String cardId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private AuthorizationResultCode resultCode;
    private BigDecimal remainingLimit;
    private Instant timestamp;

    protected AuthorizationLedgerEntry() {
    }

    public AuthorizationLedgerEntry(String cardId, BigDecimal amount, AuthorizationResultCode resultCode,
                                    BigDecimal remainingLimit, Instant timestamp) {
        this.cardId = cardId;
        this.amount = amount;
        this.resultCode = resultCode;
        this.remainingLimit = remainingLimit;
        this.timestamp = timestamp;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AuthorizationResultCode getResultCode() {
        return resultCode;
    }

    public BigDecimal getRemainingLimit() {
        return remainingLimit;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
