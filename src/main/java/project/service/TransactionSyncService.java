package project.service;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.Account;
import project.model.PlaidItem;
import project.model.Transaction;
import project.model.TransactionType;
import project.repository.AccountRepository;
import project.repository.PlaidItemRepository;
import project.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSyncService {
    private final PlaidApi plaidApi;
    private final PlaidItemRepository plaidItemRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void syncAllTransactions() {
        List<PlaidItem> plaidItems = plaidItemRepository.findAll();
        for (PlaidItem item : plaidItems) {
            try {
                syncTransactionsForItem(item);
            } catch (Exception e) {
                log.error("Error syncing transactions for item {}: {}", item.getItemId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void syncTransactionsForItem(PlaidItem item) {
        try {
            TransactionsGetRequest request = new TransactionsGetRequest()
                    .accessToken(item.getAccessToken())
                    .startDate(LocalDate.now().minusDays(30))
                    .endDate(LocalDate.now());

            TransactionsGetResponse response = plaidApi.transactionsGet(request)
                    .execute()
                    .body();

            if (response == null) {
                log.error("Null response from Plaid sync for item {}", item.getItemId());
                return;
            }

            List<com.plaid.client.model.Transaction> plaidTransactions = response.getTransactions();
            processTransactions(plaidTransactions, item);

            item.setLastSync(LocalDateTime.now());
            plaidItemRepository.save(item);

        } catch (Exception e) {
            log.error("Error in transaction sync for item {}: {}", item.getItemId(), e.getMessage());
            throw new RuntimeException("Transaction sync failed", e);
        }
    }

    private void processTransactions(List<com.plaid.client.model.Transaction> plaidTransactions, PlaidItem item) {
        for (com.plaid.client.model.Transaction plaidTx : plaidTransactions) {
            try {
                Transaction existingTx = transactionRepository
                        .findByPlaidTransactionId(plaidTx.getTransactionId())
                        .orElse(null);

                if (existingTx != null) {
                    updateTransactionFromPlaid(existingTx, plaidTx);
                } else {
                    Transaction newTx = createTransactionFromPlaid(plaidTx, item);
                    if (newTx != null) {
                        transactionRepository.save(newTx);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing transaction {}: {}", plaidTx.getTransactionId(), e.getMessage());
            }
        }
    }

    private Transaction createTransactionFromPlaid(com.plaid.client.model.Transaction plaidTx, PlaidItem item) {
        Transaction transaction = new Transaction();

        // Find the associated account
        Account account = accountRepository.findByPlaidAccountId(plaidTx.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found for plaid account ID: " + plaidTx.getAccountId()));

        transaction.setAccount(account);
        updateTransactionFieldsFromPlaid(transaction, plaidTx);

        // Set default transaction type based on amount
        double amount = plaidTx.getAmount();
        transaction.setType(amount > 0 ? TransactionType.INCOME : TransactionType.EXPENSE);

        return transaction;
    }

    private void updateTransactionFromPlaid(Transaction transaction, com.plaid.client.model.Transaction plaidTx) {
        updateTransactionFieldsFromPlaid(transaction, plaidTx);
        transactionRepository.save(transaction);
    }

    private void updateTransactionFieldsFromPlaid(Transaction transaction, com.plaid.client.model.Transaction plaidTx) {
        transaction.setPlaidTransactionId(plaidTx.getTransactionId());
        transaction.setAmount(BigDecimal.valueOf(plaidTx.getAmount()));
        transaction.setDescription(plaidTx.getName());
        transaction.setMerchant(plaidTx.getMerchantName());

        // Convert date string to LocalDateTime
        LocalDate date  = plaidTx.getDate();
        if (date != null) {
            transaction.setTransactionDate(date.atStartOfDay());
        } else {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        transaction.setPending(plaidTx.getPending());
    }
}