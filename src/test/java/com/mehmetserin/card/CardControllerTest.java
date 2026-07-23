package com.mehmetserin.card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issueCard_returnsMaskedPan() throws Exception {
        String body = """
                { "cardholderName": "Api Card User", "dailyLimit": 8000 }
                """;

        mockMvc.perform(post("/api/cards")
                        .header("X-Api-Key", "local-development-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.maskedPan").exists());
    }

    @Test
    void validatePan_returnsResult() throws Exception {
        String body = """
                { "pan": "4111111111111111" }
                """;

        mockMvc.perform(post("/api/cards/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.panLast4").value("1111"))
                .andExpect(jsonPath("$.pan").doesNotExist());
    }

    @Test
    void authorizeCard_recordsSpendAndReturnsRemainingLimit() throws Exception {
        String cardId = issueCard("Authorization Api User", 100);

        mockMvc.perform(post("/api/cards/{cardId}/authorize", cardId)
                        .header("X-Api-Key", "local-development-api-key")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"amount\": 35 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(35))
                .andExpect(jsonPath("$.spentToday").value(35))
                .andExpect(jsonPath("$.availableDailyLimit").value(65))
                .andExpect(jsonPath("$.pan").doesNotExist());
    }

    @Test
    void authorizeCard_rejectsBlockedCard() throws Exception {
        String cardId = issueCard("Blocked Api User", 100);
        mockMvc.perform(post("/api/cards/{cardId}/block", cardId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/cards/{cardId}/block", cardId)
                        .header("X-Api-Key", "local-development-api-key"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cards/{cardId}/authorize", cardId)
                        .header("X-Api-Key", "local-development-api-key")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"amount\": 10 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("CARD_BLOCKED"));
    }

    @Test
    void authorizeCard_rejectsAmountOverDailyLimit() throws Exception {
        String cardId = issueCard("Over Limit Api User", 100);

        mockMvc.perform(post("/api/cards/{cardId}/authorize", cardId)
                        .header("X-Api-Key", "local-development-api-key")
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"amount\": 101 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("INSUFFICIENT_LIMIT"));
    }

    @Test
    void authorizeCard_replaysIdempotencyKeyAndRecordsOneLedgerRow() throws Exception {
        String cardId = issueCard("Idempotency Api User", 100);
        String request = "{ \"amount\": 35 }";

        mockMvc.perform(post("/api/cards/{cardId}/authorize", cardId)
                        .header("X-Api-Key", "local-development-api-key")
                        .header("Idempotency-Key", "replay-" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentToday").value(35));

        mockMvc.perform(post("/api/cards/{cardId}/authorize", cardId)
                        .header("X-Api-Key", "local-development-api-key")
                        .header("Idempotency-Key", "replay-" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentToday").value(35));

        mockMvc.perform(get("/api/cards/{cardId}/authorizations", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].resultCode").value("APPROVED"))
                .andExpect(jsonPath("$[0].remainingLimit").value(65));
    }

    @Test
    void authorizeCard_rejectsBlankIdempotencyKey() throws Exception {
        String cardId = issueCard("Blank Key Api User", 100);

        mockMvc.perform(post("/api/cards/{cardId}/authorize", cardId)
                        .header("X-Api-Key", "local-development-api-key")
                        .header("Idempotency-Key", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"amount\": 35 }"))
                .andExpect(status().isBadRequest());
    }

    private String issueCard(String cardholderName, int dailyLimit) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cards")
                        .header("X-Api-Key", "local-development-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "cardholderName": "%s", "dailyLimit": %d }
                                """.formatted(cardholderName, dailyLimit)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.cardId");
    }

    private String uniqueKey() {
        return UUID.randomUUID().toString();
    }
}
