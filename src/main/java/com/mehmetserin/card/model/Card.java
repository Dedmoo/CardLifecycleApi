package com.mehmetserin.card.model;

import com.mehmetserin.card.model.CardModels.CardStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Card {

    @Id
    private String cardId;
    private String cardholderName;
    private String panLast4;
    private String panHash;
    private String expiry;

    @Enumerated(EnumType.STRING)
    private CardStatus status;
    private BigDecimal dailyLimit;
    private BigDecimal spentToday;
    private LocalDate spendingDate;

    @Version
    private Long version;

    protected Card() {
        // required by JPA
    }

    public Card(String cardId, String cardholderName, String panLast4, String panHash, String expiry,
                CardStatus status, BigDecimal dailyLimit) {
        this.cardId = cardId;
        this.cardholderName = cardholderName;
        this.panLast4 = panLast4;
        this.panHash = panHash;
        this.expiry = expiry;
        this.status = status;
        this.dailyLimit = dailyLimit;
        this.spentToday = BigDecimal.ZERO;
        this.spendingDate = LocalDate.now();
    }

    public String getCardId() {
        return cardId;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public String getPanLast4() {
        return panLast4;
    }

    public String getPanHash() {
        return panHash;
    }

    public String getExpiry() {
        return expiry;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public void resetSpendIfNewDay(LocalDate today) {
        if (spentToday == null || spendingDate == null || !today.equals(spendingDate)) {
            spentToday = BigDecimal.ZERO;
            spendingDate = today;
        }
    }

    public BigDecimal authorize(BigDecimal amount, LocalDate today) {
        resetSpendIfNewDay(today);
        BigDecimal available = dailyLimit.subtract(spentToday);
        spentToday = spentToday.add(amount);
        return dailyLimit.subtract(spentToday);
    }

    public BigDecimal getSpentToday(LocalDate today) {
        resetSpendIfNewDay(today);
        return spentToday;
    }

    public BigDecimal getAvailableDailyLimit(LocalDate today) {
        return dailyLimit.subtract(getSpentToday(today));
    }

    public String maskedPan() {
        return "**** **** **** " + panLast4;
    }
}
