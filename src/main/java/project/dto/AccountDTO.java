package project.dto;

import lombok.Data;
import project.model.AccountType;
import java.math.BigDecimal;

@Data
public class AccountDTO {
    private Long id;
    private String accountName;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private String bankName;

}
