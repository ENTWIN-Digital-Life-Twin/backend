package com.digitallifetwin.auth.repository;

import com.digitallifetwin.auth.entity.PasswordResetToken;
import com.digitallifetwin.auth.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserAndUsedFalse(User user);
}
