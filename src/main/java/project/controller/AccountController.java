package project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.dto.AccountDTO;
import project.dto.AccountResponseDTO;
import project.model.Account;
import project.model.AccountType;
import project.service.AccountService;

import java.util.List;
import java.util.stream.Collectors;

@RestController //This class is a REST API controller
@RequestMapping("/api/accounts") // All endpoints in this controller start with /api/accounts

@RequiredArgsConstructor // Lombok, creates constructor for final fields (dependency injection)
public class AccountController {
    // Automatically injected by Spring through constructor
    private final AccountService accountService;

    @PostMapping    //Handles POST requests to /api/accounts
    public ResponseEntity<AccountResponseDTO> createAccount(
            Authentication authentication,
            @RequestBody AccountDTO accountDTO) {
        // Authentication.getName() returns the user's email
        Account account = accountService.createAccount(
                authentication.getName(),
                accountDTO
        );
        return ResponseEntity.ok(AccountResponseDTO.from(account));
    }


    @GetMapping
    public ResponseEntity<List<AccountResponseDTO>> getUserAccounts(Authentication authentication) {
        List<Account> accounts = accountService.getUserAccounts(authentication.getName());
        List<AccountResponseDTO> responseDTOS = accounts.stream()
                .map(AccountResponseDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOS);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponseDTO> getAccount(
            Authentication authentication,
            @PathVariable Long accountId) {
        Account account = accountService.getAccount(accountId, authentication.getName());
        return ResponseEntity.ok(AccountResponseDTO.from(account));
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponseDTO> updateAccount(
            Authentication authentication,
            @PathVariable Long accountId,
            @RequestBody AccountDTO accountDTO) {
        Account account = accountService.updateAccount(
                accountId,
                authentication.getName(),
                accountDTO
        );
        return ResponseEntity.ok(AccountResponseDTO.from(account));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            Authentication authentication,
            @PathVariable Long accountId) {
        accountService.deleteAccount(accountId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}


