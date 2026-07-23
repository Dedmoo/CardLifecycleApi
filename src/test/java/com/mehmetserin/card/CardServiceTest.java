package com.mehmetserin.card;

import com.mehmetserin.card.model.Card;
import com.mehmetserin.card.model.CardModels.CardStatus;
import com.mehmetserin.card.model.CardModels.AuthorizationResultCode;
import com.mehmetserin.card.model.CardModels.CardView;
import com.mehmetserin.card.repository.AuthorizationIdempotencyRepository;
import com.mehmetserin.card.repository.AuthorizationLedgerRepository;
import com.mehmetserin.card.repository.CardRepository;
import com.mehmetserin.card.service.CardService;
import com.mehmetserin.card.service.PanHasher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardServiceTest {

    private final Map<String, Card> store = new HashMap<>();
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final AuthorizationLedgerRepository ledgerRepository = mock(AuthorizationLedgerRepository.class);
    private final AuthorizationIdempotencyRepository idempotencyRepository = mock(AuthorizationIdempotencyRepository.class);
    private final PanHasher panHasher = new PanHasher("unit-test-pepper");
    private final CardService service = new CardService(
            cardRepository, ledgerRepository, idempotencyRepository, panHasher);

    {
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            store.put(card.getCardId(), card);
            return card;
        });
        when(cardRepository.findById(any(String.class))).thenAnswer(invocation ->
                Optional.ofNullable(store.get(invocation.getArgument(0, String.class))));
        when(cardRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(store.values()));
    }

    @Test
    void issue_generatesMaskedActiveCard() {
        CardView card = service.issue("Test User", new BigDecimal("10000"));
        assertEquals(CardStatus.ACTIVE, card.status());
        assertTrue(card.maskedPan().startsWith("**** **** **** "));
        assertEquals(new BigDecimal("10000"), card.dailyLimit());
    }

    @Test
    void issue_defaultLimit_whenNotProvided() {
        CardView view = service.issue("Default Limit User", null);
        assertEquals(new BigDecimal("5000"), view.dailyLimit());
    }

    @Test
    void blockAndUnblock_changesStatus() {
        CardView card = service.issue("Block User", new BigDecimal("5000"));
        assertEquals(CardStatus.BLOCKED, service.block(card.cardId()).status());
        assertEquals(CardStatus.ACTIVE, service.unblock(card.cardId()).status());
    }

    @Test
    void updateLimit_updatesValue() {
        CardView card = service.issue("Limit User", new BigDecimal("5000"));
        CardView updated = service.updateLimit(card.cardId(), new BigDecimal("12000"));
        assertEquals(new BigDecimal("12000"), updated.dailyLimit());
    }

    @Test
    void unknownCard_throws() {
        assertThrows(CardService.CardNotFoundException.class, () -> service.get("CARD-UNKNOWN"));
    }

    @Test
    void authorize_recordsSpendAndReducesAvailableDailyLimit() {
        CardView card = service.issue("Authorization User", new BigDecimal("100"));

        var authorization = service.authorize(card.cardId(), new BigDecimal("35"), "authorize-success");

        assertEquals(new BigDecimal("35"), authorization.spentToday());
        assertEquals(new BigDecimal("65"), authorization.availableDailyLimit());
        assertEquals(AuthorizationResultCode.APPROVED, authorization.resultCode());
    }

    @Test
    void authorize_rejectsBlockedCard() {
        CardView card = service.issue("Blocked Authorization User", new BigDecimal("100"));
        service.block(card.cardId());

        var authorization = service.authorize(card.cardId(), new BigDecimal("10"), "blocked-card");

        assertEquals(AuthorizationResultCode.CARD_BLOCKED, authorization.resultCode());
    }

    @Test
    void authorize_rejectsAmountOverRemainingDailyLimit() {
        CardView card = service.issue("Limit Authorization User", new BigDecimal("100"));
        service.authorize(card.cardId(), new BigDecimal("70"), "limit-first");

        var authorization = service.authorize(card.cardId(), new BigDecimal("31"), "limit-decline");

        assertEquals(AuthorizationResultCode.INSUFFICIENT_LIMIT, authorization.resultCode());
    }
}
