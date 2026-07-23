package com.mehmetserin.card;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mehmetserin.card.service.PanHasher;

import java.util.List;
import java.util.Map;

@Component
public class LegacyPanCleanup implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PanHasher panHasher;

    public LegacyPanCleanup(JdbcTemplate jdbcTemplate, PanHasher panHasher) {
        this.jdbcTemplate = jdbcTemplate;
        this.panHasher = panHasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer legacyColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CARD' AND COLUMN_NAME = 'PAN'",
                Integer.class);
        if (legacyColumnCount != null && legacyColumnCount > 0) {
            List<Map<String, Object>> legacyCards = jdbcTemplate.queryForList(
                    "SELECT card_id, pan FROM card WHERE pan IS NOT NULL");
            for (Map<String, Object> legacyCard : legacyCards) {
                String pan = (String) legacyCard.get("PAN");
                jdbcTemplate.update("UPDATE card SET pan_last4 = ?, pan_hash = ? WHERE card_id = ?",
                        pan.substring(pan.length() - 4), panHasher.hash(pan), legacyCard.get("CARD_ID"));
            }
        }
        jdbcTemplate.execute("ALTER TABLE card DROP COLUMN IF EXISTS pan");
    }
}
