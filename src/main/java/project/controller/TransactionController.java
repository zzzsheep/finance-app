package project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import project.dto.TransactionDTO;
import project.model.Transaction;
import project.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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

        // Safe way to get user email from authentication
        String userEmail;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else {
            userEmail = principal.toString();
        }

        Transaction transaction = transactionService.createTransaction(userEmail, dto);
        return ResponseEntity.ok(transaction);
    }

    //Add method to get user's transactions
    @GetMapping
    public ResponseEntity<List<Transaction>> getUserTransactions(
            Authentication authentication) {

        String userEmail;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else {
            userEmail = principal.toString();
        }

        List<Transaction> transactions = transactionService.getUserTransactions(userEmail);
        return ResponseEntity.ok(transactions);
    }
    // Add method to get transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(
            Authentication authentication,
            @PathVariable Long id) {

        String userEmail;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else {
            userEmail = principal.toString();
        }

        Transaction transaction = transactionService.getTransactionById(userEmail, id);
        return ResponseEntity.ok(transaction);
    }

    // Add method for pending transactions
    @GetMapping("/pending")
    public ResponseEntity<List<Transaction>> getPendingTransactions(
            Authentication authentication) {

        String userEmail;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else {
            userEmail = principal.toString();
        }

        List<Transaction> pendingTransactions = transactionService.getPendingTransactions(userEmail);
        return ResponseEntity.ok(pendingTransactions);
    }

}