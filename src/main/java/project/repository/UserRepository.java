// src/main/java/project/financeapp/repository/UserRepository.java
package project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);  // Changed from User to Optional<User>
    boolean existsByEmail(String email);

}