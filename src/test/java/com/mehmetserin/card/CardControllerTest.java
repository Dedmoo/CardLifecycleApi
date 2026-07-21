package com.mehmetserin.card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
