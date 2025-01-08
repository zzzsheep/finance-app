package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.model.Account;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    List<Account> findByUserEmail(String email);
}
