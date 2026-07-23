package com.mehmetserin.card.model;

import com.mehmetserin.card.model.CardModels.AuthorizationResultCode;
import com.mehmetserin.card.model.CardModels.AuthorizationView;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class AuthorizationIdempotency {

    @Id
    private String idempotencyKey;
    private String cardId;
    private BigDecimal amount;
    private BigDecimal spentToday;
    private BigDecimal availableDailyLimit;

    @Enumerated(EnumType.STRING)
    private AuthorizationResultCode resultCode;

    protected AuthorizationIdempotency() {
    }

    public AuthorizationIdempotency(String idempotencyKey, String cardId, BigDecimal amount, BigDecimal spentToday,
                                    BigDecimal availableDailyLimit, AuthorizationResultCode resultCode) {
        this.idempotencyKey = idempotencyKey;
        this.cardId = cardId;
        this.amount = amount;
        this.spentToday = spentToday;
        this.availableDailyLimit = availableDailyLimit;
        this.resultCode = resultCode;
    }

    public String getCardId() {
        return cardId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AuthorizationView toView() {
        return new AuthorizationView(cardId, amount, spentToday, availableDailyLimit, resultCode);
    }
}
