package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.model.PlaidItem;
import project.model.User;
import java.util.List;
import java.util.Optional;

public interface PlaidItemRepository extends JpaRepository<PlaidItem, Long> {
    List<PlaidItem> findByUser(User user);
    Optional<PlaidItem> findByItemId(String itemId);
    Optional<PlaidItem> findByAccessToken(String accessToken);
    List<PlaidItem> findByUserEmail(String email);
}