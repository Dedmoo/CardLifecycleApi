package com.mehmetserin.card.service;

import com.mehmetserin.card.model.Card;
import com.mehmetserin.card.model.CardModels.CardStatus;
import com.mehmetserin.card.model.CardModels.CardView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CardService {

    // Publicly documented test IIN (not a live issuer BIN). Demo PANs only.
    private static final String BIN = "400000";
    private static final int PAN_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Card> cards = new ConcurrentHashMap<>();

    public CardView issue(String cardholderName, BigDecimal dailyLimit) {
        if (cardholderName == null || cardholderName.isBlank()) {
            throw new IllegalArgumentException("Cardholder name is required.");
        }
        BigDecimal limit = (dailyLimit == null || dailyLimit.signum() <= 0)
                ? new BigDecimal("5000")
                : dailyLimit;

        String pan = generatePan();
        String expiry = LocalDate.now().plusYears(4).format(DateTimeFormatter.ofPattern("MM/yy"));
        var card = new Card(
                "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                cardholderName.trim(),
                pan,
                expiry,
                CardStatus.ACTIVE,
                limit);

        cards.put(card.getCardId(), card);
        return toView(card);
    }

    public CardView get(String cardId) {
        return toView(require(cardId));
    }

    public List<CardView> list() {
        return cards.values().stream().map(this::toView).toList();
    }

    public CardView block(String cardId) {
        Card card = require(cardId);
        card.setStatus(CardStatus.BLOCKED);
        return toView(card);
    }

    public CardView unblock(String cardId) {
        Card card = require(cardId);
        card.setStatus(CardStatus.ACTIVE);
        return toView(card);
    }

    public CardView updateLimit(String cardId, BigDecimal dailyLimit) {
        if (dailyLimit == null || dailyLimit.signum() <= 0) {
            throw new IllegalArgumentException("Daily limit must be positive.");
        }
        Card card = require(cardId);
        card.setDailyLimit(dailyLimit);
        return toView(card);
    }

    private String generatePan() {
        var sb = new StringBuilder(BIN);
        while (sb.length() < PAN_LENGTH - 1) {
            sb.append(RANDOM.nextInt(10));
        }
        int check = LuhnValidator.checkDigit(sb.toString());
        sb.append(check);
        return sb.toString();
    }

    private Card require(String cardId) {
        Card card = cards.get(cardId);
        if (card == null) {
            throw new CardNotFoundException(cardId);
        }
        return card;
    }

    private CardView toView(Card card) {
        return new CardView(
                card.getCardId(),
                card.getCardholderName(),
                card.maskedPan(),
                card.getExpiry(),
                card.getStatus(),
                card.getDailyLimit());
    }

    public static class CardNotFoundException extends RuntimeException {
        public CardNotFoundException(String cardId) {
            super("Card not found: " + cardId);
        }
    }
}
