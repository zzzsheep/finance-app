package project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.dto.AccountDTO;
import project.model.Account;
import project.model.User;
import project.repository.AccountRepository;
import project.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    //Create a new account for a user
    public Account createAccount(String userEmail, AccountDTO accountDTO){
        // Find the user or throw an error if not found
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()-> new EntityNotFoundException("User not found"));

        //Create a new account
        Account account = new Account();
        account.setUser(user);
        account.setAccountName(account.getAccountName());
        account.setAccountNumber(accountDTO.getAccountNumber());
        account.setAccountType(accountDTO.getAccountType());
        account.setBalance(accountDTO.getBalance());
        account.setBankName(accountDTO.getBankName());
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    // Get all accounts for a user
    public List<Account> getUserAccounts(String userEmail) {
        return accountRepository.findByUserEmail(userEmail);
    }

    // Get specific account details
    public Account getAccount(Long accountId, String userEmail) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        // Security check - ensure account belongs to user
        if (!account.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Not authorized to access this account");
        }

        return account;
    }

    // Update account details
    public Account updateAccount(Long accountId, String userEmail, AccountDTO accountDTO) {
        Account account = getAccount(accountId, userEmail);

        // Update fields
        account.setAccountName(accountDTO.getAccountName());
        account.setAccountType(accountDTO.getAccountType());
        account.setBalance(accountDTO.getBalance());
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    // Delete an account
    public void deleteAccount(Long accountId, String userEmail) {
        Account account = getAccount(accountId, userEmail);
        accountRepository.delete(account);
    }
}
