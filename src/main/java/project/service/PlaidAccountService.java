package project.service;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.Account;
import project.model.AccountType;
import project.model.PlaidItem;
import project.model.User;
import project.repository.AccountRepository;
import project.repository.PlaidItemRepository;
import project.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaidAccountService {
    private final PlaidApi plaidApi;
    private final AccountRepository accountRepository;
    private final PlaidItemRepository plaidItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<Account> syncAccountsForItem(PlaidItem item) {
        try {
            AccountsGetRequest request = new AccountsGetRequest()
                    .accessToken(item.getAccessToken());

            AccountsGetResponse response = plaidApi.accountsGet(request)
                    .execute()
                    .body();

            if (response == null || response.getAccounts() == null) {
                log.error("Null response from Plaid accounts get for item {}", item.getItemId());
                return new ArrayList<>();
            }

            List<Account> createdAccounts = new ArrayList<>();
            User user = item.getUser();

            for (com.plaid.client.model.AccountBase plaidAccount : response.getAccounts()) {
                // Check if account already exists
                Account existingAccount = accountRepository
                        .findByPlaidAccountId(plaidAccount.getAccountId())
                        .orElse(null);

                if (existingAccount != null) {
                    // Update existing account
                    updateAccount(existingAccount, plaidAccount);
                    createdAccounts.add(existingAccount);
                } else {
                    // Create new account
                    Account newAccount = createAccount(plaidAccount, user);
                    createdAccounts.add(newAccount);
                }
            }

            return createdAccounts;
        } catch (Exception e) {
            log.error("Error syncing accounts for item {}: {}", item.getItemId(), e.getMessage());
            throw new RuntimeException("Account sync failed", e);
        }
    }

    private Account createAccount(AccountBase plaidAccount, User user) {
        Account account = new Account();
        account.setUser(user);
        account.setPlaidAccountId(plaidAccount.getAccountId());
        account.setAccountName(plaidAccount.getName());
        account.setAccountNumber(plaidAccount.getMask());
        account.setBalance(BigDecimal.valueOf(plaidAccount.getBalances().getCurrent()));
        account.setAccountType(mapPlaidAccountType(plaidAccount.getType()));
        account.setBankName(plaidAccount.getOfficialName());
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    private void updateAccount(Account account, AccountBase plaidAccount) {
        account.setBalance(BigDecimal.valueOf(plaidAccount.getBalances().getCurrent()));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    private AccountType mapPlaidAccountType(String plaidType) {
        switch (plaidType.toLowerCase()) {
            case "depository":
            case "checking":
                return AccountType.CHECKING;
            case "savings":
                return AccountType.SAVINGS;
            case "credit":
            case "credit card":
                return AccountType.CREDIT_CARD;
            case "loan":
            case "mortgage":
                return AccountType.LOAN;
            case "investment":
                return AccountType.INVESTMENT;
            default:
                return AccountType.CHECKING;
        }
    }
}