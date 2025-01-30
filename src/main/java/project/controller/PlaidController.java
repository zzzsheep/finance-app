package project.controller;

import com.plaid.client.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.service.PlaidService;
import project.model.PlaidItem;
import java.util.List;

@RestController
@RequestMapping("/api/plaid")
@RequiredArgsConstructor
@Slf4j
public class PlaidController {
    private final PlaidService plaidService;

    @PostMapping("/create-link-token")
    public ResponseEntity<?> createLinkToken(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            LinkTokenCreateResponse response = plaidService.createLinkToken(userEmail);
            return ResponseEntity.ok().body(new LinkTokenResponse(response.getLinkToken()));
        } catch (Exception e) {
            log.error("Error creating link token", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Error creating link token: " + e.getMessage()));
        }
    }

    @PostMapping("/exchange-token")
    public ResponseEntity<?> exchangePublicToken(
            Authentication authentication,
            @RequestBody TokenExchangeRequest request) {
        try {
            String userEmail = authentication.getName();
            String accessToken = plaidService.exchangePublicToken(
                    request.getPublicToken(),
                    userEmail,
                    request.getInstitutionId(),
                    request.getInstitutionName()
            );
            return ResponseEntity.ok()
                    .body(new SuccessResponse("Bank account successfully linked"));
        } catch (Exception e) {
            log.error("Error exchanging public token", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Error linking bank account: " + e.getMessage()));
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<AccountBase> accounts = plaidService.getAccounts(userEmail);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Error getting accounts", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Error retrieving accounts: " + e.getMessage()));
        }
    }

    @GetMapping("/linked-banks")
    public ResponseEntity<?> getLinkedBanks(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<PlaidItem> items = plaidService.getUserPlaidItems(userEmail);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Error getting linked banks", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Error retrieving linked banks: " + e.getMessage()));
        }
    }
}

@lombok.Data
class TokenExchangeRequest {
    private String publicToken;
    private String institutionId;
    private String institutionName;
}

@lombok.Data
class LinkTokenResponse {
    private String linkToken;

    public LinkTokenResponse(String linkToken) {
        this.linkToken = linkToken;
    }
}

@lombok.Data
class SuccessResponse {
    private String message;

    public SuccessResponse(String message) {
        this.message = message;
    }
}

@lombok.Data
class ErrorResponse {
    private String error;

    public ErrorResponse(String error) {
        this.error = error;
    }
}