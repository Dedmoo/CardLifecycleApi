package com.mehmetserin.card.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class PanHasher {

    private final String pepper;

    public PanHasher(@Value("${card.pan-pepper}") String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("card.pan-pepper must be configured.");
        }
        this.pepper = pepper;
    }

    public String hash(String pan) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((pan + pepper).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
