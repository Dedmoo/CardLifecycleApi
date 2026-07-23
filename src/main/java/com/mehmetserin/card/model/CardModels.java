package com.mehmetserin.card.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CardModels {

    public enum CardStatus {
        ACTIVE,
        BLOCKED
    }

    public enum AuthorizationResultCode {
        APPROVED,
        CARD_BLOCKED,
        INSUFFICIENT_LIMIT
    }

    public record IssueCardRequest(
            @NotBlank @Size(max = 100) String cardholderName,
            @Positive @Digits(integer = 12, fraction = 2) BigDecimal dailyLimit) {
    }

    public record CardView(
            String cardId,
            String cardholderName,
            String maskedPan,
            String expiry,
            CardStatus status,
            BigDecimal dailyLimit,
            BigDecimal spentToday,
            BigDecimal availableDailyLimit) {
    }

    public record ValidatePanRequest(@NotBlank @Size(max = 32) String pan) {
    }

    public record UpdateLimitRequest(
            @Positive @Digits(integer = 12, fraction = 2) BigDecimal dailyLimit) {
    }

    public record AuthorizeCardRequest(
            @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount) {
    }

    public record AuthorizationView(
            String cardId,
            BigDecimal amount,
            BigDecimal spentToday,
            BigDecimal availableDailyLimit,
            AuthorizationResultCode resultCode) {
    }

    public record AuthorizationHistoryView(
            BigDecimal amount,
            AuthorizationResultCode resultCode,
            BigDecimal remainingLimit,
            java.time.Instant timestamp) {
    }
}
