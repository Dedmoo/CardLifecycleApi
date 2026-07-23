package com.mehmetserin.card.service;

import com.mehmetserin.card.model.Card;
import com.mehmetserin.card.model.AuthorizationIdempotency;
import com.mehmetserin.card.model.AuthorizationLedgerEntry;
import com.mehmetserin.card.model.CardModels.CardStatus;
import com.mehmetserin.card.model.CardModels.CardView;
import com.mehmetserin.card.model.CardModels.AuthorizationView;
import com.mehmetserin.card.model.CardModels.AuthorizationHistoryView;
import com.mehmetserin.card.model.CardModels.AuthorizationResultCode;
import com.mehmetserin.card.repository.AuthorizationIdempotencyRepository;
import com.mehmetserin.card.repository.AuthorizationLedgerRepository;
import com.mehmetserin.card.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    // Publicly documented test IIN (not a live issuer BIN). Demo PANs only.
    private static final String BIN = "400000";
    private static final int PAN_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CardRepository cardRepository;
    private final AuthorizationLedgerRepository ledgerRepository;
    private final AuthorizationIdempotencyRepository idempotencyRepository;
    private final PanHasher panHasher;

    public CardService(CardRepository cardRepository, AuthorizationLedgerRepository ledgerRepository,
                       AuthorizationIdempotencyRepository idempotencyRepository, PanHasher panHasher) {
        this.cardRepository = cardRepository;
        this.ledgerRepository = ledgerRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.panHasher = panHasher;
    }

    @Transactional
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
                pan.substring(pan.length() - 4),
                panHasher.hash(pan),
                expiry,
                CardStatus.ACTIVE,
                limit);

        cardRepository.save(card);
        return toView(card);
    }

    @Transactional
    public CardView get(String cardId) {
        return toView(require(cardId));
    }

    @Transactional
    public List<CardView> list() {
        return cardRepository.findAll().stream().map(this::toView).toList();
    }

    @Transactional
    public CardView block(String cardId) {
        Card card = require(cardId);
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        return toView(card);
    }

    @Transactional
    public CardView unblock(String cardId) {
        Card card = require(cardId);
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
        return toView(card);
    }

    @Transactional
    public CardView updateLimit(String cardId, BigDecimal dailyLimit) {
        if (dailyLimit == null || dailyLimit.signum() <= 0) {
            throw new IllegalArgumentException("Daily limit must be positive.");
        }
        Card card = require(cardId);
        card.setDailyLimit(dailyLimit);
        cardRepository.save(card);
        return toView(card);
    }

    @Transactional
    public AuthorizationView authorize(String cardId, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Authorization amount must be positive.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required.");
        }
        AuthorizationIdempotency replay = idempotencyRepository.findById(idempotencyKey.trim()).orElse(null);
        if (replay != null) {
            if (!replay.getCardId().equals(cardId) || replay.getAmount().compareTo(amount) != 0) {
                throw new IllegalArgumentException("Idempotency-Key cannot be reused for a different request.");
            }
            return replay.toView();
        }
        Card card = require(cardId);
        LocalDate today = LocalDate.now();
        card.resetSpendIfNewDay(today);
        BigDecimal available = card.getAvailableDailyLimit(today);
        AuthorizationResultCode resultCode = card.getStatus() == CardStatus.BLOCKED
                ? AuthorizationResultCode.CARD_BLOCKED
                : amount.compareTo(available) > 0
                ? AuthorizationResultCode.INSUFFICIENT_LIMIT
                : AuthorizationResultCode.APPROVED;
        if (resultCode == AuthorizationResultCode.APPROVED) {
            available = card.authorize(amount, today);
        }
        cardRepository.save(card);
        AuthorizationView response = new AuthorizationView(
                card.getCardId(), amount, card.getSpentToday(today), available, resultCode);
        ledgerRepository.save(new AuthorizationLedgerEntry(
                card.getCardId(), amount, resultCode, available, Instant.now()));
        idempotencyRepository.save(new AuthorizationIdempotency(
                idempotencyKey.trim(), card.getCardId(), amount, response.spentToday(),
                response.availableDailyLimit(), resultCode));
        return response;
    }

    @Transactional
    public List<AuthorizationHistoryView> history(String cardId) {
        require(cardId);
        return ledgerRepository.findByCardIdOrderByTimestampAsc(cardId).stream()
                .map(entry -> new AuthorizationHistoryView(entry.getAmount(), entry.getResultCode(),
                        entry.getRemainingLimit(), entry.getTimestamp()))
                .toList();
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
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
    }

    private CardView toView(Card card) {
        return new CardView(
                card.getCardId(),
                card.getCardholderName(),
                card.maskedPan(),
                card.getExpiry(),
                card.getStatus(),
            card.getDailyLimit(),
            card.getSpentToday(LocalDate.now()),
            card.getAvailableDailyLimit(LocalDate.now()));
    }

    public static class CardNotFoundException extends RuntimeException {
        public CardNotFoundException(String cardId) {
            super("Card not found: " + cardId);
        }
    }
}
