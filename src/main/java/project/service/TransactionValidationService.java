package project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.dto.TransactionDTO;
import project.model.TransactionType;
import project.repository.AccountRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionValidationService {
    private final AccountRepository accountRepository;

    public void validateTransactionDTO(TransactionDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid transaction amount");
        }

        if (dto.getType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        if (dto.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID is required");
        }

        if (dto.getTransactionDate() == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }

        if (dto.getType() == TransactionType.TRANSFER && dto.getTargetAccountId() == null) {
            throw new IllegalArgumentException("Target account is required for transfers");
        }
    }
}

