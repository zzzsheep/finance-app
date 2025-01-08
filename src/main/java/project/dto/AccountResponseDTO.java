package project.dto;
import lombok.Data;
import project.model.Account;
import project.model.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// project/dto/AccountResponseDTO.java
@Data
public class AccountResponseDTO {
    private Long id;
    private String accountName;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private String bankName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor to convert from Account entity
    public static AccountResponseDTO from(Account account) {
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setId(account.getId());
        dto.setAccountName(account.getAccountName());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountType(account.getAccountType());
        dto.setBalance(account.getBalance());
        dto.setBankName(account.getBankName());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }
}
