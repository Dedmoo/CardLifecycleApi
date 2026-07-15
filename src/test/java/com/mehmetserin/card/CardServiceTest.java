package com.mehmetserin.card;

import com.mehmetserin.card.model.CardModels.CardStatus;
import com.mehmetserin.card.model.CardModels.CardView;
import com.mehmetserin.card.service.CardService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardServiceTest {

    private final CardService service = new CardService();

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
}
