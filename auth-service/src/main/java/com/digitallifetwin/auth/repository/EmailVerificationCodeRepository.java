package com.digitallifetwin.auth.repository;

import com.digitallifetwin.auth.entity.EmailVerificationCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    List<EmailVerificationCode> findByEmailIgnoreCaseAndUsedFalse(String email);

    Optional<EmailVerificationCode> findByEmailIgnoreCaseAndCodeHash(String email, String codeHash);
}
