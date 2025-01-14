package project.controller;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.dto.TransactionDTO;
import project.model.Transaction;
import project.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.dto.AccountDTO;
import project.dto.AccountResponseDTO;
import project.model.Account;
import project.model.AccountType;
import project.service.AccountService;

// project/controller/TransactionController.java
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            Authentication authentication,
            @RequestBody TransactionDTO dto) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userEmail = userDetails.getUsername(); // This gets the email

        Transaction transaction = transactionService.createTransaction(userEmail, dto);
        return ResponseEntity.ok(transaction);
    }
}