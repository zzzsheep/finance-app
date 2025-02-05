package project.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.model.PlaidItem;
import project.repository.PlaidItemRepository;
import project.service.TransactionSyncService;

@RestController
@RequestMapping("/api/plaid/webhook")
@RequiredArgsConstructor
@Slf4j
public class PlaidWebhookController {
    private final PlaidItemRepository plaidItemRepository;
    private final TransactionSyncService transactionSyncService;

    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received webhook: {}", payload.getWebhookType());

        try {
            switch (payload.getWebhookType()) {
                case "TRANSACTIONS":
                    handleTransactionsWebhook(payload);
                    break;
                case "ITEM":
                    handleItemWebhook(payload);
                    break;
                default:
                    log.info("Unhandled webhook type: {}", payload.getWebhookType());
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private void handleTransactionsWebhook(WebhookPayload payload) {
        if ("DEFAULT_UPDATE".equals(payload.getWebhookCode())) {
            PlaidItem item = plaidItemRepository.findByItemId(payload.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            transactionSyncService.syncTransactionsForItem(item);
        }
    }

    private void handleItemWebhook(WebhookPayload payload) {
        // Handle item status updates
        log.info("Item webhook received: {}", payload.getWebhookCode());
    }
}

@Data
class WebhookPayload {
    private String webhookType;
    private String webhookCode;
    private String itemId;
    private String error;
    private String newTransactions;
}
