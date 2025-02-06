package project.service;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.PlaidItem;
import project.model.Transaction;
import project.repository.PlaidItemRepository;
import project.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.time.LocalDate;
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSyncService {
    private final PlaidApi plaidApi;
    private final PlaidItemRepository plaidItemRepository;
    private final TransactionRepository transactionRepository;

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

            // Handle added transactions
            processAddedTransactions(response.getAdded(), item);

            // Handle modified transactions
            processModifiedTransactions(response.getModified());

            // Handle removed transactions
            processRemovedTransactions(response.getRemoved());

            // Update cursor
            item.setTransactionsCursor(response.getNextCursor());
            item.setLastSync(LocalDateTime.now());
            plaidItemRepository.save(item);

        } catch (Exception e) {
            log.error("Error in transaction sync for item {}: {}", item.getItemId(), e.getMessage());
            throw new RuntimeException("Transaction sync failed", e);
        }
    }

    private void processAddedTransactions(List<TransactionsSyncAddedTransaction> added, PlaidItem item) {
        for (TransactionsSyncAddedTransaction plaidTransaction : added) {
            Transaction transaction = new Transaction();
            // Map Plaid transaction to your transaction model
            transaction.setTransactionId(plaidTransaction.getTransactionId());
            transaction.setAmount(plaidTransaction.getAmount());
            transaction.setDescription(plaidTransaction.getName());
            transaction.setMerchant(plaidTransaction.getMerchantName());
            transaction.setTransactionDate(LocalDateTime.parse(plaidTransaction.getDate()));
            transaction.setPending(plaidTransaction.getPending());

            transactionRepository.save(transaction);
        }
    }

    private void processModifiedTransactions(List<TransactionsSyncModifiedTransaction> modified) {
        for (TransactionsSyncModifiedTransaction plaidTransaction : modified) {
            Transaction existingTransaction = transactionRepository
                    .findByTransactionId(plaidTransaction.getTransactionId())
                    .orElse(null);

            if (existingTransaction != null) {
                existingTransaction.setAmount(plaidTransaction.getAmount());
                existingTransaction.setDescription(plaidTransaction.getName());
                existingTransaction.setMerchant(plaidTransaction.getMerchantName());
                existingTransaction.setPending(plaidTransaction.getPending());

                transactionRepository.save(existingTransaction);
            }
        }
    }

    private void processRemovedTransactions(List<TransactionsSyncRemovedTransaction> removed) {
        for (TransactionsSyncRemovedTransaction plaidTransaction : removed) {
            transactionRepository
                    .findByTransactionId(plaidTransaction.getTransactionId())
                    .ifPresent(transactionRepository::delete);
        }
    }
}
