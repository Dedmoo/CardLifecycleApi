package com.mehmetserin.card.web;

import com.mehmetserin.card.model.CardModels.CardView;
import com.mehmetserin.card.model.CardModels.IssueCardRequest;
import com.mehmetserin.card.model.CardModels.UpdateLimitRequest;
import com.mehmetserin.card.model.CardModels.ValidatePanRequest;
import com.mehmetserin.card.model.CardModels.AuthorizeCardRequest;
import com.mehmetserin.card.model.CardModels.AuthorizationView;
import com.mehmetserin.card.model.CardModels.AuthorizationHistoryView;
import com.mehmetserin.card.model.CardModels.AuthorizationResultCode;
import com.mehmetserin.card.service.CardService;
import com.mehmetserin.card.service.LuhnValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<CardView> issue(@Valid @RequestBody IssueCardRequest request) {
        CardView view = cardService.issue(request.cardholderName(), request.dailyLimit());
        return ResponseEntity.created(URI.create("/api/cards/" + view.cardId())).body(view);
    }

    @GetMapping
    public List<CardView> list() {
        return cardService.list();
    }

    @GetMapping("/{cardId}")
    public CardView get(@PathVariable String cardId) {
        return cardService.get(cardId);
    }

    @PostMapping("/{cardId}/block")
    public CardView block(@PathVariable String cardId) {
        return cardService.block(cardId);
    }

    @PostMapping("/{cardId}/unblock")
    public CardView unblock(@PathVariable String cardId) {
        return cardService.unblock(cardId);
    }

    @PostMapping("/{cardId}/limit")
    public CardView updateLimit(@PathVariable String cardId, @Valid @RequestBody UpdateLimitRequest request) {
        return cardService.updateLimit(cardId, request.dailyLimit());
    }

    @PostMapping("/{cardId}/authorize")
    public ResponseEntity<AuthorizationView> authorize(
            @PathVariable String cardId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AuthorizeCardRequest request) {
        AuthorizationView response = cardService.authorize(cardId, request.amount(), idempotencyKey);
        return response.resultCode() == AuthorizationResultCode.APPROVED
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(409).body(response);
    }

    @GetMapping("/{cardId}/authorizations")
    public List<AuthorizationHistoryView> history(@PathVariable String cardId) {
        return cardService.history(cardId);
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@Valid @RequestBody ValidatePanRequest request) {
        String digits = request.pan() == null ? "" : request.pan().replaceAll("\\D", "");
        boolean valid = LuhnValidator.isValid(request.pan());
        String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
        return Map.of("valid", valid, "panLast4", last4);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "CardLifecycleApi");
    }
}
